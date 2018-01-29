package io.bdrc.ldspdi.rest.resources;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import io.bdrc.formatters.JSONLDFormatter;
import io.bdrc.ldspdi.Utils.DocFileBuilder;
import io.bdrc.ldspdi.objects.json.QueryListItem;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryConstants;



@Path("/")
public class JsonAPIResource {
    
    public static Logger log=Logger.getLogger(JsonAPIResource.class.getName());
    
    public JsonAPIResource() {
        super();
        log.addHandler(new ConsoleHandler());
    }
    
    @GET 
    @Path("/queries")
    @Produces(MediaType.TEXT_HTML)    
    public Response queriesListGet(@Context UriInfo info) {
        log.info("Call to queriesListGet()");               
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                // when prefix is null, QueryProcessor default prefix is used
                ArrayList<QueryListItem> queryList=getQueryListItems(info.getBaseUri().toString());
                log.info(queryList.toString());
                JSONLDFormatter.jsonObjectToOutputStream(queryList, os);                                 
            }
        };
        return Response.ok(stream).build();        
    }
    
    @POST 
    @Path("/queries")
    @Produces(MediaType.APPLICATION_JSON)    
    public ArrayList<QueryListItem> queriesListPost(@Context UriInfo info) {
        log.info("Call to queriesListPost()"); 
        ArrayList<QueryListItem> queryList=getQueryListItems(info.getBaseUri().toString());
        log.info(queryList.toString());
        return queryList;
    }
    
    private ArrayList<QueryListItem> getQueryListItems(String baseUri){
        ArrayList<QueryListItem> items=new ArrayList<>();
        java.nio.file.Path dpath = Paths.get(ServiceConfig.getProperty(QueryConstants.QUERY_PATH)+"public");      
        if (Files.isDirectory(dpath)) {        
            try (DirectoryStream<java.nio.file.Path> stream = Files.newDirectoryStream(dpath)) {
                for (java.nio.file.Path path : stream) {
                    String tmp=path.toString();
                    //Filtering arq files
                    if(tmp.endsWith(".arq")) {
                        String filename=tmp.substring(tmp.lastIndexOf("/")+1);
                        filename=filename.substring(0, filename.lastIndexOf("."));
                        QueryListItem qli=new QueryListItem(
                                filename,
                                baseUri+"queries/"+filename);
                        items.add(qli);                                
                    }
                }
            } catch (IOException e) {
                log.log(Level.FINEST, "Error while getting query templates", e);
                e.printStackTrace();
            }
        }
        return items;
    }

}
