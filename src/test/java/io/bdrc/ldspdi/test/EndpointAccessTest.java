package io.bdrc.ldspdi.test;

import java.io.IOException;

import javax.ws.rs.core.Application;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.BeforeClass;
import org.junit.Test;

import com.auth0.client.auth.AuthAPI;

import io.bdrc.auth.rdf.RdfAuthModel;

public class EndpointAccessTest extends JerseyTest{

    static AuthAPI auth;
    static String token;
    static String publicToken="eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJodHRwczovL2Rldi1iZHJjLmF1dGgwLmNvbS9hcGkvdjIvIiwic3ViIjoiYXV0aDB8NWJlOTkyZDlkN2VjZTg3ZjE1OWM4YmVkIiwiYXpwIjoiRzBBam1DS3NwTm5nSnNUdFJuSGFBVUNENDRaeHdvTUoiLCJpc3MiOiJodHRwczovL2Rldi1iZHJjLmF1dGgwLmNvbS8iLCJleHAiOjE3MzU3MzgyNjR9.zqOALhi8Gz1io-B1pWIgHVvkSa0U6BuGmB18FnF3CIg\n";
    static String adminToken="eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJodHRwczovL2Rldi1iZHJjLmF1dGgwLmNvbS9hcGkvdjIvIiwic3ViIjoiYXV0aDB8NWJlOTkyMGJlYzMxMjMyMGY1NjI5NGRjIiwiYXpwIjoiRzBBam1DS3NwTm5nSnNUdFJuSGFBVUNENDRaeHdvTUoiLCJpc3MiOiJodHRwczovL2Rldi1iZHJjLmF1dGgwLmNvbS8iLCJleHAiOjE3MzU3Mzc1OTB9.m1V64-90tjNRMD18RQTF8SBlMFOcqgSuPwtALZBLd8U";

    @BeforeClass
    public static void init() throws IOException {
        RdfAuthModel.initForStaticTests();
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(AuthTestResource.class)
                .register(RdfAuthTestFilter.class);

    }

    @Test
    public void NoTokenPublicAccess() throws ClientProtocolException, IOException {
        HttpClient client=HttpClientBuilder.create().build();
        HttpGet get=new HttpGet(this.getBaseUri()+"auth/public");
        System.out.println("URL >>> "+this.getBaseUri()+"auth/public");
        HttpResponse resp=client.execute(get);
        System.out.println("STATUS >>> "+resp.getStatusLine());
        assert(resp.getStatusLine().getStatusCode()==200);
    }

    @Test
    public void securedEndpointAccess() throws IOException, IllegalArgumentException {

        HttpClient client=HttpClientBuilder.create().build();
        HttpGet get=new HttpGet(this.getBaseUri()+"auth/rdf/admin");
        get.setHeader("Authorization", "Bearer " +publicToken);
        System.out.println("Public token >>> "+publicToken);
        HttpResponse resp=client.execute(get);
        System.out.println("STATUS >>> "+resp.getStatusLine());
        assert(resp.getStatusLine().getStatusCode()==403);

        client=HttpClientBuilder.create().build();
        get=new HttpGet(this.getBaseUri()+"auth/rdf/admin");
        get.setHeader("Authorization", "Bearer " +adminToken);
        resp=client.execute(get);
        System.out.println("STATUS >>> "+resp.getStatusLine());
        assert(resp.getStatusLine().getStatusCode()==200);
    }
}
