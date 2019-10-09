package io.bdrc.ldspdi.rest.resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/*******************************************************************************
 * Copyright (c) 2018 Buddhist Digital Resource Center (BDRC)
 *
 * If this file is a derivation of another work the license header will appear below;
 * otherwise, this work is licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.bdrc.formatters.TTLRDFWriter;
import io.bdrc.ldspdi.export.MarcExport;
import io.bdrc.ldspdi.export.TxtEtextExport;
import io.bdrc.ldspdi.ontology.service.core.OntClassModel;
import io.bdrc.ldspdi.ontology.service.core.OntData;
import io.bdrc.ldspdi.ontology.service.core.OntPolicy;
import io.bdrc.ldspdi.ontology.service.core.OntPropModel;
import io.bdrc.ldspdi.rest.features.SpringCacheControl;
import io.bdrc.ldspdi.results.CacheAccessModel;
import io.bdrc.ldspdi.results.ResultsCache;
import io.bdrc.ldspdi.service.OntPolicies;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.Prefixes;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.BudaMediaTypes;
import io.bdrc.ldspdi.utils.DocFileModel;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.restapi.exceptions.ErrorMessage;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

@RestController
@RequestMapping("/")
public class PublicDataResource {

    public final static Logger log = LoggerFactory.getLogger(PublicDataResource.class);

    public static final String RES_PREFIX_SHORT = ServiceConfig.getProperty("endpoints.resource.shortprefix");
    public static final String RES_PREFIX = ServiceConfig.getProperty("endpoints.resource.fullprefix");
    public static final String ADM_PREFIX_SHORT = ServiceConfig.getProperty("endpoints.admindata.shortprefix");
    public static final String GRAPH_PREFIX_SHORT = ServiceConfig.getProperty("endpoints.graph.shortprefix");

    @GetMapping("/")
    public void getHomePage(HttpServletResponse response) throws RestException, IOException {
        log.info("Call to getHomePage()");
        response.sendRedirect("/index");
    }

    @GetMapping(value = "index", produces = MediaType.TEXT_HTML_VALUE)
    @SpringCacheControl()
    public ModelAndView getIndexPage() throws RestException, IOException {
        log.info("Call to getIndexPage()");
        // DocFileModel dfm = new DocFileModel();
        ModelAndView model = new ModelAndView();
        model.addObject("model", DocFileModel.getInstance());
        model.setViewName("index");
        return model;
    }

    @GetMapping(value = "robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<Object> getRobots() {
        log.info("Call getRobots()");
        return ResponseEntity.ok().body(ServiceConfig.getRobots());
    }

    @GetMapping(value = "cache", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getCacheInfo() {
        log.info("Call to getCacheInfo()");
        ModelAndView model = new ModelAndView();
        model.addObject("model", new CacheAccessModel());
        model.setViewName("cache");
        return model;
    }

    @GetMapping(value = "/context.jsonld", produces = "application/ld+json;charset=utf-8")
    public ResponseEntity<Object> getJsonContext() throws RestException {
        log.info("Call to getJsonContext()");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
        return ResponseEntity.ok().header("Last-Modified", dateFormat.format(OntData.getLastUpdated())).eTag(OntData.getEntityTag()).body(OntData.JSONLD_CONTEXT);
    }

    @GetMapping(value = "/admindata/{res:.+}")
    @SpringCacheControl()
    public ResponseEntity<StreamingResponseBody> getAdResourceGraph(@PathVariable String res, @RequestHeader(value = "fusekiUrl", required = false) final String fusekiUrl, @RequestHeader("Accept") String format, HttpServletResponse resp,
            HttpServletRequest request) throws RestException, IOException {
        if (res.contains(".")) {
            String[] parts = res.split("\\.");
            return getAdResourceGraphExt(parts[0], parts[1], fusekiUrl, format, request);
        }
        final String prefixedRes = ADM_PREFIX_SHORT + res;
        // final Variant variant = request.selectVariant(MediaTypeUtils.resVariants);
        final MediaType mediaType = BudaMediaTypes.selectVariant(format, BudaMediaTypes.resVariants);
        log.info("Call to getAdResourceGraph with format: {} variant is {}", format, mediaType);
        if (format == null) {
            String html = Helpers.getMultiChoicesHtml(request.getServletPath(), true);
            HttpHeaders hh = new HttpHeaders();
            hh.setAll(getResourceHeaders(request.getServletPath(), null, "List", null));
            return ResponseEntity.status(300).headers(hh).header("Content-Type", "text/html").header("Content-Location", request.getServletPath() + "choice?path=" + request.getServletPath()).body(Helpers.getStream(html));

        }
        if (mediaType == null) {
            String html = Helpers.getMultiChoicesHtml(request.getServletPath(), true);
            HttpHeaders hh = new HttpHeaders();
            hh.setAll(getResourceHeaders(request.getServletPath(), null, "List", null));
            return ResponseEntity.status(406).headers(hh).header("Content-Type", "text/html").header("Content-Location", request.getServletPath() + "choice?path=" + request.getServletPath()).body(Helpers.getStream(html));
        }
        if (Helpers.equals(mediaType, MediaType.TEXT_HTML)) {
            HashMap<String, String> map = getResourceHeaders(request.getServletPath(), null, "Choice", null);
            Set<Entry<String, String>> set = map.entrySet();
            for (Entry<String, String> e : set) {
                resp.setHeader(e.getKey(), e.getValue());
            }
            resp.sendRedirect(ServiceConfig.getProperty("showUrl") + prefixedRes);

        }
        Model model = QueryProcessor.getDescribeModel(prefixedRes, fusekiUrl, null);
        if (model.size() == 0) {
            LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes);
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Helpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
        }
        String ext = BudaMediaTypes.getExtFromMime(mediaType);
        HttpHeaders hh = new HttpHeaders();
        hh.setAll(getResourceHeaders(request.getServletPath(), ext, "Choice", getEtag(model, res)));
        return ResponseEntity.ok().contentType(mediaType).headers(hh).body(Helpers.getModelStream(model, ext, RES_PREFIX + res, null));
    }

    // admindata/res with extension
    public ResponseEntity<StreamingResponseBody> getAdResourceGraphExt(@PathVariable String res, @PathVariable String ext, @RequestHeader("fusekiUrl") final String fusekiUrl, @RequestHeader("Accept") String format, HttpServletRequest request)
            throws RestException {

        final String prefixedRes = ADM_PREFIX_SHORT + res;
        final String graphType = "describe";
        final MediaType media = BudaMediaTypes.getMimeFromExtension(ext);
        if (media == null) {
            String html = Helpers.getMultiChoicesHtml(request.getServletPath(), true);
            HttpHeaders hh = new HttpHeaders();
            hh.setAll(getResourceHeaders(request.getServletPath(), null, "List", null));
            return ResponseEntity.status(300).headers(hh).header("Content-Type", "text/html").header("Content-Location", request.getServletPath() + "choice?path=" + request.getServletPath()).body(Helpers.getStream(html));
        }
        if (Helpers.equals(media, MediaType.TEXT_HTML)) {
            throw new RestException(406, new LdsError(LdsError.GENERIC_ERR).setContext(prefixedRes));
        }
        final Model model = QueryProcessor.getCoreResourceGraph(prefixedRes, fusekiUrl, null, graphType);
        if (model.size() == 0) {
            LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes);
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Helpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));

        }
        HttpHeaders hh = new HttpHeaders();
        hh.setAll(getResourceHeaders(request.getServletPath(), ext, null, getEtag(model, res)));
        return ResponseEntity.ok().contentType(media).headers(hh).body(Helpers.getModelStream(model, ext, prefixedRes, null));

    }

    // graph/res with extension
    public ResponseEntity<StreamingResponseBody> getGrResourceGraphExt(String res, String ext, String fusekiUrl, String format, HttpServletRequest request) throws RestException {
        final String prefixedRes = GRAPH_PREFIX_SHORT + res;
        final String graphType = "graph";
        final MediaType media = BudaMediaTypes.getMimeFromExtension(ext);
        if (media == null) {
            final String html = Helpers.getMultiChoicesHtml("/resource/" + res, true);
            return ResponseEntity.status(300).header("Content-Type", "text/html").header("Content-Location", request.getRequestURI() + "choice?path=" + request.getServletPath()).body(Helpers.getStream(html));

        }
        if (Helpers.equals(media, MediaType.TEXT_HTML)) {
            throw new RestException(406, new LdsError(LdsError.GENERIC_ERR).setContext(prefixedRes));
        }
        final Model model = QueryProcessor.getCoreResourceGraph(prefixedRes, fusekiUrl, null, graphType);
        if (model.size() == 0) {
            LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes);
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Helpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
        }
        HttpHeaders hh = new HttpHeaders();
        hh.setAll(getResourceHeaders(request.getServletPath(), ext, null, getEtag(model, res)));
        return ResponseEntity.ok().headers(hh).contentType(media).body(Helpers.getModelStream(model, ext, prefixedRes, null));

    }

    @GetMapping(value = "/graph/{res:.+}")
    @SpringCacheControl()
    public ResponseEntity<StreamingResponseBody> getGrResourceGraph(@PathVariable String res, @RequestHeader(value = "fusekiUrl", required = false) String fusekiUrl, @RequestHeader("Accept") String format, HttpServletResponse resp,
            HttpServletRequest request) throws RestException, IOException {
        if (res.contains(".")) {
            String[] parts = res.split("\\.");
            return getGrResourceGraphExt(parts[0], parts[1], fusekiUrl, format, request);
        }
        final String prefixedRes = GRAPH_PREFIX_SHORT + res;
        final String graphType = "graph";
        final MediaType mediaType = BudaMediaTypes.selectVariant(format, BudaMediaTypes.resVariants);
        if (format == null) {
            String html = Helpers.getMultiChoicesHtml(request.getServletPath(), true);
            HttpHeaders hh = new HttpHeaders();
            hh.setAll(getResourceHeaders(request.getServletPath(), null, "List", null));
            return ResponseEntity.status(300).headers(hh).header("Content-Type", "text/html").header("Content-Location", request.getRequestURI() + "choice?path=" + request.getServletPath()).body(Helpers.getStream(html));

        }
        if (mediaType == null) {
            String html = Helpers.getMultiChoicesHtml(request.getServletPath(), true);
            HttpHeaders hh = new HttpHeaders();
            hh.setAll(getResourceHeaders(request.getServletPath(), null, "List", null));
            return ResponseEntity.status(406).headers(hh).header("Content-Type", "text/html").header("Content-Location", request.getRequestURI() + "choice?path=" + request.getServletPath()).body(Helpers.getStream(html));
        }
        if (Helpers.equals(mediaType, MediaType.TEXT_HTML)) {
            HashMap<String, String> map = getResourceHeaders(request.getServletPath(), null, "Choice", null);
            Set<Entry<String, String>> set = map.entrySet();
            for (Entry<String, String> e : set) {
                resp.setHeader(e.getKey(), e.getValue());
            }
            resp.sendRedirect(request.getRequestURL().toString() + ".trig");
        }
        Model model = QueryProcessor.getCoreResourceGraph(prefixedRes, fusekiUrl, null, graphType);
        if (model.size() == 0) {
            LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes);
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Helpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
        }
        String ext = BudaMediaTypes.getExtFromMime(mediaType);
        HttpHeaders hh = new HttpHeaders();
        hh.setAll(getResourceHeaders(request.getServletPath(), ext, "Choice", getEtag(model, res)));
        return ResponseEntity.ok().headers(hh).contentType(mediaType).body(Helpers.getModelStream(model, ext, RES_PREFIX + res, null));
    }

    @GetMapping(value = "/prefixes")
    @SpringCacheControl()
    public ResponseEntity<StreamingResponseBody> getPrefixes() throws RestException {
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefixes(Prefixes.getMap());
        StreamingResponseBody stream = new StreamingResponseBody() {
            @Override
            public void writeTo(OutputStream os) throws IOException {
                model.write(os, "TURTLE");
            }
        };
        return ResponseEntity.ok().contentType(BudaMediaTypes.getMimeFromExtension("ttl")).body(stream);
    }

    @GetMapping(value = "/resource/{res:.+}")
    @SpringCacheControl()
    public ResponseEntity<StreamingResponseBody> getResourceGraph(@PathVariable final String res, @RequestHeader(value = "fusekiUrl", required = false) final String fusekiUrl, @RequestHeader(value = "Accept", required = false) String format,
            HttpServletResponse response, HttpServletRequest request, @RequestParam(value = "startChar", defaultValue = "0") String startChar, @RequestParam(value = "endChar", defaultValue = "999999999") String endChar)
            throws RestException, IOException {
        MediaType mediaType = BudaMediaTypes.selectVariant(format, BudaMediaTypes.resVariants);
        if (res.contains(".")) {
            String[] parts = res.split("\\.");
            return getFormattedResourceGraph(parts[0], parts[1], startChar, endChar, fusekiUrl, response, request);
        }
        final String prefixedRes = RES_PREFIX_SHORT + res;
        log.info("Call to getResourceGraphGET() with URL: {}, accept: {}, res {}", request.getServletPath(), format, res);
        if (format == null) {
            final String html = Helpers.getMultiChoicesHtml(request.getServletPath(), true);
            HttpHeaders hh = new HttpHeaders();
            hh.setAll(getResourceHeaders(request.getServletPath(), null, "List", null));
            return ResponseEntity.status(300).headers(hh).header("Content-Type", "text/html").header("Content-Location", request.getRequestURI() + "choice?path=" + request.getServletPath()).body(Helpers.getStream(html));
        }
        if (mediaType == null) {
            final String html = Helpers.getMultiChoicesHtml(request.getServletPath(), true);
            HttpHeaders hh = new HttpHeaders();
            hh.setAll(getResourceHeaders(request.getServletPath(), null, "List", null));
            return ResponseEntity.status(406).headers(hh).header("Content-Type", "text/html").header("Content-Location", request.getRequestURI() + "choice?path=" + request.getServletPath()).body(Helpers.getStream(html));
        }
        if (Helpers.equals(mediaType, MediaType.TEXT_HTML)) {
            String type = getDilaResourceType(res);
            if (!type.equals("")) {
                type = type + "/?fromInner=";
            } else {
                type = RES_PREFIX_SHORT;
            }
            HashMap<String, String> map = getResourceHeaders(request.getServletPath(), null, "Choice", null);
            Set<Entry<String, String>> set = map.entrySet();
            for (Entry<String, String> e : set) {
                response.setHeader(e.getKey(), e.getValue());
            }
            response.sendRedirect(ServiceConfig.getProperty("showUrl") + type + res);
        }
        final Model model = QueryProcessor.getCoreResourceGraph(prefixedRes, fusekiUrl, null, computeGraphType(request));
        if (model.size() == 0) {
            LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes);
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Helpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
        }
        final String ext = BudaMediaTypes.getExtFromMime(mediaType);
        HttpHeaders hh = new HttpHeaders();
        hh.setAll(getResourceHeaders(request.getServletPath(), ext, "Choice", getEtag(model, res)));
        return ResponseEntity.ok().contentType(mediaType).body(Helpers.getModelStream(model, ext, RES_PREFIX + res, null));
    }

    public ResponseEntity<StreamingResponseBody> getFormattedResourceGraph(@PathVariable("res") String res, @PathVariable("ext") String ext, @RequestParam(value = "startChar", defaultValue = "0") String startChar,
            @RequestParam(value = "endChar", defaultValue = "999999999") String endChar, @RequestHeader(value = "fusekiUrl", required = false) String fusekiUrl, HttpServletResponse response, HttpServletRequest request)
            throws RestException, IOException {
        log.info("Call to getFormattedResourceGraph() res {}, ext {}", res, ext);
        final String prefixedRes = RES_PREFIX_SHORT + res;
        final MediaType media = BudaMediaTypes.getMimeFromExtension(ext);
        log.info("Call to getFormattedResourceGraph() path is {}", request.getServletPath());
        if (media == null) {
            final String html = Helpers.getMultiChoicesHtml("/resource/" + res, true);
            return ResponseEntity.status(300).header("Content-Type", "text/html").header("Content-Location", request.getRequestURI() + "choice?path=" + request.getServletPath()).body(Helpers.getStream(html));
        }
        if (Helpers.equals(media, MediaType.TEXT_HTML)) {
            String type = getDilaResourceType(res);
            if (!type.equals("")) {
                type = type + "/?fromInner=";
            } else {
                type = RES_PREFIX_SHORT;
            }
            HashMap<String, String> map = getResourceHeaders(request.getServletPath(), null, null, null);
            Set<Entry<String, String>> set = map.entrySet();
            for (Entry<String, String> e : set) {
                response.setHeader(e.getKey(), e.getValue());
            }
            response.sendRedirect(ServiceConfig.getProperty("showUrl") + type + res);
        }
        if (ext.startsWith("mrc")) {
            return MarcExport.getResponse(media, RES_PREFIX + res);
        }
        if (ext.equals("txt")) {
            return TxtEtextExport.getResponse(RES_PREFIX + res, Integer.parseInt(startChar), Integer.parseInt(endChar));
        }
        final Model model = QueryProcessor.getCoreResourceGraph(prefixedRes, fusekiUrl, null, computeGraphType(request));
        if (model.size() == 0) {
            LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes);
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Helpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
        }
        HttpHeaders hh = new HttpHeaders();
        hh.setAll(getResourceHeaders(request.getServletPath(), ext, null, getEtag(model, res)));
        return ResponseEntity.ok().headers(hh).contentType(media).body(Helpers.getModelStream(model, ext, RES_PREFIX + res, null));

    }

    @GetMapping(value = "/{base:[a-z]+}/**")
    @SpringCacheControl()
    public Object getExtOntologyHomePage(HttpServletRequest request, @RequestHeader("Accept") String format, @PathVariable String base) throws RestException, IOException {
        String path = request.getRequestURI();
        log.info("getExtOntologyHomePage WAS CALLED WITH >> pathUri : {}/ servletPath{} ", path, request.getServletPath());
        String other = request.getServletPath().substring(base.length() + 2);
        if (other.contains(".")) {
            String[] parts = other.split("\\.");
            log.info("getExtOntologyHomePage With EXT >> base : {}/ other:{} and ext: {}", base, parts[0], parts[1]);
            return getOntologyResourceAsFile(request, base, parts[0], parts[1]);
        }
        log.info("getExtOntologyHomePage WAS CALLED WITH >> base : {}/ other:{} and format: {}", base, other, format);
        boolean isBase = false;
        String baseUri = "";
        String tmp = request.getRequestURL().toString().replace("https", "http");
        // String tmp = "http://purl.bdrc.io/ontology/admin";
        log.info("getExtOntologyHomePage tmp is >> {}", tmp);
        if (OntPolicies.isBaseUri(tmp)) {
            baseUri = parseBaseUri(tmp);
            isBase = true;
        }
        log.info("getExtOntologyHomePage absolute path >> {} and other = {}", request.getRequestURL().toString(), other);
        if (OntPolicies.isBaseUri(parseBaseUri(tmp + other))) {
            baseUri = parseBaseUri(tmp + other);
            isBase = true;
        }
        log.info("getExtOntologyHomePage baseUri is >> {}", baseUri);
        // Is the full request uri a baseuri?
        if (isBase) {
            OntPolicy pr = OntPolicies.getOntologyByBase(baseUri);
            // if accept header is present
            if (format != null) {
                MediaType mediaType = BudaMediaTypes.selectVariant(format, BudaMediaTypes.resVariants);
                if (mediaType == null) {
                    return (ResponseEntity<String>) ResponseEntity.status(406).body("No acceptable Accept header");
                }
                String url = OntPolicies.getOntologyByBase(baseUri).getFileUri();
                // using cache if available
                byte[] byteArr = (byte[]) ResultsCache.getObjectFromCache(url.hashCode());
                if (byteArr == null) {
                    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                    InputStream input = connection.getInputStream();
                    byteArr = IOUtils.toByteArray(input);
                    input.close();
                    ResultsCache.addToCache(byteArr, url.hashCode());
                }
                OntModelSpec oms = new OntModelSpec(OntModelSpec.OWL_MEM);
                OntDocumentManager odm = new OntDocumentManager();
                odm.setProcessImports(false);
                oms.setDocumentManager(odm);
                OntModel om = OntData.getOntModelByBase(baseUri);
                OntData.setOntModel(om);
                om.read(new ByteArrayInputStream(byteArr), baseUri, "TURTLE");
                // browser request : serving html page
                if (Helpers.equals(mediaType, MediaType.TEXT_HTML)) {
                    ModelAndView model = new ModelAndView();
                    model.addObject("path", path);
                    model.setViewName("ontologyHome");
                    return (ModelAndView) model;
                } else {
                    final String JenaLangStr = BudaMediaTypes.getJenaFromExtension(BudaMediaTypes.getExtFromMime(mediaType));
                    final StreamingResponseBody stream = new StreamingResponseBody() {
                        @Override
                        public void writeTo(OutputStream os) throws IOException {
                            if (JenaLangStr == "STTL") {
                                final RDFWriter writer = TTLRDFWriter.getSTTLRDFWriter(om, pr.getBaseUri());
                                writer.output(os);
                            } else {
                                org.apache.jena.rdf.model.RDFWriter wr = om.getWriter(JenaLangStr);
                                if (JenaLangStr.equals(RDFLanguages.strLangRDFXML)) {
                                    wr.setProperty("xmlbase", pr.getBaseUri());
                                }
                                wr.write(om, os, pr.getBaseUri());
                            }
                        }
                    };
                    return (ResponseEntity<StreamingResponseBody>) ResponseEntity.ok().contentType(mediaType).body(stream);
                }
            }
        } else {
            if (OntData.ontAllMod.getOntResource(tmp) == null) {
                LdsError lds = new LdsError(LdsError.ONT_URI_ERR).setContext("Ont resource is null for " + tmp);
                return (ResponseEntity<StreamingResponseBody>) ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Helpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
            }

            if (OntData.isClass(tmp, true)) {
                log.info("CLASS>>" + tmp);
                ModelAndView model = new ModelAndView();
                model.addObject("model", new OntClassModel(tmp, true));
                model.setViewName("ontClassView");
                return (ModelAndView) model;

            } else {
                log.info("PROP>>" + tmp);
                ModelAndView model = new ModelAndView();
                model.addObject("model", new OntPropModel(tmp, true));
                model.setViewName("ontPropView");
                return (ModelAndView) model;
            }
        }
        return (ResponseEntity<String>) ResponseEntity.status(404).body("Not found");
    }

    public Object getOntologyResourceAsFile(HttpServletRequest request, String base, String other, String ext) throws RestException {
        String res = request.getRequestURL().toString().replace("https", "http");
        res = res.substring(0, res.lastIndexOf('.')) + "/";
        log.info("In getOntologyResourceAsFile(), RES = {} and ext= {}", res, ext);
        final String JenaLangStr = BudaMediaTypes.getJenaFromExtension(ext);
        if (JenaLangStr == null) {
            LdsError lds = new LdsError(LdsError.URI_SYNTAX_ERR).setContext(request.getRequestURL().toString());
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Helpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
        }
        if (OntPolicies.isBaseUri(res)) {
            OntPolicy params = OntPolicies.getOntologyByBase(parseBaseUri(res));
            OntModel model = OntData.getOntModelByBase(params.getBaseUri());
            String t = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (JenaLangStr == "STTL") {
                final RDFWriter writer = TTLRDFWriter.getSTTLRDFWriter(model, params.getBaseUri());
                writer.output(baos);
            }
            if (JenaLangStr == RDFLanguages.strLangTurtle) {
                model.write(baos, "TURTLE");
            } else {
                org.apache.jena.rdf.model.RDFWriter wr = model.getWriter(JenaLangStr);
                if (JenaLangStr.equals(RDFLanguages.strLangRDFXML)) {
                    wr.setProperty("xmlbase", params.getBaseUri());
                }
                wr.write(model, baos, params.getBaseUri());
                t = baos.toString();
            }
            return ResponseEntity.ok().contentType(BudaMediaTypes.getMimeFromExtension(ext)).body(t);
        } else {
            LdsError lds = new LdsError(LdsError.ONT_URI_ERR).setContext(request.getRequestURL().toString());
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Helpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
        }
    }

    @GetMapping(value = "/ontology/data/{ext}", produces = MediaType.TEXT_HTML_VALUE)
    @SpringCacheControl()
    public ResponseEntity<StreamingResponseBody> getAllOntologyData(HttpServletRequest request, @PathVariable("ext") String ext) throws RestException {
        log.info("Call to getAllOntologyData(); with ext {}", ext);
        final String JenaLangStr = BudaMediaTypes.getJenaFromExtension(ext);
        if (JenaLangStr == null) {
            LdsError lds = new LdsError(LdsError.URI_SYNTAX_ERR).setContext(request.getRequestURL().toString());
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Helpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
        }
        OntModel model = OntData.ontAllMod;
        final StreamingResponseBody stream = new StreamingResponseBody() {
            @Override
            public void writeTo(OutputStream os) throws IOException {
                if (JenaLangStr == "STTL") {
                    model.write(os, "TURTLE");
                } else {
                    model.write(os, JenaLangStr);
                }
            }
        };
        return ResponseEntity.ok().contentType(BudaMediaTypes.getMimeFromExtension(ext)).body(stream);
    }

    @PostMapping(value = "/callbacks/github/owl-schema", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateOntology() throws RestException {
        log.info("updating Ontology models() >>");
        Thread t = new Thread(new OntData());
        t.start();
        return ResponseEntity.ok().body("Ontologies are being updated");
    }

    private static HashMap<String, String> getResourceHeaders(String url, String ext, String tcn, String eTag) {
        HashMap<String, MediaType> map = BudaMediaTypes.getResExtensionMimeMap();
        HashMap<String, String> headers = new HashMap<>();
        if (ext != null) {
            if (url.indexOf(".") < 0) {
                headers.put("Content-Location", url + "." + ext);
            } else {
                url = url.substring(0, url.lastIndexOf("."));
            }
        }
        StringBuilder sb = new StringBuilder("");
        for (Entry<String, MediaType> e : map.entrySet()) {
            sb.append("{\"" + url + "." + e.getKey() + "\" 1.000 {type " + e.getValue().toString() + "}},");
        }
        headers.put("Alternates", sb.toString().substring(0, sb.toString().length() - 1));
        if (tcn != null)
            headers.put("TCN", tcn);
        headers.put("Vary", "Negotiate, Accept");
        if (eTag != null) {
            headers.put("ETag", eTag);
        }
        return headers;
    }

    /*
     * private static ResponseBuilder setHeaders(ResponseBuilder builder,
     * HashMap<String, String> headers) { for (String key : headers.keySet()) {
     * builder.header(key, headers.get(key)); } return builder; }
     */

    private static String getEtag(Model model, String res) {
        Statement smt = model.getProperty(ResourceFactory.createResource(RES_PREFIX + res), ResourceFactory.createProperty("http://purl.bdrc.io/ontology/admin/gitRevision"));
        if (smt != null) {
            return smt.getObject().toString();
        }
        return null;
    }

    private String parseBaseUri(String s) {
        if (s.endsWith("/") || s.endsWith("#")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private String computeGraphType(HttpServletRequest req) {

        String type = "";
        if (req.getParameter("graph") != null) {
            type = "graph";
        }
        if (req.getParameter("describe") != null) {
            type = "describe";
        }
        return type;
    }

    private String getDilaResourceType(String res) {
        String type = "";
        boolean buda = Boolean.parseBoolean(ServiceConfig.getProperty("isBUDA"));
        if (buda) {
            return "";
        }
        if (res.startsWith("A")) {
            return "person";
        }
        if (res.startsWith("P")) {
            return "place";
        }
        return type;
    }

}