package io.bdrc.ldspdi.rest.controllers;

import java.io.IOException;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.InnerHitBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.bdrc.ldspdi.service.ServiceConfig;

@RestController
public class OSControllers {

    @Autowired
    private RestHighLevelClient client;
    
    static final String index_name = ServiceConfig.getProperty("opensearchIndex");

    @GetMapping("/osearch/etextchunks")
    public ResponseEntity<?> search(
            @RequestParam String id,
            @RequestParam final int cstart,
            @RequestParam final int cend) throws IOException {

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
    

}
