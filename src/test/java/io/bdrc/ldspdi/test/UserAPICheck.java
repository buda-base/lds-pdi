package io.bdrc.ldspdi.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.jena.rdf.model.Resource;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import com.auth0.client.auth.AuthAPI;
import com.auth0.net.AuthRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.TokenValidation;
import io.bdrc.auth.UserProfile;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.ldspdi.rest.resources.BdrcAuthResource;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.service.SpringBootLdspdi;
import io.bdrc.ldspdi.users.BudaUser;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.restapi.exceptions.RestException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { BdrcAuthResource.class, SpringBootLdspdi.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserAPICheck {

    public final static Logger log = LoggerFactory.getLogger(UserAPICheck.class.getName());

    static AuthAPI auth;
    static String token;
    static String publicToken;
    static String adminToken;
    static String privateToken;
    static String staffToken;

    @Autowired
    Environment environment;

    @BeforeClass
    public static void init() throws IOException {
        ServiceConfig.initForTests(null);
        Properties props = ServiceConfig.getProperties();
        InputStream input = new FileInputStream("/etc/buda/ldspdi/ldspdi.properties");
        // Properties props = new Properties();
        props.load(input);
        input.close();
        InputStream is = new FileInputStream("/etc/buda/share/shared-private.properties");
        props.load(is);
        AuthProps.init(props);
        is.close();
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
        RdfAuthModel.init();
        log.info("USERS >> {}" + RdfAuthModel.getUsers());
        set123Token();
        set456Token();
        setPrivateToken();
        setStaffToken();
    }

    private static void set123Token() throws IOException {
        AuthRequest req = auth.login("publicuser@bdrc.com", AuthProps.getProperty("publicuser@bdrc.com"));
        req.setScope("openid offline_access");
        req.setAudience("https://bdrc-io.auth0.com/api/v2/");
        publicToken = req.execute().getIdToken();
        log.info("public Token >> {}", publicToken);
    }

    private static void set456Token() throws IOException {
        AuthRequest req = auth.login("tchame@rimay.net", AuthProps.getProperty("tchame@rimay.net"));
        req.setScope("openid offline_access");
        req.setAudience("https://bdrc-io.auth0.com/api/v2/");
        adminToken = req.execute().getIdToken();
        log.info("admin Token >> {}", adminToken);
    }

    private static void setPrivateToken() throws IOException {
        AuthRequest req = auth.login("privateuser@bdrc.com", AuthProps.getProperty("privateuser@bdrc.com"));
        req.setScope("openid offline_access");
        req.setAudience("https://bdrc-io.auth0.com/api/v2/");
        privateToken = req.execute().getIdToken();
        log.info("private Token >> {}", privateToken);
    }

    private static void setStaffToken() throws IOException {
        AuthRequest req = auth.login("staffuser@bdrc.com", AuthProps.getProperty("staffuser@bdrc.com"));
        req.setScope("openid offline_access");
        req.setAudience("https://bdrc-io.auth0.com/api/v2/");
        staffToken = req.execute().getIdToken();
        log.info("staff Token >> {}", staffToken);
    }

    private static String getAPIToken() throws ClientProtocolException, IOException {
        String webTaskBaseUrl = AuthProps.getProperty("webTaskBaseUrl");
        String auth0BaseUrl = AuthProps.getProperty("auth0BaseUrl");
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = null;
        post = new HttpPost(auth0BaseUrl + "oauth/token");
        HashMap<String, String> json = new HashMap<>();
        json.put("grant_type", "client_credentials");
        json.put("client_id", AuthProps.getProperty("lds-pdiClientID"));
        json.put("client_secret", AuthProps.getProperty("lds-pdiClientSecret"));
        json.put("audience", "urn:auth0-authz-api");
        String post_data = new ObjectMapper().writer().writeValueAsString(json);
        StringEntity se = new StringEntity(post_data);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(se);
        HttpResponse response = client.execute(post);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        response.getEntity().writeTo(baos);
        String json_resp = baos.toString();
        baos.close();
        JsonNode node = new ObjectMapper().readTree(json_resp);
        String token = node.findValue("access_token").asText();
        return token;
    }

    // @Test
    public void noToken() throws ClientProtocolException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/resource-nc/user/me");
        HttpResponse resp = client.execute(get);
        log.info("RESP STATUS public resource >> {}", resp.getStatusLine());
        assert (resp.getStatusLine().getStatusCode() == 401);
    }

    // @Test
    public void tokenOfExistingUser() throws ClientProtocolException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/resource-nc/user/me");
        get.addHeader("Authorization", "Bearer " + adminToken);
        HttpResponse resp = client.execute(get);
        log.info("RESP STATUS public resource >> {}", resp.getStatusLine());
        assert (resp.getStatusLine().getStatusCode() == 200);
        log.info("RESULT >> {}", EntityUtils.toString(resp.getEntity()));
    }

    // @Test
    public void userForUser() throws ClientProtocolException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/resource-nc/user/U456");
        get.addHeader("Authorization", "Bearer " + publicToken);
        HttpResponse resp = client.execute(get);
        log.info("RESP STATUS user for NonAdminUser public >> {}", resp.getStatusLine());
        assert (resp.getStatusLine().getStatusCode() == 200);
        log.info("RESULT user for NonAdminUser public >> {}", EntityUtils.toString(resp.getEntity()));
        get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/resource-nc/user/U456");
        get.addHeader("Authorization", "Bearer " + adminToken);
        resp = client.execute(get);
        log.info("RESP STATUS user for AdminUser admin >> {}", resp.getStatusLine());
        assert (resp.getStatusLine().getStatusCode() == 200);
        log.info("RESULT user for AdminUser admin >> {}", EntityUtils.toString(resp.getEntity()));
    }

    // @Test
    public void tokenOfNonExistingBudaUser() throws ClientProtocolException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/resource-nc/user/me");
        get.addHeader("Authorization", "Bearer " + adminToken);
        HttpResponse resp = client.execute(get);
        log.info("RESP STATUS public resource >> {}", resp.getStatusLine());
        assert (resp.getStatusLine().getStatusCode() == 200);
        log.info("RESULT >> {}", EntityUtils.toString(resp.getEntity()));
    }

    // @Test
    public void viewAuth0Users() throws ClientProtocolException, IOException {
        final HttpClient client = HttpClientBuilder.create().build();
        final HttpGet get = new HttpGet("https://bdrc-io.us.webtask.io/adf6e2f2b84784b57522e3b19dfc9201/api/users");
        get.addHeader("Authorization", "Bearer " + getAPIToken());
        final HttpResponse resp = client.execute(get);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resp.getEntity().writeTo(baos);
        final JsonNode node1 = new ObjectMapper().readTree(baos.toString());
        System.out.println("Users JSON >> " + node1);
        baos.close();
        final java.util.Iterator<JsonNode> it = node1.at("/users").elements();
        while (it.hasNext()) {
            final JsonNode tmp = it.next();
            System.out.println("User json >> " + tmp);
        }
    }

    // @Test
    public void createBudauserFromToken() throws ClientProtocolException, IOException, RestException, NoSuchAlgorithmException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/resource-nc/user/me");
        get.addHeader("Authorization", "Bearer " + adminToken);
        HttpResponse resp = client.execute(get);
        log.info("RESP STATUS public resource >> {}", resp.getStatusLine());
        assert (resp.getStatusLine().getStatusCode() == 200);
        log.info("RESULT >> {}", EntityUtils.toString(resp.getEntity()));
        // Looking for the created Buda User resource in the authrw dataset
        TokenValidation tv = new TokenValidation(adminToken);
        UserProfile up = tv.getUser();
        Resource r = BudaUser.getRdfProfile(up.getUser().getUserId());
        log.info("RESOURCE >> {}", r);
        assert (r != null);
        // Looking for the created Buda User Trig serialization in the users git repo
        String bucket = Helpers.getTwoLettersBucket(r.getLocalName());
        String filepath = System.getProperty("user.dir") + "/users/" + bucket + "/";
        log.info("TRIG FILE DIRECTORY >> {}", filepath);
        assert (new File(filepath + r.getLocalName() + ".trig").exists());
    }

    @Test
    public void disableBudaUser() throws ClientProtocolException, IOException, RestException {
        // First, make sure we have a user
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/resource-nc/user/me");
        get.addHeader("Authorization", "Bearer " + staffToken);
        HttpResponse resp = client.execute(get);
        log.info("RESP STATUS disableBudaUser 1 >> {}", resp.getStatusLine());
        assert (resp.getStatusLine().getStatusCode() == 200);
        log.info("RESULT >> {}", EntityUtils.toString(resp.getEntity()));
        // gets the buda user id from token
        TokenValidation tv = new TokenValidation(staffToken);
        UserProfile up = tv.getUser();
        String userId = BudaUser.getRdfProfile(up.getUser().getUserId()).getLocalName();
        HttpDelete hd = new HttpDelete("http://localhost:" + environment.getProperty("local.server.port") + "/resource-nc/user/" + userId);
        hd.addHeader("Authorization", "Bearer " + adminToken);
        resp = client.execute(hd);
        log.info("RESP STATUS disableBudaUser 2 >> {}", resp.getStatusLine());
        assert (resp.getStatusLine().getStatusCode() == 200);
        assert (!BudaUser.isActive(userId));
        // assert (up.getUser().isBlocked());
    }

}
