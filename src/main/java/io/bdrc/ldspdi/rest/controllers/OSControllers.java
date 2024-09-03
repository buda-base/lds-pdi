package io.bdrc.ldspdi.rest.controllers;

import java.io.IOException;
import java.util.Map;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.lucene.search.join.ScoreMode;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.InnerHitBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.bdrc.auth.AccessInfo;
import io.bdrc.auth.AccessInfoAuthImpl;
import io.bdrc.auth.AccessInfo.AccessLevel;
import io.bdrc.auth.model.Endpoint;
import io.bdrc.auth.TokenValidation;
import io.bdrc.auth.UserProfile;
import jakarta.servlet.http.HttpServletRequest;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.utils.GeoLocation;
import io.bdrc.libraries.Models;

@RestController
public class OSControllers {

    @Autowired
    private RestHighLevelClient client;
    
    private static final Logger log = LoggerFactory.getLogger(OSControllers.class);
    public static Cache<String, EtextAccessProps> CACHE_EAP;
    
    static {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
        cacheManager.init();
        CACHE_EAP = cacheManager.createCache("eapcache", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, EtextAccessProps.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(1000, EntryUnit.ENTRIES)));
    }
    
    static final String index_name = ServiceConfig.getProperty("opensearchIndex");
    
    static final class EtextAccessProps {
        final String status_lname;
        final String access_lname;
        final String ei;
        final boolean ric;
        
        EtextAccessProps(final String ei, final String status_lname, final String access_lname, final boolean ric) {
            this.status_lname = status_lname;
            this.access_lname = access_lname;
            this.ric = ric;
            this.ei = ei;
        }
    }
    
    static final Property statusP = ResourceFactory.createProperty(Models.ADM+"status");
    static final Property accessP = ResourceFactory.createProperty(Models.ADM+"access");
    static final Property adminAboutP = ResourceFactory.createProperty(Models.ADM+"adminAbout");
    static final Property restrictedInChinaP = ResourceFactory.createProperty(Models.ADM+"restrictedInChina");
    
    static final EtextAccessProps getAccessProps(final String reslname) {
        // volumeOf
        // eTextInInstance
        final String qstr = "construct { ?ieadm ?ieadmp ?ieadmo } { { <"+Models.BDR+reslname+"> <"+Models.BDO+"volumeOf> ?ie . ?ieadm <"+Models.ADM+"adminAbout> ?ie ; ?ieadmp ?ieadmo . } union { <"+Models.BDR+reslname+"> <"+Models.BDO+"eTextInInstance> ?ie . ?ieadm <"+Models.ADM+"adminAbout> ?ie ; ?ieadmp ?ieadmo . } union { ?ieadm <"+Models.ADM+"adminAbout> <"+Models.BDR+reslname+"> ; ?ieadmp ?ieadmo  } }";
        final Query q = QueryFactory.create(qstr);
        log.debug("QUERY >> {}", qstr);
        final RDFConnection conn = RDFConnectionRemote.create()
                .destination(ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL)).build();
        final Model model = conn.queryConstruct(q);
        String statuslname = "StatusHidden";
        String accesslname = "AccessRestrictedByBDRC";
        boolean ric = true;
        String ei = null;
        NodeIterator ni = model.listObjectsOfProperty(statusP);
        while (ni.hasNext())
            statuslname = ni.next().asResource().getLocalName();
        ni = model.listObjectsOfProperty(accessP);
        while (ni.hasNext())
            accesslname = ni.next().asResource().getLocalName();
        ni = model.listObjectsOfProperty(restrictedInChinaP);
        while (ni.hasNext())
            ric = ni.next().asLiteral().getBoolean();
        ni = model.listObjectsOfProperty(adminAboutP);
        while (ni.hasNext())
            ei = ni.next().asResource().getLocalName();
        return new EtextAccessProps(ei, statuslname, accesslname, ric);
    }
    
    static final EtextAccessProps getAccessPropsCached(final String reslname) {
        EtextAccessProps value = CACHE_EAP.get(reslname);
        if (value == null) {
            value = getAccessProps(reslname);
            CACHE_EAP.put(reslname, value);
        }
        return value;
    }
    
    static final AccessInfo getAccessInfo(final String reslname, final HttpServletRequest request) {
        String method = ((HttpServletRequest) request).getMethod();
        UserProfile prof = null;
        if (ServiceConfig.useAuth() && !method.equalsIgnoreCase("OPTIONS")) {
            log.info("IIIF SERVER IS USING AUTH !");
            String token = getToken(((HttpServletRequest) request).getHeader("Authorization"));
            log.debug("TOKEN >> {}", token);
            if (token != null) {
                // User is logged in
                // Getting his profile
                final TokenValidation validation = new TokenValidation(token);
                if (!validation.isValid()) {
                    log.error("invalid token: {}", token);
                    token = null;
                } else {
                    prof = validation.getUser();
                    log.info("validation is {}", validation);
                    log.info("profile is {}", prof);
                }
            }
        }
        return new AccessInfoAuthImpl(prof, new Endpoint());
    }
    
    public static AccessLevel getAccessLevel(final AccessInfo ai, final String reslname, final HttpServletRequest request) {
        if (ai.isAdmin())
            return AccessLevel.OPEN;
        EtextAccessProps eap = getAccessPropsCached(reslname);
        if (GeoLocation.isFromChina(request) && eap.ric)
            return AccessLevel.NOACCESS;
        if ("AccessOpen".equals(eap.access_lname) && "StatusReleased".equals(eap.access_lname))
            return AccessLevel.OPEN;
        return ai.hasResourceAccess(eap.access_lname, eap.status_lname, eap.ei);
    }
        
    public static String getToken(final String header) {
        if (header == null || !header.startsWith("Bearer "))
            return null;
        return header.substring(7);
    }
    
    @GetMapping("/osearch/etextaccess")
    public ResponseEntity<?> etextaccess(
            @RequestParam String id, final HttpServletRequest request) throws IOException {

        if (id.startsWith("bdr:")) {
            id = id.substring(4);
        }
        
        final AccessInfo ai = getAccessInfo(id, request);
        final AccessLevel al = getAccessLevel(ai, id, request);
        if (!al.equals(AccessLevel.OPEN)) {
            return ResponseEntity.status(ai.isLogged() ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED).body("access not allowed");
        }
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/osearch/etextchunks")
    public ResponseEntity<?> etextchunks(
            @RequestParam String id,
            @RequestParam final int cstart,
            @RequestParam final int cend, final HttpServletRequest request) throws IOException {

        if (id.startsWith("bdr:")) {
            id = id.substring(4);
        }
        
        final AccessInfo ai = getAccessInfo(id, request);
        final AccessLevel al = getAccessLevel(ai, id, request);
        if (!al.equals(AccessLevel.OPEN)) {
            return ResponseEntity.status(ai.isLogged() ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED).body("access not allowed");
        }
        
        // Create a SearchRequest targeting the index
        SearchRequest searchRequest = new SearchRequest(index_name);

        // Build the search query
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.fetchSource(false);  // Do not return the _source

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        if (id.startsWith("UT")) {
            // Query by document ID
            boolQuery.must(QueryBuilders.termQuery("_id", id));
        } else if (id.startsWith("VL")) {
            // Query by etext_vol field
            boolQuery.must(QueryBuilders.termQuery("etext_vol", id));
        }

        // Nested queries for etext_pages
        BoolQueryBuilder etextPagesQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery("etext_pages.cend").gte(cstart))
                .must(QueryBuilders.rangeQuery("etext_pages.cstart").lte(cend));
        boolQuery.must(QueryBuilders.nestedQuery("etext_pages", etextPagesQuery, ScoreMode.None).innerHit(new InnerHitBuilder().setSize(10000)));

        // Nested queries for chunks
        BoolQueryBuilder chunksQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery("chunks.cend").gte(cstart))
                .must(QueryBuilders.rangeQuery("chunks.cstart").lte(cend));
        boolQuery.must(QueryBuilders.nestedQuery("chunks", chunksQuery, ScoreMode.None).innerHit(new InnerHitBuilder().setSize(10000)));

        sourceBuilder.query(boolQuery);
        searchRequest.source(sourceBuilder);
        // Execute the search
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        // Return the search results
        return ResponseEntity.ok(searchResponse.getHits().getHits());
    }

    @GetMapping("/osearch/etextpages")
    public ResponseEntity<?> etextpages(
                @RequestParam String id,
                @RequestParam final int pstart,
                @RequestParam final int pend, final HttpServletRequest request) throws IOException {
        if (id.startsWith("bdr:")) {
            id = id.substring(4);
        }
        
        // Create a SearchRequest targeting the index
        SearchRequest searchRequest = new SearchRequest(index_name);

        // Build the search query
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.fetchSource(false);  // Do not return the _source

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        if (id.startsWith("UT")) {
            // Query by document ID
            boolQuery.must(QueryBuilders.termQuery("_id", id));
        } else if (id.startsWith("VL")) {
            // Query by etext_vol field
            boolQuery.must(QueryBuilders.termQuery("etext_vol", id));
        }

        // Nested queries for etext_pages
        BoolQueryBuilder etextPagesQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery("etext_pages.pnum").gte(pstart))
                .must(QueryBuilders.rangeQuery("etext_pages.pnum").lte(pend));
        boolQuery.must(QueryBuilders.nestedQuery("etext_pages", etextPagesQuery, ScoreMode.None).innerHit(new InnerHitBuilder().setSize(10000)));
        
        sourceBuilder.query(boolQuery);
        searchRequest.source(sourceBuilder);
        // Execute the search
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] hits = searchResponse.getHits().getHits();
        int minCstart = Integer.MAX_VALUE;
        int maxCend = Integer.MIN_VALUE;
        for (SearchHit hit : hits) {
            if (hit.getInnerHits() != null) {
                SearchHits innerHits = hit.getInnerHits().get("etext_pages");
                for (SearchHit innerHit : innerHits.getHits()) {
                    Map<String, Object> etextPage = innerHit.getSourceAsMap();
                    int cstart = (int) etextPage.get("cstart");
                    int cend = (int) etextPage.get("cend");

                    // Update min and max values
                    if (cstart < minCstart) {
                        minCstart = cstart;
                    }
                    if (cend > maxCend) {
                        maxCend = cend;
                    }
                }
            }
        }
        
        if (minCstart == Integer.MAX_VALUE || maxCend == Integer.MIN_VALUE) {
            return ResponseEntity.notFound().build(); 
        }
        
        return etextchunks(id, minCstart, maxCend, request);
    }

}
