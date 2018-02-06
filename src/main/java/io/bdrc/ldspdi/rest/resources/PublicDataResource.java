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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFWriter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.Viewable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.formatters.JSONLDFormatter;
import io.bdrc.ldspdi.Utils.RestUtils;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.InjectionTracker;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.sparql.results.ResultPage;
import io.bdrc.ldspdi.sparql.results.Results;
import io.bdrc.ontology.service.core.OntAccess;
import io.bdrc.ontology.service.core.OntClassModel;


@Path("/")
public class PublicDataResource {   
    
    public static Logger log=Logger.getLogger(PublicDataResource.class.getName());
    
    QueryProcessor processor=new QueryProcessor();
    public String fusekiUrl="";
    
    
    
    public PublicDataResource() {
        super();
        ResourceConfig config=new ResourceConfig(PublicDataResource.class);
        config.register(CorsFilter.class);        
    }
    
    @GET 
    @Path("/context.jsonld")
    @Produces(MediaType.TEXT_HTML)    
    public Response getJsonContext() throws IOException{
        log.info("Call to getJsonContext()"); 
        //return ServiceConfig.JSONLD_CONTEXT_HTML; 
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                // when prefix is null, QueryProcessor default prefix is used
                JSONLDFormatter.jsonObjectToOutputStream(ServiceConfig.JSONLD_CONTEXT, os);                                 
            }
        };
        return Response.ok(stream).build(); 
    } 
    
    @POST 
    @Path("/context.jsonld")
    @Produces(MediaType.APPLICATION_JSON)
    public Response postJsonContext() throws JsonProcessingException{
        log.info("Call to getJsonContext()");    
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                // when prefix is null, QueryProcessor default prefix is used
                JSONLDFormatter.jsonObjectToOutputStream(ServiceConfig.JSONLD_CONTEXT, os);                                 
            }
        };
        return Response.ok(stream).build();          
    }

    @GET
    @Path("/resource/{res}") 
    public Response getResourceGraph(@PathParam("res") final String res,
        @HeaderParam("Accept") final String format,
        @HeaderParam("fusekiUrl") final String fuseki,
        @QueryParam("classUri") final String uri) {
        log.info("Call to getResourceGraph()");
        
        if(fuseki !=null){
            fusekiUrl=fuseki;
        }else {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);  
        }
        
        MediaType media=new MediaType("text","turtle","utf-8");     
        if(ServiceConfig.isValidMime(format)){
            media=RestUtils.getMediaType(format);
        }
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                // when prefix is null, QueryProcessor default prefix is used
                Model model=processor.getResourceGraph(res,fusekiUrl);  
                RDFWriter writer=RestUtils.getSTTLRDFWriter(model); 
                writer.output(os);                  
            }
        };
        return Response.ok(stream,media).build();       
    }
    
    @GET
    @Path("/resource/{type}/exact/{res}") 
    public Response getExactPersonURLResources(@PathParam("res") final String res,
        @PathParam("type") final String type,
        @HeaderParam("fusekiUrl") final String fuseki,
        @Context UriInfo info) {
        log.info("Call to getResourceGraph()");
        
        if(fuseki !=null){
            fusekiUrl=fuseki;
        }else {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);  
        }
        //Settings
        MediaType media=new MediaType("text","html","utf-8");
        String relativeUri=info.getRequestUri().toString().replace(info.getBaseUri().toString(), "/");
        MultivaluedMap<String,String> mp=info.getQueryParameters();
        HashMap<String,String> hm=RestUtils.convertMulti(mp);
        hm.put(QueryConstants.REQ_METHOD, "GET");
        hm.put(QueryConstants.REQ_URI, relativeUri);        
        ObjectMapper mapper = new ObjectMapper();
        
        //params              
        int pageSize =RestUtils.getPageSize(hm.get(QueryConstants.PAGE_SIZE));
        int pageNumber=RestUtils.getPageNumber(hm.get(QueryConstants.PAGE_NUMBER));
        int hash=RestUtils.getHash(hm.get(QueryConstants.RESULT_HASH));
        boolean jsonOutput=RestUtils.getJsonOutput(hm.get(QueryConstants.JSON_OUT));
        
        File file=new File(ServiceConfig.getProperty(QueryConstants.QUERY_PATH)+
                "public/URL/"+ServiceConfig.getProperty(QueryConstants.URL_TEMPLATE_EXACT));
        String query=ServiceConfig.getPrefixes()+RestUtils.getQuery(file);
        String q=InjectionTracker.getValidURLQuery(query, res,type);   
        
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                if(q.startsWith(QueryConstants.QUERY_ERROR)) {
                    os.write(q.getBytes());
                }
                else {
                    Results res = RestUtils.getResults(q, fuseki, hash, pageSize); 
                    ResultPage rp=new ResultPage(res,pageNumber,hm);
                    if(jsonOutput) {
                        mapper.writerWithDefaultPrettyPrinter().writeValue(os , rp);
                    }else {
                        os.write(RestUtils.renderHtmlResultPage(rp,relativeUri).getBytes());
                    }
                }                  
            }
        };
        return Response.ok(stream,media).build();       
    }
    
    @GET
    @Path("/resource/{type}/{res}") 
    public Response getPersonURLResources(@PathParam("res") final String res,
        @PathParam("type") final String type,
        @HeaderParam("fusekiUrl") final String fuseki,
        @Context UriInfo info) {
        log.info("Call to getResourceGraph()");
        
        if(fuseki !=null){
            fusekiUrl=fuseki;
        }else {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);  
        }
        //Settings
        MediaType media=new MediaType("text","html","utf-8");
        String relativeUri=info.getRequestUri().toString().replace(info.getBaseUri().toString(), "/");
        MultivaluedMap<String,String> mp=info.getQueryParameters();
        HashMap<String,String> hm=RestUtils.convertMulti(mp);
        hm.put(QueryConstants.REQ_METHOD, "GET");
        hm.put(QueryConstants.REQ_URI, relativeUri);        
        ObjectMapper mapper = new ObjectMapper();
        
        //params              
        int pageSize =100;
        boolean jsonOutput=RestUtils.getJsonOutput(hm.get(QueryConstants.JSON_OUT));
        String quotedForLucene="\""+res+"\"";
        
        File file=new File(ServiceConfig.getProperty(QueryConstants.QUERY_PATH)+
                "public/URL/"+ServiceConfig.getProperty(QueryConstants.URL_TEMPLATE));
        String query=RestUtils.getQuery(file);
        String q=InjectionTracker.getValidURLQuery(query, quotedForLucene,type);   
        
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                if(q.startsWith(QueryConstants.QUERY_ERROR)) {
                    os.write(q.getBytes());
                }
                else {
                    Results res = RestUtils.getResults(q, fuseki, -1, pageSize); 
                    ResultPage rp=new ResultPage(res,1,hm);
                    if(jsonOutput) {
                        mapper.writerWithDefaultPrettyPrinter().writeValue(os , rp);
                    }else {
                        os.write(RestUtils.renderSingleHtmlResultPage(rp,relativeUri).getBytes());
                    }
                }                  
            }
        };
        return Response.ok(stream,media).build();       
    }
       
    @POST
    @Path("/resource/{res}") 
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getResourceGraphPost(@PathParam("res") final String res,
        @HeaderParam("Accept") final String format,
        @HeaderParam("fusekiUrl") final String fuseki,
        @QueryParam("classUri") final String uri) {
           
        log.info("Call to getResourceGraphPost");
        if(fuseki !=null){
            fusekiUrl=fuseki;
        }else {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);  
        }
        
        MediaType media=new MediaType("text","turtle","utf-8");     
        if(ServiceConfig.isValidMime(format)){
            media=RestUtils.getMediaType(format);
        }
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                // when prefix is null, QueryProcessor default prefix is used
                Model model=processor.getResourceGraph(res,fusekiUrl);  
                Object jsonObject=JSONLDFormatter.modelToJsonObject(model, res);
                JSONLDFormatter.jsonObjectToOutputStream(jsonObject, os);                                 
            }
        };
        return Response.ok(stream,media).build();       
    }
     
    @GET
    @Path("/resource/{res}.{ext}")   
    public Response getFormattedResourceGraph(
            @PathParam("res") final String res, 
            @DefaultValue("ttl") @PathParam("ext") final String format,
            @HeaderParam("fusekiUrl") final String fuseki,
            @HeaderParam("prefix") final String prefix) {
        log.info("Call to getFormattedResourceGraph()");
        if(fuseki !=null){
            fusekiUrl=fuseki;
        }else {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }
        MediaType media=new MediaType("text","turtle");
        if(RestUtils.isValidExtension(format)){
            String mime=ServiceConfig.getProperty("m"+format);
            String[] parts=mime.split(Pattern.quote("/"));
            media =new MediaType(parts[0],parts[1]);
        }
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                // when prefix is null, QueryProcessor default prefix is used*/
                Model model=processor.getResourceGraph(res,fusekiUrl); 
                if(RestUtils.isValidExtension(format)&& !format.equalsIgnoreCase("ttl")){
                    if(format.equalsIgnoreCase("jsonld")) {
                        Object jsonObject=JSONLDFormatter.modelToJsonObject(model, res);
                        JSONLDFormatter.jsonObjectToOutputStream(jsonObject, os);
                    }else {
                        model.write(os,ServiceConfig.getProperty(format));
                    }
                }else{
                    RDFWriter writer=RestUtils.getSTTLRDFWriter(model);                   
                    writer.output(os);                                      
                }                
            }
        };
        return Response.ok(stream,media).build();       
    }
    
    @POST
    @Path("/resource/{res}.{ext}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getFormattedResourceGraphPost(
            @PathParam("res") final String res, 
            @DefaultValue("ttl") @PathParam("ext") final String format,
            @HeaderParam("fusekiUrl") final String fuseki,
            @HeaderParam("prefix") final String prefix) {
        log.info("getFormattedResourceGraphPost");
        if(fuseki !=null){
            fusekiUrl=fuseki;
        }else {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }
        MediaType media=new MediaType("text","turtle");
        if(RestUtils.isValidExtension(format)){
            String mime=ServiceConfig.getProperty("m"+format);
            String[] parts=mime.split(Pattern.quote("/"));
            media =new MediaType(parts[0],parts[1]);
        }
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                // when prefix is null, QueryProcessor default prefix is used*/
                Model model=processor.getResourceGraph(res,fusekiUrl); 
                if(RestUtils.isValidExtension(format)&& !format.equalsIgnoreCase("ttl")){
                    if(format.equalsIgnoreCase("jsonld")) {
                        Object jsonObject=JSONLDFormatter.modelToJsonObject(model, res);
                        JSONLDFormatter.jsonObjectToOutputStream(jsonObject, os);
                    }else {
                        model.write(os,ServiceConfig.getProperty(format));
                    }
                }else{
                    RDFWriter writer=RestUtils.getSTTLRDFWriter(model);                   
                    writer.output(os);                                      
                }                
            }
        };
        return Response.ok(stream,media).build();       
    }
    
    @POST
    @Path("/resource")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getFormattedResourceGraphJsonPost(            
            HashMap<String,String> map,
            @HeaderParam("fusekiUrl") final String fuseki) {
        log.info("getFormattedResourceGraphPost");
        fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        String format=map.get("ext");
        String res=map.get("res");
        MediaType media=new MediaType("text","turtle");
        if(RestUtils.isValidExtension(format)){
            String mime=ServiceConfig.getProperty("m"+format);
            String[] parts=mime.split(Pattern.quote("/"));
            media =new MediaType(parts[0],parts[1]);
        }
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                // when prefix is null, QueryProcessor default prefix is used*/
                Model model=processor.getResourceGraph(res,fusekiUrl); 
                if(RestUtils.isValidExtension(format)&& !format.equalsIgnoreCase("ttl")){
                    if(format.equalsIgnoreCase("jsonld")) {
                        Object jsonObject=JSONLDFormatter.modelToJsonObject(model, res);
                        JSONLDFormatter.jsonObjectToOutputStream(jsonObject, os);
                    }else {
                        model.write(os,ServiceConfig.getProperty(format));
                    }
                }else{
                    RDFWriter writer=RestUtils.getSTTLRDFWriter(model);                   
                    writer.output(os);                                      
                }                
            }
        };
        return Response.ok(stream,media).build();       
    }
    
    @GET
    @Path("/ontology/core/{class}")
    @Produces("text/html")
    public Viewable getCoreOntologyClassView(@PathParam("class") String cl) {
        log.info("getCoreOntologyClassView()");          
        Map<String, Object> map = new HashMap<String, Object>();
        String uri="http://purl.bdrc.io/ontology/core/"+cl;
        map.put("model", new OntClassModel(uri)); 
        return new Viewable("/ontRes.jsp", map);        
    }
    
    @GET
    @Path("/ontology/admin/{class}")
    @Produces("text/html")
    public Viewable getAdminOntologyClassView(@PathParam("class") String cl) {
        log.info("getAdminOntologyClassView()");          
        Map<String, Object> map = new HashMap<String, Object>();
        String uri="http://purl.bdrc.io/ontology/admin/"+cl;
        map.put("model", new OntClassModel(uri)); 
        return new Viewable("/ontRes.jsp", map);        
    }
    
    @GET
    @Path("/ontology.{ext}")
    //@Produces(MediaType.TEXT_PLAIN) 
    public Response getOntology(@DefaultValue("ttl") @PathParam("ext") String ext) {
        log.info("getOntology()");        
        MediaType media=new MediaType("text","turtle");
        if(RestUtils.isValidExtension(ext)){
            String mime=ServiceConfig.getProperty("m"+ext);
            String[] parts=mime.split(Pattern.quote("/"));
            media =new MediaType(parts[0],parts[1]);
        }
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                // when prefix is null, QueryProcessor default prefix is used*/
                Model model=OntAccess.MODEL;
                if(RestUtils.isValidExtension(ext)&& !ext.equalsIgnoreCase("ttl")){
                    if(ext.equalsIgnoreCase("jsonld")) {
                        //Object jsonObject=JSONLDFormatter.modelToJsonObject(model, res);
                        //JSONLDFormatter.jsonObjectToOutputStream(model, os);
                        model.write(os,ServiceConfig.getProperty("json"));
                    }else {
                        model.write(os,ServiceConfig.getProperty(ext));
                    }
                }else{
                    RDFWriter writer=RestUtils.getSTTLRDFWriter(model);                   
                    writer.output(os);                                      
                }                
            }
        };
        return Response.ok(stream,media).build();        
    }
    
}
