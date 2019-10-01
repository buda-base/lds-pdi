package io.bdrc.ldspdi.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.bdrc.ldspdi.rest.resources.PublicDataResource;
import io.bdrc.ldspdi.service.ServiceConfig;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { PublicDataResource.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration

public class TxtExportTest {

    @Autowired
    Environment environment;

    private static FusekiServer server;
    private static Dataset srvds = DatasetFactory.createTxnMem();
    private static Model model = ModelFactory.createDefaultModel();
    public static String fusekiUrl;

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
    public void testSimpleRequestSimple() throws NumberFormatException, URISyntaxException, ClientProtocolException, IOException {
        URI uri = new URIBuilder().setScheme("http").setHost("localhost").setPort(Integer.parseInt(environment.getProperty("local.server.port"))).setPath("/resource/UT11577_004_0000.txt").setParameter("startChar", Integer.toString(1234))
                .setParameter("endChar", Integer.toString(2444)).build();
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(uri);
        HttpResponse resp = client.execute(get);
        System.out.println(resp.getStatusLine());
        resp.getEntity().writeTo(System.out);
        assertTrue(resp.getStatusLine().getStatusCode() == 200);
    }
}
