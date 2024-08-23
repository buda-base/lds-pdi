package io.bdrc.ldspdi.service;

import org.apache.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;

import jakarta.annotation.PreDestroy;

@Configuration
public class OSConfig {

    private RestHighLevelClient client;

    @Bean
    public RestHighLevelClient client() {
        final HttpHost host = new HttpHost(
                ServiceConfig.getProperty("opensearchHost"), 
                Integer.parseInt(ServiceConfig.getProperty("opensearchPort")),
                ServiceConfig.getProperty("opensearchScheme")
                );
        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, 
                new UsernamePasswordCredentials(ServiceConfig.getProperty("opensearchUser"), ServiceConfig.getProperty("opensearchPassword")));

        this.client = new RestHighLevelClient(RestClient.builder(host).setHttpClientConfigCallback(httpClientBuilder -> 
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)));
        return this.client;
    }

    @PreDestroy
    public void closeClient() throws Exception {
        if (client != null) {
            client.close();
        }
    }
}