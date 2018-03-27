package io.bdrc.ldspdi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Objects;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.sparql.results.FusekiResultSet;
import io.bdrc.ldspdi.sparql.results.ResultSetWrapper;
import io.bdrc.ldspdi.sparql.results.ResultsCache;

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
            "} ";
    
    String fusekiUrl="http://buda1.bdrc.io:13180/fuseki/bdrcrw/query";
    int limit=5;
    
    @BeforeClass
    public static void init() {
        ServiceConfig.initForTests();
    }
      
    @Test
    public void testBdrcJsonResultParsing() throws IOException{      
                
        ResultSetWrapper rsw=getResults(query1+" limit "+limit, fusekiUrl);
        FusekiResultSet frs=new FusekiResultSet(rsw);
        ObjectMapper mapper = new ObjectMapper();
        String json=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(frs);
        System.out.println("***************** Bdrc Json >>"+System.lineSeparator()+json);
        ByteArrayInputStream is=new ByteArrayInputStream(json.getBytes()); 
        ResultSet rs1=ResultSetFactory.fromJSON(is);        
        is=new ByteArrayInputStream(json.getBytes());
        ResultSet rs2=ResultSetMgr.read(is, ResultSetLang.SPARQLResultSetJSON);
        ResultSetRewindable rwd1=ResultSetFactory.copyResults(rs1);
        ResultSetRewindable rwd2=ResultSetFactory.copyResults(rs2);        
        assertEquals(rwd1.size(),limit);
        assertEquals(rwd2.size(),limit);
        assertEquals(rwd1.getResultVars(), rwd2.getResultVars());
        is.close();
    }
    
    @Test
    public void testJenaJsonResultParsing() throws IOException {
        
        ResultSet rs=QueryExecutionFactory.sparqlService(fusekiUrl,QueryFactory.create(query1+" limit "+limit)).execSelect(); 
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        ResultSetFormatter.outputAsJSON(baos, rs);
        String json=new String(baos.toByteArray());
        System.out.println("***************** Jena Json >>"+System.lineSeparator()+json);
        ByteArrayInputStream is=new ByteArrayInputStream(json.getBytes()); 
        ResultSet rs1=ResultSetFactory.fromJSON(is);
        is=new ByteArrayInputStream(json.getBytes());
        ResultSet rs2=ResultSetMgr.read(is, ResultSetLang.SPARQLResultSetJSON);
        ResultSetRewindable rwd1=ResultSetFactory.copyResults(rs1);
        ResultSetRewindable rwd2=ResultSetFactory.copyResults(rs2);        
        assertEquals(rwd1.size(),limit);
        assertEquals(rwd2.size(),limit);
        assertEquals(rwd1.getResultVars(), rwd2.getResultVars());
        baos.close();
        is.close();
    }
    
    @Test
    public void compareJsonResults() throws IOException {
        //Bdrc generated Json
        ResultSetWrapper rsw=getResults(query1+" limit "+limit, fusekiUrl);
        FusekiResultSet frs=new FusekiResultSet(rsw);
        ObjectMapper mapper = new ObjectMapper();
        String json1=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(frs);
        //Jena Generated Json
        ResultSet rs=QueryExecutionFactory.sparqlService(fusekiUrl,QueryFactory.create(query1+" limit "+limit)).execSelect(); 
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        ResultSetFormatter.outputAsJSON(baos, rs);
        String json2=new String(baos.toByteArray());
        
        JsonNode node1=mapper.readTree(json1);
        JsonNode node2=mapper.readTree(json2);
        assertEquals(node1, node2);
    }
    
    public static ResultSetWrapper getResults(String query, String fuseki) {
            ResultSetWrapper res;
            long start=System.currentTimeMillis(); 
            QueryExecution qe=QueryExecutionFactory.sparqlService(fuseki,QueryFactory.create(query));
            ResultSet jrs=qe.execSelect();           
            long elapsed=System.currentTimeMillis()-start;
            int psz=Integer.parseInt(ServiceConfig.getProperty(QueryConstants.PAGE_SIZE));
            res=new ResultSetWrapper(jrs,elapsed,psz); 
            qe.close();            
            return res;
       
    }
    
    
}
