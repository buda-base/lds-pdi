package io.bdrc.taxonomy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.iterator.ExtendedIterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.bdrc.ldspdi.utils.Node;
import io.bdrc.restapi.exceptions.RestException;


public class TaxonomyTree {
    
    public final static String TYPE="http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    public final static String SUBCLASSOF="http://purl.bdrc.io/ontology/core/taxSubclassOf";
    public final static String TAXONOMY="http://purl.bdrc.io/ontology/core/Taxonomy";
    public final static String PREFLABEL="http://www.w3.org/2004/02/skos/core#prefLabel"; 
    
    
    public static ArrayList<Node<String>> allNodes=new ArrayList<>();
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Node<String> ROOT=new Node("O9TAXTBRC201605");
    public static JsonNode JSON_ROOT;
    
      
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Node buildTree(Triple t,int x,Node root) {
        Model mod=TaxModel.getModel();
        Graph mGraph =mod.getGraph();        
        ExtendedIterator<Triple> ext=mGraph.find(t); 
        Triple tp=null;
        while(ext.hasNext()) {            
            tp=ext.next();
            Node nn=new Node(tp.getSubject().getURI());
            allNodes.add(nn);
            root.addChild(nn);
            Triple ttp=new Triple(org.apache.jena.graph.Node.ANY,NodeFactory.createURI(SUBCLASSOF),NodeFactory.createURI(tp.getSubject().getURI()));
            buildTree(ttp,x+1,nn);
        }
        return root;
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Node buildTaxTree(Triple t,int x,Node root) {
        Model mod=TaxModel.getModel();
        Graph mGraph =mod.getGraph();        
        ExtendedIterator<Triple> ext=mGraph.find(t); 
        Triple tp=null;
        while(ext.hasNext()) {            
            tp=ext.next();
            Node nn=new Node(tp.getSubject().getURI());
            allNodes.add(nn);
            root.addChild(nn);
            Triple ttp=new Triple(org.apache.jena.graph.Node.ANY,NodeFactory.createURI(SUBCLASSOF),NodeFactory.createURI(tp.getSubject().getURI()));
            buildTaxTree(ttp,x+1,nn);
        }
        return root;
    }
    
    public static JsonNode buildJsonTaxTree(Triple t,int x,JsonNode root) {        
        Model mod=TaxModel.getModel();
        Graph mGraph =mod.getGraph();        
        ExtendedIterator<Triple> ext=mGraph.find(t); 
        Triple tp=null;
        ArrayNode an1=new ArrayNode(JsonNodeFactory.instance);
        ObjectMapper mapper = new ObjectMapper();
        while(ext.hasNext()) {            
            tp=ext.next();
            ExtendedIterator<Triple> label=mGraph.find(new Triple(tp.getSubject(),NodeFactory.createURI(PREFLABEL),org.apache.jena.graph.Node.ANY));
            String prefLabel="";
            while(label.hasNext()){
                Triple lb=label.next();
                prefLabel=prefLabel+lb.getObject().getLiteral()+" , ";
            }
            JsonNode newNode = mapper.createObjectNode();
            ((ObjectNode) newNode).put(tp.getSubject().getURI(), prefLabel.substring(0,prefLabel.length()-3));
            an1.add(newNode);
            ((ObjectNode) root).set("children", an1);
            Triple ttp=new Triple(org.apache.jena.graph.Node.ANY,NodeFactory.createURI(SUBCLASSOF),NodeFactory.createURI(tp.getSubject().getURI()));
            buildJsonTaxTree(ttp,x+1,newNode);
        }
        return root;
    }
    
       
    @SuppressWarnings("rawtypes")
    public static Node getNode(String str) {
        for(Node n:allNodes) {            
            if(n.getData().equals(str)) {
                return n;
            }
        }
        return null;
    }
    
    public static Node<String> getTaxNode(String str) {
        for(Node<String> n:allNodes) {            
            if(n.getData().equals(str)) {
                return n;
            }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public static LinkedList<String> getLeafToRootPath(Node<String> root,String data) {
        LinkedList<String> linkedList = new LinkedList<String>();
        System.out.println("DATA >> "+data);
        Node<String> nodeLeaf=TaxonomyTree.getNode(data);        
        linkedList.addLast(nodeLeaf.getData());
        while(nodeLeaf!=null) {            
            linkedList.addLast(nodeLeaf.getParent().getData());
            nodeLeaf=TaxonomyTree.getNode(nodeLeaf.getParent().getData());
        }        
        return linkedList;
    }
    
    @SuppressWarnings("unchecked")
    public static LinkedList<String> getRootToLeafPath(Node<String> root,String data) {
        LinkedList<String> linkedList = new LinkedList<String>();
        Node<String> nodeLeaf=TaxonomyTree.getNode(data);        
        linkedList.addFirst(nodeLeaf.getData());
        while(nodeLeaf!=null) {            
            linkedList.addFirst(nodeLeaf.getParent().getData());
            nodeLeaf=TaxonomyTree.getNode(nodeLeaf.getParent().getData());
        }        
        return linkedList;
    }
    
    public static <T> void printTaxTree(Node<String> node, String appender,int x) {        
        
        System.out.println(appender + node.getData());
        node.getChildren().forEach(each ->  printTaxTree(each, appender + appender,x+1));
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void main(String[] args) throws RestException, InterruptedException {
        ArrayList<String> test=new ArrayList<>();
        test.add("http://purl.bdrc.io/resource/T3");
        test.add("http://purl.bdrc.io/resource/T1465");
        test.add("http://purl.bdrc.io/resource/T1");
        test.add("http://purl.bdrc.io/resource/T184");
        test.add("http://purl.bdrc.io/resource/T140");
        test.add("http://purl.bdrc.io/resource/T6");
        
        TaxModel.init();
        Node root=new Node("http://purl.bdrc.io/resource/O9TAXTBRC201605");        
        Triple t=new Triple(org.apache.jena.graph.Node.ANY,NodeFactory.createURI(SUBCLASSOF),NodeFactory.createURI("http://purl.bdrc.io/resource/O9TAXTBRC201605"));
        TaxonomyTree.buildTaxTree(t, 0, root);
        //TaxonomyTree.printTaxTree(root, " ", 0);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.createObjectNode();
        ((ObjectNode) node).put("O9TAXTBRC201605", "Root Taxonomy");        
        TaxonomyTree.buildJsonTaxTree(t, 0, node);
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(System.out , node);
        } catch (IOException e) {            
            e.printStackTrace();
        }
        //System.out.println(TaxonomyTree.getRootToLeafPath(root,"http://purl.bdrc.io/resource/T3"));
        
    }
    
    

}
