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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
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
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.vocabulary.SKOS;
import org.glassfish.jersey.server.mvc.Viewable;

import io.bdrc.ldspdi.sparql.InjectionTracker;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ontology.service.core.OntClassModel;
import io.bdrc.formatters.JSONLDFormatter;
import io.bdrc.jena.sttl.CompareComplex;
import io.bdrc.jena.sttl.ComparePredicates;
import io.bdrc.jena.sttl.STTLWriter;
import io.bdrc.ldspdi.sparql.QueryFileParser;
import io.bdrc.ldspdi.service.ServiceConfig;

@Path("/")
public class PublicDataResource {

    @Context ServletContext context;
	QueryProcessor processor=new QueryProcessor();
	public String fusekiUrl="";
	String fuse="";
	
	@GET	
	public Response getData(@Context UriInfo info, @HeaderParam("fusekiUrl") final String fuseki) throws Exception{		
		
	    String baseUri=info.getBaseUri().toString();	
		if(fuseki !=null){
			fusekiUrl=fuseki;
		}else {
		    fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);  
		}
		MediaType media=new MediaType("text","html","utf-8");
		MultivaluedMap<String,String> mp=info.getQueryParameters();
		String filename= mp.getFirst("searchType")+".arq";		
		QueryFileParser qfp;
		final String query;
		
		qfp=new QueryFileParser(filename);
		String q=qfp.getQuery();
		String check=qfp.checkQueryArgsSyntax();
		if(check.length()>0) {
			throw new Exception("Exception : File->"+ filename+".arq; ERROR: "+check);
		}
		query=InjectionTracker.getValidQuery(q, mp);			
		StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
               // when prefix is null, QueryProcessor default prefix is used
                String res=processor.getResource(query, fusekiUrl, true,baseUri);
                os.write(res.getBytes());
            }
        };
        return Response.ok(stream,media).build();
	}
	
	@GET
    @Path("/ontology")
	@Produces("text/html")
	public Response getOntologyClassView(@QueryParam("classUri") String uri) {        
		MediaType media=new MediaType("text","html","utf-8");		
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("model", new OntClassModel(uri));
        return Response.ok(new Viewable("/ontClass.jsp", map),media).build();        
    }
	
	@GET
	@Path("/{res}")	
	public Response getResourceFile(
			@PathParam("res") final String res,
			@HeaderParam("Accept") final String format,
			@HeaderParam("fusekiUrl") final String fuseki,
			@QueryParam("classUri") final String uri) {
	    if(fuseki !=null){
            fusekiUrl=fuseki;
        }else {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);  
        }
		if(uri!=null) {
			Map<String, Object> map = new HashMap<String, Object>();
	        map.put("model", new OntClassModel(uri));
	        return Response.ok(new Viewable("/ontClass", map)).build();
		}
		MediaType media=new MediaType("text","turtle","utf-8");		
		if(ServiceConfig.isValidMime(format)){
			media=getMediaType(format);
		}
		StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
            	// when prefix is null, QueryProcessor default prefix is used
            	Model model=processor.getResource(res,fusekiUrl,null);	
            	RDFWriter writer=getSTTLRDFWriter(model); 
            	writer.output(os);            		
            }
        };
		return Response.ok(stream,media).build();		
	}
	
	@GET
	@Path("/{res}.{ext}")	
	public Response getTypedResourceFile(
			@PathParam("res") final String res, 
			@DefaultValue("ttl") @PathParam("ext") final String format,
			@HeaderParam("fusekiUrl") final String fuseki,
			@HeaderParam("prefix") final String prefix) {
	    if(fuseki !=null){
            fusekiUrl=fuseki;
        }else {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }
		MediaType media=new MediaType("text","turtle");
		if(isValidExtension(format)){
			String mime=ServiceConfig.getProperty("m"+format);
			String[] parts=mime.split(Pattern.quote("/"));
			media =new MediaType(parts[0],parts[1]);
		}
		StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
            	// when prefix is null, QueryProcessor default prefix is used*/
            	Model model=processor.getResource(res,fusekiUrl,prefix); 
            	if(isValidExtension(format)&& !format.equalsIgnoreCase("ttl")){
            		if(format.equalsIgnoreCase("jsonld")) {
            			Object jsonObject=JSONLDFormatter.modelToJsonObject(model, res);
            			JSONLDFormatter.jsonObjectToOutputStream(jsonObject, os);
            		}else {
            			model.write(os,ServiceConfig.getProperty(format));
            		}
            	}else{
            	    RDFWriter writer=getSTTLRDFWriter(model);            		
            		writer.output(os);            		            		
            	}            	 
            }
        };
		return Response.ok(stream,media).build();		
	}
	
	public MediaType getMediaType(String format){
		String[] parts=format.split(Pattern.quote("/"));
		return new MediaType(parts[0],parts[1]);		
	}
	
	public boolean isValidExtension(String ext){
		return (ServiceConfig.getProperty(ext)!=null);
	}
	
	public RDFWriter getSTTLRDFWriter(Model m) throws IOException{
		Lang sttl = STTLWriter.registerWriter();
		SortedMap<String, Integer> nsPrio = ComparePredicates.getDefaultNSPriorities();
		nsPrio.put(SKOS.getURI(), 1);
		nsPrio.put("http://purl.bdrc.io/ontology/admin/", 5);
		nsPrio.put("http://purl.bdrc.io/ontology/toberemoved/", 6);
		List<String> predicatesPrio = CompareComplex.getDefaultPropUris();
		predicatesPrio.add("http://purl.bdrc.io/ontology/admin/logWhen");
		predicatesPrio.add("http://purl.bdrc.io/ontology/onOrAbout");
		predicatesPrio.add("http://purl.bdrc.io/ontology/noteText");
		org.apache.jena.sparql.util.Context ctx = new org.apache.jena.sparql.util.Context();
		ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsPriorities"), nsPrio);
		ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsDefaultPriority"), 2);
		ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "complexPredicatesPriorities"), predicatesPrio);
		ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "indentBase"), 3);
		ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "predicateBaseWidth"), 12);
		RDFWriter w = RDFWriter.create().source(m.getGraph()).context(ctx).lang(sttl).build();
		return w;
	}	
}
