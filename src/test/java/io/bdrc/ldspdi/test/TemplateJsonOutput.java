package io.bdrc.ldspdi.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.sparql.results.FusekiResultSet;
import io.bdrc.ldspdi.sparql.results.ResultSetWrapper;

public class TemplateJsonOutput {
    
    String prefixes="PREFIX : <http://purl.bdrc.io/ontology/core/>\n" + 
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
            " PREFIX f: <java:io.bdrc.ldspdi.sparql.functions.>";
    
    String query1=prefixes+" select ?Work_ID ?Work_Name\n" + 
            "where {\n" + 
            "(?Work_ID ?sc ?Work_Name) text:query \"chos dbyings\" .\n" + 
            "?Work_ID a :Work.\n" + 
            "} limit 5";
    
    String fusekiUrl="http://buda1.bdrc.io:13180/fuseki/bdrcrw/query";
      
    @Test
    public void parseBdrcJsonResult() throws MalformedURLException, IOException {
        ServiceConfig.initForTests();
        ResultSetWrapper rsw=QueryProcessor.getResults(query1, fusekiUrl,null,"50");
        FusekiResultSet frs=new FusekiResultSet(rsw);
        ObjectMapper mapper = new ObjectMapper();
        String json=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(frs);
        System.out.println(json);
        ByteArrayInputStream is=new ByteArrayInputStream(json.getBytes()); 
        ResultSet rs=ResultSetFactory.fromJSON(is);
        System.out.println("ResultSet >>" +rs);
        is.close();
    }
    
}
