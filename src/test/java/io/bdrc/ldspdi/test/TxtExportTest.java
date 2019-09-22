package io.bdrc.ldspdi.test;

import java.io.IOException;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.service.SpringBootLdspdi;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureWebTestClient
@SpringBootTest(classes = SpringBootLdspdi.class)
@ActiveProfiles("local")
public class TxtExportTest {
    private static FusekiServer server;
    private static Dataset srvds = DatasetFactory.createTxnMem();
    private static Model model = ModelFactory.createDefaultModel();
    public static String fusekiUrl;
    public final static Logger log = LoggerFactory.getLogger(TxtExportTest.class.getName());
    
    @Autowired
	private WebTestClient webClient;

    @BeforeClass
    public static void init() throws JsonParseException, JsonMappingException, IOException {
        fusekiUrl = "http://localhost:2252/bdrcrw";
        ServiceConfig.initForTests(fusekiUrl);
        Utils.loadDataInModel(model);
        srvds.setDefaultModel(model);
        // Creating a fuseki server
        server = FusekiServer.create().port(2252).add("/bdrcrw", srvds).build();
        server.start();
    }

    @AfterClass
    public static void close() {
        server.stop();
        server.join();
    }

    @Test
    public void testSimpleRequestSimple() {
        final ResponseSpec res = this.webClient.get().uri(uriBuilder -> uriBuilder
        	    .path("/resource/UT11577_004_0000.txt")
                .queryParam("startChar", 1234)
                .queryParam("endChar", 2444)
    	    .build())
            .exchange();
        res.expectStatus().isOk();
        System.out.println(res.toString());
    }
}
