package io.bdrc.ldspdi.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.jena.fuseki.embedded.FusekiServer;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.riot.system.StreamRDFLib;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ldspdi.sparql.results.FusekiResultSet;
import io.bdrc.ldspdi.sparql.results.ResultSetWrapper;
import io.bdrc.ldspdi.sparql.results.Results;


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
    
    String query1=prefixes+"select ?Work_ID ?Work_Name\n" + 
            "where {\n" + 
            "?Work_ID a :Work.\n" + 
            "?Work_ID skos:prefLabel ?Work_Name\n" + 
            "  Filter(contains(?Work_Name,\"chos dbyings\"))\n" + 
            "}";
    
    String literal1=prefixes+" select ?s ?lccn\n" + 
            "where {\n" + 
            "  ?s bdo:workLccn ?lccn .\n" + 
            "  Filter(strlen(str(?lccn))>0)\n" + 
            "}";
    
    String literal2=prefixes+"select ?Work_Name\n" + 
            "where {\n" + 
            "?Work_ID a :Work.\n" + 
            "?Work_ID skos:prefLabel ?Work_Name\n" + 
            "  Filter(contains(?Work_Name,\"chos dbyings\"))\n" + 
            "}";
    
    String literal3=prefixes+" select ?s ?ct\n" + 
            "where {\n" + 
            "  ?s bdo:imageCount ?ct .\n" + 
            "  Filter(strlen(str(?ct))>0)\n" + 
            "}";
    
    String blank=prefixes+"select ?Work_Title\n" + 
            "where {\n" + 
            "?Work_ID a :Work.\n" + 
            "?Work_ID bdo:workTitle ?Work_Title\n" +              
            "}";
    
    static String fusekiUrl;
    int limit=4;
    
    static FusekiServer server ;
    static Dataset srvds = DatasetFactory.createTxnMem();
    static HashMap<String,String> datasets=new HashMap<>();
    
    static Model model = ModelFactory.createDefaultModel();
    final static Logger log=LoggerFactory.getLogger(TemplateJsonOutput.class.getName());
       
    @BeforeClass
    public static void init() {
        ServiceConfig.initForTests();
        datasets.put("W_chos_yin","W_Chos_Yin.ttl");
        datasets.put("literal1","literal1.ttl");
        datasets.put("literal3","literal3.ttl");
        loadData();     
        srvds.setDefaultModel(model);
        //Creating a fuseki server
        server = FusekiServer.create()
                .setPort(2245)
                .add("/bdrcrw", srvds)
                .build() ;
        fusekiUrl="http://localhost:2245/bdrcrw";       
        server.start() ;        
    }
      
    @Test
    public void testBdrcJsonResultParsing() throws IOException{      
                
        ResultSetWrapper rsw=getResults(query1+" limit "+limit, fusekiUrl);
        FusekiResultSet frs=new FusekiResultSet(rsw);
        ObjectMapper mapper = new ObjectMapper();
        String json=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(frs);        
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
    
    @Test
    public void testFullResultsParsing() throws NumberFormatException, IOException {
        HashMap<String,String> params=new HashMap<>();
        params.put(QueryConstants.PAGE_NUMBER, "1");
        params.put(QueryConstants.REQ_METHOD, "POST");
        ResultSetWrapper rsw=getResults(query1+" limit "+limit, fusekiUrl);
        Results rp=new Results(rsw,params);
        ObjectMapper mapper = new ObjectMapper();
        String json=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rp);
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
    public void testJsonLiteral3() throws IOException {
        ResultSetWrapper rsw=getResults(literal3+" limit "+limit, fusekiUrl);
        FusekiResultSet frs=new FusekiResultSet(rsw);
        ObjectMapper mapper = new ObjectMapper();
        String json1=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(frs); 
        ResultSet rs2=QueryExecutionFactory.sparqlService(fusekiUrl,QueryFactory.create(literal3+" limit "+limit)).execSelect(); 
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        ResultSetFormatter.outputAsJSON(baos, rs2);
        String json2=new String(baos.toByteArray());        
        JsonNode node1=mapper.readTree(json1);
        JsonNode node2=mapper.readTree(json2);
        baos.close();
        assertEquals(node1, node2);
    }
    
    @Test
    public void testJsonLiteral2() throws IOException {
        ResultSetWrapper rsw=getResults(literal2+" limit "+limit, fusekiUrl);
        FusekiResultSet frs=new FusekiResultSet(rsw);
        ObjectMapper mapper = new ObjectMapper();
        String json1=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(frs);
        
        ResultSet rs2=QueryExecutionFactory.sparqlService(fusekiUrl,QueryFactory.create(literal2+" limit "+limit)).execSelect(); 
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        ResultSetFormatter.outputAsJSON(baos, rs2);
        String json2=new String(baos.toByteArray());
        
        JsonNode node1=mapper.readTree(json1);
        JsonNode node2=mapper.readTree(json2);
        baos.close();
        assertEquals(node1, node2);
    }
    
    @Test
    public void testJsonLiteral1() throws IOException {
        ResultSetWrapper rsw=getResults(literal1+" limit "+limit, fusekiUrl);
        FusekiResultSet frs=new FusekiResultSet(rsw);
        ObjectMapper mapper = new ObjectMapper();
        String json1=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(frs);        
        
        ResultSet rs2=QueryExecutionFactory.sparqlService(fusekiUrl,QueryFactory.create(literal1+" limit "+limit)).execSelect(); 
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        ResultSetFormatter.outputAsJSON(baos, rs2);
        String json2=new String(baos.toByteArray());
        
        JsonNode node1=mapper.readTree(json1);
        JsonNode node2=mapper.readTree(json2);
        baos.close();
        assertEquals(node1, node2);
    }
    
    @Test
    public void testJsonBlank() throws IOException {
        ResultSetWrapper rsw=getResults(blank+" limit "+limit, fusekiUrl);
        FusekiResultSet frs=new FusekiResultSet(rsw);
        ObjectMapper mapper = new ObjectMapper();
        String json1=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(frs);
        System.out.println(json1);
        ResultSet rs2=QueryExecutionFactory.sparqlService(fusekiUrl,QueryFactory.create(blank+" limit "+limit)).execSelect(); 
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        ResultSetFormatter.outputAsJSON(baos, rs2);
        String json2=new String(baos.toByteArray());
        System.out.println(json2);
        baos.close();
        JsonNode node1=mapper.readTree(json1);
        JsonNode node2=mapper.readTree(json2);
        ArrayList<String> f1=new ArrayList<>();
        ArrayList<String> f2=new ArrayList<>();
        Iterator<String> it1=node1.fieldNames();
        Iterator<String> it2=node2.fieldNames();
        while(it1.hasNext()) {f1.add(it1.next());};
        while(it2.hasNext()) {f2.add(it2.next());};
        assertEquals(f1,f2);
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
    
    static void loadData(){
        //Loads the test dataset/
        Set<String> list=datasets.keySet();
        for(String data : list){
            Model m=getModelFromFileName(TestUtils.TESTDIR+datasets.get(data), Lang.TURTLE);
            model.add(m);
        }       
    }
    
    static Model getModelFromFileName(String fname, Lang lang) {
        Model m = ModelFactory.createDefaultModel();
        Graph g = m.getGraph();
        try {
            RDFParserBuilder pb = RDFParser.create()
                     .source(fname)
                     .lang(lang);                    
            pb.parse(StreamRDFLib.graph(g));
        } catch (RiotException e) {
            log.error("error reading "+fname, e);
            e.printStackTrace();
            return null;
        }       
        return m;
    } 
}
