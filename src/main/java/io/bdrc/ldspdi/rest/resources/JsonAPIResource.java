package io.bdrc.ldspdi.rest.resources;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.ldspdi.Utils.Helpers;
import io.bdrc.ldspdi.objects.json.QueryListItem;
import io.bdrc.ldspdi.objects.json.QueryTemplate;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ldspdi.sparql.QueryFileParser;


@Path("/")
public class JsonAPIResource {
    
    public static Logger log=Logger.getLogger(JsonAPIResource.class.getName());
    private ArrayList<String> fileList;
    
    public JsonAPIResource() {
        super();
        ResourceConfig config=new ResourceConfig( JsonAPIResource.class);
        config.register(CorsFilter.class);
        log.addHandler(new ConsoleHandler());
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
                log.info(queryList.toString());                
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
                QueryFileParser qfp=new QueryFileParser(name);
                HashMap<String, String> meta=qfp.getMetaInf();
                QueryTemplate qt= new QueryTemplate(
                        qfp.getTemplateName(),
                        Helpers.bdrcEncode("/resource/templates"+meta.get(QueryConstants.QUERY_URL)),
                        meta.get(QueryConstants.QUERY_SCOPE),
                        meta.get(QueryConstants.QUERY_RESULTS),
                        meta.get(QueryConstants.QUERY_RETURN_TYPE),
                        meta.get(QueryConstants.QUERY_PARAMS),
                        Helpers.bdrcEncode(meta.get(QueryConstants.QUERY_URL)),
                        qfp.getQuery());
                ObjectMapper mapper = new ObjectMapper();
                mapper.writerWithDefaultPrettyPrinter().writeValue(os , qt);
            }
        };
        return Response.ok(stream).build();        
    }
    
    @POST 
    @Path("/queries/{template}")
    @Produces(MediaType.APPLICATION_JSON)    
    public QueryTemplate queryDescPost(@PathParam("template") String name) {
        log.info("Call to queriesListGet()");               
        QueryFileParser qfp=new QueryFileParser(name);
        HashMap<String, String> meta=qfp.getMetaInf();
        QueryTemplate qt= new QueryTemplate(
                qfp.getTemplateName(),
                "/resource/templates"+meta.get(QueryConstants.QUERY_URL),
                meta.get(QueryConstants.QUERY_SCOPE),
                meta.get(QueryConstants.QUERY_RESULTS),
                meta.get(QueryConstants.QUERY_RETURN_TYPE),
                meta.get(QueryConstants.QUERY_PARAMS),
                meta.get(QueryConstants.QUERY_URL),
                qfp.getQuery());
                
        return qt;        
    }
    
    private ArrayList<QueryListItem> getQueryListItems(ArrayList<String> filesList){
        ArrayList<QueryListItem> items=new ArrayList<>();        
        for(String file:fileList) {
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
                log.log(Level.FINEST, "Error while getting query templates", e);
                e.printStackTrace();
            }
        }
        return files;       
    }
    
    

}
