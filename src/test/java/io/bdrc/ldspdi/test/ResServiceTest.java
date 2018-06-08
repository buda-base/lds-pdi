package io.bdrc.ldspdi.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.apache.jena.atlas.io.StringWriterI;
import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.impl.XMLLiteralType;
import org.apache.jena.fuseki.embedded.FusekiServer;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.out.NodeFormatterTTL;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.apache.jena.riot.system.StreamRDFLib;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.rest.resources.PublicDataResource;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.ldspdi.utils.MediaTypeUtils;
import io.bdrc.restapi.exceptions.RestException;
import io.bdrc.restapi.exceptions.RestExceptionMapper;

public class ResServiceTest extends JerseyTest{
    
    private static FusekiServer server ;
    private static Dataset srvds = DatasetFactory.createTxnMem();
    private static Model model = ModelFactory.createDefaultModel(); 
    public static String fusekiUrl;
    public final static Logger log=LoggerFactory.getLogger(ResServiceTest.class.getName());
    public final static String[] methods= {"GET", "POST"};
    
    @BeforeClass
    public static void init() {        
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
        server.join();
    }
    
    @Override
    protected Application configure() {
        return new ResourceConfig(PublicDataResource.class).register(RestExceptionMapper.class);
    }
    
    @Test
    public void normalResource() throws IOException {
        
        Set<Entry<String,String>> fmt=MediaTypeUtils.JENAFORMAT.entrySet();
        for(String method:methods) {
            for(Entry<String,String> ent:fmt) {            
                String mime=MediaTypeUtils.getMimeFromExtension(ent.getKey());
                Response res = target("/resource/P1AG29").request()                    
                        .accept(mime)
                        .header("fusekiUrl", fusekiUrl)
                        .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                        .method(method);                
                assertTrue(res.getStatus() == 200);
                assertTrue(res.getHeaderString("Vary").equals("Negotiate, Accept"));
                assertTrue(res.getHeaderString("TCN").equals("Choice"));
                assertTrue(checkAlternates("resource/P1AG29",res.getHeaderString("Alternates")));                       
                assertTrue(res.getHeaderString("Content-Type").equals(mime));
                assertTrue(res.getHeaderString("Content-Location").equals("resource/P1AG29."+MediaTypeUtils.getExtFormatFromMime(mime)));
                Model dist = getModelFromFileName(TestUtils.TESTDIR+"P1AG29.ttl",Lang.TURTLE);
                Model returned=ModelFactory.createDefaultModel();
                returned.read(res.readEntity(InputStream.class),"",ent.getValue());
                assertTrue(dist.isIsomorphicWith(returned));            
            }
        }
    }
    
    @Test
    public void html() {
        for(String method:methods) {
            Response res = target("/resource/P1AG29").request()
                    .accept("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("fusekiUrl", fusekiUrl)
                    .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                    .method(method);
            assertTrue(res.getStatus() == 303);
            assertTrue(res.getHeaderString("Vary").equals("Negotiate, Accept"));
            assertTrue(res.getHeaderString("TCN").equals("Choice"));
            assertTrue(checkAlternates("resource/P1AG29",res.getHeaderString("Alternates")));
            assertTrue(res.getHeaderString("Location").equals("http://library.bdrc.io/show/bdr:P1AG29"));
        }
    }
    
    @Test
    public void wrongAccept() {
        for(String method:methods) {
            Response res = target("/resource/test").request()
                    .accept("application/xhtml+xml")
                    .header("fusekiUrl", fusekiUrl)
                    .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                    .method(method);
            if(method.equals("GET")) {
                assertTrue(res.getStatus() == 406);
                assertTrue(res.getHeaderString("TCN").equals("List"));
            }
            if(method.equals("POST")) {
                assertTrue(res.getStatus() == 406);                
            }
        }
    }
    
    @Test
    public void GetResourceByExtension() {
        Set<String> map=MediaTypeUtils.getExtensionMimeMap().keySet();
        for(String ent:map) {                
            Response res = target("/resource/P1AG29."+ent).request()
                    .header("fusekiUrl", fusekiUrl)
                    .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                    .get();
            assertTrue(res.getStatus() == 200);
            assertTrue(res.getHeaderString("Content-Type").equals(MediaTypeUtils.getMimeFromExtension(ent)));
            assertTrue(res.getHeaderString("Vary").equals("Negotiate, Accept"));
            assertTrue(res.getHeaderString("TCN").equals("Choice"));
            assertTrue(checkAlternates("resource/P1AG29",res.getHeaderString("Alternates")));
        }
    }
    
    @Test
    public void WrongResource() {
        Response res = target("/resource/wrong.ttl").request()
                .header("fusekiUrl", fusekiUrl)
                .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                .get();
        assertTrue(res.getStatus() == 404);
    }
    
    @Test
    public void WrongExt() throws RestException {
        Response res = target("/resource/C68.wrong").request()
                .header("fusekiUrl", fusekiUrl)
                .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                .get();
        assertTrue(res.getStatus() == 300);
        assertTrue(res.readEntity(String.class).equals(Helpers.getMultiChoicesHtml("/resource/C68",true)));
    }
    
    @Test
    public void nores() {
        for(String method:methods) {
            Response res = target("/resource/nonexistant").request()
                    .accept("text/turtle")
                    .header("fusekiUrl", fusekiUrl)
                    .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                    .method(method);
            System.out.println(res.getHeaders());
            assertTrue(res.getStatus() == 404);
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
    
    static void loadData(){
        //Loads the test dataset/
        ArrayList<String> list=TestUtils.getResourcesList();
        for(String res : list){
            Model m=getModelFromFileName(TestUtils.TESTDIR+res+".ttl", Lang.TURTLE);
            model.add(m);
        }       
    }
    
    static void logResponse(Response res) {
        log.info(" Status >> "+res.getStatus());
        log.info(" Vary >> "+res.getHeaderString("Vary"));
        log.info(" TCN >> "+res.getHeaderString("TCN"));
        log.info(" Alternates >> "+res.getHeaderString("Alternates"));
        log.info(" Content-Type >> "+res.getHeaderString("Content-type"));
        log.info(" Content-Location >> "+res.getHeaderString("Content-Location"));
        log.info(" Entity >> "+res.getEntity());
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
            log.error("error reading "+fname);
            e.printStackTrace();
            return null;
        }       
        return m;
    }
    
    public boolean checkAlternates(String url,String alternates) {
        HashMap<String,String> map =MediaTypeUtils.getExtensionMimeMap();
        StringBuilder sb=new StringBuilder("");
        for(Entry<String,String> e :map.entrySet()) {
            sb.append("{\""+url+"."+e.getKey()+"\" 1.000 {type "+e.getValue()+"}},");               
        }
        String alt=sb.toString().substring(0, sb.toString().length()-1);
        List<String> alt1=Arrays.asList(alternates.split(","));        
        List<String> alt2=Arrays.asList(alt.split(","));        
        for(String test:alt2) {            
            if(!alt1.contains(test)) {
                log.error("Alternates check failed >>> "+test + alt1.contains(test));
                return false;
            }
        }
        return true;
    }

}
