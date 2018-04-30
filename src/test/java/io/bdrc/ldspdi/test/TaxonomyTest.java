package io.bdrc.ldspdi.test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.junit.BeforeClass;
import org.junit.Test;

import io.bdrc.ldspdi.utils.Node;
import io.bdrc.restapi.exceptions.RestException;
import io.bdrc.taxonomy.TaxModel;
import io.bdrc.taxonomy.Taxonomy;


public class TaxonomyTest {    
        
    static Node<String> TREE;
    
    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void init() throws RestException {
        TaxModel.init();                     
        Triple t=new Triple(org.apache.jena.graph.Node.ANY,NodeFactory.createURI(Taxonomy.SUBCLASSOF),NodeFactory.createURI("http://purl.bdrc.io/resource/O9TAXTBRC201605"));
        TREE=Taxonomy.buildTree(t, 1, Taxonomy.ROOT);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void getNode() throws RestException {
       
        Node<String> test=new Node("http://purl.bdrc.io/resource/O9TAXTBRC201605_0392");
        Node<String> ntest=Taxonomy.getNode("http://purl.bdrc.io/resource/O9TAXTBRC201605_0392");
        assertTrue(ntest!=null);
    }
    
    @Test
    public void getLeafToRootPath() {
        LinkedList<String> test=new LinkedList<>();
        test.addFirst("http://purl.bdrc.io/resource/O9TAXTBRC201605");
        test.addFirst("http://purl.bdrc.io/resource/O9TAXTBRC201605_1383");
        test.addFirst("http://purl.bdrc.io/resource/O9TAXTBRC201605_1491");
        test.addFirst("http://purl.bdrc.io/resource/T3");
        System.out.println(test);
        String data="http://purl.bdrc.io/resource/T3";
        LinkedList<String> ll=Taxonomy.getLeafToRootPath(data);        
        System.out.println(ll);
        assertTrue(ll.equals(test));
    }
    
    @Test
    public void getRootToLeafPath() {
        LinkedList<String> test=new LinkedList<>();
        test.addLast("http://purl.bdrc.io/resource/O9TAXTBRC201605");
        test.addLast("http://purl.bdrc.io/resource/O9TAXTBRC201605_1383");
        test.addLast("http://purl.bdrc.io/resource/O9TAXTBRC201605_1491");
        test.addLast("http://purl.bdrc.io/resource/T3");        
        String data="http://purl.bdrc.io/resource/T3";
        LinkedList<String> ll=Taxonomy.getRootToLeafPath(data);        
        assertTrue(ll.equals(test));
    }
    
    public <T> void printTaxTree(Node<String> node, String appender,int x) { 
        System.out.println(appender + node.getData());
        node.getChildren().forEach(each ->  printTaxTree(each, appender + appender,x+1));
    }
    
    /*@Test
    public void getPartialGraph() {
        Graph partial=Taxonomy.getPartialTreeTriples(Taxonomy.ROOT, getTestTopics());
        printGraph(partial);
    }
    
    @Test
    public void getPartialLDGraph() {
        Graph partial=Taxonomy.getPartialLDTreeTriples(Taxonomy.ROOT, getTestTopics(),getTestCountTopics());
        printGraph(partial);
        
    }
    
    public void printGraph(Graph partial) {
        ExtendedIterator<Triple> ext=partial.find();
        while(ext.hasNext()) {
            System.out.println(ext.next());
        }
    }
    
    public void writeLDGraph(Graph partial) {
        Model mod=ModelFactory.createModelForGraph(partial);
        mod.write(System.out,RDFLanguages.strLangJSONLD);
    }*/
    
    public HashMap<String,Integer> getTestCountTopics(){
        HashMap<String,Integer> test=new HashMap<>();
        test.put("http://purl.bdrc.io/resource/T3",3);
        test.put("http://purl.bdrc.io/resource/T1465",1);
        test.put("http://purl.bdrc.io/resource/T1",2);
        test.put("http://purl.bdrc.io/resource/T184",4);
        test.put("http://purl.bdrc.io/resource/T140",5);
        test.put("http://purl.bdrc.io/resource/T6",6);
        return test;
    }
    
    public HashSet<String> getTestTopics(){
        HashSet<String> test=new HashSet<>();
        test.add("http://purl.bdrc.io/resource/T3");
        test.add("http://purl.bdrc.io/resource/T1465");
        test.add("http://purl.bdrc.io/resource/T1");
        test.add("http://purl.bdrc.io/resource/T184");
        test.add("http://purl.bdrc.io/resource/T140");
        test.add("http://purl.bdrc.io/resource/T6");
        return test;
    }
    
    
    
    

}
