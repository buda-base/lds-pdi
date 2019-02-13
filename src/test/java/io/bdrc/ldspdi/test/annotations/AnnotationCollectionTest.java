package io.bdrc.ldspdi.test.annotations;

import java.io.IOException;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.bdrc.ldspdi.annotations.AnnotationCollectionEndpoint;
import io.bdrc.ldspdi.annotations.AnnotationEndpoint;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.test.Utils;
import io.bdrc.ldspdi.utils.MediaTypeUtils;
import io.bdrc.restapi.exceptions.RestExceptionMapper;

public class AnnotationCollectionTest extends JerseyTest {

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

    @Override
    protected Application configure() {
        return new ResourceConfig(AnnotationCollectionEndpoint.class).register(RestExceptionMapper.class);
    }

    @Test
    public void wholeCollection() throws JsonProcessingException, IOException {
        final Response res = target("/anncollection/ANCVOL1")
                .request()
                .header("Accept", MediaTypeUtils.MT_JSONLD)
                .header("Prefer", AnnotationCollectionEndpoint.PREFER_OA_PCI)
                .get();
        System.out.println("result:");
        System.out.println(res.getStatus());
        System.out.println(res.readEntity(String.class));
    }

    @Test
    public void wholeCollectionPage() throws JsonProcessingException, IOException {
        final Response res = target("/anncollection/ANCVOL1/pi/1")
                .request()
                .header("Accept", MediaTypeUtils.MT_JSONLD)
                .header("Prefer", AnnotationCollectionEndpoint.PREFER_OA_PCD)
                .get();
        System.out.println("result:");
        System.out.println(res.getStatus());
        System.out.println(res.readEntity(String.class));
    }

    @Test
    public void subCollection() throws JsonProcessingException, IOException {
        final Response res = target("/anncollection/ANCVOL1/sub/pages/1-5")
                .request()
                .header("Accept", MediaTypeUtils.MT_JSONLD)
                .header("Prefer", AnnotationEndpoint.LDP_PMC)
                .get();
        System.out.println("result:");
        System.out.println(res.getStatus());
        System.out.println(res.readEntity(String.class));
    }

    @Test
    public void subCollectionPage() throws JsonProcessingException, IOException {
        final Response res = target("/anncollection/ANCVOL1/sub/pages/1-5/pd/1")
                .request()
                .header("Accept", MediaTypeUtils.MT_JSONLD)
                .header("Prefer", AnnotationEndpoint.LDP_PMC)
                .get();
        System.out.println("result:");
        System.out.println(res.getStatus());
        System.out.println(res.readEntity(String.class));
    }

    @Test
    public void subCollectionPageSuffix() throws JsonProcessingException, IOException {
        final Response res = target("/anncollection/ANCVOL1/sub/pages/1-5/pd/1.jsonld")
                .request()
                .get();
        System.out.println(res.getHeaders());
        System.out.println("result:");
        System.out.println(res.getStatus());
        System.out.println(res.readEntity(String.class));
    }

}
