package io.bdrc.ldspdi.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
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
    static String publicToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJodHRwczovL2Rldi1iZHJjLmF1dGgwLmNvbS9hcGkvdjIvIiwic3ViIjoiYXV0aDB8NWJlOTkyZDlkN2VjZTg3ZjE1OWM4YmVkIiwiYXpwIjoiRzBBam1DS3NwTm5nSnNUdFJuSGFBVUNENDRaeHdvTUoiLCJpc3MiOiJodHRwczovL2Rldi1iZHJjLmF1dGgwLmNvbS8iLCJleHAiOjE3MzU3MzgyNjR9.zqOALhi8Gz1io-B1pWIgHVvkSa0U6BuGmB18FnF3CIg\n";
    static String adminToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJodHRwczovL2Rldi1iZHJjLmF1dGgwLmNvbS9hcGkvdjIvIiwic3ViIjoiYXV0aDB8NWJlOTkyMGJlYzMxMjMyMGY1NjI5NGRjIiwiYXpwIjoiRzBBam1DS3NwTm5nSnNUdFJuSGFBVUNENDRaeHdvTUoiLCJpc3MiOiJodHRwczovL2Rldi1iZHJjLmF1dGgwLmNvbS8iLCJleHAiOjE3MzU3Mzc1OTB9.m1V64-90tjNRMD18RQTF8SBlMFOcqgSuPwtALZBLd8U";

    @BeforeClass
    public static void init() throws IOException {
        ServiceConfig.initForTests("");
        RdfAuthModel.initForStaticTests();
        RdfAuthModel.getFullModel().write(System.out, "TURTLE");
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

    // @Test
    public void securedEndpointAccess() throws IOException, IllegalArgumentException {

        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/auth/rdf/admin");
        get.setHeader("Authorization", "Bearer " + publicToken);
        System.out.println("Public token >>> " + publicToken);
        HttpResponse resp = client.execute(get);
        System.out.println("ENTITY >>" + EntityUtils.toString(resp.getEntity()));
        System.out.println("STATUS 1 >>> " + resp.getStatusLine());
        assert (resp.getStatusLine().getStatusCode() == 403);

        client = HttpClientBuilder.create().build();
        get = new HttpGet("http://localhost:" + environment.getProperty("local.server.port") + "/auth/rdf/admin");
        get.setHeader("Authorization", "Bearer " + adminToken);
        resp = client.execute(get);
        System.out.println("STATUS 2 >>> " + resp.getStatusLine());
        assert (resp.getStatusLine().getStatusCode() == 200);
    }
}
