package io.bdrc.ldspdi.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.apache.jena.fuseki.embedded.FusekiServer;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.StreamRDFLib;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.ldspdi.rest.resources.PublicTemplatesResource;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.restapi.exceptions.RestExceptionMapper;

public class TemplatesTest extends JerseyTest{
    
    private static FusekiServer server ;
    private static Dataset srvds = DatasetFactory.createTxnMem();
    private static Model model = ModelFactory.createDefaultModel(); 
    public static String fusekiUrl;
    public final static Logger log=LoggerFactory.getLogger(ResServiceTest.class.getName());       
    public final static String[] methods= {"GET", "POST"};
    
    @BeforeClass
    public static void init() {        
        ServiceConfig.initForTests();
        loadData();     
        srvds.setDefaultModel(model);
        //Creating a fuseki server
        server = FusekiServer.create()
                .setPort(2244)
                .add("/bdrcrw", srvds)
                .build() ;
        fusekiUrl="http://localhost:2244/bdrcrw";       
        server.start() ;         
    }
    
    @AfterClass
    public static void close() {
        server.stop();
        server.join();
    }
    
    @Override
    protected Application configure() {
        return new ResourceConfig(PublicTemplatesResource.class).register(RestExceptionMapper.class);
    }
    
    @Test
    public void wrongTemplateName() throws JsonProcessingException, IOException {
        Response res = target("/query/wrongTemplateName").request()
                .header("fusekiUrl", fusekiUrl)
                .get();
        String entity=res.readEntity(String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node=mapper.readTree(entity);
        String msg=node.findValue("message").asText();
        log.info("JSON MESSAGE >> "+msg);
        log.info("Query template parsing failed :"
        +ServiceConfig.getProperty("queryPath")+"public/wrongTemplateName.arq  (Aucun fichier ou dossier de ce type)");
        assertTrue(res.getStatus() == 500);
        assert(msg.equals("Query template parsing failed for: wrongTemplateName.arq"));
    }
    
    
    static void loadData(){
        //Loads the test dataset/
        ArrayList<String> list=TestUtils.getResourcesList();
        for(String res : list){
            Model m=getModelFromFileName(TestUtils.TESTDIR+res+".ttl", Lang.TURTLE);
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
            log.error("error reading "+fname);
            e.printStackTrace();
            return null;
        }       
        return m;
    }

}
