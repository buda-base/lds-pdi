package io.bdrc.ldspdi.rest.controllers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.bdrc.auth.AccessInfoAuthImpl;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.ldspdi.exceptions.ErrorMessage;
import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.results.ResultPage;
import io.bdrc.ldspdi.results.ResultSetWrapper;
import io.bdrc.ldspdi.results.Results;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.LdsQuery;
import io.bdrc.ldspdi.sparql.LdsQueryService;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.libraries.BudaMediaTypes;
import io.bdrc.libraries.GlobalHelpers;
import io.bdrc.libraries.StreamingHelpers;

@RestController
@RequestMapping("/")
public class BdrcAuthController {

    public final static Logger log = LoggerFactory.getLogger(BdrcAuthController.class);
    public static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";
    public static final String BDU_PFX = "http://purl.bdrc.io/resource-nc/user/";
    public static final String PRIVATE_PFX = "http://purl.bdrc.io/graph-nc/user-private/";
    public static final String PUBLIC_PFX = "http://purl.bdrc.io/graph-nc/user/";
    public static boolean adminView = "true".equalsIgnoreCase(ServiceConfig.getProperty("exposeAuthAdminAPI"));

    @GetMapping(value = "/auth/details", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getUsers() {
        ModelAndView model = new ModelAndView();
        if (adminView) {
            log.info("Call getUsers()");
            model.setViewName("authDetails");
        } else {
            model.setViewName("authDisabled");
        }
        return model;
    }

    @GetMapping(value = "/resource-nc/user/{res}")
    public ResponseEntity<StreamingResponseBody> userResource(@PathVariable("res") final String res,
            HttpServletResponse response, HttpServletRequest request) throws IOException, RestException {
        log.info("Call userResource()");
        Helpers.setCacheControl(response, "public");
        try {
            String token = getToken(request.getHeader("Authorization"));
            if (token == null) {
                return ResponseEntity.status(200).body(StreamingHelpers.getModelStream(
                        getUserModelFromUserId(false, res), "jsonld", ServiceConfig.PREFIX.getPrefixMap()));
            } else {
                AccessInfoAuthImpl acc = (AccessInfoAuthImpl) request.getAttribute("access");
                // auth0Id corresponding to the logged on user - from the token
                log.info("User in userResource() >> {}", acc.getUser());
                String auth0Id = acc.getUser().getUserId();
                Resource usr = getRdfProfile(auth0Id);
                if (usr == null) {
                    return ResponseEntity.status(404)
                            .body(StreamingHelpers.getStream("No user was found with resource Id=" + res));
                }
                // auth0Id corresponding to the requested userId - from the path
                // variable
                String n = getAuth0IdFromUserId(res).asResource().getURI();
                n = n.substring(n.lastIndexOf("/") + 1);
                if (acc.getUserProfile().isAdmin() || auth0Id.equals(n)) {
                    return ResponseEntity.status(200).body(StreamingHelpers.getModelStream(
                            getUserModel(true, getRdfProfile(n)), "jsonld", ServiceConfig.PREFIX.getPrefixMap()));
                }
                if (!adminView) {
                    return ResponseEntity.status(200).body(StreamingHelpers.getStream("Auth Admin API is disabled"));
                }
                return ResponseEntity.status(200).body(StreamingHelpers.getModelStream(
                        getUserModel(false, getRdfProfile(n)), "jsonld", ServiceConfig.PREFIX.getPrefixMap()));
            }
        } catch (Exception e) {
            log.error("Call userResource() failed", e);
            return ResponseEntity.status(404).body(StreamingHelpers
                    .getStream("Could not find the requested resource " + res + " Exception is:" + e.getMessage()));
        }
    }

    @GetMapping(value = "/resource-nc/users")
    public Object getAllUsers(HttpServletResponse response, HttpServletRequest request)
            throws IOException, RestException {
        ModelAndView model = new ModelAndView();
        if (!adminView) {
            model.setViewName("authDisabled");
            return model;
        }
        Helpers.setCacheControl(response, "private");
        try {
            log.info("Call to getAllUsers()");
            HashMap<String, String> hm = Helpers.convertMulti(request.getParameterMap());
            String pageSize = hm.get(QueryConstants.PAGE_SIZE);
            String pageNumber = hm.get(QueryConstants.PAGE_NUMBER);
            if (pageNumber == null) {
                pageNumber = "1";
            }
            hm.put(QueryConstants.REQ_URI, request.getRequestURL().toString() + "?" + request.getQueryString());
            hm.put(QueryConstants.REQ_METHOD, "GET");
            final LdsQuery qfp = LdsQueryService.get("budaUsers.arq", "private");
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

            final String query = qfp.getParametizedQuery(hm, false);
            if (query.startsWith(QueryConstants.QUERY_ERROR)) {
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                        .body(StreamingHelpers.getStream(query));
            }
            log.info("Parametized Query >> : {}", query);
            log.debug("PARAMS MAP >> : {}", hm);
            if (query.startsWith(QueryConstants.QUERY_ERROR)) {
                throw new RestException(500,
                        new LdsError(LdsError.SPARQL_ERR).setContext(" in getQueryTemplateResults() " + query));
            }
            String fmt = hm.get(QueryConstants.FORMAT);
            if ("xml".equals(fmt)) {
                ResultSet rs = QueryProcessor.getResults(query, ServiceConfig.getProperty("fusekiAuthData") + "query");
                response.setContentType("text/html");
                return ResultSetFormatter.asXMLString(rs);
            }
            ResultSetWrapper res = QueryProcessor.getResults(query,
                    ServiceConfig.getProperty("fusekiAuthData") + "query", hm.get(QueryConstants.RESULT_HASH),
                    hm.get(QueryConstants.PAGE_SIZE));
            if ("json".equals(fmt)) {
                Results r = new Results(res, hm);
                byte[] buff = GlobalHelpers.getJsonBytes(r);
                return (ResponseEntity<InputStreamResource>) ResponseEntity.ok().contentLength(buff.length)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Content-Disposition", "attachment; filename=\"budaUsers.json\"")
                        .body(new InputStreamResource(new ByteArrayInputStream(buff)));
            }
            if ("csv".equals(fmt)) {
                byte[] buff = res.getCsvAsBytes(hm, true);
                return (ResponseEntity<InputStreamResource>) ResponseEntity.ok().contentLength(buff.length)
                        .contentType(BudaMediaTypes.MT_CSV)
                        .header("Content-Disposition", "attachment; filename=\"budaUsers_p" + pageNumber + ".csv\"")
                        .body(new InputStreamResource(new ByteArrayInputStream(buff)));

            }
            if ("csv_f".equals(fmt)) {
                byte[] buff = res.getCsvAsBytes(hm, false);
                return (ResponseEntity<InputStreamResource>) ResponseEntity.ok().contentLength(buff.length)
                        .contentType(BudaMediaTypes.MT_CSV)
                        .header("Content-Disposition", "attachment; filename=\"budaUsers_p" + pageNumber + ".csv\"")
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
            RestException re = new RestException(500, LdsError.UNKNOWN_ERR, e.getClass().getName(), baos.toString(),
                    "");
            try {
                baos.close();
            } catch (IOException e1) {
                throw new RestException(500, LdsError.UNKNOWN_ERR, e.getClass().getName(),
                        "Failed to close exception trace byte output stream", "");
            }
            throw re;
        }
        return (ModelAndView) model;
    }

    @GetMapping(value = "/resource-nc/userSearch")
    public Object getUsers(HttpServletResponse response, HttpServletRequest request) throws IOException, RestException {
        Helpers.setCacheControl(response, "private");
        ModelAndView model = new ModelAndView();
        if (!adminView) {
            model.setViewName("authDisabled");
            return model;
        }
        try {
            log.info("Call to getAllUsers()");
            HashMap<String, String> hm = Helpers.convertMulti(request.getParameterMap());
            String pageSize = hm.get(QueryConstants.PAGE_SIZE);
            String pageNumber = hm.get(QueryConstants.PAGE_NUMBER);
            if (pageNumber == null) {
                pageNumber = "1";
            }
            hm.put(QueryConstants.REQ_URI, request.getRequestURL().toString() + "?" + request.getQueryString());
            hm.put(QueryConstants.REQ_METHOD, "GET");
            final LdsQuery qfp = LdsQueryService.get("budaUserSearch.arq", "private");
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

            final String query = qfp.getParametizedQuery(hm, false);
            if (query.startsWith(QueryConstants.QUERY_ERROR)) {
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                        .body(StreamingHelpers.getStream(query));
            }
            log.info("Parametized Query >> : {}", query);
            log.debug("PARAMS MAP >> : {}", hm);
            if (query.startsWith(QueryConstants.QUERY_ERROR)) {
                throw new RestException(500,
                        new LdsError(LdsError.SPARQL_ERR).setContext(" in getQueryTemplateResults() " + query));
            }
            String fmt = hm.get(QueryConstants.FORMAT);
            if ("xml".equals(fmt)) {
                ResultSet rs = QueryProcessor.getResults(query, ServiceConfig.getProperty("fusekiAuthData") + "query");
                response.setContentType("text/html");
                return ResultSetFormatter.asXMLString(rs);
            }
            ResultSetWrapper res = QueryProcessor.getResults(query,
                    ServiceConfig.getProperty("fusekiAuthData") + "query", hm.get(QueryConstants.RESULT_HASH),
                    hm.get(QueryConstants.PAGE_SIZE));
            if ("json".equals(fmt)) {
                Results r = new Results(res, hm);
                byte[] buff = GlobalHelpers.getJsonBytes(r);
                return (ResponseEntity<InputStreamResource>) ResponseEntity.ok().contentLength(buff.length)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Content-Disposition", "attachment; filename=\"budaUsers.json\"")
                        .body(new InputStreamResource(new ByteArrayInputStream(buff)));
            }
            if ("csv".equals(fmt)) {
                byte[] buff = res.getCsvAsBytes(hm, true);
                return (ResponseEntity<InputStreamResource>) ResponseEntity.ok().contentLength(buff.length)
                        .contentType(BudaMediaTypes.MT_CSV)
                        .header("Content-Disposition", "attachment; filename=\"budaUsers_p" + pageNumber + ".csv\"")
                        .body(new InputStreamResource(new ByteArrayInputStream(buff)));

            }
            if ("csv_f".equals(fmt)) {
                byte[] buff = res.getCsvAsBytes(hm, false);
                return (ResponseEntity<InputStreamResource>) ResponseEntity.ok().contentLength(buff.length)
                        .contentType(BudaMediaTypes.MT_CSV)
                        .header("Content-Disposition", "attachment; filename=\"budaUsers_p" + pageNumber + ".csv\"")
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
            RestException re = new RestException(500, LdsError.UNKNOWN_ERR, e.getClass().getName(), baos.toString(),
                    "");
            try {
                baos.close();
            } catch (IOException e1) {
                throw new RestException(500, LdsError.UNKNOWN_ERR, e.getClass().getName(),
                        "Failed to close exception trace byte output stream", "");
            }
            throw re;
        }
        return (ModelAndView) model;
    }

    @GetMapping(value = "/resource-nc/auth/{res}")
    public ResponseEntity<StreamingResponseBody> getAuthResource(@PathVariable("res") final String res)
            throws RestException {
        log.info("Call getAuthResource()");
        if (!adminView) {
            return ResponseEntity.status(200).body(StreamingHelpers.getStream("Auth Admin API is disabled"));
        }
        String query = "describe <http://purl.bdrc.io/resource-nc/auth/" + res + ">";
        Model m = QueryProcessor.getGraphFromModel(query, QueryProcessor.getAuthGraph(null, "authDataGraph"));
        m.setNsPrefixes(ServiceConfig.PREFIX.getPrefixMapping());
        if (m.size() == 0) {
            LdsError lds = new LdsError(LdsError.MISSING_RES_ERR).setContext(res);
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON)
                    .body(StreamingHelpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
        }
        Calendar cal = Calendar.getInstance();
        if (ServiceConfig.useAuth()) {
            Long t = RdfAuthModel.getUpdated();
            if (t == null) {
                t = System.currentTimeMillis();
            }
            cal.setTimeInMillis(t);
        }
        SimpleDateFormat formatter = new SimpleDateFormat(PATTERN_ASCTIME, Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        return ResponseEntity.ok().header("Last-Modified", formatter.format(cal.getTime()))
                .contentType(BudaMediaTypes.getMimeFromExtension("ttl"))
                .body(StreamingHelpers.getModelStream(m, "ttl", ServiceConfig.PREFIX.getPrefixMap()));
    }

    @GetMapping(value = "/authmodel")
    public ResponseEntity<StreamingResponseBody> getAuthModel() throws RestException {
        log.info("Call to getAuthModel()");
        if (!adminView) {
            return ResponseEntity.status(200).body(StreamingHelpers.getStream("Auth Admin API is disabled"));
        }
        Calendar cal = Calendar.getInstance();
        if (ServiceConfig.useAuth()) {
            cal.setTimeInMillis(RdfAuthModel.getUpdated());
        }
        SimpleDateFormat formatter = new SimpleDateFormat(PATTERN_ASCTIME, Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        return ResponseEntity.ok().header("Last-Modified", formatter.format(cal.getTime()))
                .contentType(BudaMediaTypes.getMimeFromExtension("ttl")).body(StreamingHelpers.getModelStream(
                        QueryProcessor.getAuthGraph(null, "authDataGraph"), null, ServiceConfig.PREFIX.getPrefixMap()));
    }

    @GetMapping(value = "/authmodel/updated")
    public long getAuthModelUpdated() {
        log.info("Call to getAuthModelUpdated()");
        if (ServiceConfig.useAuth()) {
            return RdfAuthModel.getUpdated();
        } else {
            return 999999999;
        }
    }

    @PostMapping(value = "/callbacks/github/bdrc-auth")
    public ResponseEntity<String> updateAuthModel() throws RestException, InterruptedException, ExecutionException {
        log.info("updating Auth data model() >>");
        //RdfAuthModel.updateAuthData(null);
        return ResponseEntity.ok("Auth Model is updating");
    }

    @PostMapping(value = "/callbacks/model/bdrc-auth")
    public ResponseEntity<String> readAuthModel() {
        log.info("updating Auth data model() >>");
        RdfAuthModel.readAuthModel();
        return ResponseEntity.ok("Updated auth Model was read");
    }

    private Resource getRdfProfile(String auth0Id) throws IOException {
        Resource r = null;
        String query = "select distinct ?s where  {  ?s <http://purl.bdrc.io/ontology/ext/user/hasUserProfile> <http://purl.bdrc.io/resource-nc/auth/"
                + auth0Id + "> }";
        log.info("QUERY >> {} and service: {} ", query, ServiceConfig.getProperty("fusekiAuthData") + "query");
        QueryExecution qe = QueryProcessor.getResultSet(query, ServiceConfig.getProperty("fusekiAuthData") + "query");
        ResultSet rs = qe.execSelect();
        if (rs.hasNext()) {
            r = rs.next().getResource("?s");
            log.debug("RESOURCE >> {} and rdfId= {} ", r);
            return r;
        }
        qe.close();
        return null;
    }

    public static RDFNode getAuth0IdFromUserId(String userId) throws IOException {
        String query = "select distinct ?o where  {  <" + BDU_PFX + userId
                + "> <http://purl.bdrc.io/ontology/ext/user/hasUserProfile> ?o }";
        log.info("QUERY >> {} and service: {} ", query, ServiceConfig.getProperty("fusekiAuthData") + "query");
        QueryExecution qe = QueryProcessor.getResultSet(query, ServiceConfig.getProperty("fusekiAuthData") + "query");
        ResultSet rs = qe.execSelect();
        if (rs.hasNext()) {
            Resource r = rs.next().getResource("?o");
            log.debug("RESOURCE >> {} and rdfId= {} ", r);
            return r;
        }
        return null;
    }

    public static Model getUserModel(boolean full, Resource r) throws IOException {
        if (r == null) {
            return null;
        }
        RDFConnectionRemoteBuilder builder = RDFConnectionFuseki.create()
                .destination(ServiceConfig.getProperty("fusekiAuthData"));
        RDFConnectionFuseki fusConn = ((RDFConnectionFuseki) builder.build());
        Model mod = ModelFactory.createDefaultModel();
        String rdfId = r.getURI().substring(r.getURI().lastIndexOf("/") + 1);
        mod.add(fusConn.fetch(PUBLIC_PFX + rdfId));
        if (full) {
            mod.add(fusConn.fetch(PRIVATE_PFX + rdfId));
        }
        fusConn.close();
        return mod;
    }

    public static Model getUserModelFromUserId(boolean full, String resId) throws IOException {
        if (resId == null) {
            return null;
        }
        RDFConnectionRemoteBuilder builder = RDFConnectionFuseki.create()
                .destination(ServiceConfig.getProperty("fusekiAuthData"));
        RDFConnectionFuseki fusConn = ((RDFConnectionFuseki) builder.build());
        Model mod = ModelFactory.createDefaultModel();
        mod.add(fusConn.fetch(PUBLIC_PFX + resId));
        if (full) {
            mod.add(fusConn.fetch(PRIVATE_PFX + resId));
        }
        fusConn.close();
        return mod;
    }

    private static String getToken(final String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7);
    }

}
