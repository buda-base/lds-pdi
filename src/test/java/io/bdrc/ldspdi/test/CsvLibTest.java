package io.bdrc.ldspdi.test;

import java.io.IOException;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import io.bdrc.ldspdi.rest.resources.PublicTemplatesResource;
import io.bdrc.ldspdi.results.Row;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.restapi.exceptions.RestExceptionMapper;

public class CsvLibTest extends JerseyTest{

    static String fusekiUrl="http://buda1.bdrc.io:13180/fuseki/bdrcrw/query";

    @BeforeClass
    public static void init() {
        ServiceConfig.initForTests(fusekiUrl);
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(PublicTemplatesResource.class).register(RestExceptionMapper.class);
    }

    @Test
    public void getResults() throws IOException{
        Response res = target("/query/volumesForWork")
                .queryParam("R_RES","bdr:W23703")
                .queryParam("format", "json")
                .queryParam("pageSize", "3")
                .request()
                .get();
        String entity=res.readEntity(String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node=mapper.readTree(entity);
        CsvMapper mapperC = new CsvMapper();
        CsvSchema schema = mapperC.schemaFor(Row.class);
        ObjectWriter wt=mapperC.writerFor(JsonNode.class).with(schema);
        wt.writeValue(System.out, node);
        System.out.println(node);
    }
}
