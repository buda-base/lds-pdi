package io.bdrc.ldspdi.test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.text.WordUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
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
import com.fasterxml.jackson.databind.JsonMappingException;

import io.bdrc.ldspdi.export.CSLJsonExport;
import io.bdrc.ldspdi.export.CSLJsonExport.CSLResObj;
import io.bdrc.ldspdi.export.RISExport;
import io.bdrc.ldspdi.export.RISExport.RISObject;
import io.bdrc.ldspdi.rest.controllers.CitationFormatsController;
import io.bdrc.ldspdi.rest.controllers.PublicDataController;
import io.bdrc.ldspdi.service.ServiceConfig;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { CitationFormatsController.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration
public class CSLExportTest {

    @Autowired
    Environment environment;

    private static FusekiServer server;
    private static Dataset srvds = DatasetFactory.createTxnMem();
    private static Model model = ModelFactory.createDefaultModel();
    public static String fusekiUrl;
    public final static Logger log = LoggerFactory.getLogger(MarcExportTest.class.getName());

    @BeforeClass
    public static void init() throws JsonParseException, JsonMappingException, IOException {
        fusekiUrl = "http://localhost:2251/bdrcrw";
        //ServiceConfig.initForTests(fusekiUrl);
        Utils.loadDataInModel(model);
        srvds.setDefaultModel(model);
        // try {
        // OntData.init();
        // } catch (RestException e) {
        // e.printStackTrace();
        // }
        // Creating a fuseki server
        server = FusekiServer.create().port(2251).add("/bdrcrw", srvds).build();
        server.start();
        ServiceConfig.initForTests(fusekiUrl);
    }

    @AfterClass
    public static void close() {
        server.stop();
        server.join();
    }
    
    @Test
    public void testSimpleRequestSimple() throws ClientProtocolException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/CSLObj/bdr:MW22084_0044-31");
        HttpResponse response = client.execute(get);
        System.out.println(response.getStatusLine().getStatusCode());
        System.out.println("result:");
        response.getEntity().writeTo(System.out);
        //try (FileOutputStream fos = new FileOutputStream("/tmp/csl.json")) {
        //    response.getEntity().writeTo(fos);
        //}
    }
    
    @Test
    public void testDirectCall() throws FileNotFoundException, IOException {
        final Model m = Utils.getModelFromFileName(Utils.TESTDIR + "CSLExportTest-afterRequest.ttl", Lang.TURTLE);
        Resource main = m.getResource("http://purl.bdrc.io/resource/MW22084_0044-31");
        CSLResObj res = CSLJsonExport.getObject(m, main);
        try (FileOutputStream fos = new FileOutputStream("/tmp/csl-direct.json")) {
            final String s = res.mapper.writeValueAsString(res);
            fos.write(s.getBytes());
        }
        main = m.getResource("http://purl.bdrc.io/resource/MW00KG09211");
        res = CSLJsonExport.getObject(m, main);
        try (FileOutputStream fos = new FileOutputStream("/tmp/csl-direct-2.json")) {
            final String s = res.mapper.writeValueAsString(res);
            fos.write(s.getBytes());
        }
    }
    
    
    @Test
    public void testRISDirect() throws ClientProtocolException, IOException {
        final Model m = Utils.getModelFromFileName(Utils.TESTDIR + "CSLExportTest-afterRequest.ttl", Lang.TURTLE);
        Resource main = m.getResource("http://purl.bdrc.io/resource/MW22084_0044-31");
        CSLResObj csl = CSLJsonExport.getObject(m, main);
        RISObject ris = RISExport.RISFromCSL(csl, "latn");
        System.out.println("result:");
        System.out.println(ris.toString());
        //try (FileOutputStream fos = new FileOutputStream("/tmp/csl.json")) {
        //    response.getEntity().writeTo(fos);
        //}
    }
    
    @Test
    public void testRISSimpleRequestSimple() throws ClientProtocolException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/RIS/bdr:MW22084_0044-31/latn");
        HttpResponse response = client.execute(get);
        System.out.println(response.getStatusLine().getStatusCode());
        System.out.println("result:");
        response.getEntity().writeTo(System.out);
        //try (FileOutputStream fos = new FileOutputStream("/tmp/csl.json")) {
        //    response.getEntity().writeTo(fos);
        //}
    }

}
