package io.bdrc.ldspdi.rest.resources;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.text.StrSubstitutor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.vocabulary.SKOS;

import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.jena.sttl.CompareComplex;
import io.bdrc.jena.sttl.ComparePredicates;
import io.bdrc.jena.sttl.STTLWriter;
import io.bdrc.ldspdi.parse.DocFileBuilder;
import io.bdrc.ldspdi.parse.PdiQueryParserException;
import io.bdrc.ldspdi.parse.QueryFileParser;
import io.bdrc.ldspdi.service.ServiceConfig;

@Path("/")
public class PublicDataResource {

	QueryProcessor processor=new QueryProcessor();
	public String fusekiUrl=ServiceConfig.getProperty("fuseki");
	
	@GET	
	//@Path("query")
	public Response getData(@Context UriInfo info) {
		System.out.println("BASE URI="+info.getBaseUri());
		String baseUri=info.getBaseUri().toString();
		MediaType media=new MediaType("text","html","utf-8");
		MultivaluedMap<String,String> mp=info.getQueryParameters();
		HashMap<String,String> converted=new HashMap<>();
		Set<String> set=mp.keySet();
		for(String st:set) {
			List<String> str=mp.get(st);
			for(String ss:str) {
				
				converted.put(st, ss);					
			}
		}		
		QueryFileParser qfp;
		final String query;
		try {
			qfp=new QueryFileParser(converted);
			String q=qfp.getQuery();
			String check=qfp.checkQueryArgsSyntax();
			if(check.length()>0) {
				throw new PdiQueryParserException("PdiQueryParserException : File->"
												  + converted.get("searchType")+".arq; ERROR: "+check);
			}
			StrSubstitutor sub = new StrSubstitutor(converted);
		    query = sub.replace(q);
		}
		catch(PdiQueryParserException pex) {
			StreamingOutput stream = new StreamingOutput() {
	            public void write(OutputStream os) throws IOException, WebApplicationException {
	            	// when prefix is null, QueryProcessor default prefix is used*/
	            	String res=pex.getMessage();
	            	os.write(res.getBytes());
	            }
	        };
	        return Response.ok(stream,media).build();
		}
		catch(IOException ex) {
			StreamingOutput stream = new StreamingOutput() {
	            public void write(OutputStream os) throws IOException, WebApplicationException {
	            	// when prefix is null, QueryProcessor default prefix is used*/
	            	os.write(ex.getLocalizedMessage().getBytes());
	            }
	        };
	        return Response.ok(stream,media).build();
			
		}	
		StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
            	// when prefix is null, QueryProcessor default prefix is used*/
            	String res=processor.getResource(query, null, true,baseUri);
            	os.write(res.getBytes());
            }
        };
		return Response.ok(stream,media).build();
		
	}
	
	@GET
	@Path("/{res}")	
	public Response getResourceFile(@PathParam("res") final String res) {
					
		MediaType media=new MediaType("text","turtle","utf-8");
		
		StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
            	// when prefix is null, QueryProcessor default prefix is used*/
            	Model model=processor.getResource(res,fusekiUrl,null);	
            	RDFWriter writer=getSTTLRDFWriter(model); 
            	writer.output(os);            		
            }
        };
		return Response.ok(stream,media).build();		
	}
	
	@GET
	@Path("/welcome")	
	public Response getWelcomeFile() {
					
		MediaType media=new MediaType("text","html","utf-8");
		
		
		StreamingOutput stream = new StreamingOutput() {
			String content=DocFileBuilder.getContent();			
            public void write(OutputStream os) throws IOException, WebApplicationException {
            	System.out.println("In rest: "+content);
            	os.write(content.getBytes());            		
            }
        };
		return Response.ok(stream,media).build();		
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
