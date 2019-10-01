package io.bdrc.ldspdi.test;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import com.auth0.client.auth.AuthAPI;
import com.auth0.exception.Auth0Exception;
import com.auth0.net.AuthRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.ldspdi.service.ServiceConfig;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TestResource.class, RdfAuthTestFilter.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration
public class EndpointAccessTest {

    @Autowired
    Environment environment;

    static AuthAPI auth;
    static String token;
    static String publicToken;
    static String adminToken;

    @BeforeClass
    public static void init() throws Exception {
        ServiceConfig.initForTests("");
        RdfAuthModel.initForStaticTests();
        auth = new AuthAPI("bdrc-io.auth0.com", AuthProps.getProperty("lds-pdiClientID"), AuthProps.getProperty("lds-pdiClientSecret"));
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost("https://bdrc-io.auth0.com/oauth/token");
        HashMap<String, String> json = new HashMap<>();
        json.put("grant_type", "client_credentials");
        json.put("client_id", AuthProps.getProperty("lds-pdiClientID"));
        json.put("client_secret", AuthProps.getProperty("lds-pdiClientSecret"));
        json.put("audience", "https://bdrc-io.auth0.com/api/v2/");
        ObjectMapper mapper = new ObjectMapper();
        String post_data = mapper.writer().writeValueAsString(json);
        StringEntity se = new StringEntity(post_data);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(se);
        HttpResponse response = client.execute(post);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        response.getEntity().writeTo(baos);
        String json_resp = baos.toString();
        baos.close();
        JsonNode node = mapper.readTree(json_resp);
        token = node.findValue("access_token").asText();
        setPublicToken();
        setAdminToken();
    }

    @Test
    public void NoTokenPublicAccess() throws ClientProtocolException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/auth/public");
        System.out.println("URL >>> " + "http://localhost:" + environment.getProperty("local.server.port") + "/auth/public");
        HttpResponse resp = client.execute(get);
        System.out.println("STATUS >>> " + resp.getStatusLine());
        assertTrue(resp.getStatusLine().getStatusCode() == 200);
    }

    @Test
    public void securedEndpointAccess() throws IOException, IllegalArgumentException {

        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/auth/rdf/admin");
        get.setHeader("Authorization", "Bearer " + publicToken);
        HttpResponse resp = client.execute(get);
        System.out.println("ENTITY >>" + EntityUtils.toString(resp.getEntity()));
        System.out.println("STATUS 1 >>> " + resp.getStatusLine());
        assertTrue(resp.getStatusLine().getStatusCode() == 403);

        client = HttpClientBuilder.create().build();
        get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/auth/rdf/admin");
        get.setHeader("Authorization", "Bearer " + adminToken);
        resp = client.execute(get);
        System.out.println("STATUS 2 >>> " + resp.getStatusLine());
        assertTrue(resp.getStatusLine().getStatusCode() == 200);
    }

    private static void setPublicToken() {
        AuthRequest req = auth.login("publicuser@bdrc.com", AuthProps.getProperty("publicuser@bdrc.com"));
        req.setScope("openid offline_access");
        req.setAudience("https://bdrc-io.auth0.com/api/v2/");
        try {
            publicToken = req.execute().getIdToken();
        } catch (Auth0Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }
    }

    private static void setAdminToken() {
        AuthRequest req = auth.login("tchame@rimay.net", AuthProps.getProperty("tchame@rimay.net"));
        req.setScope("openid offline_access");
        req.setAudience("https://bdrc-io.auth0.com/api/v2/");
        try {
            adminToken = req.execute().getIdToken();
        } catch (Auth0Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }
    }
}
