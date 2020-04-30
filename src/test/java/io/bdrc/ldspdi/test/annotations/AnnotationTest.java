package io.bdrc.ldspdi.test.annotations;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.bdrc.ldspdi.annotations.AnnotationEndpoint;
import io.bdrc.ldspdi.rest.controllers.PublicDataController;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.test.Utils;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AnnotationEndpoint.class, PublicDataController.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration
public class AnnotationTest {

    @Autowired
    Environment environment;

    private static FusekiServer server;
    private static Dataset srvds = DatasetFactory.createTxnMem();
    private static Model model = ModelFactory.createDefaultModel();
    public static String fusekiUrl;
    public final static Logger log = LoggerFactory.getLogger(AnnotationTest.class.getName());
    public final static String JsonLdCTWithAnnoProfile = "application/ld+json;profile=\"http://www.w3.org/ns/anno.jsonld\"";

    public final static String JsonLdCTWithOaProfile = "application/ld+json;profile=\"http://www.w3.org/ns/oa.jsonld\"";

    @BeforeClass
    public static void init() throws JsonParseException, JsonMappingException, IOException {
        fusekiUrl = "http://localhost:2248/bdrcrw";
        ServiceConfig.initForTests(fusekiUrl);
        Utils.loadDataInModel(model);
        srvds.setDefaultModel(model);
        // Creating a fuseki server
        server = FusekiServer.create().port(2248).add("/bdrcrw", srvds).build();
        server.start();
    }

    @AfterClass
    public static void close() {
        server.stop();
        server.join();
    }

    @Test
    public void basicContentType() throws JsonProcessingException, IOException {

        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/annotations/AN1234");
        get.addHeader("Accept", JsonLdCTWithAnnoProfile);
        HttpResponse response = client.execute(get);
        System.out.println("result:");
        System.out.println(response.getStatusLine().getStatusCode());
        HttpEntity ent = response.getEntity();
        // ent.writeTo(System.out);
        System.out.println(EntityUtils.toString(ent, "UTF-8"));
        assert (response.getStatusLine().getStatusCode() == 200);

        // assertTrue(response.getFirstHeader("Content-Type").getValue().equals(JsonLdCTWithAnnoProfile));
    }

    // @Test
    public void basicType() throws JsonProcessingException, IOException {

        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/resource/P1AG29");
        get.addHeader("Accept", "application/ld+json");
        HttpResponse response = client.execute(get);

        System.out.println("result:");
        System.out.println(response.getStatusLine().getStatusCode());
        response.getEntity().writeTo(System.out);
        assert (response.getStatusLine().getStatusCode() == 200);

        // assertTrue(response.getFirstHeader("Content-Type").getValue().equals(JsonLdCTWithAnnoProfile));
    }

}
