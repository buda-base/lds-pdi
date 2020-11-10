package io.bdrc.ldspdi.rest.controllers;

import java.io.BufferedReader;
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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.bdrc.ldspdi.exceptions.ErrorMessage;
import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.export.MarcExport;
import io.bdrc.ldspdi.export.TxtEtextExport;
import io.bdrc.ldspdi.ontology.service.core.OntClassModel;
import io.bdrc.ldspdi.ontology.service.core.OntData;
import io.bdrc.ldspdi.ontology.service.core.OntPolicy;
import io.bdrc.ldspdi.ontology.service.core.OntPropModel;
import io.bdrc.ldspdi.results.CacheAccessModel;
import io.bdrc.ldspdi.service.GitService;
import io.bdrc.ldspdi.service.OntPolicies;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.service.Webhook;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.DocFileModel;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.libraries.BudaMediaTypes;
import io.bdrc.libraries.StreamingHelpers;
import io.bdrc.libraries.formatters.TTLRDFWriter;

@RestController
@RequestMapping("/")
public class PublicDataController {

    public final static Logger log = LoggerFactory.getLogger("default");

    public static final String RES_PREFIX_SHORT = ServiceConfig.getProperty("endpoints.resource.shortprefix");
    public static final String RES_PREFIX = ServiceConfig.getProperty("endpoints.resource.fullprefix");
    public static final String ADM_PREFIX_SHORT = ServiceConfig.getProperty("endpoints.admindata.shortprefix");
    public static final String GRAPH_PREFIX_SHORT = ServiceConfig.getProperty("endpoints.graph.shortprefix");
    public static final String GRAPH_PREFIX_FULL = ServiceConfig.getProperty("endpoints.graph.fullprefix");
    private List<String> COLUMBIA_IDS = null;

    public void ensureColumbiaIDsLoaded() {
        if (COLUMBIA_IDS == null) {
            log.info("loading columbia-id.csv");
            InputStream resource = PublicDataController.class.getClassLoader().getResourceAsStream("columbia-id.csv");
            COLUMBIA_IDS = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8)).lines()
                    .collect(Collectors.toList());
            log.info("loaded {} Columbia IDs", COLUMBIA_IDS.size());
        }
    }

    @GetMapping("/")
    public void getHomePage(HttpServletResponse response) throws RestException, IOException {
        log.info("Call to getHomePage()");
        Helpers.setCacheControl(response, "public");
        response.sendRedirect("/index");
    }

    @GetMapping(value = "index", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getIndexPage(HttpServletRequest request, HttpServletResponse response)
            throws RestException, IOException {
        log.info("Call to getIndexPage()");
        ModelAndView model = new ModelAndView();
        model.addObject("model", DocFileModel.getInstance());
        model.setViewName("index");
        Helpers.setCacheControl(response, "public");
        return model;
    }

    @GetMapping(value = "robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<Object> getRobots(HttpServletResponse response) {
        log.info("Call getRobots()");
        Helpers.setCacheControl(response, "public");
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
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(86400, TimeUnit.SECONDS).cachePublic())
                .header("Last-Modified", dateFormat.format(OntData.getLastUpdated())).eTag(OntData.getOntCommitId())
                .body(OntData.JSONLD_CONTEXT);
    }

    @GetMapping(value = "/admindata/{res:.+}")
    public ResponseEntity<StreamingResponseBody> getAdResourceGraph(@PathVariable String res,
            @RequestHeader(value = "fusekiUrl", required = false) final String fusekiUrl,
            @RequestHeader("Accept") String format, HttpServletResponse resp, HttpServletRequest request)
            throws RestException, IOException {
        Helpers.setCacheControl(resp, "public");
        if (res.contains(".")) {
            String[] parts = res.split("\\.");
            return getAdResourceGraphExt(parts[0], parts[1], fusekiUrl, format, request);
        }
        final String prefixedRes = ADM_PREFIX_SHORT + res;
        final MediaType mediaType = BudaMediaTypes.selectVariant(format, BudaMediaTypes.resVariants);
        log.info("Call to getAdResourceGraph with format: {} variant is {}", format, mediaType);
        if (format == null) {
            String html = Helpers.getMultiChoicesHtml(request.getServletPath(), true);
            HttpHeaders hh = new HttpHeaders();
            hh.setAll(getResourceHeaders(request.getServletPath(), null, "List", null));
            return ResponseEntity.status(300).headers(hh).header("Content-Type", "text/html")
                    .header("Content-Location", request.getServletPath() + "choice?path=" + request.getServletPath())
                    .body(StreamingHelpers.getStream(html));

        }
        if (mediaType == null) {
            String html = Helpers.getMultiChoicesHtml(request.getServletPath(), true);
            HttpHeaders hh = new HttpHeaders();
            hh.setAll(getResourceHeaders(request.getServletPath(), null, "List", null));
            return ResponseEntity.status(406).headers(hh).header("Content-Type", "text/html")
                    .header("Content-Location", request.getServletPath() + "choice?path=" + request.getServletPath())
                    .body(StreamingHelpers.getStream(html));
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
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON)
                    .body(StreamingHelpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
        }
        String ext = BudaMediaTypes.getExtFromMime(mediaType);
        HttpHeaders hh = new HttpHeaders();
        hh.setAll(getResourceHeaders(request.getServletPath(), ext, "Choice", getEtag(model, res)));
        return ResponseEntity.ok().contentType(mediaType).headers(hh).body(StreamingHelpers.getModelStream(model, ext,
                RES_PREFIX + res, null, ServiceConfig.PREFIX.getPrefixMap()));
    }

    // admindata/res with extension
    public ResponseEntity<StreamingResponseBody> getAdResourceGraphExt(@PathVariable String res,
            @PathVariable String ext, @RequestHeader("fusekiUrl") final String fusekiUrl,
            @RequestHeader("Accept") String format, HttpServletRequest request) throws RestException {
        final String prefixedRes = ADM_PREFIX_SHORT + res;
        final String fullResURI = GRAPH_PREFIX_FULL + res;
        final String graphType = "describe";
        final MediaType media = BudaMediaTypes.getMimeFromExtension(ext);
        if (media == null) {
            String html = Helpers.getMultiChoicesHtml(request.getServletPath(), true);
            HttpHeaders hh = new HttpHeaders();
            hh.setAll(getResourceHeaders(request.getServletPath(), null, "List", null));
            return ResponseEntity.status(300).headers(hh).header("Content-Type", "text/html")
                    .header("Content-Location", request.getServletPath() + "choice?path=" + request.getServletPath())
                    .body(StreamingHelpers.getStream(html));
        }
        if (Helpers.equals(media, MediaType.TEXT_HTML)) {
            throw new RestException(406, new LdsError(LdsError.GENERIC_ERR).setContext(prefixedRes));
        }
        final Model model = QueryProcessor.getCoreResourceGraph(prefixedRes, fusekiUrl, null, graphType);

        if (model.size() == 0) {
            LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes);
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON)
                    .body(StreamingHelpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));

        }
        HttpHeaders hh = new HttpHeaders();
        hh.setAll(getResourceHeaders(request.getServletPath(), ext, null, getEtag(model, res)));
        return ResponseEntity.ok().contentType(media).headers(hh).body(
                StreamingHelpers.getModelStream(model, ext, fullResURI, null, ServiceConfig.PREFIX.getPrefixMap()));

    }

    // graph/res with extension
    public ResponseEntity<StreamingResponseBody> getGrResourceGraphExt(String res, String ext, String fusekiUrl,
            String format, HttpServletRequest request) throws RestException {
        final String prefixedRes = GRAPH_PREFIX_SHORT + res;
        final String fullResURI = GRAPH_PREFIX_FULL + res;
        final String graphType = "graph";
        final MediaType media = BudaMediaTypes.getMimeFromExtension(ext);
        if (media == null) {
            final String html = Helpers.getMultiChoicesHtml("/resource/" + res, true);
            return ResponseEntity.status(300).header("Content-Type", "text/html")
                    .header("Content-Location", request.getRequestURI() + "choice?path=" + request.getServletPath())
                    .body(StreamingHelpers.getStream(html));

        }
        if (Helpers.equals(media, MediaType.TEXT_HTML)) {
            throw new RestException(406, new LdsError(LdsError.GENERIC_ERR).setContext(prefixedRes));
        }
        final Model model = QueryProcessor.getCoreResourceGraph(prefixedRes, fusekiUrl, null, graphType);
        if (model.size() == 0) {
            LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes);
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON)
                    .body(StreamingHelpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
        }
        HttpHeaders hh = new HttpHeaders();
        hh.setAll(getResourceHeaders(request.getServletPath(), ext, null, getEtag(model, res)));
        return ResponseEntity.ok().headers(hh).contentType(media).body(
                StreamingHelpers.getModelStream(model, ext, fullResURI, null, ServiceConfig.PREFIX.getPrefixMap()));

    }

    @GetMapping(value = "/graph/{res:.+}")
    public ResponseEntity<StreamingResponseBody> getGrResourceGraph(@PathVariable String res,
            @RequestHeader(value = "fusekiUrl", required = false) String fusekiUrl,
            @RequestHeader("Accept") String format, HttpServletResponse resp, HttpServletRequest request)
            throws RestException, IOException {
        Helpers.setCacheControl(resp, "public");
        if (res.contains(".")) {
            // String[] parts = res.split("\\.");
            return getGrResourceGraphExt(res.substring(0, res.lastIndexOf(".")),
                    res.substring(res.lastIndexOf(".") + 1), fusekiUrl, format, request);
        }
        final String prefixedRes = GRAPH_PREFIX_SHORT + res;
        final String fullResURI = GRAPH_PREFIX_FULL + res;
        final String graphType = "graph";
        final MediaType mediaType = BudaMediaTypes.selectVariant(format, BudaMediaTypes.resVariants);
        if (format == null) {
            String html = Helpers.getMultiChoicesHtml(request.getServletPath(), true);
            HttpHeaders hh = new HttpHeaders();
            hh.setAll(getResourceHeaders(request.getServletPath(), null, "List", null));
            return ResponseEntity.status(300).headers(hh).header("Content-Type", "text/html")
                    .header("Content-Location", request.getRequestURI() + "choice?path=" + request.getServletPath())
                    .body(StreamingHelpers.getStream(html));

        }
        if (mediaType == null) {
            String html = Helpers.getMultiChoicesHtml(request.getServletPath(), true);
            HttpHeaders hh = new HttpHeaders();
            hh.setAll(getResourceHeaders(request.getServletPath(), null, "List", null));
            return ResponseEntity.status(406).headers(hh).header("Content-Type", "text/html")
                    .header("Content-Location", request.getRequestURI() + "choice?path=" + request.getServletPath())
                    .body(StreamingHelpers.getStream(html));
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
        log.info("Call to getResourceGraph with prefixedRes {}, fuseki {}, graphType {}", prefixedRes, fusekiUrl,
                graphType);
        if (model.size() == 0) {
            LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes);
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON)
                    .body(StreamingHelpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
        }
        String ext = BudaMediaTypes.getExtFromMime(mediaType);
        HttpHeaders hh = new HttpHeaders();
        hh.setAll(getResourceHeaders(request.getServletPath(), ext, "Choice", getEtag(model, res)));
        return ResponseEntity.ok().headers(hh).contentType(mediaType).body(
                StreamingHelpers.getModelStream(model, ext, fullResURI, null, ServiceConfig.PREFIX.getPrefixMap()));
    }

    @GetMapping(value = "/prefixes")
    public ResponseEntity<StreamingResponseBody> getPrefixes(HttpServletResponse resp) throws RestException {
        Helpers.setCacheControl(resp, "public");
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefixes(ServiceConfig.PREFIX.getMap());
        StreamingResponseBody stream = StreamingHelpers.getModelStream(model, "ttl",
                ServiceConfig.PREFIX.getPrefixMap());
        return ResponseEntity.ok().contentType(BudaMediaTypes.getMimeFromExtension("ttl")).body(stream);
    }

    @GetMapping(value = "/resource/{res:.+}")
    public ResponseEntity<StreamingResponseBody> getResourceGraph(@PathVariable String res,
            @RequestHeader(value = "fusekiUrl", required = false) final String fusekiUrl,
            @RequestHeader(value = "Accept", required = false) String format, HttpServletResponse response,
            HttpServletRequest request, @RequestParam(value = "startChar", defaultValue = "0") String startChar,
            @RequestParam(value = "endChar", defaultValue = "999999999") String endChar)
            throws RestException, IOException {
        Helpers.setCacheControl(response, "public");
        MediaType mediaType = BudaMediaTypes.selectVariant(format, BudaMediaTypes.resVariants);
        if (res.contains(".")) {
            String[] parts = res.split("\\.");
            return getFormattedResourceGraph(parts[0], parts[1], startChar, endChar, fusekiUrl, response, request);
        }
        String prefixedRes = RES_PREFIX_SHORT + res;
        String fullResURI = GRAPH_PREFIX_FULL + res;
        log.info("Call to getResourceGraphGET() with URL: {}, accept: {}, variant: {}, res {}",
                request.getServletPath(), format, mediaType, res);
        if (format == null) {
            final String html = Helpers.getMultiChoicesHtml(request.getServletPath(), true);
            HttpHeaders hh = new HttpHeaders();
            hh.setAll(getResourceHeaders(request.getServletPath(), null, "List", null));
            return ResponseEntity.status(300).headers(hh).header("Content-Type", "text/html")
                    .header("Content-Location", request.getRequestURI() + "choice?path=" + request.getServletPath())
                    .body(StreamingHelpers.getStream(html));
        }
        if (mediaType == null) {
            final String html = Helpers.getMultiChoicesHtml(request.getServletPath(), true);
            HttpHeaders hh = new HttpHeaders();
            hh.setAll(getResourceHeaders(request.getServletPath(), null, "List", null));
            return ResponseEntity.status(406).headers(hh).header("Content-Type", "text/html")
                    .header("Content-Location", request.getRequestURI() + "choice?path=" + request.getServletPath())
                    .body(StreamingHelpers.getStream(html));
        }
        if (Helpers.equals(mediaType, MediaType.TEXT_HTML)) {
            log.info("mediatype is html", res);
            ensureColumbiaIDsLoaded();
            if (COLUMBIA_IDS.contains(res)) {
                log.info("mapping {} to its W counterpart for Columbia redirection", res);
                res = "W" + res.substring(1);
                prefixedRes = RES_PREFIX_SHORT + res;
                fullResURI = GRAPH_PREFIX_FULL + res;
            }
        }
        final Model model = QueryProcessor.getCoreResourceGraph(prefixedRes, fusekiUrl, null,
                computeGraphType(request));
        if (model.size() == 0) {
            LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes);
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON)
                    .body(StreamingHelpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
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
            String root = Helpers.getRootInstanceUri(prefixedRes, model);
            log.info("resource has root {}", root);
            if (root != null) {
                response.sendRedirect(ServiceConfig.getProperty("showUrl") + root + "?part=" + prefixedRes);
                log.info("send redirect {}", ServiceConfig.getProperty("showUrl") + root + "?part=" + prefixedRes);
            } else {
                log.info("send redirect {}", ServiceConfig.getProperty("showUrl") + type + res);
                response.sendRedirect(ServiceConfig.getProperty("showUrl") + type + res);
            }
        }

        final String ext = BudaMediaTypes.getExtFromMime(mediaType);
        HttpHeaders hh = new HttpHeaders();
        hh.setAll(getResourceHeaders(request.getServletPath(), ext, "Choice", getEtag(model, res)));
        return ResponseEntity.ok().contentType(mediaType).body(
                StreamingHelpers.getModelStream(model, ext, fullResURI, null, ServiceConfig.PREFIX.getPrefixMap()));
    }

    
    public static final String defaultMaxVal = "999999999";
    public static final Integer defaultMaxValI = 999999999;
    public ResponseEntity<StreamingResponseBody> getFormattedResourceGraph(@PathVariable("res") String res,
            @PathVariable("ext") String ext, @RequestParam(value = "startChar", defaultValue = "0") String startChar,
            @RequestParam(value = "endChar", defaultValue = defaultMaxVal) String endChar,
            @RequestHeader(value = "fusekiUrl", required = false) String fusekiUrl, HttpServletResponse response,
            HttpServletRequest request) throws RestException, IOException {
        log.info("Call to getFormattedResourceGraph() res {}, ext {}", res, ext);
        final String prefixedRes = RES_PREFIX_SHORT + res;
        final String fullResURI = GRAPH_PREFIX_FULL + res;
        final MediaType media = BudaMediaTypes.getMimeFromExtension(ext);
        log.info("Call to getFormattedResourceGraph() path is {}", request.getServletPath());
        if (media == null) {
            final String html = Helpers.getMultiChoicesHtml("/resource/" + res, true);
            return ResponseEntity.status(300).header("Content-Type", "text/html")
                    .header("Content-Location", request.getRequestURI() + "choice?path=" + request.getServletPath())
                    .body(StreamingHelpers.getStream(html));
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
            return TxtEtextExport.getResponse(request, RES_PREFIX + res, Integer.parseInt(startChar),
                    Integer.parseInt(endChar), res);
        }
        final Model model = QueryProcessor.getCoreResourceGraph(prefixedRes, fusekiUrl, null,
                computeGraphType(request));
        if (model.size() == 0) {
            LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes);
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON)
                    .body(StreamingHelpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
        }
        HttpHeaders hh = new HttpHeaders();
        hh.setAll(getResourceHeaders(request.getServletPath(), ext, null, getEtag(model, res)));
        return ResponseEntity.ok().headers(hh).contentType(media).body(
                StreamingHelpers.getModelStream(model, ext, fullResURI, null, ServiceConfig.PREFIX.getPrefixMap()));

    }

    @GetMapping(value = "/{base:[a-z]+}/**")
    public Object getExtOntologyHomePage(HttpServletResponse resp, HttpServletRequest request,
            @RequestHeader("Accept") String format, @PathVariable String base) throws RestException, IOException {
        Helpers.setCacheControl(resp, "public");
        String path = request.getRequestURI();
        log.info("getExtOntologyHomePage WAS CALLED WITH >> pathUri : {}/ servletPath{} ", path,
                request.getServletPath());
        String other = request.getServletPath().substring(base.length() + 2);
        if (other.contains(".")) {
            String[] parts = other.split("\\.");
            log.info("getExtOntologyHomePage With EXT >> base : {}/ other:{} and ext: {}", base, parts[0], parts[1]);
            return getOntologyResourceAsFile(request, parts[1]);
        }
        // if (ServiceConfig.SERVER_ROOT.equals("localhost:8080")) {
        if (ServiceConfig.SERVER_ROOT.equals("purl.bdrc.io")) {
            log.info("getExtOntologyHomePage WAS CALLED WITH >> base : {}/ other:{} and format: {}", base, other,
                    format);
            boolean isBase = false;
            String baseUri = "";
            String tmp = request.getRequestURL().toString().replace("https", "http");
            log.info("getExtOntologyHomePage tmp is >> {}", tmp);
            if (OntPolicies.isBaseUri(tmp)) {
                baseUri = parseBaseUri(tmp);
                isBase = true;
            }
            log.info("getExtOntologyHomePage absolute path >> {} and other = {}", request.getRequestURL().toString(),
                    other);
            if (OntPolicies.isBaseUri(parseBaseUri(tmp + other))) {
                baseUri = parseBaseUri(tmp + other);
                isBase = true;
            }
            log.info("getExtOntologyHomePage baseUri is >> {}", baseUri);
            // Is the full request uri a baseuri?
            if (isBase) {
                // if accept header is present
                if (format != null) {
                    log.info("getExtOntologyHomePage IS BASE and Format is >> {}", format);
                    MediaType mediaType = BudaMediaTypes.selectVariant(format, BudaMediaTypes.resVariants);
                    log.info("getExtOntologyHomePage VARIANT is >> {} and path is {}", mediaType, baseUri);
                    if (mediaType == null) {
                        return (ResponseEntity<String>) ResponseEntity.status(406).body("No acceptable Accept header");
                    }
                    String url = OntPolicies.getOntologyByBase(baseUri).getFile().replace("RDF", "owl-schema");
                    log.info("getExtOntologyHomePage FILE URI is >> {} and path is {}", url, baseUri);
                    // using cache if available
                    OntModel om = OntData.getOntModelByBase(baseUri);
                    // browser request : serving html page
                    if (Helpers.equals(mediaType, MediaType.TEXT_HTML)) {
                        ModelAndView model = new ModelAndView();
                        model.addObject("path", baseUri);
                        model.setViewName("ontologyHome");
                        return (ModelAndView) model;
                    } else {
                        final String JenaLangStr = BudaMediaTypes
                                .getJenaFromExtension(BudaMediaTypes.getExtFromMime(mediaType));
                        log.info("getExtOntologyHomePage JenaLangStr is >> {}", JenaLangStr);
                        return (String) writeStream(om, JenaLangStr).toString();
                    }
                }
            } else {
                if (OntData.ontAllMod.getOntResource(tmp) == null) {
                    LdsError lds = new LdsError(LdsError.ONT_URI_ERR).setContext("Ont resource is null for " + tmp);
                    return (ResponseEntity<StreamingResponseBody>) ResponseEntity.status(404)
                            .contentType(MediaType.APPLICATION_JSON).body(StreamingHelpers
                                    .getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
                }
                if (format != null) {
                    MediaType mediaType = BudaMediaTypes.selectVariant(format, BudaMediaTypes.resVariants);
                    log.info("getExtOntologyHomePage VARIANT NOT BASE is >> {} and path is {}", mediaType, tmp);
                    if (mediaType == null) {
                        return (ResponseEntity<String>) ResponseEntity.status(406).body("No acceptable Accept header");
                    }
                    if (OntData.isClass(tmp)) {
                        log.info("CLASS>>" + tmp);
                        OntClassModel ocm = new OntClassModel(tmp);
                        if (Helpers.equals(mediaType, MediaType.TEXT_HTML)) {
                            ModelAndView model = new ModelAndView();
                            model.addObject("model", ocm);
                            model.setViewName("ontClassView");
                            return (ModelAndView) model;
                        }

                    } else {
                        log.info("PROP>>" + tmp);
                        OntPropModel opm = new OntPropModel(tmp);
                        if (Helpers.equals(mediaType, MediaType.TEXT_HTML)) {
                            ModelAndView model = new ModelAndView();
                            model.addObject("model", opm);
                            model.setViewName("ontPropView");
                            return (ModelAndView) model;
                        }
                    }
                } else {
                    return (ResponseEntity<String>) ResponseEntity.status(406).body("No acceptable Accept header");
                }
            }
            return (ResponseEntity<String>) ResponseEntity.status(404).body("Not found");
        } else {
            ModelAndView model = new ModelAndView();
            model.setViewName("disabled");
            return (ModelAndView) model;
        }
    }

    public Object getOntologyResourceAsFile(HttpServletRequest request, String ext) throws RestException {
        String reasonerUri = "";
        String infProfile = request.getHeader("Accept-Profile");
        if (infProfile != null) {
            reasonerUri = infProfile;
        } else {
            infProfile = request.getParameter("profile");
            if (infProfile != null) {
                reasonerUri = infProfile;
            }
        }
        Reasoner reasoner = null;
        if (!"".contentEquals(reasonerUri)) {
            reasoner = ReasonerRegistry.theRegistry().create(reasonerUri, null);
        }
        String res = request.getRequestURL().toString().replace("https", "http");
        res = res.substring(0, res.lastIndexOf('.')) + "/";

        final String JenaLangStr = BudaMediaTypes.getJenaFromExtension(ext);
        log.info("In getOntologyResourceAsFile(), RES = {} and ext= {} and jenalang={}", res, ext, JenaLangStr);
        if (JenaLangStr == null) {
            LdsError lds = new LdsError(LdsError.URI_SYNTAX_ERR).setContext(request.getRequestURL().toString());
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON)
                    .body(StreamingHelpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
        }
        log.info("In getOntologyResourceAsFile(), isBaseUri uri {} baseuri= {}", res, OntPolicies.isBaseUri(res));
        if (OntPolicies.isBaseUri(res)) {
            OntPolicy params = OntPolicies.getOntologyByBase(parseBaseUri(res));
            Model model = OntData.getOntModelByBase(params.getBaseUri());
            // Inference here if required
            if (reasoner != null && model != null) {
                model = ModelFactory.createInfModel(reasoner, model);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (model != null) {
                if (JenaLangStr == "STTL") {
                    final RDFWriter writer = (RDFWriter) TTLRDFWriter.getSTTLRDFWriter(model, params.getBaseUri());
                    writer.output(baos);
                } else {
                    if (JenaLangStr == RDFLanguages.strLangTurtle) {
                        model.write(baos, "TURTLE");
                    } else {
                        org.apache.jena.rdf.model.RDFWriter wr = model.getWriter(JenaLangStr);
                        if (JenaLangStr.equals(RDFLanguages.strLangRDFXML)) {
                            wr.setProperty("xmlbase", params.getBaseUri());
                        }
                        wr.write(model, baos, params.getBaseUri());
                    }
                }
            } else {
                return ResponseEntity.status(404).body("No model found for " + params.getBaseUri());
            }
            if (reasoner != null) {
                return ResponseEntity.ok().eTag(OntData.getOntCommitId()).header("Profile", reasonerUri)
                        .header("Content-type",
                                BudaMediaTypes.getMimeFromExtension(ext) + ";profile=\"" + reasonerUri + "\"")
                        .body(baos.toString());
            }
            return ResponseEntity.ok().eTag(OntData.getOntCommitId()).header("Content-Disposition", "inline")
                    .contentType(BudaMediaTypes.getMimeFromExtension(ext)).body(baos.toString());
        } else {
            res = res.replace(ServiceConfig.getProperty("serverRoot"), "purl.bdrc.io");
            if (res.endsWith("/")) {
                res = res.substring(0, res.length() - 1);
            }
            String query = "describe <" + res + ">";
            log.info("Looking up model for {}", res);
            Model model = QueryProcessor.getGraphFromModel(query, OntData.ontAllMod);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (model != null && model.size() > 0) {
                if (JenaLangStr == "STTL") {
                    final RDFWriter writer = (RDFWriter) TTLRDFWriter.getSTTLRDFWriter(model, null);
                    writer.output(baos);
                } else {
                    if (JenaLangStr == RDFLanguages.strLangTurtle) {
                        model.write(baos, "TURTLE");
                    } else {
                        org.apache.jena.rdf.model.RDFWriter wr = model.getWriter(JenaLangStr);
                        if (JenaLangStr.equals(RDFLanguages.strLangRDFXML)) {
                            wr.setProperty("xmlbase", null);
                        }
                        wr.write(model, baos, null);
                    }
                }
                return ResponseEntity.ok().eTag(OntData.getOntCommitId()).header("Content-Disposition", "inline")
                        .contentType(BudaMediaTypes.getMimeFromExtension(ext)).body(baos.toString());
            } else {
                LdsError lds = new LdsError(LdsError.ONT_URI_ERR).setContext(request.getRequestURL().toString());
                return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(
                        StreamingHelpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
            }
        }
    }

    @PostMapping(value = "/callbacks/github/owl-schema", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateOntology(@RequestBody String payload) throws RestException, IOException {
        log.info("updating Ontology models() >>");
        if (!ServiceConfig.isInChina()) {
            Webhook wh = new Webhook(payload, GitService.ONTOLOGIES);
            Thread t = new Thread(wh);
            t.start();
        } else {
            return ResponseEntity.ok().body("Ontologies webhook is not used in this configuration");
        }
        return ResponseEntity.ok().body("Ontologies are being updated");
    }

    @PostMapping(value = "/callbacks/github/shapes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateShapesOntology(@RequestBody String payload) throws RestException, IOException {
        if (!ServiceConfig.isInChina()) {
            Webhook wh = new Webhook(payload, GitService.SHAPES);
            Thread t = new Thread(wh);
            t.start();
            return ResponseEntity.ok().body("Shapes Ontologies are being updated");
        } else {
            return ResponseEntity.ok().body("Shapes Ontologies are not used in this configuration");
        }
    }

    @GetMapping(value = "/ontology/data/{ext}")
    public Object getAllOntologyData(HttpServletRequest request, HttpServletResponse resp,
            @PathVariable("ext") String ext) throws RestException {
        log.info("Call to getAllOntologyData(); with ext {}", ext);
        Model model = OntData.ontAllMod;
        Helpers.setCacheControl(resp, "public");
        final String JenaLangStr = BudaMediaTypes.getJenaFromExtension(ext);
        // Inference here if required
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (model != null) {
            if (JenaLangStr == "STTL") {
                final RDFWriter writer = (RDFWriter) TTLRDFWriter.getSTTLRDFWriter(model,
                        "http://purl.bdrc.io/ontology/core/");
                writer.output(baos);
            } else {
                if (JenaLangStr == RDFLanguages.strLangTurtle) {
                    model.write(baos, "TURTLE");
                } else {
                    org.apache.jena.rdf.model.RDFWriter wr = model.getWriter(JenaLangStr);

                    wr.write(model, baos, "http://purl.bdrc.io/ontology/core/");
                }
            }
        } else {
            return ResponseEntity.status(404).body("No model found ");
        }
        return ResponseEntity.ok().eTag(OntData.getOntCommitId()).header("Content-Disposition", "inline")
                .contentType(BudaMediaTypes.getMimeFromExtension(ext)).body(baos.toString());
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

    private ByteArrayOutputStream writeStream(Model m, String lang) {
        log.info("Jena Lang for writer is {}", lang);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        if (lang == "STTL") {
            final RDFWriter writer = TTLRDFWriter.getSTTLRDFWriter(m, "");
            writer.output(os);
        } else {
            if (lang.equals(RDFLanguages.strLangRDFXML)) {
                m.write(os, lang, "");
            } else {
                m.write(os, lang);
            }
        }
        return os;
    }

    private static String getEtag(Model model, String res) {
        Statement smt = model.getProperty(ResourceFactory.createResource(RES_PREFIX + res),
                ResourceFactory.createProperty("http://purl.bdrc.io/ontology/admin/gitRevision"));
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