package io.bdrc.ldspdi.test;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.apache.jena.fuseki.embedded.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.bdrc.ldspdi.rest.resources.PublicDataResource;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.restapi.exceptions.RestExceptionMapper;

public class MediaTypeTest extends JerseyTest {
    
    public static Model defaultModel = null;
    public static FusekiServer fusekiServer = null;
    public static final String fusekiUrl = "http://localhost:2244/bdrcrw";
    public static final String alternates = "{\"resource/test.rt\" 1.000 {type application/rdf+thrift}},{\"resource/test.nq\" 1.000 {type application/n-quads}},{\"resource/test.rdf\" 1.000 {type application/rdf+xml}},{\"resource/test.owl\" 1.000 {type application/owl+xml}},{\"resource/test.nt\" 1.000 {type application/n-triples}},{\"resource/test.rj\" 1.000 {type application/json}},{\"resource/test.json\" 1.000 {type application/json}},{\"resource/test.trig\" 1.000 {type text/trig}},{\"resource/test.jsonld\" 1.000 {type application/ld+json}},{\"resource/test.trix\" 1.000 {type application/trix+xml}},{\"resource/test.ttl\" 1.000 {type text/turtle}}";    

    @BeforeClass
    public static void init() {
        Dataset srvds = DatasetFactory.createTxnMem();
        defaultModel = ModelFactory.createDefaultModel();
        Resource r = defaultModel.createResource("http://purl.bdrc.io/resource/test");
        defaultModel.add(r, RDF.type, defaultModel.createResource("http://example.com/resourceType"));
        srvds.setDefaultModel(defaultModel);
        fusekiServer = FusekiServer.create()
                .setPort(2244)
                .add("/bdrcrw", srvds)
                .build() ;
        fusekiServer.start();
        ServiceConfig.initForTests();
    }
    
    @AfterClass
    public static void stop() {
        fusekiServer.stop();
        fusekiServer.join();
    }
    
    @Override
    protected Application configure() {
        return new ResourceConfig(PublicDataResource.class).register(RestExceptionMapper.class);
    }

    @Test
    public void html() {
        Response res = target("/resource/test").request()
                .accept("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("fusekiUrl", fusekiUrl)
                .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                .get();
        assertTrue(res.getStatus() == 303);
        assertTrue(res.getHeaderString("Vary").equals("Negotiate, Accept"));
        assertTrue(res.getHeaderString("TCN").equals("Choice"));
        assertTrue(res.getHeaderString("Alternates").equals(alternates));
        assertTrue(res.getHeaderString("Location").equals("http://library.bdrc.io/show/bdr:test"));
    }
    
    @Test
    public void normalResource() {
        Response res = target("/resource/test").request()
                .accept("text/turtle")
                .header("fusekiUrl", fusekiUrl)
                .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                .get();
        assertTrue(res.getStatus() == 200);
        assertTrue(res.getHeaderString("Vary").equals("Negotiate, Accept"));
        assertTrue(res.getHeaderString("TCN").equals("Choice"));
        assertTrue(res.getHeaderString("Alternates").equals(alternates));
        assertTrue(res.getHeaderString("Content-type").equals("text/turtle"));
        assertTrue(res.getHeaderString("Content-Location").equals("resource/test.ttl"));
        InputStream ttl = res.readEntity(InputStream.class);
        Model dist = ModelFactory.createDefaultModel();
        dist.read(ttl, null, "TURTLE");
        assertTrue(dist.isIsomorphicWith(defaultModel));
    }

    @Test
    public void nores() {
        Response res = target("/resource/nonexistant").request()
                .accept("text/turtle")
                .header("fusekiUrl", fusekiUrl)
                .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                .get();
        System.out.println(res.getHeaders());
        assertTrue(res.getStatus() == 404);
    }

    @Test
    public void wrongAccept() {
        Response res = target("/resource/test").request()
                .accept("application/xhtml+xml")
                .header("fusekiUrl", fusekiUrl)
                .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                .get();
        System.out.println(res.getStatus());
        assertTrue(res.getStatus() == 406);
        assertTrue(res.getHeaderString("TCN").equals("List"));
    }
    
}
