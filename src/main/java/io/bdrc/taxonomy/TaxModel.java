package io.bdrc.taxonomy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.reasoner.rulesys.Rule.Parser;
import org.apache.jena.vocabulary.ReasonerVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.restapi.exceptions.RestException;

public class TaxModel {    
       
    static InfModel infMod;
    static Model m;
    static HashMap<String,String> items;
    public final static Logger log=LoggerFactory.getLogger(TaxModel.class.getName());
    
    public static void init() throws RestException {
        try {
            
            InputStream stream = TaxModel.class.getClassLoader().getResourceAsStream("O9TAXTBRC201605.ttl");
            log.info("Basic taxonomy model size yyyy>> ");
            m = ModelFactory.createDefaultModel();
            m.read(stream, "", "TURTLE");
            stream.close();
            log.info("Basic taxonomy model size >> "+m.size());
            List<Rule> rules = new ArrayList<Rule>(); 
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    TaxModel.class.getClassLoader().getResourceAsStream("taxonomy.rules")));
            Parser p = Rule.rulesParserFromReader(in);
            rules.addAll(Rule.parseRules(p));
            in.close();
            Reasoner reasoner = new GenericRuleReasoner(rules);
            reasoner.setParameter(ReasonerVocabulary.PROPruleMode, "forward");
            infMod = ModelFactory.createInfModel(reasoner, m);
            log.info("Inferred taxonomy size >> "+infMod.size());
            items=getRootCatByTopics();
    
        } catch (IOException io) {
            log.error("Error initializing TaxModel", io);            
        }        
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
    
    public static InfModel getInfModel() {
        return infMod;
    }
    
    public static Model getModel() {
        return m;
    }

}
