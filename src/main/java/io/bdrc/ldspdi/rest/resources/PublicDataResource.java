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

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFWriter;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.jsp.JspMvcFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.formatters.JSONLDFormatter;
import io.bdrc.formatters.TTLRDFWriter;
import io.bdrc.ldspdi.ontology.service.core.OntClassModel;
import io.bdrc.ldspdi.ontology.service.core.OntData;
import io.bdrc.ldspdi.ontology.service.core.OntPropModel;
import io.bdrc.ldspdi.rest.features.CacheControlFilterFactory;
import io.bdrc.ldspdi.rest.features.CorsFilter;
import io.bdrc.ldspdi.rest.features.GZIPWriterInterceptor;
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
    
    
    public ArrayList<String> validMedia=new ArrayList<>();
    
        
    public PublicDataResource() {
        super();
        ResourceConfig config=new ResourceConfig(PublicDataResource.class);
        config.register(LoggingFeature.class);
        config.register(CorsFilter.class); 
        config.register(GZIPWriterInterceptor.class);
        config.property(JspMvcFeature.TEMPLATE_BASE_PATH, "").register(JspMvcFeature.class);
        config.register(CacheControlFilterFactory.class);
        validMedia.add(MediaType.TEXT_HTML);
        validMedia.add(MediaType.APPLICATION_XHTML_XML);
    }
    
    @GET   
    @JerseyCacheControl()
    @Produces(MediaType.TEXT_HTML)    
    public Viewable getHomePage() throws RestException{
        log.info("Call to getHomePage()");         
        return new Viewable("/index.jsp",new DocFileModel()); 
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
    public Viewable getCacheInfo() {
        log.info("Call to getCacheInfo()");        
        return new Viewable("/cache.jsp",new CacheAccessModel()); 
    }
    
    @GET
    @Path("/context.jsonld")
    public Response getJsonContext() throws RestException {
        log.info("Call to getJsonContext()");
        return Response.ok(ServiceConfig.JSONLD_CONTEXT, "application/ld+json").build();
    }

    @GET
    @Path("/resource/{res}")
    @JerseyCacheControl()
    public Response getResourceGraph(@PathParam("res") final String res,
        @HeaderParam("Accept") final String format,
        @HeaderParam("fusekiUrl") final String fuseki) throws RestException{
        
        String redirect="http://library.bdrc.io/show/bdr:"+res;
        if(format==null || format.equals(MediaType.APPLICATION_XHTML_XML) ||
                format.equals(MediaType.TEXT_HTML) || !MediaTypeUtils.isMime(format)) {            
            try {
                return Response.seeOther(new URI(redirect)).build();
            } catch (URISyntaxException e) {
                throw new RestException(500,RestException.GENERIC_APP_ERROR_CODE,"getResourceGraph : URISyntaxException"+e.getMessage());
            }
        }
        log.info("Call to getResourceGraph()");
        if(fuseki !=null){ 
            fusekiUrl=fuseki;            
        }            
        Model model=QueryProcessor.getResourceGraph(res,fusekiUrl,null);
        if(model.size()==0) {
            throw new RestException(404,RestException.GENERIC_APP_ERROR_CODE,"No graph was found for resource Id : \""+res+"\"");
        }
        return Response.ok(ResponseOutputStream.getModelStream(model,MediaTypeUtils.getExtFormatFromMime(format)),MediaTypeUtils.getMediaTypeFromExt(format)).build();       
    }
    
    @POST
    @Path("/resource/{res}") 
    @JerseyCacheControl()
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getResourceGraphPost(@PathParam("res") final String res,
        @HeaderParam("Accept") final String format,
        @HeaderParam("fusekiUrl") final String fuseki) throws RestException{
           
        log.info("Call to getResourceGraphPost");
        if(fuseki !=null){
            fusekiUrl=fuseki;            
        }           
        Model model=QueryProcessor.getResourceGraph(res,fusekiUrl,null); 
        if(model.size()==0) {
            throw new RestException(404,RestException.GENERIC_APP_ERROR_CODE,"No graph was found for resource Id : \""+res+"\"");
        }
        Object jsonObject=JSONLDFormatter.modelToJsonObject(model, res);        
        return Response.ok(ResponseOutputStream.getJsonLDResponseStream(jsonObject),MediaTypeUtils.getMediaTypeFromExt(format)).build();       
    }
     
    @GET
    @Path("/resource/{res}.{ext}")   
    @JerseyCacheControl()
    public Response getFormattedResourceGraph(
            @PathParam("res") final String res, 
            @DefaultValue("ttl") @PathParam("ext") final String format,
            @HeaderParam("fusekiUrl") final String fuseki) throws RestException{
        
        log.info("Call to getFormattedResourceGraph()");
        
        if(fuseki !=null){
            fusekiUrl=fuseki;            
        }
        MediaType media=MediaTypeUtils.getMediaTypeFromExt(format);
        Model model=QueryProcessor.getResourceGraph(res,fusekiUrl,null);
        if(model.size()==0) {
            throw new RestException(404,RestException.GENERIC_APP_ERROR_CODE,"No graph was found for resource Id : \""+res+"\"");
        }
        return Response.ok(ResponseOutputStream.getModelStream(model, format, res),media).build();       
    }
    
    @POST
    @Path("/resource/{res}.{ext}") 
    @JerseyCacheControl()
    public Response getFormattedResourceGraphPost(
            @PathParam("res") final String res, 
            @DefaultValue("ttl") @PathParam("ext") final String format,
            @HeaderParam("fusekiUrl") final String fuseki) throws RestException{
        
        log.info("Call to getFormattedResourceGraphPost()");
        
        if(fuseki !=null){
            fusekiUrl=fuseki;            
        }
        MediaType media=MediaTypeUtils.getMediaTypeFromExt(format);
        Model model=QueryProcessor.getResourceGraph(res,fusekiUrl,null);
        if(model.size()==0) {
            throw new RestException(404,RestException.GENERIC_APP_ERROR_CODE,"No graph was found for resource Id : \""+res+"\"");
        }     
        return Response.ok(ResponseOutputStream.getModelStream(model, format, res),media).build();      
    }
    
       
    @GET
    @Path("/ontology/{path}/{class}")
    @Produces("text/html")
    public Viewable getCoreOntologyClassView(@PathParam("class") String cl, @PathParam("path") String path) throws RestException{
        log.info("getCoreOntologyClassView()");          
        String uri="http://purl.bdrc.io/ontology/"+path+"/"+cl;        
        if(OntData.ontMod.getOntResource(uri)==null) {
            throw new RestException(404,RestException.GENERIC_APP_ERROR_CODE,"There is no resource matching the following URI: \""+uri+"\"");
        } 
        if(OntData.isClass(uri)) {
            /** class view **/
            return new Viewable("/ontClassView.jsp", new OntClassModel(uri));
        }else {
            /** Properties view **/
            return new Viewable("/ontPropView.jsp",new OntPropModel(uri));
        }      
    }
    
    @GET
    @Path("/ontology.{ext}")     
    public Response getOntology(@DefaultValue("ttl") @PathParam("ext") String ext) {        
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
        return Response.ok(stream,MediaTypeUtils.getMediaTypeFromExt(ext)).build();        
    }
    
    @GET
    @Path("/ontology")
    @Produces("text/html")
    public Viewable getOntologyHomePage() {        
        log.info("Call to getOntologyHomePage()");          
        return new Viewable("/ontologyHome.jsp",OntData.ontMod);        
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
    
}
