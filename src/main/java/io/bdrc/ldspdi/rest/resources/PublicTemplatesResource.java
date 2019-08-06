package io.bdrc.ldspdi.rest.resources;

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

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import org.apache.jena.rdf.model.Model;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.rest.features.JerseyCacheControl;
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
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.ldspdi.utils.MediaTypeUtils;
import io.bdrc.ldspdi.utils.ResponseOutputStream;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

@Path("/")
public class PublicTemplatesResource {

    public final static Logger log = LoggerFactory.getLogger(PublicDataResource.class.getName());

    @GET
    @Path("/query/table/{file}")
    @JerseyCacheControl()
    @Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, "text/csv" })
    public Response getQueryTemplateResults(@Context UriInfo info, @HeaderParam("fusekiUrl") final String fusekiUrl, @PathParam("file") String file) throws RestException {
        log.info("Call to getQueryTemplateResults() {}, params: {}", file, info.getQueryParameters());
        // Settings
        HashMap<String, String> hm = Helpers.convertMulti(info.getQueryParameters());
        hm.put(QueryConstants.REQ_URI, info.getRequestUri().toString().replace(info.getBaseUri().toString(), "/"));
        hm.put(QueryConstants.REQ_METHOD, "GET");
        // process
        final LdsQuery qfp = LdsQueryService.get(file + ".arq");
        final String query = qfp.getParametizedQuery(hm, true);
        if (query.startsWith(QueryConstants.QUERY_ERROR)) {
            throw new RestException(500, new LdsError(LdsError.SPARQL_ERR).setContext(" in getQueryTemplateResults() " + query));
        }
        ResultSetWrapper res = QueryProcessor.getResults(query, fusekiUrl, hm.get(QueryConstants.RESULT_HASH), hm.get(QueryConstants.PAGE_SIZE));
        String fmt = hm.get(QueryConstants.FORMAT);
        if ("json".equals(fmt)) {
            return Response.ok(ResponseOutputStream.getJsonResponseStream(new Results(res, hm)), MediaType.APPLICATION_JSON_TYPE).header("Content-Disposition", "attachment; filename=\"" + file + ".json\"").build();
        }
        if ("csv".equals(fmt)) {
            return Response.ok(res.getCsvStreamOutput(hm), MediaTypeUtils.MT_CSV).header("Content-Disposition", "attachment; filename=\"" + file + ".csv\"").build();
        }
        hm.put(QueryConstants.REQ_METHOD, "GET");
        hm.put("query", qfp.getQueryHtml());
        ResultPage model = new ResultPage(res, hm.get(QueryConstants.PAGE_NUMBER), hm, qfp.getTemplate());
        return Response.ok(new Viewable("/resPage.jsp", model)).build();
    }

    @POST
    @Path("/query/table/{file}")
    @JerseyCacheControl()
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getQueryTemplateResultsJsonPost(@HeaderParam("fusekiUrl") final String fuseki, @PathParam("file") String file, HashMap<String, String> map, @Context UriInfo info) throws RestException {
        log.info("Call to getQueryTemplateResultsJsonPost()");
        if (map == null || map.size() == 0) {
            throw new RestException(500, new LdsError(LdsError.MISSING_PARAM_ERR).setContext("in getQueryTemplateResultsJsonPost() : Map =" + map));
        }
        final LdsQuery qfp = LdsQueryService.get(file + ".arq");
        final String query = qfp.getParametizedQuery(map, true);
        if (query.startsWith(QueryConstants.QUERY_ERROR)) {
            return Response.ok(ResponseOutputStream.getJsonResponseStream(query)).build();
        } else {
            Results rp = null;
            ResultSetWrapper res = QueryProcessor.getResults(query, fuseki, map.get(QueryConstants.RESULT_HASH), map.get(QueryConstants.PAGE_SIZE));
            map.put(QueryConstants.RESULT_HASH, Integer.toString(res.getHash()));
            map.put(QueryConstants.PAGE_SIZE, Integer.toString(res.getPageSize()));
            map.put(QueryConstants.REQ_URI, info.getRequestUri().toString().replace(info.getBaseUri().toString(), "/"));

            rp = new Results(res, map);
            return Response.ok(ResponseOutputStream.getJsonResponseStream(rp)).build();
        }
    }

    @GET
    @Path("/query/graph/{file}")
    @JerseyCacheControl()
    public Response getGraphTemplateResults(@Context UriInfo info, @HeaderParam("fusekiUrl") final String fuseki, @QueryParam("format") final String format, @PathParam("file") String file, @Context Request request) throws RestException {
        String path = info.getPath() + info.relativize(info.getRequestUri());
        Variant variant = request.selectVariant(MediaTypeUtils.graphVariants);
        log.info("Call to getGraphTemplateResults() with URL: {}, accept {}, variant {}", path, format, variant);
        if (format == null && variant == null) {
            final ResponseBuilder rb = Response.status(300).entity(Helpers.getMultiChoicesHtml(path, false)).header("Content-Type", "text/html").header("Content-Location", info.getBaseUri() + "choice?path=" + path);
            return setHeaders(rb, getGraphResourceHeaders(path, null, "List")).build();
        }
        // Settings
        HashMap<String, String> hm = Helpers.convertMulti(info.getQueryParameters());

        // process
        final LdsQuery qfp = LdsQueryService.get(file + ".arq");
        final String query = qfp.getParametizedQuery(hm, false);
        // format is prevalent
        MediaType mediaType = MediaTypeUtils.getMimeFromExtension(format);
        if (mediaType == null) {
            mediaType = variant.getMediaType();
        }
        Model model = QueryProcessor.getGraph(query, fuseki, null);
        if (model.size() == 0) {
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(file + " in getGraphTemplateResults()"));
        }
        final String ext = MediaTypeUtils.getExtFromMime(mediaType);
        ResponseBuilder builder = Response.ok(ResponseOutputStream.getModelStream(model, ext), mediaType);
        return setHeaders(builder, getGraphResourceHeaders(path, ext, "Choice")).build();
    }

    @POST
    @Path("/query/graph/{file}")
    @JerseyCacheControl()
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getGraphTemplateResultsPost(@HeaderParam("fusekiUrl") final String fuseki, @DefaultValue("application/ld+json") @HeaderParam("Accept") String accept, @PathParam("file") String file, @Context UriInfo info, @Context Request request,
            HashMap<String, String> map) throws RestException {
        final Variant variant = request.selectVariant(MediaTypeUtils.graphVariants);
        log.info("Call to getGraphTemplateResultsPost() with file: {}, accept: {}, variant: {}, map: {}", file, accept, variant, map);
        if (variant == null) {
            throw new RestException(406, new LdsError(LdsError.NO_ACCEPT_ERR).setContext(file + " in getGraphTemplateResultsPost()"));
        }
        // process
        final LdsQuery qfp = LdsQueryService.get(file + ".arq");
        final String query = qfp.getParametizedQuery(map, false);
        // format is prevalent
        MediaType mediaType = MediaTypeUtils.getMimeFromExtension(accept);
        if (mediaType == null) {
            mediaType = variant.getMediaType();
        }
        Model model = QueryProcessor.getGraph(query, fuseki, null);
        if (model.size() == 0) {
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(file + " in getGraphTemplateResultsPost()"));
        }
        return Response.ok(ResponseOutputStream.getModelStream(model, MediaTypeUtils.getExtFromMime(mediaType)), mediaType).build();
    }

    @POST
    @Path("/callbacks/github/lds-queries")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateQueries() throws RestException {
        log.info("updating query templates >>");
        Thread t = new Thread(new GitService());
        t.start();
        Prefixes.loadPrefixes();
        LdsQueryService.clearCache();
        ResultsCache.clearCache();
        return Response.ok().build();
    }

    @POST
    @Path("/clearcache")
    public String clearCache() throws RestException {
        log.info("clearing cache >>");
        if (ResultsCache.clearCache()) {
            return "OK";
        } else {
            return "ERROR";
        }
    }

    private static HashMap<String, String> getGraphResourceHeaders(String url, final String ext, final String tcn) {
        final HashMap<String, MediaType> map = MediaTypeUtils.getResExtensionMimeMap();
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

    private static ResponseBuilder setHeaders(ResponseBuilder builder, HashMap<String, String> headers) {
        for (String key : headers.keySet()) {
            builder.header(key, headers.get(key));
        }
        return builder;
    }

}