package io.bdrc.ldspdi.rest.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.vocabulary.SKOS;
import org.apache.logging.log4j.util.Strings;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.service.ServiceConfig;

@RestController
@RequestMapping("/")
public class ESController {
    
    public static final Logger logger = LoggerFactory.getLogger(ESController.class);
    
    public static Cache<String, Map<String,String>> CACHE;
    
    public static void initCache() {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
        cacheManager.init();
        CACHE = cacheManager.createCache("oslabelscache", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, (Class<Map<String, String>>)(Class<?>)HashMap.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(3000, EntryUnit.ENTRIES)));
    }
    
    static {
        initCache();
    }
    
    static final void add_to_res_fun(final QuerySolution qs, final Map<String,Map<String,String>> res) {
        final String eqname = "bdr:"+qs.getResource("?e").getLocalName();
        final Literal label = qs.getLiteral("?l");
        final Map<String,String> elabels = res.computeIfAbsent(eqname, k -> new HashMap<>());
        elabels.put(label.getLanguage(), label.getString());
    }
    
    // takes a list of ids and return a map where the ids are the keys and the labels are the values (in the form of a Map<String,String>
    public static void getLabelsInto(List<String> idList, Map<String,Map<String,String>> res) {
        if (idList.size() < 1)
            return;
        final String squery = "prefix bdr:   <http://purl.bdrc.io/resource/> \nselect ?e ?l { VALUES ?e { " + Strings.join(idList, ' ') + " } ?e <"+SKOS.prefLabel.getURI()+"> ?l }";
        final Map<String,Map<String,String>> tmpres = new HashMap<>();
        final Consumer<QuerySolution> add_to_cache = x -> add_to_res_fun(x, tmpres);
        try {
            RDFConnection conn = RDFConnection.connect(ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL));
            conn.querySelect(squery, add_to_cache);
        } catch (Exception ex) {
            logger.error("cannot run "+squery, ex);
            return;
        }
        for (final Entry<String,Map<String,String>> e : tmpres.entrySet()) {
            CACHE.put(e.getKey(), e.getValue());
            res.put(e.getKey(), e.getValue());
        }
    }
    
    public static Map<String,Map<String,String>> getCachedLabels(List<String> idList) {
        final List<String> idMissingLabels = new ArrayList<>();
        final Map<String,Map<String,String>> res = new HashMap<>();
        for (final String id : idList) {
            final String normalizedId = id.startsWith("bdr:") ? id : "bdr:"+id;
            final Map<String,String> inCache = CACHE.get(normalizedId);
            if (inCache != null)
                res.put(normalizedId, inCache);
            else
                idMissingLabels.add(normalizedId);
        }
        getLabelsInto(idMissingLabels, res);
        return res;
    }
    
    @PostMapping(value = "osquery_utils/getLabels", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String,Map<String,String>>> LabelMappingsController(@RequestBody List<String> idList) throws JsonProcessingException, RestException {
        Map<String,Map<String,String>> labelsForIds = getCachedLabels(idList);
        return ResponseEntity.ok().header("Allow", "GET, OPTIONS, HEAD").contentType(MediaType.APPLICATION_JSON).body(labelsForIds);
    }
}
