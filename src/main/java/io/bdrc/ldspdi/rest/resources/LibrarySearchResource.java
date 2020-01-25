package io.bdrc.ldspdi.rest.resources;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.bdrc.ldspdi.exceptions.ErrorMessage;
import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.rest.features.SpringCacheControl;
import io.bdrc.ldspdi.results.library.TypeResults;
import io.bdrc.ldspdi.results.library.WorkResults;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.AsyncSparql;
import io.bdrc.ldspdi.sparql.LdsQuery;
import io.bdrc.ldspdi.sparql.LdsQueryService;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.ldspdi.utils.Watcher;
import io.bdrc.libraries.StreamingHelpers;

@RestController
@RequestMapping("/")
public class LibrarySearchResource {

    public final static Logger log = LoggerFactory.getLogger(LibrarySearchResource.class);
    public String fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);

    @GetMapping(value = "/lib/{file}", produces = MediaType.APPLICATION_JSON_VALUE)
    @SpringCacheControl()
    public ResponseEntity<StreamingResponseBody> getLibGraphGet(HttpServletRequest request, @RequestHeader(value = "fusekiUrl", required = false) final String fuseki, @PathVariable("file") String file) throws RestException {

        log.info("Call to getLibGraphGet() with template name >> " + file);
        HashMap<String, String> map = Helpers.convertMulti(request.getParameterMap());
        Thread t = null;
        AsyncSparql async = null;
        if (file.equals("rootSearchGraph")) {
            async = new AsyncSparql(fusekiUrl, "Etexts_count.arq", map);
            t = new Thread(async);
            t.run();
        }
        final LdsQuery qfp = LdsQueryService.get(file + ".arq", "library");
        final String query = qfp.getParametizedQuery(map);
        log.debug("Call to getLibGraphGet() with query >> " + query);
        final Model model = QueryProcessor.getGraph(query, fusekiUrl, null);
        Map<String, Object> res = null;
        switch (file) {
        case "workFacetGraph":
        case "associatedWorks":
            res = WorkResults.getResultsMap(model);
            break;
        case "workInstancesGraph":
        case "personGraph":
        case "associatedPersons":
        case "associatedSimpleTypes":
        case "associatedPlaces":
        case "placeGraph":
        case "typeSimpleGraph":
        case "chunksFacetGraph":
        case "imLuckyAssociatedGraph":
        case "imLuckySearchGraph":
        case "etextContentFacetGraph":
            res = TypeResults.getResultsMap(model);
            break;
        default:
            LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext("unknown query "+file);
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(StreamingHelpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));

        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(StreamingHelpers.getJsonObjectStream(res));
    }

}
