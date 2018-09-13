package io.bdrc.ldspdi.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.ldspdi.rest.resources.PublicTemplatesResource;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.utils.MediaTypeUtils;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestExceptionMapper;

public class TemplatesTest extends JerseyTest{

    private static FusekiServer server ;
    private static Dataset srvds = DatasetFactory.createTxnMem();
    private static Model model = ModelFactory.createDefaultModel();
    public static String fusekiUrl;
    public final static Logger log=LoggerFactory.getLogger(ResServiceTest.class.getName());
    public final static String[] methods= {"GET", "POST"};

    @BeforeClass
    public static void init() {
        fusekiUrl="http://localhost:2247/bdrcrw";
        ServiceConfig.initForTests(fusekiUrl);
        Utils.loadDataInModel(model);
        srvds.setDefaultModel(model);
        //Creating a fuseki server
        server = FusekiServer.create()
                .setPort(2247)
                .add("/bdrcrw", srvds)
                .build() ;
        server.start() ;
    }

    @AfterClass
    public static void close() {
        server.stop();
        server.join();
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(PublicTemplatesResource.class).register(RestExceptionMapper.class);
    }

    @Test
    public void simpleGet() throws JsonProcessingException, IOException {
        Map<String, String> args = new HashMap<>();
        args.put("R_RES","bdr:R8LS12819");
        Response res = target("/graph/graph").request()
                .accept(MediaTypeUtils.MT_JSONLD)
                .post(Entity.json(args));
        assertTrue(res.getStatus() == 200);
        String entity = res.readEntity(String.class);
        final String expected = "{\n" +
                "  \"@id\" : \"bdr:R8LS12819\",\n" +
                "  \"adm:status\" : \"bdr:StatusReleased\",\n" +
                "  \"@context\" : \"http://purl.bdrc.io/context.jsonld\"\n" +
                "}\n";
        assertTrue(entity.equals(expected));
    }

    @Test
    public void wrongTemplateNameGet() throws JsonProcessingException, IOException {
        Response res = target("/query/wrongTemplateName").request()
                .get();
        String entity=res.readEntity(String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node=mapper.readTree(entity);
        int code=Integer.parseInt(node.findValue("code").asText());
        assertTrue(res.getStatus() == 500);
        assertTrue(code==LdsError.PARSE_ERR);
    }

    @Test
    public void wrongTemplateNamePost() throws JsonProcessingException, IOException {
        MultivaluedMap<String, String> map=new MultivaluedHashMap<>();
        map.add("L_NAME","dgon gsar");
        Response res = target("/query/wrongTemplateName").request()
                .post(Entity.form(map));
        String entity=res.readEntity(String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node=mapper.readTree(entity);
        int code=Integer.parseInt(node.findValue("code").asText());
        assertTrue(res.getStatus() == 500);
        assertTrue(code==LdsError.PARSE_ERR);
    }

    @Test
    public void TemplateGet() throws JsonProcessingException, IOException {
            Response res = target("/query/missingArg")
                    .queryParam("L_NAME", "rgyal")
                    .request()
                    .get();
            System.out.println("STATUS >>"+res.getStatus());
            assertTrue(res.getStatus() == 200);
    }

    @Test
    public void TemplatePost() throws JsonProcessingException, IOException {
        MultivaluedMap<String, String> map=new MultivaluedHashMap<>();
        map.add("L_NAME","rgyal");
        Response res = target("/query/missingArg").request()
                .post(Entity.form(map));
        assertTrue(res.getStatus() == 200);
    }

    @Test
    public void missingParameter() throws JsonProcessingException, IOException {
        for(String method:methods) {
            Response res = target("/query/missingArg").request()
                    .method(method);
            String entity=res.readEntity(String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node=mapper.readTree(entity);
            int code=Integer.parseInt(node.findValue("code").asText());
            assertTrue(res.getStatus() == 500);
            assertTrue(code==LdsError.MISSING_PARAM_ERR);
        }
    }

    @Test
    public void WrongParamNameGet() throws JsonProcessingException, IOException {
        Response res = target("/query/missingArg)")
                .queryParam("WRONG", "rgyal")
                .request()
                .get();
        String entity=res.readEntity(String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node=mapper.readTree(entity);
        int code=Integer.parseInt(node.findValue("code").asText());
        assertTrue(res.getStatus() == 500);
        assertTrue(code==LdsError.PARSE_ERR);
    }

    @Test
    public void WrongParamNamePost() throws JsonProcessingException, IOException {
        MultivaluedMap<String, String> map=new MultivaluedHashMap<>();
        map.add("WRONG","rgyal");
        Response res = target("/query/missingArg").request()
                .post(Entity.form(map));
        String entity=res.readEntity(String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node=mapper.readTree(entity);
        int code=Integer.parseInt(node.findValue("code").asText());
        assertTrue(res.getStatus() == 500);
        assertTrue(code==LdsError.MISSING_PARAM_ERR);
    }

    @Test
    public void wrongGraphTemplateNameGet() throws JsonProcessingException, IOException {
        Response res = target("/graph/wrongTemplateName").request()
                .get();
        String entity=res.readEntity(String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node=mapper.readTree(entity);
        int code=Integer.parseInt(node.findValue("code").asText());
        assertTrue(res.getStatus() == 500);
        assertTrue(code==LdsError.PARSE_ERR);
    }

    @Test
    public void wrongGraphTemplateNamePost() throws JsonProcessingException, IOException {
        Response res = target("/graph/wrongTemplateName").request()
                .accept(MediaType.APPLICATION_JSON)
                .header("Content-Type","application/json")
                .post(Entity.entity("{\"ddd\":\"\"}",MediaType.APPLICATION_JSON));
        String entity=res.readEntity(String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node=mapper.readTree(entity);
        int code=Integer.parseInt(node.findValue("code").asText());
        assertTrue(res.getStatus() == 500);
        assertTrue(code==LdsError.PARSE_ERR);
    }

}
