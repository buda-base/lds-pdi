package io.bdrc.ldspdi.test;

import java.util.HashMap;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;

import io.bdrc.ldspdi.sparql.InjectionTracker;
import io.bdrc.ldspdi.sparql.QueryFileParser;
import io.bdrc.restapi.exceptions.RestException;

public class RootSearchTest {
    
    public static HashMap<String,String> map;
    public static String fusekiUrl="http://buda1.bdrc.io:13180/fuseki/bdrcrw/query";
    public static String sparqlPrefixes="PREFIX : <http://purl.bdrc.io/ontology/core/>\n" + 
            " PREFIX bdo: <http://purl.bdrc.io/ontology/core/>\n" + 
            " PREFIX adm: <http://purl.bdrc.io/ontology/admin/>\n" + 
            " PREFIX bdr: <http://purl.bdrc.io/resource/>\n" + 
            " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" + 
            " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + 
            " PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" + 
            " PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" + 
            " PREFIX bf: <http://id.loc.gov/ontologies/bibframe/>\n" + 
            " PREFIX tbr: <http://purl.bdrc.io/ontology/toberemoved/>\n" + 
            " PREFIX vcard: <http://www.w3.org/2006/vcard/ns#>\n" + 
            " PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" + 
            " PREFIX text: <http://jena.apache.org/text#>\n" + 
            " PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" + 
            " PREFIX dcterms: <http://purl.org/dc/terms/>\n" + 
            " PREFIX f: <java:io.bdrc.ldspdi.sparql.functions.> ";
    static {
        
            
            map=new HashMap<>();
            map.put("L_NAME", "\"'od zer\"");
            map.put("LG_NAME", "bo-x-ewts");
       
    }
    
    public static String query=" CONSTRUCT \n" + 
            "  { \n" + 
            "    ?TT a ?type .\n" + 
            "    ?TT <http://purl.bdrc.io/ontology/core/tmp/prop> ?p.\n" + 
            "    ?TT ?pp ?lit .\n" + 
            "    ?TT skos:prefLabel ?l\n" + 
            "  }\n" + 
            "WHERE {\n" + 
            " optional{?TT skos:prefLabel ?l.}\n" +
            "    optional{ ?TT rdf:type ?type}\n" +
            "  {\n" + 
            "    (?s ?sc ?lit) text:query ( rdfs:label ?L_NAME ). \n" + 
            "    optional{?TT ?p ?s.}\n" + 
            "    \n" + 
            "    optional{?s rdf:type ?pp} \n" + 
             
            "    } \n" + 
            "  union\n" + 
            "  { (?TT ?sc ?lit) text:query ( skos:altLabel ?L_NAME ). \n" +             
            "    \n" + 
            "  }\n" + 
            "  union\n" + 
            "  { (?TT ?sc ?lit) text:query ( skos:prefLabel ?L_NAME ). \n" +             
            "  }\n" + 
            "}";
    
    public static Model getModel() throws RestException { 
        
        QueryFileParser qfp=new QueryFileParser("RootSearchGraphComplet.arq","library","");
        qfp.checkQueryArgsSyntax();
        System.out.println("Lit Params >>> "+qfp.getLitLangParams());
        String qt=InjectionTracker.getValidQuery(sparqlPrefixes+" "+qfp.getQuery(),map,qfp.getLitLangParams(),false);
        Query q=QueryFactory.create(qt);
        //System.out.println(q.toString());
        System.out.println("Start >>> "+System.currentTimeMillis());
        QueryExecution qe = QueryExecutionFactory.sparqlService(fusekiUrl,q);
        qe.setTimeout(Long.parseLong("5000"));
        Model model = qe.execConstruct();
        qe.close();
        return model;
    }
    
    public static void main(String[] args) throws RestException {
        
        Model mod=RootSearchTest.getModel();
        System.out.println("End >>> "+System.currentTimeMillis());
        System.out.println("Model size >>> "+mod.size());
    }

}
