package io.bdrc.ldspdi.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.LdsPdiExceptionHandler;
import io.bdrc.ldspdi.ontology.service.core.OntData;
import io.bdrc.ldspdi.rest.controllers.PublicTemplatesController;
import io.bdrc.ldspdi.service.ServiceConfig;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { PublicTemplatesController.class, OntData.class, LdsPdiExceptionHandler.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration
public class TemplatesTest1 {

    @Autowired
    Environment environment;

    private static FusekiServer server;
    private static Dataset srvds = DatasetFactory.createTxnMem();
    private static Model model = ModelFactory.createDefaultModel();
    public static String fusekiUrl;
    public final static Logger log = LoggerFactory.getLogger(ResServiceTest.class.getName());
    public final static String[] methods = { "GET", "POST" };

    @BeforeClass
    public static void init() throws JsonParseException, JsonMappingException, IOException {
        fusekiUrl = "http://localhost:2247/bdrcrw";
        ServiceConfig.initForTests(fusekiUrl);
        Utils.loadDataInModel(model);
        srvds.setDefaultModel(model);
        // Creating a fuseki server
        server = FusekiServer.create().port(2247).add("/bdrcrw", srvds).build();
        server.start();
    }

    @AfterClass
    public static void close() {
        server.stop();
        server.join();
    }

    @Test
    public void simplePost() throws JsonProcessingException, IOException {
        ObjectMapper om = new ObjectMapper();
        Map<String, String> args = new HashMap<>();
        args.put("R_RES", "bdr:R8LS12819");
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost("http://localhost:" + environment.getProperty("local.server.port") + "/query/graph/graph");
        post.addHeader("Accept", "application/ld+json");
        post.addHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(om.writeValueAsString(args)));
        HttpResponse resp = client.execute(post);
        assertTrue(resp.getStatusLine().getStatusCode() == 200);
        String entity = EntityUtils.toString(resp.getEntity());
        final String expected = "{\n" + "  \"@id\" : \"bdr:R8LS12819\",\n" + "  \"adm:status\" : \"bdr:StatusReleased\",\n" + "  \"@context\" : \"http://purl.bdrc.io/context.jsonld\"\n" + "}\n";
        assertTrue(entity.equals(expected));
    }

    @Test
    public void wrongTemplateNameGet() throws JsonProcessingException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/query/table/wrongTemplateName");
        HttpResponse resp = client.execute(get);
        String entity = EntityUtils.toString(resp.getEntity());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(entity);
        int code = Integer.parseInt(node.findValue("code").asText());
        assertTrue(resp.getStatusLine().getStatusCode() == 500);
        assertTrue(code == LdsError.PARSE_ERR);
    }

    @Test
    public void wrongTemplateNamePost() throws JsonProcessingException, IOException {
        Map<String, String> map = new HashMap<>();
        map.put("L_NAME", "dgon gsar");
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost("http://localhost:" + environment.getProperty("local.server.port") + "/query/table/wrongTemplateName");
        post.addHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(map)));
        HttpResponse resp = client.execute(post);
        String entity = EntityUtils.toString(resp.getEntity());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(entity);
        int code = Integer.parseInt(node.findValue("code").asText());
        assertTrue(resp.getStatusLine().getStatusCode() == 500);
        assertTrue(code == LdsError.PARSE_ERR);
    }

    @Test
    public void TemplateGet() throws JsonProcessingException, IOException, NumberFormatException, URISyntaxException {
        URI uri = new URIBuilder().setScheme("http").setHost("localhost").setPort(Integer.parseInt(environment.getProperty("local.server.port"))).setPath("/query/table/missingArg").setParameter("L_NAME", "rgyal").build();
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(uri);
        HttpResponse resp = client.execute(get);
        System.out.println("STATUS >>" + resp.getStatusLine());
        assertTrue(resp.getStatusLine().getStatusCode() == 200);
    }

    @Test
    public void TemplatePost() throws JsonProcessingException, IOException {
        Map<String, String> map = new HashMap<>();
        map.put("L_NAME", "rgyal");
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost("http://localhost:" + environment.getProperty("local.server.port") + "/query/table/missingArg");
        post.addHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(map)));
        HttpResponse resp = client.execute(post);
        assertTrue(resp.getStatusLine().getStatusCode() == 200);
    }

    @Test
    public void missingParameter() throws JsonProcessingException, IOException {

        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/query/table/missingArg");
        HttpResponse resp = client.execute(get);
        String entity = EntityUtils.toString(resp.getEntity());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(entity);
        int code = Integer.parseInt(node.findValue("code").asText());
        assertTrue(resp.getStatusLine().getStatusCode() == 500);
        assertTrue(code == LdsError.MISSING_PARAM_ERR);

        Map<String, String> map = new HashMap<>();
        HttpClient client1 = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost("http://localhost:" + environment.getProperty("local.server.port") + "/query/table/missingArg");
        post.addHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(map)));
        HttpResponse resp1 = client1.execute(post);
        String entity1 = EntityUtils.toString(resp1.getEntity());
        ObjectMapper mapper1 = new ObjectMapper();
        JsonNode node1 = mapper1.readTree(entity1);
        int code1 = Integer.parseInt(node1.findValue("code").asText());
        assertTrue(resp1.getStatusLine().getStatusCode() == 500);
        assertTrue(code1 == LdsError.MISSING_PARAM_ERR);

    }

    @Test
    public void WrongParamNameGet() throws JsonProcessingException, IOException, NumberFormatException, URISyntaxException {
        URI uri = new URIBuilder().setScheme("http").setHost("localhost").setPort(Integer.parseInt(environment.getProperty("local.server.port"))).setPath("/query/table/missingArg").setParameter("WRONG", "rgyal").build();
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(uri);
        HttpResponse resp = client.execute(get);
        String entity = EntityUtils.toString(resp.getEntity());
        System.out.println("ENTITY >>" + entity);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(entity);
        int code = Integer.parseInt(node.findValue("code").asText());
        assertTrue(resp.getStatusLine().getStatusCode() == 500);
        assertTrue(code == LdsError.MISSING_PARAM_ERR);
    }

    @Test
    public void WrongParamNamePost() throws JsonProcessingException, IOException {
        Map<String, String> map = new HashMap<>();
        map.put("WRONG", "rgyal");
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost("http://localhost:" + environment.getProperty("local.server.port") + "/query/table/missingArg");
        post.addHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(map)));
        HttpResponse resp = client.execute(post);
        String entity = EntityUtils.toString(resp.getEntity());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(entity);
        int code = Integer.parseInt(node.findValue("code").asText());
        assertTrue(resp.getStatusLine().getStatusCode() == 500);
        assertTrue(code == LdsError.MISSING_PARAM_ERR);
    }

    @Test
    public void wrongGraphTemplateNameGet() throws JsonProcessingException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/query/graph/wrongTemplateName");
        HttpResponse resp = client.execute(get);
        String entity = EntityUtils.toString(resp.getEntity());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(entity);
        int code = Integer.parseInt(node.findValue("code").asText());
        assertTrue(resp.getStatusLine().getStatusCode() == 500);
        assertTrue(code == LdsError.PARSE_ERR);
    }

    @Test
    public void wrongGraphTemplateNamePost() throws JsonProcessingException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost("http://localhost:" + environment.getProperty("local.server.port") + "/query/graph/wrongTemplateName");
        post.setHeader("Content-Type", "application/json");
        post.setHeader("Accept", "application/json");
        post.setEntity(new StringEntity("{\"ddd\":\"\"}"));
        HttpResponse resp = client.execute(post);
        String entity = EntityUtils.toString(resp.getEntity());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(entity);
        int code = Integer.parseInt(node.findValue("code").asText());
        assertTrue(resp.getStatusLine().getStatusCode() == 500);
        assertTrue(code == LdsError.PARSE_ERR);
    }

}
