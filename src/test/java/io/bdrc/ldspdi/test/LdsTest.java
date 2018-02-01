package io.bdrc.ldspdi.test;

/*******************************************************************************
 * Copyright (c) 2017 Buddhist Digital Resource Center (BDRC)
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

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.ws.rs.core.Application;

import org.apache.jena.fuseki.embedded.FusekiServer;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.vocabulary.SKOS;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.Response;

import io.bdrc.formatters.JSONLDFormatter;
import io.bdrc.jena.sttl.CompareComplex;
import io.bdrc.jena.sttl.ComparePredicates;
import io.bdrc.jena.sttl.STTLWriter;
import io.bdrc.ldspdi.rest.resources.PublicDataResource;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.test.TestUtils;


public class LdsTest extends JerseyTest {
	
	public static Writer logWriter;	
	private static FusekiServer server ;
	private static Dataset srvds = DatasetFactory.createTxnMem();
	private static Model model = ModelFactory.createDefaultModel();	
	public static Lang sttl = STTLWriter.registerWriter();
	public static String fusekiUrl;
	
	@BeforeClass
	public static void init() {
		try {
            logWriter = new PrintWriter(new OutputStreamWriter(System.err, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
		ServiceConfig.initForTests();
		loadData();		
		srvds.setDefaultModel(model);
		//Creating a fuseki server
		server = FusekiServer.create()
	            .setPort(2244)
	            .add("/bdrcrw", srvds)
	            .build() ;
		fusekiUrl="http://localhost:2244/bdrcrw";		
	    server.start() ; 	     
	}
	
	@AfterClass
    public static void close() {
		server.stop();		
    }
	
	@Override
    protected Application configure() {
		return new ResourceConfig(PublicDataResource.class);
    }
	
	
	@Test
	public void testQueryProcessorModel() throws IOException{
		// Makes sure that requested model is actually returned
		// by the query processor
		
		ArrayList<String> list=TestUtils.getResourcesList();
		for(String res : list){
			Model[] md=prepareAssertModel(res);
			assertTrue(md[0].isIsomorphicWith(md[1]));			
		}	
	}
	
	@Test
	public void testGetModel() throws IOException{
		// Makes sure that requested model is actually returned
		// by the rest API
		
		ArrayList<String> resList=TestUtils.getResourcesList();
		//Browser-like query without extension nor accept header --> returns STTL by default
		for(String res : resList){
			Model[] md= prepareGetAssertModel(res);	
			assertTrue(md[0].isIsomorphicWith(md[1]));
		}		
	}
	
	@Test
    public void testJSONLDFormatter() throws IOException{
        // Loads resource model from .ttl file
        // Creates a jsonld object of that model and saves it to.jsonld file
        // Loads Model from jsonld file
        ArrayList<String> resList=TestUtils.getResourcesList();
        for(String res:resList) {       
            //Excluding unsupported Etext format
            if(!res.startsWith("U")) {
                // Checking Model preservation through JSONLDFormatter process
                Model m=getModelFromFileName(TestUtils.TESTDIR+res+".ttl", RDFLanguages.RDFXML);                
                Object jsonObject=JSONLDFormatter.modelToJsonObject(m, res);
                FileOutputStream fos=new FileOutputStream(new File(TestUtils.TESTDIR+res+".jsonld"));
                JSONLDFormatter.jsonObjectToOutputStream(jsonObject, fos);
                fos.close();                
                Model json=getModelFromFileName(TestUtils.TESTDIR+res+".jsonld",RDFLanguages.JSONLD);
                assertTrue(m.isIsomorphicWith(json));
                File to_delete=new File(TestUtils.TESTDIR+res+".jsonld");
                to_delete.delete();
                // Checking Model against QueryProcessor                              
                QueryProcessor processor=new QueryProcessor();
                Model mq=processor.getResourceGraph(res,fusekiUrl);
                assertTrue(mq.isIsomorphicWith(json));
            }
        }
    }
	
	@Test
	public void testGetSTTLSyntax() throws IOException{
		ArrayList<String> resList=TestUtils.getResourcesList();
		// Browser-like query without extension nor accept header 
		// --> returns STTL by default
		// Tests the STTL string produced by the API
		
		for(String res : resList){
			//Excluding unsupported Etext format
			if(!res.startsWith("U")) {
				String[] st= prepareGetAssertSTTLSyntax(res);
				assertTrue(st[0].equals(st[1]));
			}
		}		
	}
		
	
	
	@Test
	public void testResponseOtherContentType() throws IOException{
		// Browser-like query with extension without accept header 
		// Tests all content Types produced by the API
		// except for sttl and jsonld
		
		ArrayList<String> resList=TestUtils.getResourcesList();
		Set<String> formats=TestUtils.getContentTypes().keySet();
		//Browser-like query with extension without accept header
		for(String res : resList){				
			for(String fmt:formats){				
				if(!fmt.equals("ttl") && !fmt.equals("jsonld") ){							
					
					String ct1=TestUtils.getContentTypes().get(fmt);			
					Response output = target("/resource/"+res+"."+fmt)
							.request()
							.header("fusekiUrl", fusekiUrl)
							.get();
					String ct2=output.getHeaderString("content-type");					
					output.close();
					assertEquals(ct1.trim(),ct2.trim());
				}
			}		
		}
		// Like curl queries : without extension, with accept header 
		// Tests the contentType produced by the API
		
		Collection<String> cts=TestUtils.getContentTypes().values();
		for(String res : resList){
			for(String ct: cts){
				if(!ct.equals("text/turtle") && !ct.equals("application/ld+json")) {
					Response output = target("/resource/"+res)
							.request()
							.header("fusekiUrl", fusekiUrl)
							.header("Accept", ct)
							.get();
					String ct1=output.getHeaderString("content-type");	
					output.close();
					assertEquals(ct.trim(),ct1.trim());
				}
			}
				
		}
	}	 
		
	private Model[] prepareAssertModel(String res) throws IOException{
		// Loads resource model from .ttl file
		// Adjusts prefix mapping
		// Get resource model from embedded fuseki via QueryProcessor
		// returns both models to be compared by testQueryProcessorModel()
		
		QueryProcessor processor=new QueryProcessor();
		
		Model m=getModelFromFileName(TestUtils.TESTDIR+res+".ttl", sttl);		
		Model mq=processor.getResourceGraph(res,fusekiUrl);			
		Model[] ret={m,mq};		
		return ret;
	}
	
	private Model[] prepareGetAssertModel(String res) throws IOException{
		// Loads resource model from .ttl file
		// Adjusts prefix mapping
		// Gets resource model from rest API
		// and returns them for comparison by testGetModel() 
		
		Model m=getModelFromFileName(TestUtils.TESTDIR+res+".ttl", sttl);		
		Model m_rest = ModelFactory.createDefaultModel();
		Response output = target("/resource/"+res).request().header("fusekiUrl", fusekiUrl).get();
		String resp=output.readEntity(String.class).trim();		
		ByteArrayInputStream is=new ByteArrayInputStream(resp.getBytes());
		m_rest.read(is,null,"TURTLE");
		Model[] md={m,m_rest};		
		is.close();
		return md;
	}
	
	private String[] prepareGetAssertSTTLSyntax(String res) throws IOException{
		// Loads resource model from .ttl file
		// Adjusts prefix mapping
		// Gets resource model from rest API
		// Writes both model and returns them as strings to be compared
		// by testGetModelSyntax()  
		
		Model m=getModelFromFileName(TestUtils.TESTDIR+res+".ttl", sttl);
		RDFWriter wFile=getSttlRDFWriter(m);
		Map<String,String> prefixMap=m.getNsPrefixMap();
		String prefix=TestUtils.convertToString(prefixMap);
		
		Model m_rest = ModelFactory.createDefaultModel();
		Response output = target("/resource/"+res)
				.request()
				.header("fusekiUrl", fusekiUrl)
				.header("prefix", prefix)
				.get();
		String resp=output.readEntity(String.class).trim();		
		output.close();		
		ByteArrayInputStream is=new ByteArrayInputStream(resp.getBytes());
		m_rest.read(is,null,"TURTLE");
		RDFWriter w=getSttlRDFWriter(m_rest);
		
		ByteArrayOutputStream baos1=new ByteArrayOutputStream();
		ByteArrayOutputStream baos2=new ByteArrayOutputStream();
				
		wFile.output(baos1);
		w.output(baos2);	
		
		String[] ret={baos2.toString(),baos2.toString()};
		is.close();
		baos1.close();
		baos2.close();
		return ret;
	}
	
	
	static void loadData(){
		//Loads the test dataset/
		ArrayList<String> list=TestUtils.getResourcesList();
		for(String res : list){
			Model m=getModelFromFileName(TestUtils.TESTDIR+res+".ttl", sttl);
			model.add(m);
		}		
	}
		
	static Model getModelFromFileName(String fname, Lang lang) {
		Model m = ModelFactory.createDefaultModel();
		Graph g = m.getGraph();
		try {
		    RDFParserBuilder pb = RDFParser.create()
		             .source(fname)
		             .lang(lang);		             
		    pb.parse(StreamRDFLib.graph(g));
		} catch (RiotException e) {
		    writeLog("error reading "+fname);
		    e.printStackTrace();
		    return null;
		}		
		return m;
	}
		
	static void writeLog(String s) {
        try {
            logWriter.write(s+"\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }	
		
	public RDFWriter getSttlRDFWriter(Model m) throws IOException{
		Lang sttl = STTLWriter.registerWriter();
		SortedMap<String, Integer> nsPrio = ComparePredicates.getDefaultNSPriorities();
		nsPrio.put(SKOS.getURI(), 1);
		nsPrio.put("http://purl.bdrc.io/ontology/admin/", 5);
		nsPrio.put("http://purl.bdrc.io/ontology/toberemoved/", 6);
		List<String> predicatesPrio = CompareComplex.getDefaultPropUris();
		predicatesPrio.add("http://purl.bdrc.io/ontology/admin/logWhen");
		predicatesPrio.add("http://purl.bdrc.io/ontology/onOrAbout");
		predicatesPrio.add("http://purl.bdrc.io/ontology/noteText");
		Context ctx = new Context();
		ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsPriorities"), nsPrio);
		ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsDefaultPriority"), 2);
		ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "complexPredicatesPriorities"), predicatesPrio);
		ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "indentBase"), 3);
		ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "predicateBaseWidth"), 12);
		RDFWriter w = RDFWriter.create().source(m.getGraph()).context(ctx).lang(sttl).build();
		return w;
	}

}
