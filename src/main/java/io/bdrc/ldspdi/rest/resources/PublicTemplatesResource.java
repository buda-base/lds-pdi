package io.bdrc.ldspdi.rest.resources;

import java.io.ByteArrayInputStream;

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

import io.bdrc.ldspdi.rest.features.SpringCacheControl;
import io.bdrc.ldspdi.results.ResultPage;
import io.bdrc.ldspdi.results.ResultSetWrapper;
import io.bdrc.ldspdi.results.Results;
import io.bdrc.ldspdi.results.ResultsCache;
import io.bdrc.ldspdi.service.GitService;
import io.bdrc.ldspdi.sparql.LdsQuery;
import io.bdrc.ldspdi.sparql.LdsQueryService;
import io.bdrc.ldspdi.sparql.Prefixes;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.BudaMediaTypes;
import io.bdrc.ldspdi.utils.DocFileModel;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.restapi.exceptions.ErrorMessage;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

@RestController
@RequestMapping("/")
public class PublicTemplatesResource {

    public final static Logger log = LoggerFactory.getLogger(PublicTemplatesResource.class);

    @GetMapping(value = "/query/table/{file}")
    @SpringCacheControl()
    public Object getQueryTemplateResults(HttpServletResponse response, HttpServletRequest request, @RequestHeader(value = "fusekiUrl", required = false) final String fusekiUrl, @PathVariable("file") String file) throws RestException {
        log.info("Call to getQueryTemplateResults() {}, params: {}", file, request.getParameterMap()); // Settings
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
                    return (ResponseEntity<String>) ResponseEntity.status(403).body("The requested page size exceeds the current limit (" + qfp.getLimit_max() + ")");
                }
            } catch (Exception e) {
                throw new RestException(500, LdsError.UNKNOWN_ERR, e.getMessage());
            }
        }
        final String query = qfp.getParametizedQuery(hm);
        log.info("Parametized Query >> : {}", query);
        log.info("PARAMS MAP >> : {}", hm);
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
            byte[] buff = Helpers.getJsonBytesStream(r);
            return (ResponseEntity<InputStreamResource>) ResponseEntity.ok().contentLength(buff.length).contentType(MediaType.APPLICATION_JSON).header("Content-Disposition", "attachment; filename=\"" + file + ".json\"")
                    .body(new InputStreamResource(new ByteArrayInputStream(buff)));
        }
        if ("csv".equals(fmt)) {
            byte[] buff = res.getCsvAsBytes(hm, true);
            return (ResponseEntity<InputStreamResource>) ResponseEntity.ok().contentLength(buff.length).contentType(BudaMediaTypes.MT_CSV).header("Content-Disposition", "attachment; filename=\"" + file + "_p" + pageNumber + ".csv\"")
                    .body(new InputStreamResource(new ByteArrayInputStream(buff)));

        }
        if ("csv_f".equals(fmt)) {
            byte[] buff = res.getCsvAsBytes(hm, false);
            return (ResponseEntity<InputStreamResource>) ResponseEntity.ok().contentLength(buff.length).contentType(BudaMediaTypes.MT_CSV).header("Content-Disposition", "attachment; filename=\"" + file + "_p" + pageNumber + ".csv\"")
                    .body(new InputStreamResource(new ByteArrayInputStream(buff)));
        }
        hm.put(QueryConstants.REQ_METHOD, "GET");
        hm.put("query", qfp.getQueryHtml());
        ResultPage mod = new ResultPage(res, hm.get(QueryConstants.PAGE_NUMBER), hm, qfp.getTemplate());
        ModelAndView model = new ModelAndView();
        model.addObject("model", mod);
        model.setViewName("resPage");
        return (ModelAndView) model;
    }

    @PostMapping(value = "/query/table/{file}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SpringCacheControl()
    public ResponseEntity<StreamingResponseBody> getQueryTemplateResultsJsonPost(@RequestHeader(value = "fusekiUrl", required = false) final String fuseki, @PathVariable("file") String file, @RequestBody HashMap<String, String> map,
            HttpServletRequest request) throws RestException {
        log.info("Call to getQueryTemplateResultsJsonPost() with params : {}", map);
        if (map == null || map.size() == 0) {
            LdsError lds = new LdsError(LdsError.MISSING_PARAM_ERR).setContext("in getQueryTemplateResultsJsonPost() : Map =" + map);
            return ResponseEntity.status(500).contentType(MediaType.APPLICATION_JSON).body(Helpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(500, lds)));

        }
        final LdsQuery qfp = LdsQueryService.get(file + ".arq");
        final String query = qfp.getParametizedQuery(map);
        if (query.startsWith(QueryConstants.QUERY_ERROR)) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Helpers.getStream(query));
        }
        String fmt = map.get(QueryConstants.FORMAT);
        if ("xml".equals(fmt)) {
            ResultSet res = QueryProcessor.getResults(query, fuseki);
            return ResponseEntity.ok().contentType(MediaType.TEXT_XML).body(Helpers.getResultSetAsXml(res));
        } else {
            ResultSetWrapper res = QueryProcessor.getResults(query, fuseki, map.get(QueryConstants.RESULT_HASH), map.get(QueryConstants.PAGE_SIZE));
            map.put(QueryConstants.RESULT_HASH, Integer.toString(res.getHash()));
            map.put(QueryConstants.PAGE_SIZE, Integer.toString(res.getPageSize()));
            map.put(QueryConstants.REQ_URI, request.getRequestURL().toString() + "?" + request.getQueryString());
            Results r = new Results(res, map);
            String json = new String(Helpers.getJsonBytesStream(r));
            return ResponseEntity.ok().body(Helpers.getStream(json));
        }
    }

    @GetMapping(value = "/query/graph/{file}")
    @SpringCacheControl()
    public ResponseEntity<StreamingResponseBody> getGraphTemplateResults(HttpServletRequest request, @RequestHeader(value = "fusekiUrl", required = false) final String fuseki,
            @RequestParam(value = "format", defaultValue = "jsonld") final String format, @PathVariable("file") String file) throws RestException {
        String path = request.getServletPath();
        MediaType variant = BudaMediaTypes.selectVariant(request.getHeader("Accept"), BudaMediaTypes.graphVariants);
        log.info("Call to getGraphTemplateResults() with URL: {}, accept {}, variant {}", path, format, variant);
        if (format == null && variant == null) {
            HttpHeaders hh = new HttpHeaders();
            hh.setAll(getGraphResourceHeaders(path, null, "List"));
            return ResponseEntity.status(300).headers(hh).header("Content-Type", "text/html").header("Content-Location", request.getRequestURI() + "choice?path=" + path).body(Helpers.getStream(Helpers.getMultiChoicesHtml(path, false)));

        }
        // Settings
        HashMap<String, String> hm = Helpers.convertMulti(request.getParameterMap());
        // process
        final LdsQuery qfp = LdsQueryService.get(file + ".arq");
        final String query = qfp.getParametizedQuery(hm);
        // format is prevalent
        MediaType mediaType = BudaMediaTypes.getMimeFromExtension(format);
        if (mediaType == null) {
            mediaType = variant;
        }
        Model model = QueryProcessor.getGraph(query, fuseki, null);
        if (model.size() == 0) {
            LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(file + " and params=" + hm.toString());
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Helpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));

        }
        final String ext = BudaMediaTypes.getExtFromMime(mediaType);
        HttpHeaders hh = new HttpHeaders();
        hh.setAll(getGraphResourceHeaders(path, ext, "Choice"));
        return ResponseEntity.ok().contentType(mediaType).body(Helpers.getModelStream(model, ext));

    }

    @PostMapping(value = "/query/graph/{file}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SpringCacheControl()
    public ResponseEntity<StreamingResponseBody> getGraphTemplateResultsPost(@RequestHeader(value = "fusekiUrl", required = false) final String fuseki, @RequestHeader(value = "Accept", defaultValue = "application/ld+json") String accept,
            @PathVariable("file") String file, HttpServletRequest request, @RequestBody HashMap<String, String> map) throws RestException {
        if (accept.equals("*/*")) {
            accept = "application/ld+json";
        }
        final MediaType variant = BudaMediaTypes.selectVariant(accept, BudaMediaTypes.graphVariants);
        log.info("Call to getGraphTemplateResultsPost() with file: {}, accept: {}, variant: {}, map: {}", file, accept, variant, map);
        if (variant == null) {
            LdsError lds = new LdsError(LdsError.NO_ACCEPT_ERR).setContext(file + " in getGraphTemplateResultsPost()");
            return ResponseEntity.status(406).contentType(MediaType.APPLICATION_JSON).body(Helpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(406, lds)));
        }
        // process
        final LdsQuery qfp = LdsQueryService.get(file + ".arq");
        final String query = qfp.getParametizedQuery(map);
        // format is prevalent
        MediaType mediaType = BudaMediaTypes.getMimeFromExtension(accept);
        if (mediaType == null) {
            mediaType = variant;
        }
        Model model = QueryProcessor.getGraph(query, fuseki, null);
        if (model.size() == 0) {
            LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(file + " and params=" + map.toString());
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Helpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
        }
        return ResponseEntity.ok().contentType(mediaType).body(Helpers.getModelStream(model, BudaMediaTypes.getExtFromMime(mediaType)));
    }

    @PostMapping(value = "/callbacks/github/lds-queries", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateQueries() throws RestException {
        log.info("updating query templates >>");
        Thread t = new Thread(new GitService());
        t.start();
        Prefixes.loadPrefixes();
        DocFileModel.clearCache();
        LdsQueryService.clearCache();
        ResultsCache.clearCache();
        return ResponseEntity.ok().body("Lds-queries have been updated");
    }

    @PostMapping(value = "/clearcache")
    public ResponseEntity<String> clearCache() throws RestException {
        log.info("clearing cache >>");
        if (ResultsCache.clearCache()) {
            return ResponseEntity.ok().body("OK");
        } else {
            return ResponseEntity.ok().body("ERROR");
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