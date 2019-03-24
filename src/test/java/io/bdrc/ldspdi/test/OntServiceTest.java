package io.bdrc.ldspdi.test;

import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.restapi.exceptions.RestExceptionMapper;

public class OntServiceTest extends JerseyTest {

    public final static Logger log = LoggerFactory.getLogger(OntServiceTest.class.getName());

    @Override
    protected Application configure() {
        return new ResourceConfig(TestResource.class).register(RestExceptionMapper.class);
    }

    @Test
    public void loadOnto() {
        Response res = target("ontology/admin").request().accept("text/turtle").get();
        // Response res =
        // target("onto/admin").request().accept("application/rdf+xml").get();
        System.out.println("TARGET >>" + target("ontology/admin").request() + " Status=" + res.getStatus());
        assertTrue(res.getStatus() == 200);
        // System.out.println(">>>>>>>>>>>>>>>>>>>>>" + res.readEntity(String.class));
        // assertTrue(res.readEntity(String.class).equals(Helpers.getMultiChoicesHtml("/resource/C68",true)));
    }

    /*
     * @Test public void loadAdminTTL() throws ClientProtocolException, IOException
     * { HttpClient client = HttpClientBuilder.create().build(); HttpGet get = new
     * HttpGet(this.getBaseUri() + "ontology/admin"); // get.addHeader("Accept",
     * "text/turtle"); get.addHeader("Accept", "application/rdf+xml");
     * System.out.println("URL >>> " + this.getBaseUri() + "ontology/admin");
     * HttpResponse resp = client.execute(get); System.out.println("STATUS >>> " +
     * resp.getStatusLine()); assert (resp.getStatusLine().getStatusCode() == 200);
     * }
     * 
     * @Test public void loadAccessTTL() throws ClientProtocolException, IOException
     * { HttpClient client = HttpClientBuilder.create().build(); HttpGet get = new
     * HttpGet(this.getBaseUri() + "ontology/Access"); // get.addHeader("Accept",
     * "text/turtle"); get.addHeader("Accept", "application/rdf+xml");
     * System.out.println("URL >>> " + this.getBaseUri() + "ontology/Access");
     * HttpResponse resp = client.execute(get); System.out.println("STATUS >>> " +
     * resp.getStatusLine()); assert (resp.getStatusLine().getStatusCode() == 200);
     * }
     */

}