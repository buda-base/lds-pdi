package io.bdrc.ldspdi.test.annotations;

import java.io.IOException;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.apache.jena.fuseki.embedded.FusekiServer;
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

import com.fasterxml.jackson.core.JsonProcessingException;

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
    public static void init() {
        fusekiUrl = "http://localhost:2249/bdrcrw";
        ServiceConfig.initForTests(fusekiUrl);
        Utils.loadDataInModel(model);
        srvds.setDefaultModel(model);
        // Creating a fuseki server
        server = FusekiServer.create().setPort(2249).add("/bdrcrw", srvds).build();
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
    public void basicContentType() throws JsonProcessingException, IOException {
        final Response res = target("/anncollection/ANCVOL1")
                .request()
                .header("Accept", MediaTypeUtils.MT_JSONLD)
                .header("Prefer", AnnotationEndpoint.LDP_PMC)
                .get();
        System.out.println(res.getStatus());
        System.out.println(res.readEntity(String.class));
    }

}