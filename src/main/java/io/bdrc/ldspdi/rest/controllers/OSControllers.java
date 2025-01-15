package io.bdrc.ldspdi.rest.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
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
import org.opensearch.search.sort.SortOrder;
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
        UserProfile prof = new UserProfile();
        if (ServiceConfig.useAuth() && !method.equalsIgnoreCase("OPTIONS")) {
            log.info("ldspdi is using auth !");
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
        } else if (id.startsWith("VE")) {
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
    
    public final static class Snippet_info {
        public Integer start_cnum = null;
        public String etext_vol;
        public String etext_instance;
        public String ut;
        public Integer start_page_num = null;
        public Integer volumeNumber;
        public Double etext_quality = null;
        public int precision;
        public List<String[]> snippet = null;
    }
    
    public Snippet_info snippet_direct(
            @RequestParam String id) throws IOException {
        // Create a SearchRequest targeting the index
        SearchRequest searchRequest = new SearchRequest(index_name);
        // Build the search query
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.fetchSource(true);  // Do not return the _source
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (id.startsWith("UT")) {
            // Query by document ID
            boolQuery.must(QueryBuilders.termQuery("_id", id));
        } else if (id.startsWith("MW")) {
            // Query by either etext_for_instance or etext_for_root_instance
            BoolQueryBuilder shouldQuery = QueryBuilders.boolQuery();
            shouldQuery.should(QueryBuilders.termQuery("etext_for_instance", id));
            shouldQuery.should(QueryBuilders.termQuery("etext_for_root_instance", id));
            shouldQuery.minimumShouldMatch(1); // At least one should match
            boolQuery.must(shouldQuery);
        } else if (id.startsWith("VE")) {
            // Query by etext_vol field
            boolQuery.must(QueryBuilders.termQuery("etext_vol", id));
        } else if (id.startsWith("IE")) {
            boolQuery.must(QueryBuilders.termQuery("etext_instance", id));
        } else {
            return null;
        }
        sourceBuilder.sort("volumeNumber", SortOrder.ASC);
        sourceBuilder.sort("etextNumber", SortOrder.ASC);

        // Limit the number of results to 2
        sourceBuilder.size(2);
        sourceBuilder.query(boolQuery);
        searchRequest.source(sourceBuilder);
        // Execute the search
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        
        if (searchResponse.getHits().getTotalHits().value < 1) {
            return null;
        }
        final SearchHit sh = searchResponse.getHits().getAt(0);
        final Map<String, Object> s = sh.getSourceAsMap();
        final Snippet_info si = new Snippet_info();
        si.ut = sh.getId();
        si.etext_vol = s.get("etext_vol").toString();
        si.etext_instance = s.get("etext_instance").toString();
        si.volumeNumber = (Integer) s.get("volumeNumber");
        if (s.get("etext_quality") != null)
            si.etext_quality = ((Number) s.get("etext_quality")).doubleValue();
        si.precision = 3;

        // add start_page_num by taking the "pnum" of the first object in the "etext_pages" array
        List<Map<String, Object>> etext_pages = (List<Map<String, Object>>) s.get("etext_pages");
        if (etext_pages != null && !etext_pages.isEmpty()) {
            si.start_page_num = (Integer) etext_pages.get(0).get("pnum");
        }

        // add start_cnum by taking the "cstart" field of the first object in the "chunks" array
        List<Map<String, Object>> chunks = (List<Map<String, Object>>) s.get("chunks");
        if (chunks != null && !chunks.isEmpty()) {
            si.start_cnum = (Integer) chunks.get(0).get("cstart");
        }
        si.snippet = snippet_for_chunks(chunks, -1);
        
        return si;
    }
    
    public List<String[]> snippet_for_chunks(final List<Map<String, Object>> chunks, final int cstart) {
        // create snippet by doing the following: while nbchars < 1000, read the next object in chunks 
        // (starting at the beginning), and add an entry in the form of a string array with the first 
        // string being the characters (cut so that the total number is 1000), the second being the 
        // field in the chunk object that starts with "text_"
        final List<String[]> snippetList = new ArrayList<>();
        if (chunks == null || chunks.isEmpty())
            return snippetList;
        int nbchars = 0;
        for (Map<String, Object> chunk : chunks) {
            if (cstart != -1 && cstart > (Integer) chunk.get("cend"))
                continue;
            // Find the text field (text_bo, text_en, or text_zh)
            String textField = null;
            for (String key : chunk.keySet()) {
                if (key.startsWith("text_")) {
                    textField = key;
                    break;
                }
            }
            
            if (textField != null) {
                String chunkText = chunk.get(textField).toString();
                if (cstart != -1 && (Integer) chunk.get("cstart") < cstart) {
                    int chunk_start = cstart - (Integer) chunk.get("cstart");
                    chunkText = chunkText.substring(chunk_start);
                }
                
                // If adding this chunk would exceed 1000 chars, truncate it
                if (nbchars + chunkText.length() > 1000) {
                    chunkText = chunkText.substring(0, 1000 - nbchars);
                }
                final String[] snippet_item = new String[2];
                snippet_item[0] = chunkText;
                snippet_item[1] = textField.substring(5);
                snippetList.add(snippet_item);
                
                nbchars += chunkText.length();
                
                // Stop if we've reached or exceeded 1000 chars
                if (nbchars >= 1000) {
                    break;
                }
            }
        }
        
        return snippetList;
    }
    
    public Snippet_info snippet_for_loc(final Location loc) throws IOException {
        if (loc.page_start == 0)
            loc.page_start = 1;
        if (loc.volume_start == 0)
            loc.volume_start = 1;
        // Create a SearchRequest targeting the index
        SearchRequest searchRequest = new SearchRequest(index_name);
        // Build the search query
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.fetchSource(true);  // Do not return the _source
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.termQuery("etext_for_root_instance", loc.rootInstanceLname));
        boolQuery.must(QueryBuilders.termQuery("volumeNumber", loc.volume_start));
        // Nested queries for etext_pages
        BoolQueryBuilder etextPagesQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery("etext_pages.pnum").gte(loc.page_start))
                .must(QueryBuilders.rangeQuery("etext_pages.pnum").lte(loc.page_start+5));
        boolQuery.must(QueryBuilders.nestedQuery("etext_pages", etextPagesQuery, ScoreMode.None).innerHit(new InnerHitBuilder().setSize(10000)));
        sourceBuilder.sort("etextNumber", SortOrder.ASC);

        // Limit the number of results to 2
        sourceBuilder.size(2);
        sourceBuilder.query(boolQuery);
        searchRequest.source(sourceBuilder);
        // Execute the search
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        if (searchResponse.getHits().getTotalHits().value < 1) {
            return null;
        }
        final SearchHit sh = searchResponse.getHits().getAt(0);
        final Map<String, Object> s = sh.getSourceAsMap();
        final Snippet_info si = new Snippet_info();
        si.ut = sh.getId();
        si.etext_vol = s.get("etext_vol").toString();
        si.etext_instance = s.get("etext_instance").toString();
        si.volumeNumber = loc.volume_start;
        if (s.get("etext_quality") != null)
            si.etext_quality = ((Number) s.get("etext_quality")).doubleValue();
        si.precision = loc.precision;
        si.start_page_num = loc.page_start;
        
        List<Map<String, Object>> etext_pages = (List<Map<String, Object>>) s.get("etext_pages");
        int cstart = -1;
        if (etext_pages != null && !etext_pages.isEmpty()) {
            for (int i = 0 ; i < etext_pages.size() ; i ++) {
                final Map<String,Object> page = etext_pages.get(i);
                if ((Integer) page.get("pnum") < loc.page_start)
                    continue;
                cstart = (Integer) page.get("cstart");
                si.start_page_num = (Integer) page.get("pnum");
                break;
            }
        }

        // add start_cnum by taking the "cstart" field of the first object in the "chunks" array
        List<Map<String, Object>> chunks = (List<Map<String, Object>>) s.get("chunks");
        if (chunks != null && !chunks.isEmpty()) {
            si.start_cnum = (Integer) chunks.get(0).get("cstart");
        }
        si.snippet = snippet_for_chunks(chunks, cstart);
        
        return si;
    }
    
    public static final class Location {
        int precision = -1;
        int page_start = 0;
        int volume_start = 0;
        String rootInstanceLname = null;
        String ieLname = null;
    }
    
    static final Property contentLocationP = ResourceFactory.createProperty(Models.BDO+"contentLocation");
    static final Property partOfP = ResourceFactory.createProperty(Models.BDO+"partOf");
    static final Property contentLocationPageP = ResourceFactory.createProperty(Models.BDO+"contentLocationPage");
    static final Property contentLocationVolumeP = ResourceFactory.createProperty(Models.BDO+"contentLocationVolume");
    static final Property inRootInstanceP = ResourceFactory.createProperty(Models.BDO+"inRootInstance");
    static final Property hasEtextInstance = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/tmp/hasEtextInstance");
    
    public static Location getLoc(final String mwlname) {
        final String qstr = "prefix :      <http://purl.bdrc.io/ontology/core/>\n"
                + "prefix bdr:   <http://purl.bdrc.io/resource/>\n"
                + "prefix adm:   <http://purl.bdrc.io/ontology/admin/>\n"
                + "prefix bda:   <http://purl.bdrc.io/admindata/>\n"
                + "prefix tmp:   <http://purl.bdrc.io/ontology/tmp/>\n"
                + "\n"
                + "construct {\n"
                + "  ?mw :contentLocation ?cl .\n"
                + "  ?cl ?clp ?clo .\n"
                + "  ?mw :partOf ?mwp .\n"
                + "  bdr:"+mwlname+" :inRootInstance ?mwroot .\n"
                + "  ?mwroot tmp:hasEtextInstance ?ie .\n"
                + "} where {\n"
                + "  {\n"
                + "    bdr:"+mwlname+" :partOf* ?mw .\n"
                + "    ?mw :partOf ?mwp .\n"
                + "    ?mw :contentLocation ?cl .\n"
                + "    ?cl ?clp ?clo .\n"
                + "  } union {\n"
                + "    bdr:"+mwlname+" :inRootInstance* ?mwroot . \n"
                + "    ?mwroot :instanceHasReproduction ?ie .\n"
                + "    FILTER(exists{?ie a :EtextInstance . ?ieadm adm:adminAbout ?ie ; adm:status bda:StatusReleased })\n"
                + "  }\n"
                + "}";
        final Query q = QueryFactory.create(qstr);
        log.debug("QUERY >> {}", qstr);
        final RDFConnection conn = RDFConnectionRemote.create()
                .destination(ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL)).build();
        final Model model = conn.queryConstruct(q);
        if (model == null)
            return null;
        boolean first = true;
        Resource mw = model.createResource(Models.BDR+mwlname);
        Location l = new Location();
        final Resource root = mw.getPropertyResourceValue(inRootInstanceP);
        if (root == null)
            return null;
        l.rootInstanceLname = root.getLocalName();
        l.ieLname = root.getPropertyResourceValue(hasEtextInstance).getLocalName();
        while (mw != null) {
            final Resource cl = mw.getPropertyResourceValue(contentLocationP);
            if (cl == null) {
                mw = mw.getPropertyResourceValue(partOfP);
                first = false;
                continue;
            }
            final Statement pageS = cl.getProperty(contentLocationPageP);
            final Statement volumeS = cl.getProperty(contentLocationVolumeP);
            if (pageS == null && volumeS == null) {
                mw = mw.getPropertyResourceValue(partOfP);
                first = false;
                continue;
            }
            final Integer pageN = pageS != null ? pageS.getLiteral().getInt() : null;
            final Integer volumeN = volumeS != null ? volumeS.getLiteral().getInt() : null;
            if (volumeN != null) {
                if (l.volume_start == 0)
                    l.volume_start = volumeN;
                else
                    break;
            }
            if (pageN != null && l.page_start == 0)
                l.page_start = pageN;
            if (l.page_start != 0) {
                l.precision = first ? 2 : 0;
                if (l.volume_start == 0)
                    l.volume_start = 1;
                break;
            }
            first = false;
            mw = mw.getPropertyResourceValue(partOfP);
        }
        if (l.page_start != 0 && l.volume_start == 0)
            l.volume_start = 1; // when there is only one volume this is quite common
        if (l.page_start == 0 && l.volume_start != 0)
            l.precision = 1;
        else if (l.precision == -1)
            l.precision = 0;
        return l;
    }
    
    @GetMapping("/osearch/snippet")
    public ResponseEntity<Snippet_info> snippet(
            @RequestParam String id) throws IOException {
        if (id.startsWith("bdr:")) {
            id = id.substring(4);
        }
        // First, let's search in OpenSearch directly if the id is a MW or a UT. There won't be a result
        // every time but it's a fast path
        Snippet_info si = snippet_direct(id);
        if (si != null) {
            return ResponseEntity.ok(si);
        }
        final Location loc = getLoc(id);
        if (loc == null) {
            return ResponseEntity.notFound().build();
        }
        si = snippet_for_loc(loc);
        if (si == null && loc.ieLname != null) {
            si = snippet_direct(loc.ieLname);
            if (si != null)
                si.precision = 0;
        }
        if (si == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(si);
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
        } else if (id.startsWith("VE")) {
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
