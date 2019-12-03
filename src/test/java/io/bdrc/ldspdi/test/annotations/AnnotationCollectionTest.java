package io.bdrc.ldspdi.test.annotations;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.bdrc.ldspdi.annotations.AnnotationCollectionEndpoint;
import io.bdrc.ldspdi.annotations.AnnotationEndpoint;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.test.Utils;
import io.bdrc.libraries.BudaMediaTypes;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AnnotationCollectionEndpoint.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration
public class AnnotationCollectionTest {

    @Autowired
    Environment environment;

    private static FusekiServer server;
    private static Dataset srvds = DatasetFactory.createTxnMem();
    private static Model model = ModelFactory.createDefaultModel();
    public static String fusekiUrl;
    public final static Logger log = LoggerFactory.getLogger(AnnotationCollectionTest.class.getName());

    @BeforeClass
    public static void init() throws JsonParseException, JsonMappingException, IOException {
        fusekiUrl = "http://localhost:2249/bdrcrw";
        ServiceConfig.initForTests(fusekiUrl);
        Utils.loadDataInModel(model);
        srvds.setDefaultModel(model);
        // Creating a fuseki server
        server = FusekiServer.create().port(2249).add("/bdrcrw", srvds).build();
        server.start();
    }

    @AfterClass
    public static void close() {
        server.stop();
        server.join();
    }

    // @Test
    public void wholeCollection() {
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/anncollection/ANCVOL1");
            MediaType mt = BudaMediaTypes.MT_JSONLD;
            get.addHeader("Accept", mt.getType() + "/" + mt.getSubtype());
            get.addHeader("Prefer", AnnotationCollectionEndpoint.PREFER_OA_PCI);
            HttpResponse response = client.execute(get);
            System.out.println("wholeCollection result:");
            System.out.println(response.getStatusLine().getStatusCode());
            response.getEntity().writeTo(System.out);
            assert (response.getStatusLine().getStatusCode() == 200);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // @Test
    public void wholeCollectionPage() {
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/anncollection/ANCVOL1/pi/1");
            MediaType mt = BudaMediaTypes.MT_JSONLD;
            get.addHeader("Accept", mt.getType() + "/" + mt.getSubtype());
            get.addHeader("Prefer", AnnotationCollectionEndpoint.PREFER_OA_PCD);
            HttpResponse response;
            response = client.execute(get);
            System.out.println("wholeCollectionPage result:");
            System.out.println(response.getStatusLine().getStatusCode());
            response.getEntity().writeTo(System.out);
            assert (response.getStatusLine().getStatusCode() == 200);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // @Test
    public void subCollection() {
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/anncollection/ANCVOL1/sub/pages/1-5");
            MediaType mt = BudaMediaTypes.MT_JSONLD;
            get.addHeader("Accept", mt.getType() + "/" + mt.getSubtype());
            get.addHeader("Prefer", AnnotationEndpoint.LDP_PMC);
            HttpResponse response = client.execute(get);
            System.out.println("subCollection result:");
            System.out.println(response.getStatusLine().getStatusCode());
            response.getEntity().writeTo(System.out);
            assert (response.getStatusLine().getStatusCode() == 200);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // @Test
    public void subCollectionPage() {
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/anncollection/ANCVOL1/sub/pages/1-5/pd/1");
            MediaType mt = BudaMediaTypes.MT_JSONLD;
            get.addHeader("Accept", mt.getType() + "/" + mt.getSubtype());
            get.addHeader("Prefer", AnnotationEndpoint.LDP_PMC);
            HttpResponse response = client.execute(get);
            System.out.println("subCollectionPage result:");
            System.out.println(response.getStatusLine().getStatusCode());
            response.getEntity().writeTo(System.out);
            assert (response.getStatusLine().getStatusCode() == 200);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void subCollectionPageSuffix() {
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/anncollection/ANCVOL1/sub/pages/1-5/pd/1.jsonld");
            HttpResponse response = client.execute(get);
            System.out.println("subCollectionPageSuffix result:");
            System.out.println(response.getStatusLine().getStatusCode());
            response.getEntity().writeTo(System.out);
            // assert (response.getStatusLine().getStatusCode() == 200);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
