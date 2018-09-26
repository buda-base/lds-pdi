package io.bdrc.ldspdi.test;

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

import io.bdrc.ldspdi.rest.resources.PublicDataResource;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.restapi.exceptions.RestExceptionMapper;

public class MarcExportTest extends JerseyTest {

    private static FusekiServer server;
    private static Dataset srvds = DatasetFactory.createTxnMem();
    private static Model model = ModelFactory.createDefaultModel();
    public static String fusekiUrl;
    public final static Logger log = LoggerFactory.getLogger(CsvTest.class.getName());

    @BeforeClass
    public static void init() {
        fusekiUrl = "http://localhost:2251/bdrcrw";
        ServiceConfig.initForTests(fusekiUrl);
        Utils.loadDataInModel(model);
        srvds.setDefaultModel(model);
        // Creating a fuseki server
        server = FusekiServer.create().setPort(2251).add("/bdrcrw", srvds).build();
        server.start();
    }

    @AfterClass
    public static void close() {
        server.stop();
        server.join();
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(PublicDataResource.class).register(RestExceptionMapper.class);
    }

    @Test
    public void testSimpleRequestSimple() {
        final Response res = target("/resource/W23819.mrcx")
                .request()
                .get();
        System.out.println("result:");
        System.out.println(res.readEntity(String.class));
    }

}
