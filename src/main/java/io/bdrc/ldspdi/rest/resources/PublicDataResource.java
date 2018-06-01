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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

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
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFWriter;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.formatters.TTLRDFWriter;
import io.bdrc.ldspdi.ontology.service.core.OntClassModel;
import io.bdrc.ldspdi.ontology.service.core.OntData;
import io.bdrc.ldspdi.ontology.service.core.OntPropModel;
import io.bdrc.ldspdi.rest.features.JerseyCacheControl;
import io.bdrc.ldspdi.results.CacheAccessModel;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.DocFileModel;
import io.bdrc.ldspdi.utils.ResponseOutputStream;
import io.bdrc.restapi.exceptions.RestException;


@Path("/")
public class PublicDataResource {   
    
    public final static Logger log=LoggerFactory.getLogger(PublicDataResource.class.getName());  
    public String fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
    
    @GET   
    @JerseyCacheControl()
    @Produces(MediaType.TEXT_HTML)    
    public Response getHomePage() throws RestException{
        log.info("Call to getHomePage()");         
        return Response.ok(new Viewable("/index.jsp",new DocFileModel())).build();     
    }
    
    @GET 
    @Path("/robots.txt")       
    public Response getRobots() {
        log.info("Call getRobots()"); 
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                os.write(ServiceConfig.getRobots().getBytes());                    
            }
        };
        return Response.ok(stream,MediaType.TEXT_PLAIN_TYPE).build();  
    }
    
    @GET 
    @Path("cache")
    @Produces(MediaType.TEXT_HTML)    
    public Response getCacheInfo() {
        log.info("Call to getCacheInfo()"); 
        return Response.ok(new Viewable("/cache.jsp",new CacheAccessModel())).build();        
    }
    
    @GET 
    @Path("choice")
    @Produces(MediaType.TEXT_HTML)    
    public Response getMultiChoice(@QueryParam("path") String it,@Context UriInfo info) {
        log.info("Call to getMultiChoice()");
        return Response.ok(new Viewable("/multiChoice.jsp",info.getBaseUri()+it)).build();        
    }
    
    @GET
    @Path("/context.jsonld")
    public Response getJsonContext(@Context Request request) throws RestException {
        log.info("Call to getJsonContext()");
        EntityTag tag=OntData.getEntityTag();
        ResponseBuilder builder = request.evaluatePreconditions(tag);
        if(builder == null){
            builder = Response.ok(OntData.JSONLD_CONTEXT, "application/ld+json");
            builder.header("Last-Modified", OntData.getLastUpdated()).tag(tag);
        }
        return builder.build();        
    }

    @GET
    @Path("/resource/{res}")
    @JerseyCacheControl()
    public Response getResourceGraph(@PathParam("res") final String res,
        @HeaderParam("Accept") String format,
        @HeaderParam("fusekiUrl") final String fuseki,
        @Context UriInfo info,
        @Context HttpHeaders headers) throws RestException{        
        log.info("Call to getResourceGraphGET() with URL: "+info.getPath()+" Accept: "+format);        
        
        if(format==null) {            
            ResponseBuilder rb=Response.status(300).header("Link",info.getBaseUri()+"choice?path="+info.getPath());
            return setHeaders(rb,getResourceHeaders(info.getPath(),null,"List")).build();
        }
        
        ArrayList<String> validMimes=MediaTypeUtils.getValidMime(headers.getAcceptableMediaTypes());
        /** Redirection to /show if format is null or of html type **/
        if(validMimes.size()==0 || format.contains(MediaType.APPLICATION_XHTML_XML) ||
                format.contains(MediaType.TEXT_HTML)) {            
            try {
                ResponseBuilder builder=Response.seeOther(new URI(ServiceConfig.getProperty("showUrl")+res));
                return setHeaders(builder,getResourceHeaders(info.getPath(),null,"Choice")).build();
                
                //return Response.seeOther(new URI(ServiceConfig.getProperty("showUrl")+res)).build();
            } catch (URISyntaxException e) {
                throw new RestException(500,RestException.GENERIC_APP_ERROR_CODE,"getResourceGraph : URISyntaxException"+e.getMessage());
            }
        }
        /** Accept header is not null and not of html type **/ 
        if(validMimes.size()==0 && !format.equals("*/*")) {
            return Response.status(406).build();
        }
        if(validMimes.size()>0) {
            format=validMimes.get(0);
        }
        if(fuseki !=null){ 
            fusekiUrl=fuseki;            
        }            
        Model model=QueryProcessor.getResourceGraph(res,fusekiUrl,null);
        if(model.size()==0) {
            throw new RestException(404,RestException.GENERIC_APP_ERROR_CODE,"No graph was found for resource Id : \""+res+"\"");
        }
        ResponseBuilder builder=Response.ok(ResponseOutputStream.getModelStream(model,MediaTypeUtils.getExtFormatFromMime(format)),MediaTypeUtils.getMediaTypeFromMime(format));
        return setHeaders(builder,getResourceHeaders(info.getPath(),MediaTypeUtils.getExtFormatFromMime(format),"Choice")).build();             
    }
    
    @POST
    @Path("/resource/{res}") 
    @JerseyCacheControl()
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getResourceGraphPost(@PathParam("res") final String res,
        @HeaderParam("Accept") String format,
        @HeaderParam("fusekiUrl") final String fuseki,
        @Context UriInfo info,
        @Context HttpHeaders headers) throws RestException{        
        log.info("Call to getResourceGraphPost() with URL: "+info.getPath()+" Accept: "+format);
        log.info("Valid mediaTypes "+MediaTypeUtils.getValidMime(headers.getAcceptableMediaTypes()));
        
        ArrayList<String> validMimes=MediaTypeUtils.getValidMime(headers.getAcceptableMediaTypes());
        if(format==null) {            
            ResponseBuilder rb=Response.status(300).header("Link",info.getBaseUri()+"choice?path="+info.getPath());
            return setHeaders(rb,getResourceHeaders(info.getPath(),null,"List")).build();
        }
        /** Accept header is not null and not of html type **/ 
        else if(validMimes.size()==0 && !format.equals("*/*")) {
            return Response.status(406).build();
        }
        if(validMimes.size()>0) {
            format=validMimes.get(0);
        }
        if(fuseki !=null){ 
            fusekiUrl=fuseki;            
        }            
        Model model=QueryProcessor.getResourceGraph(res,fusekiUrl,null);
        if(model.size()==0) {
            throw new RestException(404,RestException.GENERIC_APP_ERROR_CODE,"No graph was found for resource Id : \""+res+"\"");
        }
        ResponseBuilder builder=Response.ok(ResponseOutputStream.getModelStream(model,MediaTypeUtils.getExtFormatFromMime(format)),MediaTypeUtils.getMediaTypeFromMime(format));
        return setHeaders(builder,getResourceHeaders(info.getPath(),MediaTypeUtils.getExtFormatFromMime(format),"Choice")).build();
    }
     
    @GET
    @Path("/resource/{res}.{ext}")   
    @JerseyCacheControl()
    public Response getFormattedResourceGraph(
            @PathParam("res") final String res, 
            @DefaultValue("ttl") @PathParam("ext") final String format,
            @HeaderParam("fusekiUrl") final String fuseki,
            @Context UriInfo info) throws RestException{
        log.info("Call to getFormattedResourceGraph()"); 
        if(!MediaTypeUtils.isMime(MediaTypeUtils.getMimeFromExtension(format))) {
            return Response.status(406).build();
        }
        if(fuseki !=null){
            fusekiUrl=fuseki;            
        }
        MediaType media=MediaTypeUtils.getMediaTypeFromExt(format);
        Model model=QueryProcessor.getResourceGraph(res,fusekiUrl,null);
        if(model.size()==0) {
            throw new RestException(404,RestException.GENERIC_APP_ERROR_CODE,"No graph was found for resource Id : \""+res+"\"");
        }
        ResponseBuilder builder=Response.ok(ResponseOutputStream.getModelStream(model, format, res),media);
        return setHeaders(builder,getResourceHeaders(info.getPath(),format,"Choice")).build();                     
    }
    
    @POST
    @Path("/resource/{res}.{ext}") 
    @JerseyCacheControl()
    public Response getFormattedResourceGraphPost(
            @PathParam("res") final String res, 
            @DefaultValue("ttl") @PathParam("ext") final String format,
            @HeaderParam("fusekiUrl") final String fuseki,
            @Context UriInfo info) throws RestException{
        
        log.info("Call to getFormattedResourceGraphPost()");
        if(!MediaTypeUtils.isMime(MediaTypeUtils.getMimeFromExtension(format))) {
            return Response.status(406).build();
        }
        if(fuseki !=null){
            fusekiUrl=fuseki;            
        }
        MediaType media=MediaTypeUtils.getMediaTypeFromExt(format);
        Model model=QueryProcessor.getResourceGraph(res,fusekiUrl,null);
        if(model.size()==0) {
            throw new RestException(404,RestException.GENERIC_APP_ERROR_CODE,"No graph was found for resource Id : \""+res+"\"");
        }
        ResponseBuilder builder=Response.ok(ResponseOutputStream.getModelStream(model, format, res),media);
        return setHeaders(builder,getResourceHeaders(info.getPath(),format,"Choice")).build();
    }
    
       
    @GET
    @Path("/ontology/{path}/{class}")    
    @Produces("text/html")
    public Response getCoreOntologyClassView(@PathParam("class") String cl, 
            @PathParam("path") String path,
            @Context Request request) throws RestException{
        log.info("getCoreOntologyClassView()");          
        String uri="http://purl.bdrc.io/ontology/"+path+"/"+cl; 
        Date lastUpdate=OntData.getLastUpdated();
        EntityTag etag=new EntityTag(Integer.toString((lastUpdate.toString()+uri).hashCode()));
        ResponseBuilder builder = request.evaluatePreconditions(etag);        
        if(OntData.ontMod.getOntResource(uri)==null) {
            throw new RestException(404,RestException.GENERIC_APP_ERROR_CODE,"There is no resource matching the following URI: \""+uri+"\"");
        } 
        if(OntData.isClass(uri)) {
            /** class view **/
            if(builder == null){
                builder = Response.ok(new Viewable("/ontClassView.jsp", new OntClassModel(uri)));              
            }        
        }else {
            /** Properties view **/
            if(builder == null){
                builder = Response.ok(new Viewable("/ontPropView.jsp",new OntPropModel(uri)));                
            }          
        } 
        builder.header("Last-Modified", OntData.getLastUpdated()).tag(etag);
        return builder.build();
    }
    
    @GET
    @Path("/ontology.{ext}")     
    public Response getOntology(@DefaultValue("ttl") @PathParam("ext") String ext,@Context Request request) {        
        log.info("getOntology()");
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                
                Model model=OntData.ontMod;
                if(MediaTypeUtils.getJenaFromExtension(ext)!=null && !ext.equalsIgnoreCase("ttl")){
                    if(ext.equalsIgnoreCase("jsonld")) {
                        model.write(os,MediaTypeUtils.getJenaFromExtension("json"));
                    }else {
                        model.write(os,MediaTypeUtils.getJenaFromExtension(ext));
                    }
                }else{
                    RDFWriter writer=TTLRDFWriter.getSTTLRDFWriter(model);                   
                    writer.output(os);                                      
                }                
            }
        };
        EntityTag tag=OntData.getEntityTag();
        ResponseBuilder builder = request.evaluatePreconditions(tag);
        if(builder == null){
            builder = Response.ok(stream,MediaTypeUtils.getMediaTypeFromExt(ext));
            builder.header("Last-Modified", OntData.getLastUpdated()).header("Vary", "Accept").tag(tag);
        }
        return builder.build();        
    }
    
    @GET
    @Path("/ontology")
    @Produces("text/html")
    public Response getOntologyHomePage(@Context Request request) {        
        log.info("Call to getOntologyHomePage()"); 
        Date lastUpdate=OntData.getLastUpdated();
        EntityTag etag=new EntityTag(Integer.toString((lastUpdate.toString()+"/ontologyHome.jsp").hashCode()));
        ResponseBuilder builder = request.evaluatePreconditions(etag);
        if(builder == null){
            builder = Response.ok(new Viewable("/ontologyHome.jsp",OntData.ontMod));
            builder.header("Last-Modified", OntData.getLastUpdated()).tag(etag);
        }
        return builder.build();                
    }
    
    @POST
    @Path("/callbacks/github/owl-schema") 
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateOntology() throws RestException{        
        log.info("updating Ontology model() >>");
        Thread t=new Thread(new OntData());
        t.start();               
        return Response.ok().build();       
    }
    
    private static HashMap<String,String> getResourceHeaders(String url,String ext, String tcn) {
        HashMap<String,String> map =MediaTypeUtils.getExtensionMimeMap();
        HashMap<String,String> headers=new HashMap<>();
        if(ext!=null) {
            if(url.indexOf(".")<0) {
                headers.put("Content-Location", url+"."+ext);
            }else {
                url=url.substring(0, url.lastIndexOf("."));
            }
        }
        StringBuilder sb=new StringBuilder("");
        for(String ex:map.keySet()) {
            sb.append("{\""+url+"."+ex+"\" 1.000 {type "+MediaTypeUtils.getMimeFromExtension(ex)+"}},");               
        }
        headers.put("Alternates", sb.toString().substring(0, sb.toString().length()-1));
        headers.put("TCN", tcn);
        headers.put("Vary", "Negotiate,Accept");
        return headers;
    } 
    
    private static ResponseBuilder setHeaders(ResponseBuilder builder, HashMap<String,String> headers) {
        for(String key:headers.keySet()) {
            builder.header(key, headers.get(key));
        }
        return builder;
    }
    
}
