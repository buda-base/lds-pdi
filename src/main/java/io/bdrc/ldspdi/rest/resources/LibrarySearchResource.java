package io.bdrc.ldspdi.rest.resources;

import java.util.HashMap;

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
import io.bdrc.ldspdi.results.library.ChunksResults;
import io.bdrc.ldspdi.results.library.EtextResults;
import io.bdrc.ldspdi.results.library.PersonAllResults;
import io.bdrc.ldspdi.results.library.PersonResults;
import io.bdrc.ldspdi.results.library.PlaceAllResults;
import io.bdrc.ldspdi.results.library.ResourceResults;
import io.bdrc.ldspdi.results.library.RootResults;
import io.bdrc.ldspdi.results.library.TopicAllResults;
import io.bdrc.ldspdi.results.library.WorkAllResults;
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

    @PostMapping(value = "/lib/{file}", produces = MediaType.APPLICATION_JSON_VALUE)
    @SpringCacheControl()
    public ResponseEntity<StreamingResponseBody> getLibGraphPost(@RequestHeader(value = "fusekiUrl", required = false) final String fuseki, @PathVariable("file") final String file, @RequestBody HashMap<String, String> map) throws RestException {
        log.info("Call to getLibGraphPost() with template name >> " + file);
        Thread t = null;
        AsyncSparql async = null;
        if (file.equals("rootSearchGraph")) {
            async = new AsyncSparql(fusekiUrl, "Etexts_count.arq", map);
            t = new Thread(async);
            t.run();
        }
        final LdsQuery qfp = LdsQueryService.get(file + ".arq", "library");
        final String query = qfp.getParametizedQuery(map);
        long deb = System.currentTimeMillis();
        final Model model = QueryProcessor.getGraph(query, fusekiUrl, null);
        long end = System.currentTimeMillis();
        new Watcher(end - deb, query, file).run();
        HashMap<String, Object> res = null;
        switch (file) {
        case "rootSearchGraph":
            log.info("MAP >>> " + map);
            int etext_count = 0;
            if (t != null) {
                try {
                    t.join();
                    ResultSet rs = async.getRes();
                    etext_count = rs.next().getLiteral("?c").getInt();
                } catch (InterruptedException e) {
                    throw new RestException(500, new LdsError(LdsError.ASYNC_ERR).setContext("getLibGraphPost()", e));
                }
            }
            res = RootResults.getResultsMap(model, etext_count);
            break;
        case "personFacetGraph":
            res = PersonResults.getResultsMap(model);
            break;
        case "workFacetGraph":
        case "workAllAssociations":
            res = WorkAllResults.getResultsMap(model);
            break;
        case "allAssocResource":
            res = ResourceResults.getResultsMap(model);
            break;
        case "personAllAssociations":
            res = PersonAllResults.getResultsMap(model);
            break;
        case "topicAllAssociations":
            res = TopicAllResults.getResultsMap(model);
            break;
        case "placeAllAssociations":
            res = PlaceAllResults.getResultsMap(model);
            break;
        case "chunksFacetGraph":
            res = ChunksResults.getResultsMap(model);
            break;
        case "roleAllAssociations":
            res = ResourceResults.getResultsMap(model);
            break;
        default:
            LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(file);
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(StreamingHelpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(StreamingHelpers.getJsonObjectStream(res));
    }

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
        HashMap<String, Object> res = null;
        switch (file) {
        case "rootSearchGraph":
            int etext_count = 0;
            if (t != null) {
                try {
                    t.join();
                    ResultSet rs = async.getRes();
                    etext_count = rs.next().getLiteral("?c").getInt();
                } catch (InterruptedException e) {
                    throw new RestException(500, new LdsError(LdsError.ASYNC_ERR).setContext("getLibGraphGet()", e));
                }
            }
            res = RootResults.getResultsMap(model, etext_count);
            break;
        case "personFacetGraph":
            res = PersonResults.getResultsMap(model);
            break;
        case "workAllAssociations":
            res = WorkAllResults.getResultsMap(model);
            break;
        case "workFacetGraph":
            res = WorkResults.getResultsMap(model);
            break;
        case "allAssocResource":
            res = ResourceResults.getResultsMap(model);
            break;
        case "personAllAssociations":
            res = PersonAllResults.getResultsMap(model);
            break;
        case "topicAllAssociations":
            res = TopicAllResults.getResultsMap(model);
            break;
        case "placeAllAssociations":
            res = PlaceAllResults.getResultsMap(model);
            break;
        case "etextFacetGraph":
            res = EtextResults.getResultsMap(model);
            break;
        case "chunksByEtextGraph":
            res = EtextResults.getResultsMap(model);
            break;
        case "chunksFacetGraph":
            res = ChunksResults.getResultsMap(model);
            break;
        case "roleAllAssociations":
            res = ResourceResults.getResultsMap(model);
            break;
        default:
            LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(file);
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(StreamingHelpers.getJsonObjectStream((ErrorMessage) ErrorMessage.getErrorMessage(404, lds)));

        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(StreamingHelpers.getJsonObjectStream(res));
    }

}
