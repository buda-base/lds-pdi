package io.bdrc.taxonomy;

import java.util.HashMap;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.LdsQuery;
import io.bdrc.ldspdi.sparql.LdsQueryService;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.restapi.exceptions.RestException;

public class TaxModel {


    static Model m;
    static HashMap<String,String> items;
    public final static Logger log=LoggerFactory.getLogger(TaxModel.class.getName());

    public static void init() throws RestException {
        final LdsQuery qfp = LdsQueryService.get(ServiceConfig.getProperty("taxtree")+".arq","library");
        HashMap<String,String> map=new HashMap<>();
        map.put("R_RES","bdr:O9TAXTBRC201605");
        String query=qfp.getParametizedQuery(map,false);
        m=QueryProcessor.getGraph(query,ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL),null);
        items=getRootCatByTopics();
    }


    private static HashMap<String,String> getRootCatByTopics() throws RestException{
        HashMap<String,String> items=new HashMap<>();
        String query="select distinct ?g ?root\n" +
                "where {\n" +
                "  ?g <http://purl.bdrc.io/ontology/core/taxSubclassOf>+ ?root .\n" +
                " FILTER EXISTS {?root <http://purl.bdrc.io/ontology/core/taxSubclassOf> <http://purl.bdrc.io/resource/O9TAXTBRC201605>}"+
                "} order by ?g";
        ResultSet rs=QueryProcessor.getResultsFromModel(query, m);
        while(rs.hasNext()) {
            QuerySolution qs=rs.next();
            items.put(qs.get("?g").asNode().getURI(), qs.get("?root").asNode().getURI());
        }
        return items;
    }

    public static String getTaxonomyItem(String key) {
        return items.get(key);
    }

    public static Model getModel() {
        return m;
    }

}
