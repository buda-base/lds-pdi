package io.bdrc.ldspdi.rest.controllers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

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

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
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
import io.bdrc.ldspdi.results.ResultPage;
import io.bdrc.ldspdi.results.ResultSetWrapper;
import io.bdrc.ldspdi.results.Results;
import io.bdrc.ldspdi.results.ResultsCache;
import io.bdrc.ldspdi.service.GitService;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.service.Webhook;
import io.bdrc.ldspdi.sparql.LdsQuery;
import io.bdrc.ldspdi.sparql.LdsQueryService;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.GeoLocation;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.libraries.BudaMediaTypes;
import io.bdrc.libraries.GlobalHelpers;
import io.bdrc.libraries.StreamingHelpers;

@RestController
@RequestMapping("/")
public class PublicTemplatesController {

    public final static Logger log = LoggerFactory.getLogger(PublicTemplatesController.class);

    @GetMapping(value = "/query/table/{file}")
    public Object getQueryTemplateResults(HttpServletResponse response, HttpServletRequest request,
            @RequestHeader(value = "fusekiUrl", required = false) final String fusekiUrl, @PathVariable("file") String file) throws RestException {
        log.info("Call to getQueryTemplateResults() {}, params: {}", file, request.getParameterMap()); // Settings
        Helpers.setCacheControl(response, "public");
        ModelAndView model = new ModelAndView();
        try {
            HashMap<String, String> hm = Helpers.convertMulti(request.getParameterMap());

            String pageSize = hm.get(QueryConstants.PAGE_SIZE);
            String pageNumber = hm.get(QueryConstants.PAGE_NUMBER);
            if (pageNumber == null) {
                pageNumber = "1";
            }
            hm.put(QueryConstants.REQ_URI, request.getRequestURL().toString() + "?" + request.getQueryString());
            hm.put(QueryConstants.REQ_METHOD, "GET");
            Set<Entry<String, String>> set = hm.entrySet();
            for (Entry<String, String> e : set) {
                log.info("Key {} and value {}", e.getKey(), e.getValue());
            }
            // process
            final LdsQuery qfp = LdsQueryService.get(file + ".arq");
            if (pageSize != null) {
                try {
                    if (Long.parseLong(pageSize) > qfp.getLimit_max()) {
                        return (ResponseEntity<String>) ResponseEntity.status(403)
                                .body("The requested page size exceeds the current limit (" + qfp.getLimit_max() + ")");
                    }
                } catch (Exception e) {
                    throw new RestException(500, LdsError.UNKNOWN_ERR, e.getMessage());
                }
            }
            if (qfp.getRequiredParams().contains(QueryConstants.RIC)) {
                hm.put(QueryConstants.RIC, String.valueOf(GeoLocation.isFromChina(request)));
            }
            final String query = qfp.getParametizedQuery(hm, true);
            log.debug("Parametized Query >> : {}", query);

            if (query.startsWith(QueryConstants.QUERY_ERROR)) {
                throw new RestException(500, new LdsError(LdsError.SPARQL_ERR).setContext(" in getQueryTemplateResults() " + query));
            }
            String fmt = hm.get(QueryConstants.FORMAT);
            if ("xml".equals(fmt)) {
                ResultSet rs = QueryProcessor.getResults(query, fusekiUrl);
                response.setContentType("text/html");
                return ResultSetFormatter.asXMLString(rs);
            }
            ResultSetWrapper res = QueryProcessor.getResults(query, fusekiUrl, hm.get(QueryConstants.RESULT_HASH), hm.get(QueryConstants.PAGE_SIZE));
            if ("json".equals(fmt)) {
                Results r = new Results(res, hm);
                byte[] buff = GlobalHelpers.getJsonBytes(r);
                return (ResponseEntity<InputStreamResource>) ResponseEntity.ok().eTag(ServiceConfig.getQueriesCommit()).contentLength(buff.length)
                        .contentType(MediaType.APPLICATION_JSON).header("Content-Disposition", "attachment; filename=\"" + file + ".json\"")
                        .body(new InputStreamResource(new ByteArrayInputStream(buff)));
            }
            if ("csv".equals(fmt)) {
                byte[] buff = res.getCsvAsBytes(hm, true);
                return (ResponseEntity<InputStreamResource>) ResponseEntity.ok().eTag(ServiceConfig.getQueriesCommit()).contentLength(buff.length)
                        .contentType(BudaMediaTypes.MT_CSV)
                        .header("Content-Disposition", "attachment; filename=\"" + file + "_p" + pageNumber + ".csv\"")
                        .body(new InputStreamResource(new ByteArrayInputStream(buff)));

            }
            if ("csv_f".equals(fmt)) {
                byte[] buff = res.getCsvAsBytes(hm, false);
                return (ResponseEntity<InputStreamResource>) ResponseEntity.ok().eTag(ServiceConfig.getQueriesCommit()).contentLength(buff.length)
                        .contentType(BudaMediaTypes.MT_CSV)
                        .header("Content-Disposition", "attachment; filename=\"" + file + "_p" + pageNumber + ".csv\"")
                        .body(new InputStreamResource(new ByteArrayInputStream(buff)));
            }
            hm.put(QueryConstants.REQ_METHOD, "GET");
            hm.put("query", qfp.getQueryHtml());
            ResultPage mod = new ResultPage(res, hm.get(QueryConstants.PAGE_NUMBER), hm, qfp.getTemplate());
            model.addObject("model", mod);
            model.setViewName("resPage");
        } catch (Exception e) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(baos));
            RestException re = new RestException(500, LdsError.UNKNOWN_ERR, e.getClass().getName(), baos.toString(), "");
            try {
                baos.close();
            } catch (IOException e1) {
                throw new RestException(500, LdsError.UNKNOWN_ERR, e.getClass().getName(), "Failed to close exception trace byte output stream", "");
            }
            throw re;
        }
        return (ModelAndView) model;
    }

    @PostMapping(value = "/query/table/{file}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> getQueryTemplateResultsJsonPost(HttpServletRequest request, HttpServletResponse response,
            @RequestHeader(value = "fusekiUrl", required = false) final String fuseki, @PathVariable("file") String file,
            @RequestBody HashMap<String, String> map) throws RestException {
        Helpers.setCacheControl(response, "public");
        try {
            log.info("Call to getQueryTemplateResultsJsonPost() with params : {}", map);
            if (map == null || map.size() == 0) {
                LdsError lds = new LdsError(LdsError.MISSING_PARAM_ERR).setContext("in getQueryTemplateResultsJsonPost() : Map =" + map);
                return ResponseEntity.status(500).contentType(MediaType.APPLICATION_JSON)
                        .body(StreamingHelpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(500, lds)));

            }
            final LdsQuery qfp = LdsQueryService.get(file + ".arq");
            if (qfp.getRequiredParams().contains(QueryConstants.RIC)) {
                map.put(QueryConstants.RIC, String.valueOf(GeoLocation.isFromChina(request)));
            }
            final String query = qfp.getParametizedQuery(map, true);
            if (query.startsWith(QueryConstants.QUERY_ERROR)) {
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(StreamingHelpers.getStream(query));
            }
            String fmt = map.get(QueryConstants.FORMAT);
            if ("xml".equals(fmt)) {
                ResultSet res = QueryProcessor.getResults(query, fuseki);
                return ResponseEntity.ok().eTag(ServiceConfig.getQueriesCommit()).contentType(MediaType.TEXT_XML)
                        .body(Helpers.getResultSetAsXml(res));
            } else {
                ResultSetWrapper res = QueryProcessor.getResults(query, fuseki, map.get(QueryConstants.RESULT_HASH),
                        map.get(QueryConstants.PAGE_SIZE));
                map.put(QueryConstants.RESULT_HASH, Integer.toString(res.getHash()));
                map.put(QueryConstants.PAGE_SIZE, Integer.toString(res.getPageSize()));
                map.put(QueryConstants.REQ_URI, request.getRequestURL().toString() + "?" + request.getQueryString());
                Results r = new Results(res, map);
                String json = new String(GlobalHelpers.getJsonBytes(r));
                return ResponseEntity.ok().eTag(ServiceConfig.getQueriesCommit()).body(StreamingHelpers.getStream(json));
            }
        } catch (Exception e) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(baos));
            RestException re = new RestException(500, LdsError.UNKNOWN_ERR, e.getClass().getName(), baos.toString(), "");
            try {
                baos.close();
            } catch (IOException e1) {
                throw new RestException(500, LdsError.UNKNOWN_ERR, e.getClass().getName(), "Failed to close exception trace byte output stream", "");
            }
            throw re;
        }
    }

    @GetMapping(value = "/query/graph/{file}")
    public ResponseEntity<StreamingResponseBody> getGraphTemplateResults(HttpServletResponse response, HttpServletRequest request,
            @RequestHeader(value = "fusekiUrl", required = false) final String fuseki,
            @RequestParam(value = "format", required = false) String format, @PathVariable("file") String file) throws RestException {
        Helpers.setCacheControl(response, "public");
        MediaType mediaType = null;
        Model model = null;
        String ext = null;
        try {
            String path = request.getServletPath();
            MediaType variant = BudaMediaTypes.selectVariant(request.getHeader("Accept"), BudaMediaTypes.graphVariants);
            log.info("Call to getGraphTemplateResults() with URL: {}, accept {}, variant {}", path, format, variant);
            if (format == null && variant == null) {
                format = "jsonld";
            }
            // dead code but we need to keep jsonld as the default for now for pdl
            if (format == null && variant == null) {
                HttpHeaders hh = new HttpHeaders();
                hh.setAll(getGraphResourceHeaders(path, null, "List"));
                return ResponseEntity.status(300).headers(hh).header("Content-Type", "text/html")
                        .header("Content-Location", request.getRequestURI() + "choice?path=" + path)
                        .body(StreamingHelpers.getStream(Helpers.getMultiChoicesHtml(path, false)));

            }
            // Settings
            HashMap<String, String> hm = Helpers.convertMulti(request.getParameterMap());
            // process
            final LdsQuery qfp = LdsQueryService.get(file + ".arq");
            if (qfp.getRequiredParams().contains(QueryConstants.RIC)) {
                hm.put(QueryConstants.RIC, String.valueOf(GeoLocation.isFromChina(request)));
            }
            final String query = qfp.getParametizedQuery(hm, true);
            log.debug("getGraphTemplateResults() Parametized query {}", query);
            // format is prevalent
            mediaType = BudaMediaTypes.getMimeFromExtension(format);
            if (mediaType == null) {
                mediaType = variant;
            }
            model = QueryProcessor.getGraph(query, fuseki, null);
            if (model.size() == 0) {
                LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(file + " and params=" + hm.toString());
                return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON)
                        .body(StreamingHelpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));

            }
            ext = BudaMediaTypes.getExtFromMime(mediaType);
            HttpHeaders hh = new HttpHeaders();
            hh.setAll(getGraphResourceHeaders(path, ext, "Choice"));
        } catch (Exception e) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(baos));
            RestException re = new RestException(500, LdsError.UNKNOWN_ERR, e.getClass().getName(), baos.toString(), "");
            try {
                baos.close();
            } catch (IOException e1) {
                throw new RestException(500, LdsError.UNKNOWN_ERR, e.getClass().getName(), "Failed to close exception trace byte output stream", "");
            }
            throw re;
        }
        return ResponseEntity.ok().eTag(ServiceConfig.getQueriesCommit()).contentType(mediaType)
                .body(StreamingHelpers.getModelStream(model, ext, ServiceConfig.PREFIX.getPrefixMap()));

    }

    @GetMapping(value = "/query/ask/{file}")
    public ResponseEntity<String> getAskTemplateResults(HttpServletResponse response, HttpServletRequest request,
            @RequestHeader(value = "fusekiUrl", required = false) final String fuseki,
            @RequestParam(value = "format", required = false) String format, @PathVariable("file") String file) throws RestException {
        // returning the json string "null", "true" or "false" (without the quotation marks)
        Helpers.setCacheControl(response, "public");
        String res = "null";
        try {
            // Settings
            HashMap<String, String> hm = Helpers.convertMulti(request.getParameterMap());
            // process
            final LdsQuery qfp = LdsQueryService.get(file + ".arq");
            if (qfp.getRequiredParams().contains(QueryConstants.RIC)) {
                hm.put(QueryConstants.RIC, String.valueOf(GeoLocation.isFromChina(request)));
            }
            final String query = qfp.getParametizedQuery(hm, true);
            log.debug("getAskTemplateResults() Parametized query {}", query);
            res = QueryProcessor.getAsk(query, fuseki, null) ? "true" : "false";
        } catch (Exception e) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(baos));
            RestException re = new RestException(500, LdsError.UNKNOWN_ERR, e.getClass().getName(), baos.toString(), "");
            try {
                baos.close();
            } catch (IOException e1) {
                throw new RestException(500, LdsError.UNKNOWN_ERR, e.getClass().getName(), "Failed to close exception trace byte output stream", "");
            }
            throw re;
        }
        return ResponseEntity.ok().eTag(ServiceConfig.getQueriesCommit()).contentType(MediaType.APPLICATION_JSON)
                .body(res);
    }
    
    @PostMapping(value = "/query/graph/{file}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> getGraphTemplateResultsPost(
            @RequestHeader(value = "fusekiUrl", required = false) final String fuseki,
            @RequestHeader(value = "Accept", defaultValue = "application/ld+json") String accept, @PathVariable("file") String file,
            HttpServletResponse response, HttpServletRequest request, @RequestBody HashMap<String, String> map) throws RestException {
        Helpers.setCacheControl(response, "public");
        if (accept.equals("*/*")) {
            accept = "application/ld+json";
        }
        MediaType mediaType = null;
        Model model = null;
        try {
            final MediaType variant = BudaMediaTypes.selectVariant(accept, BudaMediaTypes.graphVariants);
            log.info("Call to getGraphTemplateResultsPost() with file: {}, accept: {}, variant: {}, map: {}", file, accept, variant, map);
            if (variant == null) {
                LdsError lds = new LdsError(LdsError.NO_ACCEPT_ERR).setContext(file + " in getGraphTemplateResultsPost()");
                return ResponseEntity.status(406).contentType(MediaType.APPLICATION_JSON)
                        .body(StreamingHelpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(406, lds)));
            }
            // process
            final LdsQuery qfp = LdsQueryService.get(file + ".arq");
            if (qfp.getRequiredParams().contains(QueryConstants.RIC)) {
                map.put(QueryConstants.RIC, String.valueOf(GeoLocation.isFromChina(request)));
            }
            final String query = qfp.getParametizedQuery(map, true);
            // format is prevalent
            mediaType = BudaMediaTypes.getMimeFromExtension(accept);
            if (mediaType == null) {
                mediaType = variant;
            }
            model = QueryProcessor.getGraph(query, fuseki, null);
            if (model.size() == 0) {
                LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(file + " and params=" + map.toString());
                return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON)
                        .body(StreamingHelpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
            }
        } catch (Exception e) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(baos));
            RestException re = new RestException(500, LdsError.UNKNOWN_ERR, e.getClass().getName(), baos.toString(), "");
            try {
                baos.close();
            } catch (IOException e1) {
                throw new RestException(500, LdsError.UNKNOWN_ERR, e.getClass().getName(), "Failed to close exception trace byte output stream", "");
            }
            throw re;
        }
        return ResponseEntity.ok().eTag(ServiceConfig.getQueriesCommit()).contentType(mediaType)
                .body(StreamingHelpers.getModelStream(model, BudaMediaTypes.getExtFromMime(mediaType), ServiceConfig.PREFIX.getPrefixMap()));
    }

    @PostMapping(value = "/callbacks/github/lds-queries", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateQueries(@RequestBody String payload) throws RestException {
        try {
            log.info("updating query templates >>");
            Webhook wh = new Webhook(payload, GitService.QUERIES);
            Thread t = new Thread(wh);
            t.start();
            ServiceConfig.loadPrefixes();
        } catch (Exception e) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(baos));
            RestException re = new RestException(500, LdsError.UNKNOWN_ERR, e.getClass().getName(), baos.toString(), "");
            try {
                baos.close();
            } catch (IOException e1) {
                throw new RestException(500, LdsError.UNKNOWN_ERR, e.getClass().getName(), "Failed to close exception trace byte output stream", "");
            }
            throw re;
        }
        return ResponseEntity.ok().body("Lds-queries have been updated");
    }

    @PostMapping(value = "/clearcache")
    public ResponseEntity<String> clearCache() throws RestException {
        log.info("clearing cache >>");
        try {
            if (ResultsCache.clearCache()) {
                return ResponseEntity.ok().body("OK");
            } else {
                return ResponseEntity.ok().body("ERROR");
            }
        } catch (Exception e) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(baos));
            RestException re = new RestException(500, LdsError.UNKNOWN_ERR, e.getClass().getName(), baos.toString(), "");
            try {
                baos.close();
            } catch (IOException e1) {
                throw new RestException(500, LdsError.UNKNOWN_ERR, e.getClass().getName(), "Failed to close exception trace byte output stream", "");
            }
            throw re;
        }
    }

    private static HashMap<String, String> getGraphResourceHeaders(String url, final String ext, final String tcn) {
        final HashMap<String, MediaType> map = BudaMediaTypes.getResExtensionMimeMap();
        final HashMap<String, String> headers = new HashMap<>();

        if (ext != null) {
            if (url.indexOf(".") < 0) {
                headers.put("Content-Location", url + "&format=" + ext);
            } else {
                url = url.substring(0, url.lastIndexOf("."));
            }
        }

        StringBuilder sb = new StringBuilder("");
        for (Entry<String, MediaType> e : map.entrySet()) {
            sb.append("{\"" + url + "&format=" + e.getKey() + "\" 1.000 {type " + e.getValue().toString() + "}},");
        }
        headers.put("Alternates", sb.toString().substring(0, sb.toString().length() - 1));
        headers.put("TCN", tcn);
        headers.put("Vary", "Negotiate, Accept");
        return headers;
    }

}