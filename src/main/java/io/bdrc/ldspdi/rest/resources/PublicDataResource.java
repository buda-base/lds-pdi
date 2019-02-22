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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.ws.rs.Consumes;
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

import org.apache.commons.io.IOUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFWriter;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.formatters.TTLRDFWriter;
import io.bdrc.ldspdi.ontology.service.core.OntClassModel;
import io.bdrc.ldspdi.ontology.service.core.OntData;
import io.bdrc.ldspdi.ontology.service.core.OntParams;
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
import io.bdrc.shacl.validation.ResourceShapeBuilder;


@Path("/")
public class PublicDataResource {

    public final static Logger log = LoggerFactory.getLogger(PublicDataResource.class.getName());

    public static final String RES_PREFIX_SHORT = "bdr";
    public static final String RES_PREFIX = "http://purl.bdrc.io/resource/";

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
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                os.write(ServiceConfig.getRobots().getBytes());
            }
        };
        return Response.ok(stream,MediaType.TEXT_PLAIN_TYPE).build();
    }

    @GET
    @Path("/shacl/{file}")
    public Response getShaclFile(@Context UriInfo info, @PathParam("file") final String file) {
        log.info("Call getRobots()");
        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                InputStream str=PublicDataResource.class.getClassLoader().getResourceAsStream(file+".ttl");
                os.write(IOUtils.toByteArray(str));
            }
        };
        return Response.ok(stream,MediaType.TEXT_PLAIN_TYPE).build();
    }

    @GET
    @Path("/shape/{clazz}")
    public Response getShapeFile(@Context UriInfo info, @PathParam("clazz") final String clazz) throws RestException {
        log.info("Call getRobots()");
        ResourceShapeBuilder builder=new ResourceShapeBuilder("http://purl.bdrc.io/ontology/core/"+clazz);
        Model m=builder.getShapeModel();
        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                m.write(os,"TURTLE");
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
        log.info("Call to getMultiChoice() with path={}", it);
        return Response.ok(new Viewable("/multiChoice.jsp",info.getBaseUri()+it)).build();
    }

    @GET
    @Path("/context.jsonld")
    public Response getJsonContext(@Context Request request) throws RestException {
        log.info("Call to getJsonContext()");
        EntityTag tag=OntData.getEntityTag();
        ResponseBuilder builder = request.evaluatePreconditions(tag);
        if(builder == null){
            builder = Response.ok(OntData.JSONLD_CONTEXT, MediaTypeUtils.MT_JSONLD);
            builder.header("Last-Modified", OntData.getLastUpdated()).tag(tag);
        }
        return builder.build();
    }

    @GET
    @Path("/resource/{res}")
    @JerseyCacheControl()
    public Response getResourceGraph(@PathParam("res") final String res,
            @HeaderParam("fusekiUrl") final String fusekiUrl,
            @HeaderParam("Accept") String format,
            @Context UriInfo info,
            @Context Request request) throws RestException {
        final String prefixedRes = RES_PREFIX_SHORT+':'+res;
        log.info("Call to getResourceGraphGET() with URL: {}, accept: {}", info.getPath(), format);
        final Variant variant = request.selectVariant(MediaTypeUtils.resVariants);
        if(format == null) {
            final String html=Helpers.getMultiChoicesHtml(info.getPath(),true);
            final ResponseBuilder rb=Response.status(300).entity(html).header("Content-Type", "text/html").
                    header("Content-Location",info.getBaseUri()+"choice?path="+info.getPath());
            return setHeaders(rb,getResourceHeaders(info.getPath(),null,"List",null)).build();
        }
        if(variant == null) {
            final String html=Helpers.getMultiChoicesHtml(info.getPath(),true);
            final ResponseBuilder rb=Response.status(406).entity(html).header("Content-Type", "text/html").
                    header("Content-Location",info.getBaseUri()+"choice?path="+info.getPath());
            return setHeaders(rb,getResourceHeaders(info.getPath(),null,"List",null)).build();
        }
        final MediaType mediaType = variant.getMediaType();
        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            try {
                ResponseBuilder builder=Response.seeOther(new URI(ServiceConfig.getProperty("showUrl")+prefixedRes));
                return setHeaders(builder,getResourceHeaders(info.getPath(),null,"Choice",null)).build();
            } catch (URISyntaxException e) {
                throw new RestException(500,new LdsError(LdsError.URI_SYNTAX_ERR).
                        setContext("getResourceGraphGet()",e));
            }
        }
        final Model model=QueryProcessor.getCoreResourceGraph(prefixedRes, fusekiUrl, null);
        if(model.size()==0) {
            throw new RestException(404,new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes));
        }
        final String ext = MediaTypeUtils.getExtFromMime(mediaType);
        final ResponseBuilder builder=Response.ok(ResponseOutputStream.getModelStream(model, ext, RES_PREFIX+res, null), mediaType);
        return setHeaders(builder,getResourceHeaders(info.getPath(),ext,"Choice",getEtag(model,res))).build();
    }

    @POST
    @Path("/resource/{res}")
    @JerseyCacheControl()
    public Response getResourceGraphPost(@PathParam("res") final String res,
            @HeaderParam("fusekiUrl") final String fusekiUrl,
            @HeaderParam("Accept") String format,
            @Context UriInfo info,
            @Context Request request) throws RestException{
        final String prefixedRes = RES_PREFIX_SHORT+':'+res;
        final Variant variant = request.selectVariant(MediaTypeUtils.resVariants);
        log.info("Call to getResourceGraphPost() with URL: {}, variant: {}, accept: {}", info.getPath(), variant, format);
        if(format== null) {
            final String html=Helpers.getMultiChoicesHtml(info.getPath(),true);
            final ResponseBuilder rb=Response.status(300).entity(html).header("Content-Type", "text/html").
                    header("Content-Location",info.getBaseUri()+"choice?path="+info.getPath());
            return rb.build();
        }
        if (variant == null) {
            return Response.status(406).build();
        }
        final MediaType mediaType = variant.getMediaType();
        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            try {
                ResponseBuilder builder=Response.seeOther(new URI(ServiceConfig.getProperty("showUrl")+prefixedRes));
                return setHeaders(builder,getResourceHeaders(info.getPath(),null,"Choice",null)).build();
            } catch (URISyntaxException e) {
                throw new RestException(500,new LdsError(LdsError.URI_SYNTAX_ERR).setContext("getResourceGraphPost()",e));
            }
        }
        Model model=QueryProcessor.getCoreResourceGraph(prefixedRes, fusekiUrl, null);
        if(model.size()==0) {
            throw new RestException(404,new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes));
        }
        final String ext = MediaTypeUtils.getExtFromMime(mediaType);
        ResponseBuilder builder=Response.ok(ResponseOutputStream.getModelStream(model, ext, RES_PREFIX+res, null), mediaType);
        return setHeaders(builder,getResourceHeaders(info.getPath(),ext,"Choice",getEtag(model,res))).build();
    }

    @GET
    @Path("/resource/{res}.{ext}")
    @JerseyCacheControl()
    public Response getFormattedResourceGraph(
            @PathParam("res") final String res,
            @PathParam("ext") final String ext,
            @HeaderParam("fusekiUrl") String fusekiUrl,
            @Context final UriInfo info) throws RestException{
        log.info("Call to getFormattedResourceGraph()");
        final String prefixedRes = RES_PREFIX_SHORT+':'+res;
        final MediaType media = MediaTypeUtils.getMimeFromExtension(ext);
        if (media == null) {
            final String html=Helpers.getMultiChoicesHtml("/resource/"+res,true);
            final ResponseBuilder rb=Response.status(300).entity(html).header("Content-Type", "text/html").
                    header("Content-Location",info.getBaseUri()+"choice?path="+info.getPath());
            return rb.build();
        }
        if (media.equals(MediaType.TEXT_HTML_TYPE)) {
            try {
                ResponseBuilder builder=Response.seeOther(new URI(ServiceConfig.getProperty("showUrl")+prefixedRes));
                return setHeaders(builder,getResourceHeaders(info.getPath(),null,null,null)).build();
            } catch (URISyntaxException e) {
                throw new RestException(500,new LdsError(LdsError.URI_SYNTAX_ERR).setContext("getResourceGraphPost()",e));
            }
        }
        if (ext.equals("mrcx")) {
            return MarcExport.getResponse(media, RES_PREFIX+res);
        }
        final Model model=QueryProcessor.getCoreResourceGraph(prefixedRes,fusekiUrl,null);
        if(model.size()==0) {
            throw new RestException(404,new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes));
        }
        final ResponseBuilder builder=Response.ok(ResponseOutputStream.getModelStream(model, ext, RES_PREFIX+res, null),media);
        return setHeaders(builder,getResourceHeaders(info.getPath(),ext,null,getEtag(model,res))).build();
    }

    /*@GET
    @Path("/ontology/{path}/{class}.{ext}")
    @JerseyCacheControl()
    public Response getCoreOntologyClassViewExt(@PathParam("class") final String cl,
            @PathParam("path") final String path,
            @PathParam("ext") final String ext,
            @Context final Request request,
            @Context final UriInfo info) throws RestException{
        log.info("getCoreOntologyClassView()");
        final String uri="http://purl.bdrc.io/ontology/"+path+"/"+cl;
        final EntityTag etag=OntData.getEntityTag();
        ResponseBuilder builder = request.evaluatePreconditions(etag);
        if(OntData.ontMod.getOntResource(uri) == null) {
            throw new RestException(404,new LdsError(LdsError.ONT_URI_ERR).setContext(uri));
        }
        if (builder != null) {
            builder.header("Last-Modified", OntData.getLastUpdated()).tag(etag);
            return builder.build();
        }
        final MediaType media = MediaTypeUtils.getMimeFromExtension(ext);
        if (media == null) {
            final String html=Helpers.getMultiChoicesHtml("/ontology/"+path+"/"+cl, true);
            final ResponseBuilder rb=Response.status(300).entity(html).header("Content-Type", "text/html").
                    header("Content-Location",info.getBaseUri()+"choice?path="+info.getPath());
            return rb.build();
        }
        if (media.equals(MediaType.TEXT_HTML_TYPE)) {
            if (OntData.isClass(uri)) {
                builder = Response.ok(new Viewable("/ontClassView.jsp", new OntClassModel(uri)));
            } else {
                builder = Response.ok(new Viewable("/ontPropView.jsp",new OntPropModel(uri)));
            }
        } else {
            final Model model = OntData.describeUri(uri);
            builder = Response.ok(ResponseOutputStream.getModelStream(model, ext, uri, null),media);
        }
        // there could be more headers here
        builder.header("Last-Modified", OntData.getLastUpdated()).tag(etag);
        return builder.build();
    }*/

    @GET    
    @Path("/{base : .*}/{other}")    
    @JerseyCacheControl()
    public Response getExtOntologyHomePage(
    		@Context final UriInfo info, 
    		@Context Request request,
    		@HeaderParam("Accept") String format,
    		@PathParam("base") String base, 
    		@PathParam("other") String other) throws RestException {    	
    	ResponseBuilder builder = null;
    	//Is the full request uri a baseuri? If so, setting up current ont and serving its the home page
    	if(ServiceConfig.getConfig().isBaseUri(info.getAbsolutePath().toString())) {    		
    		OntParams pr=ServiceConfig.getConfig().getOntologyByBase(info.getAbsolutePath().toString());
    		OntModel mod=OntData.getOntModelByBase(info.getAbsolutePath().toString()); 
    		if(format !=null) {
    			Variant variant = request.selectVariant(MediaTypeUtils.resVariants);
    			if (variant == null) {
    	            return Response.status(406).build();
    	        }
    	        MediaType mediaType = variant.getMediaType();
    	        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
    	        	builder = Response.ok(new Viewable("/ontologyHome.jsp",mod));
    	        }else {
    	        	final String JenaLangStr = MediaTypeUtils.getJenaFromExtension(MediaTypeUtils.getExtFromMime(mediaType));
    	        	final StreamingOutput stream = new StreamingOutput() {
    	                @Override
    	                public void write(OutputStream os) throws IOException, WebApplicationException {
    	                    if (JenaLangStr == "STTL") {
    	                        final RDFWriter writer = TTLRDFWriter.getSTTLRDFWriter(mod,pr.getBaseuri());
    	                        writer.output(os);
    	                    }else {
    	                    	//here using the absolute path as baseUri since it has been recognized 
    	                    	//as the base uri of a declared ontology (in ontologies.yml file)    	                    	
    	                    	mod.write(os, JenaLangStr, pr.getBaseuri());
    	                    }
    	                }
    	            };
    	            builder = Response.ok(stream, MediaTypeUtils.getMimeFromExtension(MediaTypeUtils.getExtFromMime(mediaType)));    	        	
    	        }
    		}          
    	}else     	
    		{
    		//if not, checking if a valid ontology matches the baseUri part of the request
        	//if so : serving properties or class pages
	    	OntParams ont=ServiceConfig.getConfig().getOntologyByBase(info.getBaseUri()+base+"/");	    	
	    	if(ont !=null) {
	    		OntData.setOntModel(ont.getName());		    		
	    		if(OntData.ontMod.getOntResource(info.getAbsolutePath().toString()) == null) {	    			
	    			throw new RestException(404,new LdsError(LdsError.ONT_URI_ERR).setContext("Ont resource is null for"+ info.getAbsolutePath().toString()));
	    	    }
	            if(builder == null){
	            	if (OntData.isClass(info.getAbsolutePath().toString())) {	
	            		log.info("CLASS>>"+info.getAbsolutePath().toString());
	                    builder = Response.ok(new Viewable("/ontClassView.jsp", new OntClassModel(info.getAbsolutePath().toString())));
	                	} else {
	                	log.info("PROP>>"+info.getAbsolutePath().toString());
	                    builder = Response.ok(new Viewable("/ontPropView.jsp",new OntPropModel(info.getAbsolutePath().toString())));
	                    System.out.println("OntPropModel >>"+new OntPropModel(info.getAbsolutePath().toString()));
	                }
	            }		
	    	}else {
	    		throw new RestException(404,new LdsError(LdsError.ONT_URI_ERR).setContext("Ontparams is null for "+info.getBaseUri()+base+"/"));
	    	}
    	}
    	return builder.build();
    }
    
    @GET    
    @Path("/{base : .*}/{other}.{ext}")
    @Produces("text/html")
    @JerseyCacheControl()
    public Response getOntologyResourceAsFile(
    		@Context final UriInfo info, 
    		@Context Request request,
    		@PathParam("base") String base, 
    		@PathParam("other") String other,
    		@PathParam("ext") String ext) throws RestException {
    	String res=info.getAbsolutePath().toString();
    	res=res.substring(0, res.lastIndexOf('.'))+"/";    	
    	ResponseBuilder builder = null;
    	final String JenaLangStr = MediaTypeUtils.getJenaFromExtension(ext);
        if (JenaLangStr == null) {
            throw new RestException(404, new LdsError(LdsError.URI_SYNTAX_ERR).setContext(info.getAbsolutePath().toString()));
        }    	
    	if(ServiceConfig.getConfig().isBaseUri(res)) {
    		OntParams params=ServiceConfig.getConfig().getOntologyByBase(res);
    		final String baseUri=res;
    		Model model=OntData.setOntModel(params.getName());    		   
            final StreamingOutput stream = new StreamingOutput() {
                @Override
                public void write(OutputStream os) throws IOException, WebApplicationException {
                    if (JenaLangStr == "STTL") {
                        final RDFWriter writer = TTLRDFWriter.getSTTLRDFWriter(model,baseUri);
                        writer.output(os);
                    }else {
                    	model.write(os, JenaLangStr,baseUri);
                    }
                }
            };
            builder = Response.ok(stream, MediaTypeUtils.getMimeFromExtension(ext));
    	}else {
    		//to implement here : serving serialized single class or props
    		throw new RestException(404,new LdsError(LdsError.ONT_URI_ERR).setContext(info.getAbsolutePath().toString()));	    	
    	}
    	return builder.build();
    }

    @POST
    @Path("/callbacks/github/owl-schema")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateOntology() throws RestException{
    	//To be reworked to include all declared ontologies
        log.info("updating Ontology models() >>");
        Thread t=new Thread(new OntData());
        t.start();
        return Response.ok("Ontologies were updated").build();
    }

    private static HashMap<String,String> getResourceHeaders(String url,String ext, String tcn, String eTag) {
        HashMap<String,MediaType> map = MediaTypeUtils.getResExtensionMimeMap();
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
        if (tcn != null)
            headers.put("TCN", tcn);
        headers.put("Vary", "Negotiate, Accept");
        if(eTag!=null) {
            headers.put("ETag", eTag);
        }
        return headers;
    }

    private static ResponseBuilder setHeaders(ResponseBuilder builder, HashMap<String,String> headers) {
        for(String key:headers.keySet()) {
            builder.header(key, headers.get(key));
        }
        return builder;
    }

    private static String getEtag(Model model,String res) {
        Statement smt=model.getProperty(ResourceFactory.createResource("http://purl.bdrc.io/resource/"+res),
                ResourceFactory.createProperty("http://purl.bdrc.io/ontology/admin/gitRevision"));
        if(smt!=null) {
            return smt.getObject().toString();
        }
        return null;
    }

}