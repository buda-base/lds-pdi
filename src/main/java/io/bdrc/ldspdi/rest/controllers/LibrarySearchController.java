package io.bdrc.ldspdi.rest.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.bdrc.auth.AccessInfo;
import io.bdrc.auth.AccessInfoAuthImpl;
import io.bdrc.auth.rdf.Subscribers;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.rest.features.CorsFilter;
import io.bdrc.ldspdi.results.library.TypeResults;
import io.bdrc.ldspdi.results.library.WorkResults;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.LdsQuery;
import io.bdrc.ldspdi.sparql.LdsQueryService;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.GeoLocation;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.libraries.BudaMediaTypes;
import io.bdrc.libraries.StreamingHelpers;

@RestController
@RequestMapping("/")
public class LibrarySearchController {

    public final static Logger log = LoggerFactory.getLogger(LibrarySearchController.class);
    public String fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
    public static final String ADM = "http://purl.bdrc.io/ontology/admin/";
    
    public static final void hackForLuceneBug(final HashMap<String, String> argsmap, final String file) {
        if (file.startsWith("etext"))
            return;
        // dirty hack for https://github.com/buda-base/lucene-bo/issues/42
        if (argsmap.containsKey("LG_NAME") && "bo".equals(argsmap.get("LG_NAME"))) {
            String lname = argsmap.get("L_NAME");
            lname = lname.replace("\u0f71", "");
            lname = lname.replace("\u0f75", "\u0f74");
            lname = lname.replace("\u0f4e", "\u0f53");
            lname = lname.replace("\u0f4a", "\u0f4f");
            lname = lname.replace("\u0f4c", "\u0f51");
            lname = lname.replace("\u0f73", "\u0f72");
            lname = lname.replace("\u0f9e", "\u0fa3");
            lname = lname.replace("\u0f9a", "\u0f9f");
            lname = lname.replace("\u0f9c", "\u0fa1");
            argsmap.put("L_NAME", lname);
        }
    }

    @GetMapping(value = "/lib/{file}")
    public ResponseEntity<StreamingResponseBody> getLibGraphGet(HttpServletRequest request, HttpServletResponse response,
            @RequestHeader(value = "fusekiUrl", required = false) final String fuseki, @PathVariable("file") String file,
            @RequestHeader("Accept") String format) throws RestException {
        log.info("Call to getLibGraphGet() with template name >> " + file);
        HashMap<String, String> map = Helpers.convertMulti(request.getParameterMap());
        hackForLuceneBug(map, file);
        final LdsQuery qfp = LdsQueryService.get(file + ".arq", "library");
        if (qfp.getRequiredParams().contains(QueryConstants.RIC)) {
            map.put(QueryConstants.RIC, String.valueOf(GeoLocation.isFromChina(request)));
        }
        if (qfp.getRequiredParams().contains(QueryConstants.SUBSCRIBEDLIST)) {
            final String ipAddress = request.getHeader("X-Real-IP");
            final String subscriberLname = Subscribers.getCachedSubscriber(ipAddress);
            if (subscriberLname == null || subscriberLname.isEmpty()) {
                map.put(QueryConstants.SUBSCRIBEDLIST, "bdr:nil");
            } else {
                List<String> collections = Subscribers.collectionsOfSubscriber(subscriberLname);
                if (collections == null || collections.isEmpty()) {
                    map.put(QueryConstants.SUBSCRIBEDLIST, "bdr:nil");
                } else {
                    map.put(QueryConstants.SUBSCRIBEDLIST, "bdr:"+String.join(",bdr:", collections));
                }
            }
        }
        final String query = qfp.getParametizedQuery(map, true);
        log.debug("Call to getLibGraphGet() with query >> " + query);
        final Model model = QueryProcessor.getGraph(query, fusekiUrl, null);
        Map<String, Object> res = null;
        switch (file) {
        case "ChunksByPage":
        case "Chunks":
            // in these cases we filter out the response according to the restrictions of
            // the etext
            return filteredChunkResponse(request, model, format);
        case "workFacetGraph":
        case "serialWorkFacetGraph":
        case "instanceFacetGraph":
        case "iinstanceFacetGraph":
        case "einstanceFacetGraph":
        case "iinstanceSyncedIn":
        case "dateWorks":
        case "dateInstances":
        case "idWorks":
        case "idInstances":
        case "associatedWorks":
        case "associatedInstances":
        case "associatedIInstances":
        case "associatedEInstances":
        case "instancesCreatedIn":
        case "worksCreatedIn":
        case "worksInCollection":
        case "instancesInCollection":
        case "instancesInCollectionWithProperties":
            res = WorkResults.getResultsMap(model);
            break;
        default:
            res = TypeResults.getResultsMap(model);
            break;
        }
        Helpers.setCacheControl(response, "public");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(StreamingHelpers.getJsonObjectStream(res));
    }

    public ResponseEntity<StreamingResponseBody> filteredChunkResponse(final HttpServletRequest request, final Model m, final String format) {
        // find admin data:
        final ResIterator admDataI = m.listResourcesWithProperty(m.createProperty(ADM, "adminAbout"));
        if (!admDataI.hasNext()) {
            log.info("can't find adminAbout in the results, return 404");
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(StreamingHelpers.getStream("\"Nothing found (adminAbout)\""));
        }
        final Resource admData = admDataI.next();
        final Resource access = admData.getPropertyResourceValue(m.createProperty(ADM, "access"));
        final Resource status = admData.getPropertyResourceValue(m.createProperty(ADM, "status"));
        final Resource einst = admData.getPropertyResourceValue(m.createProperty(ADM, "adminAbout"));
        final Statement ricS = admData.getProperty(m.createProperty(ADM, "restrictedInChina"));
        if (access == null || status == null || ricS == null) {
            log.info("can't find access, status or restrictedInChina in the results, return 404");
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(StreamingHelpers.getStream("\"Nothing found\""));
        }
        boolean ric = ricS.getBoolean();
        if (ric && GeoLocation.isFromChina(request)) {
            return ResponseEntity.status(451).cacheControl(CacheControl.noCache()).contentType(MediaType.APPLICATION_JSON)
                    .body(StreamingHelpers.getStream("\"Etext not available in your geographical area\""));
        }
        // we filter out the triples that are only useful for the filter and not to the
        // actual answer:
        //m.removeAll(admData, null, null);
        if (m.isEmpty())
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON).body(StreamingHelpers.getStream("\"Nothing found (empty)\""));
        AccessInfo acc = (AccessInfo) request.getAttribute("access");
        if (acc == null)
            acc = new AccessInfoAuthImpl();
        final AccessInfoAuthImpl.AccessLevel al = acc.hasResourceAccess(access.getLocalName(), status.getLocalName(), einst.getURI());
        if (al != AccessInfoAuthImpl.AccessLevel.OPEN) {
            return ResponseEntity.status(acc.isLogged() ? 403 : 401).cacheControl(CacheControl.noCache())
                    .contentType(MediaType.APPLICATION_JSON).body(StreamingHelpers.getStream("\"Insufficient rights\""));
        }
        CacheControl cc = CacheControl.maxAge(CorsFilter.ACCESS_CONTROL_MAX_AGE_IN_SECONDS, TimeUnit.SECONDS);
        if (!access.getLocalName().equals("AccessOpen") || ric) {
            cc = cc.cachePrivate();
        } else {
            cc = cc.cachePublic();
        }
        MediaType mediaType = BudaMediaTypes.selectVariant(format, BudaMediaTypes.resVariantsNoHtml);
        if (mediaType == null) {
            mediaType = BudaMediaTypes.MT_TTL;
        }
        final String ext = BudaMediaTypes.getExtFromMime(mediaType);
        return ResponseEntity.status(200).contentType(mediaType).cacheControl(cc)
                .body(StreamingHelpers.getModelStream(m, ext, einst.getURI(), null, ServiceConfig.PREFIX.getPrefixMap()));
    }

}