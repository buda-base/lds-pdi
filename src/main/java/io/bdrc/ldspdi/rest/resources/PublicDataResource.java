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
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFWriter;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.formatters.TTLRDFWriter;
import io.bdrc.ldspdi.ontology.service.core.OntClassModel;
import io.bdrc.ldspdi.ontology.service.core.OntData;
import io.bdrc.ldspdi.ontology.service.core.OntPropModel;
import io.bdrc.ldspdi.rest.features.JerseyCacheControl;
import io.bdrc.ldspdi.results.CacheAccessModel;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.DocFileModel;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.ldspdi.utils.MediaTypeUtils;
import io.bdrc.ldspdi.utils.ResponseOutputStream;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;


@Path("/")
public class PublicDataResource {   
    
    public final static Logger log=LoggerFactory.getLogger(PublicDataResource.class.getName());  
    public String fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
    
    public static final String RES_PREFIX = "bdr";
    
    
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
        log.info("Call to getMultiChoice() with path="+it);
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
        @HeaderParam("fusekiUrl") final String fuseki,
        @HeaderParam("Accept") String format,
        @Context UriInfo info,
        @Context Request request) throws RestException {
        final String prefixedRes = RES_PREFIX+':'+res;
        log.info("Call to getResourceGraphGET() with URL: "+info.getPath()+" Accept >> "+format); 
        Variant variant = request.selectVariant(MediaTypeUtils.resVariants);
        if(format == null) {
            final String html=Helpers.getMultiChoicesHtml(info.getPath(),true);
            final ResponseBuilder rb=Response.status(300).entity(html).header("Content-Type", "text/html").
                    header("Content-Location",info.getBaseUri()+"choice?path="+info.getPath());
            return setHeaders(rb,getResourceHeaders(info.getPath(),null,"List")).build();
        }
        if(variant == null) {
            final String html=Helpers.getMultiChoicesHtml(info.getPath(),true);
            final ResponseBuilder rb=Response.status(406).entity(html).header("Content-Type", "text/html").
                    header("Content-Location",info.getBaseUri()+"choice?path="+info.getPath());
            return setHeaders(rb,getResourceHeaders(info.getPath(),null,"List")).build();
        }
        final MediaType mediaType = variant.getMediaType();
        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {            
            try {
                ResponseBuilder builder=Response.seeOther(new URI(ServiceConfig.getProperty("showUrl")+prefixedRes));
                return setHeaders(builder,getResourceHeaders(info.getPath(),null,"Choice")).build();
            } catch (URISyntaxException e) {
                throw new RestException(500,new LdsError(LdsError.URI_SYNTAX_ERR).
                        setContext("getResourceGraphGet()",e));
            }
        }
        if(fuseki !=null){ 
            fusekiUrl=fuseki;            
        }            
        Model model=QueryProcessor.getCoreResourceGraph(prefixedRes,fusekiUrl,null);
        if(model.size()==0) {
            throw new RestException(404,new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes));
        }
        final String ext = MediaTypeUtils.getExtFormatFromMime(mediaType.toString());
        ResponseBuilder builder=Response.ok(ResponseOutputStream.getModelStream(model,ext), mediaType);
        return setHeaders(builder,getResourceHeaders(info.getPath(),ext,"Choice")).build();
    }
    
    @POST
    @Path("/resource/{res}") 
    @JerseyCacheControl()
    public Response getResourceGraphPost(@PathParam("res") final String res,
        @HeaderParam("fusekiUrl") final String fuseki,
        @HeaderParam("Accept") String format,
        @Context UriInfo info,        
        @Context Request request) throws RestException{ 
        final String prefixedRes = RES_PREFIX+':'+res;
        Variant variant = request.selectVariant(MediaTypeUtils.resVariants);
        log.info("Call to getResourceGraphPost() with URL: "+info.getPath()+ " Variant >> "+variant+ " Accept >> "+format); 
        if(format== null) {
            final String html=Helpers.getMultiChoicesHtml(info.getPath(),true);
            final ResponseBuilder rb=Response.status(300).entity(html).header("Content-Type", "text/html").
                    header("Content-Location",info.getBaseUri()+"choice?path="+info.getPath());
            return rb.build();            
        }
        if(variant == null) {
            return Response.status(406).build();
        }
        final MediaType mediaType = variant.getMediaType();
        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {            
            try {
                ResponseBuilder builder=Response.seeOther(new URI(ServiceConfig.getProperty("showUrl")+prefixedRes));
                return setHeaders(builder,getResourceHeaders(info.getPath(),null,"Choice")).build();
            } catch (URISyntaxException e) {
                throw new RestException(500,new LdsError(LdsError.URI_SYNTAX_ERR).setContext("getResourceGraphPost()",e));
            }
        }
        if(fuseki !=null){ 
            fusekiUrl=fuseki;            
        }            
        Model model=QueryProcessor.getCoreResourceGraph(prefixedRes,fusekiUrl,null);
        if(model.size()==0) {
            throw new RestException(404,new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes));
        }
        final String ext = MediaTypeUtils.getExtFormatFromMime(mediaType.toString());
        ResponseBuilder builder=Response.ok(ResponseOutputStream.getModelStream(model,ext), mediaType);
        return setHeaders(builder,getResourceHeaders(info.getPath(),ext,"Choice")).build();
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
        final String prefixedRes = RES_PREFIX+':'+res;
        if(MediaTypeUtils.getMimeFromExtension(format)==null) {
            final String html=Helpers.getMultiChoicesHtml("/resource/"+res,true);
            final ResponseBuilder rb=Response.status(300).entity(html).header("Content-Type", "text/html").
                    header("Content-Location",info.getBaseUri()+"choice?path="+info.getPath());
            return rb.build();
        }
        if(fuseki !=null){
            fusekiUrl=fuseki;            
        }
        MediaType media=MediaTypeUtils.getMimeFromExtension(format);
        Model model=QueryProcessor.getCoreResourceGraph(prefixedRes,fusekiUrl,null);
        if(model.size()==0) {
            throw new RestException(404,new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes));
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
            throw new RestException(404,new LdsError(LdsError.ONT_URI_ERR).setContext(uri));
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
            builder = Response.ok(stream,MediaTypeUtils.getMimeFromExtension(ext));
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
    
    @GET
    @Path("/authmodel")
    public Response getAuthModel(@Context Request request) throws RestException {        
        log.info("Call to getAuthModel()"); 
        return Response.ok(ResponseOutputStream.getModelStream(
                QueryProcessor.getAuthDataGraph(fusekiUrl)),MediaTypeUtils.getMimeFromExtension("ttl"))
                .build();                 
    }
    
    @GET
    @Path("/authmodel/updated")
    public long getAuthModelUpdated(@Context Request request) {        
        //log.info("Call to getAuthModelUpdated()"); 
        return RdfAuthModel.getUpdated();                 
    }
    
    @POST
    @Path("/callbacks/github/bdrc-auth") 
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateAuthModel() throws RestException{        
        log.info("updating Auth data model() >>");
        Thread t=new Thread(new RdfAuthModel());
        t.start();
        return Response.ok("Auth Model was updated").build();       
    }
    
    @POST
    @Path("/callbacks/github/owl-schema") 
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateOntology() throws RestException{        
        log.info("updating Ontology models() >>");
        Thread t=new Thread(new OntData());
        t.start();  
        return Response.ok("Ontologies were updated").build();       
    }
    
    private static HashMap<String,String> getResourceHeaders(String url,String ext, String tcn) {
        HashMap<String,MediaType> map = MediaTypeUtils.getExtensionMimeMap();
        HashMap<String,String> headers=new HashMap<>();
        if(ext!=null) {
            if(url.indexOf(".")<0) {
                headers.put("Content-Location", url+"."+ext);
            }else {
                url=url.substring(0, url.lastIndexOf("."));
            }
        }
        StringBuilder sb=new StringBuilder("");
        for(Entry<String,MediaType> e :map.entrySet()) {
            sb.append("{\""+url+"."+e.getKey()+"\" 1.000 {type "+e.getValue().toString()+"}},");               
        }
        headers.put("Alternates", sb.toString().substring(0, sb.toString().length()-1));
        headers.put("TCN", tcn);
        headers.put("Vary", "Negotiate, Accept");
        return headers;
    } 
    
    private static ResponseBuilder setHeaders(ResponseBuilder builder, HashMap<String,String> headers) {
        for(String key:headers.keySet()) {
            builder.header(key, headers.get(key));
        }
        return builder;
    }
    
}