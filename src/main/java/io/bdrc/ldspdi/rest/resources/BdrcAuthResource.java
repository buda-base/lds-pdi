package io.bdrc.ldspdi.rest.resources;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.bdrc.auth.Access;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.ldspdi.exceptions.ErrorMessage;
import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.results.ResultsCache;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.libraries.BudaMediaTypes;
import io.bdrc.libraries.Prefixes;
import io.bdrc.libraries.StreamingHelpers;

@RestController
@RequestMapping("/")
public class BdrcAuthResource {

    public final static Logger log = LoggerFactory.getLogger(BdrcAuthResource.class);
    public static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";
    public static final String BDU_PFX = "http://purl.bdrc.io/resource-nc/user/";
    public static final String PRIVATE_PFX = "http://purl.bdrc.io/graph-nc/user-private/";
    public static final String PUBLIC_PFX = "http://purl.bdrc.io/graph-nc/user/";

    @GetMapping(value = "/auth/details", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getUsers() {
        log.info("Call getUsers()");
        ModelAndView model = new ModelAndView();
        model.setViewName("authDetails");
        return model;
    }

    @GetMapping(value = "/resource-nc/user/{res}")
    public ResponseEntity<StreamingResponseBody> userResource(@PathVariable("res") final String res, HttpServletResponse response, HttpServletRequest request) throws IOException, RestException {
        log.info("Call userResource()");
        String token = getToken(request.getHeader("Authorization"));
        if (token == null) {
            return ResponseEntity.status(200).body(StreamingHelpers.getModelStream(getUserModelFromUserId(false, res), "jsonld"));
        } else {
            Access acc = (Access) request.getAttribute("access");
            // auth0Id corresponding to the logged on user - from the token
            String auth0Id = acc.getUser().getAuthId();
            log.info("User in userResource() >> {}", acc.getUser());
            auth0Id = acc.getUser().getUserId();
            Resource usr = getRdfProfile(auth0Id);
            if (usr == null) {
                return ResponseEntity.status(404).body(StreamingHelpers.getStream("No user was found with resource Id=" + res));
            }
            // auth0Id corresponding to the requested userId - from the path variable
            String n = getAuth0IdFromUserId(res).asResource().getURI();
            n = n.substring(n.lastIndexOf("/") + 1);
            if (acc.getUser().isAdmin() || auth0Id.equals(n)) {
                return ResponseEntity.status(200).body(StreamingHelpers.getModelStream(getUserModel(true, getRdfProfile(n)), "jsonld"));
            }
            return ResponseEntity.status(200).body(StreamingHelpers.getModelStream(getUserModel(false, getRdfProfile(n)), "jsonld"));
        }
    }

    @GetMapping(value = "/resource-nc/auth/{res}")
    public ResponseEntity<StreamingResponseBody> getAuthResource(@PathVariable("res") final String res) throws RestException {
        log.info("Call getAuthResource()");
        String query = "describe <http://purl.bdrc.io/resource-nc/auth/" + res + ">";
        Model m = QueryProcessor.getGraphFromModel(query, QueryProcessor.getAuthGraph(null, "authDataGraph"));
        m.setNsPrefixes(Prefixes.getPrefixMapping());
        if (m.size() == 0) {
            LdsError lds = new LdsError(LdsError.MISSING_RES_ERR).setContext(res);
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(StreamingHelpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
        }
        Calendar cal = Calendar.getInstance();
        if (ServiceConfig.useAuth()) {
            cal.setTimeInMillis(RdfAuthModel.getUpdated());
        }
        SimpleDateFormat formatter = new SimpleDateFormat(PATTERN_ASCTIME, Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        return ResponseEntity.ok().header("Last-Modified", formatter.format(cal.getTime())).contentType(BudaMediaTypes.getMimeFromExtension("ttl")).body(StreamingHelpers.getModelStream(m, "ttl"));
    }

    @GetMapping(value = "/authmodel")
    public ResponseEntity<StreamingResponseBody> getAuthModel() throws RestException {
        log.info("Call to getAuthModel()");
        Calendar cal = Calendar.getInstance();
        if (ServiceConfig.useAuth()) {
            cal.setTimeInMillis(RdfAuthModel.getUpdated());
        }
        SimpleDateFormat formatter = new SimpleDateFormat(PATTERN_ASCTIME, Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        return ResponseEntity.ok().header("Last-Modified", formatter.format(cal.getTime())).contentType(BudaMediaTypes.getMimeFromExtension("ttl")).body(StreamingHelpers.getModelStream(QueryProcessor.getAuthGraph(null, "authDataGraph"), null));
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

    @GetMapping(value = "/callbacks/github/bdrc-auth")
    public ResponseEntity<String> updateAuthModel() throws RestException {
        log.info("updating Auth data model() >>");
        Thread t = new Thread(new RdfAuthModel());
        t.start();
        return ResponseEntity.ok("Auth Model was updated");
    }

    private Resource getRdfProfile(String auth0Id) throws IOException {
        Resource r = (Resource) ResultsCache.getObjectFromCache(auth0Id.hashCode());
        if (r != null) {
            return r;
        }
        String query = "select distinct ?s where  {  ?s <http://purl.bdrc.io/ontology/ext/user/hasUserProfile> <http://purl.bdrc.io/resource-nc/auth/" + auth0Id + "> }";
        log.info("QUERY >> {} and service: {} ", query, ServiceConfig.getProperty("fusekiAuthData") + "query");
        QueryExecution qe = QueryProcessor.getResultSet(query, ServiceConfig.getProperty("fusekiAuthData") + "query");
        ResultSet rs = qe.execSelect();
        if (rs.hasNext()) {
            r = rs.next().getResource("?s");
            log.info("RESOURCE >> {} and rdfId= {} ", r);
            ResultsCache.addToCache(r, auth0Id.hashCode());
            return r;
        }
        qe.close();
        return null;
    }

    public static RDFNode getAuth0IdFromUserId(String userId) throws IOException {
        String query = "select distinct ?o where  {  <" + BDU_PFX + userId + "> <http://purl.bdrc.io/ontology/ext/user/hasUserProfile> ?o }";
        log.info("QUERY >> {} and service: {} ", query, ServiceConfig.getProperty("fusekiAuthData") + "query");
        QueryExecution qe = QueryProcessor.getResultSet(query, ServiceConfig.getProperty("fusekiAuthData") + "query");
        ResultSet rs = qe.execSelect();
        if (rs.hasNext()) {
            Resource r = rs.next().getResource("?o");
            log.info("RESOURCE >> {} and rdfId= {} ", r);
            return r;
        }
        return null;
    }

    public static Model getUserModel(boolean full, Resource r) throws IOException {
        if (r == null) {
            return null;
        }
        RDFConnectionRemoteBuilder builder = RDFConnectionFuseki.create().destination(ServiceConfig.getProperty("fusekiAuthData"));
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
        RDFConnectionRemoteBuilder builder = RDFConnectionFuseki.create().destination(ServiceConfig.getProperty("fusekiAuthData"));
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
