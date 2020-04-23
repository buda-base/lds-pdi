package io.bdrc.ldspdi.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.jena.atlas.io.StringWriterI;
import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.impl.XMLLiteralType;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.out.NodeFormatterTTL;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
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

import io.bdrc.ldspdi.rest.resources.PublicDataResource;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.libraries.BudaMediaTypes;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { PublicDataResource.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration
public class ResServiceTest {

    @Autowired
    Environment environment;

    private static FusekiServer server;
    private static Dataset srvds = DatasetFactory.createTxnMem();
    private static Model model = ModelFactory.createDefaultModel();
    public static String fusekiUrl;
    public final static Logger log = LoggerFactory.getLogger(ResServiceTest.class.getName());

    @BeforeClass
    public static void init() throws JsonParseException, JsonMappingException, IOException {
        fusekiUrl = "http://localhost:2246/bdrcrw";
        ServiceConfig.initForTests(fusekiUrl);
        Utils.loadDataInModel(model);
        srvds.setDefaultModel(model);
        // Creating a fuseki server
        server = FusekiServer.create().port(2246).add("/bdrcrw", srvds).build();
        server.start();
    }

    @AfterClass
    public static void close() {
        server.stop();
        server.join();
    }

    @Test
    public void AllOk() throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/resource/C68.ttl");
        HttpResponse resp = client.execute(get);
        System.out.println("STATUS AllOk() >> " + resp.getStatusLine().getStatusCode());
        assertTrue(resp.getStatusLine().getStatusCode() == 200);
        resp.getEntity().writeTo(System.out);
    }

    @Test
    public void html() throws ClientProtocolException, IOException {
        HttpClient client = HttpClientBuilder.create().disableRedirectHandling().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/resource/P1AG29");
        get.setHeader("Accept", "text/html");
        HttpResponse resp = client.execute(get);
        resp.getEntity().writeTo(System.out);
        System.out.println("STATUS >> " + resp.getStatusLine().getStatusCode());
        assertTrue(resp.getStatusLine().getStatusCode() == 302);
        assertTrue(resp.getFirstHeader("TCN").getValue().equals("Choice"));
        assertTrue(checkAlternates("/resource/P1AG29", resp.getFirstHeader("Alternates").getValue()));
    }

    @Test
    public void wrongAccept() throws ClientProtocolException, IOException {
        HttpClient client = HttpClientBuilder.create().disableRedirectHandling().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/resource/test");
        get.setHeader("Accept", "image/jpeg");
        HttpResponse resp = client.execute(get);
        assertTrue(resp.getStatusLine().getStatusCode() == 406);
        assertTrue(resp.getFirstHeader("TCN").getValue().equals("List"));

    }

    @Test
    public void GetResourceByExtension() throws ClientProtocolException, IOException {
        Set<String> map = BudaMediaTypes.getResExtensionMimeMap().keySet();
        for (String ent : map) {
            if (ent.equals("html"))
                continue;
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/resource/P1AG29." + ent);
            HttpResponse resp = client.execute(get);
            assertTrue(resp.getFirstHeader("Content-Type").getValue().equals(BudaMediaTypes.getMimeFromExtension(ent).toString()));
            assertTrue(checkAlternates("/resource/P1AG29", resp.getFirstHeader("Alternates").getValue()));
        }
    }

    @Test
    public void WrongResource() throws ClientProtocolException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/resource/wrong.ttl");
        HttpResponse resp = client.execute(get);
        assertTrue(resp.getStatusLine().getStatusCode() == 404);
    }

    @Test
    public void WrongExt() throws ParseException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/resource/C68.wrong");
        HttpResponse resp = client.execute(get);
        assertTrue(resp.getStatusLine().getStatusCode() == 300);
        assertTrue(EntityUtils.toString(resp.getEntity()).equals(Helpers.getMultiChoicesHtml("/resource/C68", true)));
    }

    @Test
    public void nores() throws ClientProtocolException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/resource/nonexistant");
        get.setHeader("Accept", "text/turtle");
        HttpResponse resp = client.execute(get);
        assertTrue(resp.getStatusLine().getStatusCode() == 404);

    }

    @Test
    public void testHtmlLitFormatter() {
        PrefixMap pm = PrefixMapFactory.create();
        pm.add("xsd", "http://www.w3.org/2001/XMLSchema#");
        pm.add("owl", "http://www.w3.org/2002/07/owl#");
        pm.add("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

        NodeFormatterTTL nfttl = new NodeFormatterTTL(null, pm, null);
        Model m = ModelFactory.createDefaultModel();
        Literal l = m.createTypedLiteral("2009-10-22T18:31:49.12Z", XSDDatatype.XSDdateTime);
        Literal l1 = m.createTypedLiteral(3.141592, new BaseDatatype("http://www.w3.org/2002/07/owl#real"));
        Literal l2 = m.createTypedLiteral("Dharma is beautiful", XMLLiteralType.theXMLLiteralType);
        Literal l3 = m.createTypedLiteral("true", XSDDatatype.XSDboolean);
        Literal l4 = m.createLiteral("rgyud bla ma", "bo-x-ewts");
        Literal l5 = m.createTypedLiteral("buddha is goodness", XSDDatatype.XSDstring);
        Literal l6 = m.createTypedLiteral(-5, XSDDatatype.XSDinteger);

        StringWriterI sw = new StringWriterI();
        nfttl.formatLiteral(sw, l.asNode());
        sw.flush();
        assertTrue(sw.toString().equals("\"2009-10-22T18:31:49.12Z\"^^xsd:dateTime"));

        sw = new StringWriterI();
        nfttl.formatLiteral(sw, l1.asNode());
        sw.flush();
        assertTrue(sw.toString().equals("\"3.141592\"^^owl:real"));

        sw = new StringWriterI();
        nfttl.formatLiteral(sw, l2.asNode());
        sw.flush();
        assertTrue(sw.toString().equals("\"Dharma is beautiful\"^^rdf:XMLLiteral"));

        sw = new StringWriterI();
        nfttl.formatLiteral(sw, l3.asNode());
        sw.flush();
        assertTrue(sw.toString().equals("true"));

        sw = new StringWriterI();
        nfttl.formatLiteral(sw, l4.asNode());
        sw.flush();
        assertTrue(sw.toString().equals("\"rgyud bla ma\"@bo-x-ewts"));

        sw = new StringWriterI();
        nfttl.formatLiteral(sw, l5.asNode());
        sw.flush();
        assertTrue(sw.toString().equals("\"buddha is goodness\""));

        sw = new StringWriterI();
        nfttl.formatLiteral(sw, l6.asNode());
        sw.flush();
        assertTrue(sw.toString().equals("-5"));

    }

    public boolean checkAlternates(String url, String alternates) {
        HashMap<String, MediaType> map = BudaMediaTypes.getResExtensionMimeMap();
        StringBuilder sb = new StringBuilder("");
        for (Entry<String, MediaType> e : map.entrySet()) {
            sb.append("{\"" + url + "." + e.getKey() + "\" 1.000 {type " + e.getValue().toString() + "}},");
        }
        String alt = sb.toString().substring(0, sb.toString().length() - 1);
        List<String> alt1 = Arrays.asList(alternates.split(","));
        List<String> alt2 = Arrays.asList(alt.split(","));
        for (String test : alt2) {
            if (!alt1.contains(test)) {
                log.error("Alternates check failed >>> " + test + alt1.contains(test));
                System.out.println("Alternates check failed >>> " + test + alt1.contains(test));
                return false;
            }
        }
        return true;
    }
}
