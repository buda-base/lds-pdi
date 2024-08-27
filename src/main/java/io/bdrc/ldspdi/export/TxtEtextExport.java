package io.bdrc.ldspdi.export;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.InnerHitBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.bdrc.auth.AccessInfo;
import io.bdrc.auth.AccessInfoAuthImpl;
import io.bdrc.ewtsconverter.EwtsConverter;
import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.rest.controllers.PublicDataController;
import io.bdrc.ldspdi.rest.features.CorsFilter;
import io.bdrc.ldspdi.results.ResultSetWrapper;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.LdsQuery;
import io.bdrc.ldspdi.sparql.LdsQueryService;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.GeoLocation;
import io.bdrc.libraries.StreamingHelpers;

public class TxtEtextExport {
    
    public static final EwtsConverter ewtsConverter = new EwtsConverter();
    public final static Logger log = LoggerFactory.getLogger(TxtEtextExport.class);
    static final String index_name = ServiceConfig.getProperty("opensearchIndex");
    
    public static final class Chunk {
        public final String text;
        public final String os_langtag;
        public final int cstart;
        public final int cend;

        public Chunk(final String text, final String os_langtag, final int cstart, final int cend) {
            this.text = text;
            this.cstart = cstart;
            this.cend = cend;
            this.os_langtag = os_langtag;
        }
    }
    
    public static List<Chunk> getChunks(RestHighLevelClient client, String id, final int cstart, final int cend) {
        List<Chunk> chunks = new ArrayList<>();
        
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

        // Nested queries for chunks
        BoolQueryBuilder chunksQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery("chunks.cend").gte(cstart))
                .must(QueryBuilders.rangeQuery("chunks.cstart").lte(cend));
        boolQuery.must(QueryBuilders.nestedQuery("chunks", chunksQuery, ScoreMode.None).innerHit(new InnerHitBuilder().setSize(10000)));

        sourceBuilder.query(boolQuery);
        searchRequest.source(sourceBuilder);

        // Execute the search
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("Chunks OS query failed", e);
            return chunks;
        }

        // Parse the response to extract chunks
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            final Map<String, SearchHits> innerHits = hit.getInnerHits();
            if (innerHits != null && innerHits.containsKey("chunks")) {
                for (SearchHit innerHit : innerHits.get("chunks").getHits()) {
                    Map<String, Object> sourceAsMap = innerHit.getSourceAsMap();
                    final int chunkCstart = (int) sourceAsMap.get("cstart");
                    final int chunkCend = (int) sourceAsMap.get("cend");
                    if (sourceAsMap.containsKey("text_bo")) {
                        final String text = (String) sourceAsMap.get("text_bo");
                        chunks.add(new Chunk(text, "bo", chunkCstart, chunkCend));
                    } else if (sourceAsMap.containsKey("text_en")) {
                        final String text = (String) sourceAsMap.get("text_en");
                        chunks.add(new Chunk(text, "en", chunkCstart, chunkCend));
                    } else if (sourceAsMap.containsKey("text_hani")) {
                        final String text = (String) sourceAsMap.get("text_hani");
                        chunks.add(new Chunk(text, "hani", chunkCstart, chunkCend));
                    } 
                }
            }
        }

        // Sort the chunks by cstart
        chunks.sort((c1, c2) -> Integer.compare(c1.cstart, c2.cstart));

        return chunks;
    }
    
    public static String getStringForTxt(final List<Chunk> chunks, final Integer startChar, final Integer endChar, final Map<String,String> ltagConversionMap) {
        final StringBuilder sb = new StringBuilder();
        for (Chunk c : chunks) {
            final String qsContentSToAdd;
            if (c.cstart < startChar && c.cend > endChar) {
                qsContentSToAdd = c.text.substring(startChar - c.cstart, endChar - c.cend);
            } else if (c.cstart < startChar) {
                qsContentSToAdd = c.text.substring(startChar - c.cstart);
            } else if (c.cend > endChar) {
                qsContentSToAdd = c.text.substring(0, endChar - c.cend);
            } else {
                qsContentSToAdd = c.text;
            }
            if (ltagConversionMap.containsKey(c.os_langtag)) {
                // here we just assume that we're converting to ewts since it's the only thing that
                // can happen in the code
                sb.append(ewtsConverter.toWylie(qsContentSToAdd));
            } else {
                sb.append(qsContentSToAdd);
            }
        }
        return sb.toString();
    }
    
    public static final Map<String,String> getLtagConversionMap(final List<Locale> llist) {
        final Map<String,String> res = new HashMap<>();
        for (final Locale l : llist) {
            final String ltag = l.toLanguageTag();
            if (ltag.equals("bo-x-ewts")) {
                res.put("bo", "bo-x-ewts");
            }
        }
        return res;
    }
    
    public static void addToLtagConversionMap(final String prefLangs, Map<String,String> ltagConversionMap) {
        if (prefLangs == null || prefLangs.isEmpty())
            return;
        for (final String l : prefLangs.split(",")) {
            if (l.equals("bo-x-ewts")) {
                ltagConversionMap.put("bo", "bo-x-ewts");
            }
        }
    }
    
    // TODO: access control
    
    public static ResultSetWrapper getResults(final String resUri) throws RestException {
        Map<String, String> args = new HashMap<>();
        args.put("R_RES", resUri);
        // process
        final LdsQuery qfp = LdsQueryService.get("ChunksByRange.arq", "library");
        final String query = qfp.getParametizedQuery(args, true);
        ResultSetWrapper res = QueryProcessor.getResults(query, null, null, "100000");
        return res;
    }
    
    public final static ResponseEntity<StreamingResponseBody> getResponse(final RestHighLevelClient client, final HttpServletRequest request, final String id, final Integer startChar, final Integer endChar, final String resName, final String prefLangs) throws RestException {
        log.info("get text for "+id+" ("+startChar+"-"+endChar+")");
        List<Chunk> res = getChunks(client, id, startChar, endChar);
        if (res.size() < 1) {
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setMsg("Resource does not exist or no character in range"));
        }
        
        String fName =  resName;
        if ((!startChar.equals(0)) || !endChar.equals(PublicDataController.defaultMaxValI)) {
            fName += "-" + startChar.toString() + "-";
            if (!endChar.equals(PublicDataController.defaultMaxValI)) {
                fName += endChar.toString();
            } else {
                fName += "end";
            }
        }
        fName += ".txt";
        final List<Locale> locales = Collections.list(request.getLocales());
        final Map<String,String> ltagConversionMap = getLtagConversionMap(locales);
        addToLtagConversionMap(prefLangs, ltagConversionMap);
        final String resStr = getStringForTxt(res, startChar, endChar, ltagConversionMap);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).header("Allow", "GET, OPTIONS, HEAD")
                .header("Vary", "Negotiate, Accept")
                .header("Content-Disposition", "attachment; filename=\""+fName+"\"")
                .body(StreamingHelpers.getStream(resStr));
    }

}
