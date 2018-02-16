package io.bdrc.ldspdi.rest.resources;

/*******************************************************************************
 * Copyright (c) 2018 Buddhist Digital Resource Center (BDRC)
 * 
 * If this file is a derivation of another work the license header will appear below; 
 * otherwise, this work is licensed under the Apache License, Version 2.0 
 * (the "License"); you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.ldspdi.objects.json.QueryListItem;
import io.bdrc.ldspdi.objects.json.QueryTemplate;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ldspdi.sparql.QueryFileParser;


@Path("/")
public class JsonAPIResource {
    
    public static Logger log=LoggerFactory.getLogger(JsonAPIResource.class.getName());
    private ArrayList<String> fileList;
    
    public JsonAPIResource() {
        super();
        ResourceConfig config=new ResourceConfig( JsonAPIResource.class);
        config.register(CorsFilter.class);
        fileList=getQueryTemplates();
    }
    
    @GET 
    @Path("/queries")
    @Produces(MediaType.TEXT_HTML)    
    public Response queriesListGet() {
        log.info("Call to queriesListGet()");               
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                // when prefix is null, QueryProcessor default prefix is used
                ArrayList<QueryListItem> queryList=getQueryListItems(fileList);                                
                ObjectMapper mapper = new ObjectMapper();
                mapper.writerWithDefaultPrettyPrinter().writeValue(os , queryList);
            }
        };
        return Response.ok(stream).build();        
    }
    
    @POST 
    @Path("/queries")
    @Produces(MediaType.APPLICATION_JSON)    
    public ArrayList<QueryListItem> queriesListPost() {
        log.info("Call to queriesListPost()"); 
        ArrayList<QueryListItem> queryList=getQueryListItems(fileList);
        log.info(queryList.toString());
        return queryList;
    }
    
    @GET 
    @Path("/queries/{template}")
    @Produces(MediaType.TEXT_HTML)    
    public Response queryDescGet(@PathParam("template") String name) {
        log.info("Call to queriesListGet()");               
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                // when prefix is null, QueryProcessor default prefix is used
                QueryFileParser qfp=new QueryFileParser(name+".arq");
                ObjectMapper mapper = new ObjectMapper();
                mapper.writerWithDefaultPrettyPrinter().writeValue(os , qfp.getTemplate());
            }
        };
        return Response.ok(stream).build();        
    }
    
    @POST 
    @Path("/queries/{template}")
    @Produces(MediaType.APPLICATION_JSON)    
    public QueryTemplate queryDescPost(@PathParam("template") String name) {
        log.info("Call to queriesListGet()");               
        QueryFileParser qfp=new QueryFileParser(name+".arq"); 
        return qfp.getTemplate();        
    }
    
    private ArrayList<QueryListItem> getQueryListItems(ArrayList<String> filesList){
        ArrayList<QueryListItem> items=new ArrayList<>();        
        for(String file:filesList) {
            QueryListItem qli=new QueryListItem(file,"/queries/"+file);
            items.add(qli);
        }
        return items;
    }
    
    private ArrayList<String> getQueryTemplates() {
        ArrayList<String> files=new ArrayList<>();
        java.nio.file.Path dpath = Paths.get(ServiceConfig.getProperty(QueryConstants.QUERY_PATH)+"public");      
        if (Files.isDirectory(dpath)) {        
            try (DirectoryStream<java.nio.file.Path> stream = Files.newDirectoryStream(dpath)) {
                for (java.nio.file.Path path : stream) {
                    String tmp=path.toString();
                    //Filtering arq files
                    if(tmp.endsWith(".arq")) {
                        files.add(tmp.substring(tmp.lastIndexOf("/")+1));
                    }
                }
            } catch (IOException e) {
                log.error("Error while getting query templates", e);
                e.printStackTrace();
            }
        }
        return files;       
    }
    
    

}
