package io.bdrc.ldspdi.rest.resources;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.jena.rdf.model.Model;
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

import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;
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

    @GetMapping(value = "/resource-nc/auth/{res}")
    public ResponseEntity<StreamingResponseBody> getAuthResource(@PathVariable("res") final String res) throws RestException {
        log.info("Call getAuthResource()");
        String query = "describe <http://purl.bdrc.io/resource-auth/" + res + ">";
        Model m = QueryProcessor.getGraphFromModel(query, QueryProcessor.getAuthGraph(null, "authDataGraph"));
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
        // if (ServiceConfig.useAuth()) {
        System.out.println("updating Auth data model() >>");
        log.info("updating Auth data model() >>");
        Thread t = new Thread(new RdfAuthModel());
        t.start();
        return ResponseEntity.ok("Auth Model was updated");
        // }
        // return Response.ok("Auth usage is disabled").build();
    }

}
