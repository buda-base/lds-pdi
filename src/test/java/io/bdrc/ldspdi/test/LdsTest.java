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
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import javax.ws.rs.core.Application;

import org.apache.jena.atlas.io.StringWriterI;
import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.impl.RDFLangString;
import org.apache.jena.datatypes.xsd.impl.XMLLiteralType;
import org.apache.jena.fuseki.embedded.FusekiServer;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.out.NodeFormatterTTL;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
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
	    ResourceConfig config=new ResourceConfig(PublicDataResource.class);        
		return config;
    }
	
	
	@Test
	public void testQueryProcessorModel(){
		// Makes sure that requested model is actually returned
		// by the query processor
		
		ArrayList<String> list=TestUtils.getResourcesList();
		for(String res : list){
			Model[] md=prepareAssertModel(res);
			assertTrue(md[0].isIsomorphicWith(md[1]));			
		}	
	}

    @Test
    public void testHtmlLitFormatter(){
        PrefixMap pm = PrefixMapFactory.create();
        pm.add("xsd", "http://www.w3.org/2001/XMLSchema#");
        pm.add("owl", "http://www.w3.org/2002/07/owl#");
        pm.add("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        
        NodeFormatterTTL nfttl = new NodeFormatterTTL(null, pm, null);
        Model m = ModelFactory.createDefaultModel();
        Literal l = m.createTypedLiteral("2009-10-22T18:31:49.12Z", XSDDatatype.XSDdateTime);
        Literal l1=m.createTypedLiteral(3.141592, new BaseDatatype("http://www.w3.org/2002/07/owl#real"));
        Literal l2=m.createTypedLiteral("Dharma is beautiful", XMLLiteralType.theXMLLiteralType);
        Literal l3=m.createTypedLiteral("true", XSDDatatype.XSDboolean);
        Literal l4=m.createLiteral("rgyud bla ma", "bo-x-ewts");
        Literal l5=m.createTypedLiteral("buddha is goodness", XSDDatatype.XSDstring);
        Literal l6=m.createTypedLiteral(-5, XSDDatatype.XSDinteger);        
        
        StringWriterI sw = new StringWriterI();        
        nfttl.formatLiteral(sw, l.asNode());
        sw.flush();        
        assertTrue(sw.toString().equals("\"2009-10-22T18:31:49.12Z\"^^xsd:dateTime"));
        
        sw = new StringWriterI();        
        nfttl.formatLiteral(sw, l1.asNode());
        sw.flush();        
        assertTrue(sw.toString().equals("\"3.141592\"^^owl:real"));        
           
        sw = new StringWriterI();        
        nfttl.formatLiteral(sw, l2.asNode());
        sw.flush();        
        assertTrue(sw.toString().equals("\"Dharma is beautiful\"^^rdf:XMLLiteral"));
        
        sw = new StringWriterI();
        nfttl.formatLiteral(sw, l3.asNode());
        sw.flush();
        assertTrue(sw.toString().equals("true"));
        
        sw = new StringWriterI();
        nfttl.formatLiteral(sw, l4.asNode());
        sw.flush();
        assertTrue(sw.toString().equals("\"rgyud bla ma\"@bo-x-ewts"));
        
        sw = new StringWriterI();
        nfttl.formatLiteral(sw, l5.asNode());
        sw.flush();        
        assertTrue(sw.toString().equals("\"buddha is goodness\""));
        
        sw = new StringWriterI();
        nfttl.formatLiteral(sw, l6.asNode());
        sw.flush();        
        assertTrue(sw.toString().equals("-5"));
        
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
                Model mq=QueryProcessor.getResourceGraph(res,fusekiUrl);
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
			
	private Model[] prepareAssertModel(String res){
		// Loads resource model from .ttl file
		// Adjusts prefix mapping
		// Get resource model from embedded fuseki via QueryProcessor
		// returns both models to be compared by testQueryProcessorModel()
		
		Model m=getModelFromFileName(TestUtils.TESTDIR+res+".ttl", sttl);		
		Model mq=QueryProcessor.getResourceGraph(res,fusekiUrl);			
		Model[] ret={m,mq};		
		return ret;
	}
	
	private Model[] prepareGetAssertModel(String res) throws IOException{
		// Loads resource model from .ttl file
		// Gets resource model from rest API
		// and returns them for comparison by testGetModel() 
		
		Model m=getModelFromFileName(TestUtils.TESTDIR+res+".ttl", sttl);		
		Model m_rest = ModelFactory.createDefaultModel();
		Response output = target("/resource/"+res).request().header("fusekiUrl", fusekiUrl).get();
		String resp=output.readEntity(String.class).trim();		
		ByteArrayInputStream is=new ByteArrayInputStream(resp.getBytes());
		m_rest.read(is,null,Lang.TURTLE.getName());
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
		
	public RDFWriter getSttlRDFWriter(Model m){
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
