package io.bdrc.ldspdi.rest.resources;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.bdrc.auth.Access;
import io.bdrc.auth.model.AuthDataModelBuilder;
import io.bdrc.auth.model.User;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.service.UserDataService;
import io.bdrc.ldspdi.sparql.Prefixes;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.users.BudaUser;
import io.bdrc.ldspdi.users.UserPatches;
import io.bdrc.ldspdi.utils.BudaMediaTypes;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.restapi.exceptions.ErrorMessage;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

@RestController
@RequestMapping("/")
public class BdrcAuthResource {

    public final static Logger log = LoggerFactory.getLogger(BdrcAuthResource.class);
    public static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";
    public String fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);

    @GetMapping(value = "/auth/details", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getUsers() {
        log.info("Call getUsers()");
        ModelAndView model = new ModelAndView();
        model.setViewName("authDetails");
        return model;
    }

    @GetMapping(value = "/resource-nc/user/me")
    public ResponseEntity<StreamingResponseBody> meUser(HttpServletResponse response, HttpServletRequest request) throws IOException, RestException {
        log.info("Call meUser()");
        String token = getToken(request.getHeader("Authorization"));
        if (token == null) {
            return ResponseEntity.status(401).body(Helpers.getStream("No token available"));
        } else {
            Access acc = (Access) request.getAttribute("access");
            log.info("Access >> {}", acc);
            // TODO there should be a function in bdrc-auth-lib that does this
            String auth0Id = acc.getUser().getAuthId();
            auth0Id = auth0Id.substring(auth0Id.indexOf("|") + 1);
            Resource usr = BudaUser.getRdfProfile(auth0Id);
            if (usr == null) {
                UserDataService.addNewBudaUser(acc.getUser());
                usr = BudaUser.getRdfProfile(auth0Id);
                log.info("User Resource >> {}", usr);
            }
            return ResponseEntity.status(200).body(Helpers.getModelStream(BudaUser.getUserModel(true, usr), "jsonld"));
        }
    }

    @GetMapping(value = "/resource-nc/user/{res}")
    public ResponseEntity<StreamingResponseBody> userResource(@PathVariable("res") final String res, HttpServletResponse response, HttpServletRequest request) throws IOException, RestException {
        log.info("Call userResource()");
        String token = getToken(request.getHeader("Authorization"));
        if (token == null) {
            return ResponseEntity.status(200).body(Helpers.getModelStream(BudaUser.getUserModelFromUserId(false, res), "jsonld"));
        } else {
            Access acc = (Access) request.getAttribute("access");
            BudaUser.createBudaUserModels(acc.getUser());
            // auth0Id corresponding to the logged on user - from the token
            String auth0Id = acc.getUser().getAuthId();
            auth0Id = auth0Id.substring(auth0Id.indexOf("|") + 1);
            Resource usr = BudaUser.getRdfProfile(auth0Id);
            if (usr == null) {
                UserDataService.addNewBudaUser(acc.getUser());
                usr = BudaUser.getRdfProfile(auth0Id);
                log.info("User Resource >> {}", usr);
            }
            // auth0Id corresponding to the requested userId - from the path variable
            String n = BudaUser.getAuth0IdFromUserId(res).asResource().getURI();
            n = n.substring(n.lastIndexOf("/") + 1);
            if (acc.getUser().isAdmin() || auth0Id.equals(n)) {
                return ResponseEntity.status(200).body(Helpers.getModelStream(BudaUser.getUserModel(true, BudaUser.getRdfProfile(n)), "jsonld"));
            }
            return ResponseEntity.status(200).body(Helpers.getModelStream(BudaUser.getUserModel(false, BudaUser.getRdfProfile(n)), "jsonld"));
        }
    }

    @DeleteMapping(value = "/resource-nc/user/{res}")
    public ResponseEntity<StreamingResponseBody> userDelete(@PathVariable("res") final String res, HttpServletResponse response, HttpServletRequest request) throws Exception {
        log.info("Call userResource()");
        String token = getToken(request.getHeader("Authorization"));
        if (token == null) {
            return ResponseEntity.status(403).body(Helpers.getStream("You must be authenticated in order to disable this user"));
        } else {
            Access acc = (Access) request.getAttribute("access");
            if (acc.getUser().isAdmin()) {
                String auth0Id = BudaUser.getAuth0IdFromUserId(res).asNode().getURI();
                User usr = RdfAuthModel.getUser(auth0Id.substring(auth0Id.lastIndexOf("/") + 1));
                String auth0FullId = usr.getAuthId();
                // first update the Buda User rdf profile
                BudaUser.update(res, UserPatches.getSetActivePatch(res, false));
                // next, mark (patch) the corresponding Auth0 user as "blocked'
                AuthDataModelBuilder.patchUser(auth0FullId, "{\"blocked\"=true}");
            }
            BudaUser.createBudaUserModels(acc.getUser());
            // auth0Id corresponding to the logged on user - from the token
            String auth0Id = acc.getUser().getUserId();
            Resource usr = BudaUser.getRdfProfile(auth0Id);
            if (usr == null) {
                UserDataService.addNewBudaUser(acc.getUser());
                usr = BudaUser.getRdfProfile(auth0Id);
                log.info("User Resource >> {}", usr);
            }
            // auth0Id corresponding to the requested userId - from the path variable
            String n = BudaUser.getAuth0IdFromUserId(res).asResource().getURI();
            n = n.substring(n.lastIndexOf("/") + 1);
            if (acc.getUser().isAdmin()) {
                return ResponseEntity.status(200).body(Helpers.getModelStream(BudaUser.getUserModel(true, BudaUser.getRdfProfile(n)), "jsonld"));
            }
            return ResponseEntity.status(200).body(Helpers.getModelStream(BudaUser.getUserModel(false, BudaUser.getRdfProfile(n)), "jsonld"));
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
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(Helpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
        }
        Calendar cal = Calendar.getInstance();
        if (ServiceConfig.useAuth()) {
            cal.setTimeInMillis(RdfAuthModel.getUpdated());
        }
        SimpleDateFormat formatter = new SimpleDateFormat(PATTERN_ASCTIME, Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        return ResponseEntity.ok().header("Last-Modified", formatter.format(cal.getTime())).contentType(BudaMediaTypes.getMimeFromExtension("ttl")).body(Helpers.getModelStream(m, "ttl"));
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
        return ResponseEntity.ok().header("Last-Modified", formatter.format(cal.getTime())).contentType(BudaMediaTypes.getMimeFromExtension("ttl")).body(Helpers.getModelStream(QueryProcessor.getAuthGraph(fusekiUrl, "authDataGraph")));
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

    public static String getToken(final String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7);
    }

}
