package io.bdrc.taxonomy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.iterator.ExtendedIterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.bdrc.ldspdi.results.Field;
import io.bdrc.ldspdi.results.LiteralStringField;
import io.bdrc.ldspdi.utils.Node;

public class Taxonomy {
    
    public static ArrayList<Node<String>> allNodes=new ArrayList<>();
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Node<String> ROOT=new Node("http://purl.bdrc.io/resource/O9TAXTBRC201605");
    public final static String SUBCLASSOF="http://purl.bdrc.io/ontology/core/taxSubclassOf";
    public final static String HASSUBCLASS="http://purl.bdrc.io/ontology/core/taxHasSubclass";
    public final static String COUNT="http://purl.bdrc.io/ontology/tmp/count";
    public final static String PREFLABEL="http://www.w3.org/2004/02/skos/core#prefLabel";
    
    static {
        Triple t=new Triple(org.apache.jena.graph.Node.ANY,NodeFactory.createURI(Taxonomy.SUBCLASSOF),NodeFactory.createURI("http://purl.bdrc.io/resource/O9TAXTBRC201605"));
        Taxonomy.buildTree(t, 1, Taxonomy.ROOT);
    }
    
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
    
    @SuppressWarnings("unchecked")
    public static LinkedList<String> getLeafToRootPath(String leaf) {
        LinkedList<String> linkedList = new LinkedList<String>();        
        Node<String> nodeLeaf=Taxonomy.getNode(leaf); 
        if(nodeLeaf==null) {
            return linkedList;
        }
        linkedList.addLast(nodeLeaf.getData());
        while(nodeLeaf!=null) {            
            linkedList.addLast(nodeLeaf.getParent().getData());
            nodeLeaf=Taxonomy.getNode(nodeLeaf.getParent().getData());
        }        
        return linkedList;
    }
    
    public static JsonNode buildJsonTaxTree(Triple t, JsonNode root, Graph mGraph,HashMap<String,Integer> topics) {       
        Model mod=TaxModel.getModel();
        Graph modGraph=mod.getGraph();
        if(t==null) {
            t=new Triple(org.apache.jena.graph.Node.ANY,NodeFactory.createURI(SUBCLASSOF),NodeFactory.createURI("http://purl.bdrc.io/resource/O9TAXTBRC201605"));
        }
        ExtendedIterator<Triple> ext=mGraph.find(t); 
        Triple tp=null;
        ArrayNode an1=new ArrayNode(JsonNodeFactory.instance);
        ObjectMapper mapper = new ObjectMapper();
        while(ext.hasNext()) {            
            tp=ext.next();
            ExtendedIterator<Triple> label=modGraph.find(new Triple(tp.getSubject(),NodeFactory.createURI(PREFLABEL),org.apache.jena.graph.Node.ANY));
            ArrayList<Field> f=new ArrayList<>();
            while(label.hasNext()){
                Triple lb=label.next();                    
                f.add(new LiteralStringField("literal",lb.getObject().getLiteralLanguage(),lb.getObject().getLiteral().getValue().toString()));
            }
            JsonNode newNode = mapper.createObjectNode();
            //((ObjectNode) newNode).put(tp.getSubject().getURI(), prefLabel.substring(0,prefLabel.length()-3));
            ((ObjectNode) newNode).putPOJO(tp.getSubject().getURI(), new TaxonomyItem(topics.get(tp.getSubject().getURI()),f));
            an1.add(newNode);
            ((ObjectNode) root).set("children", an1);
            Triple ttp=new Triple(org.apache.jena.graph.Node.ANY,NodeFactory.createURI(SUBCLASSOF),NodeFactory.createURI(tp.getSubject().getURI()));
            buildJsonTaxTree(ttp,newNode,mGraph,topics); 
        }
        return root;
    }
    
        
    public static LinkedList<String> getRootToLeafPath(String leaf) {
        LinkedList<String> tmp= getLeafToRootPath(leaf);
        Collections.reverse(tmp);
        return tmp;
    }
    
    public static Graph getPartialTreeTriples(Node<String> root,HashSet<String> leafTopics){
        Model mod=ModelFactory.createDefaultModel();
        Graph partialTree=mod.getGraph();
        String previous=ROOT.getData();        
        for(String uri:leafTopics) {
            LinkedList<String> ll=getRootToLeafPath(uri);            
            for(String node:ll) {
                if(!node.equals(previous) && !node.equals(ROOT.getData())) {
                    Triple t=new Triple(NodeFactory.createURI(node),NodeFactory.createURI(SUBCLASSOF),NodeFactory.createURI(previous));
                    partialTree.add(t);
                }
                previous=node;
            }
        }
        return partialTree;
    }
    
    public static Graph getPartialLDTreeTriples(Node<String> root,HashSet<String> leafTopics,HashMap<String,Integer> topics){
        Model model=TaxModel.getModel();
        Graph modGraph=model.getGraph();
        Model mod=ModelFactory.createDefaultModel();
        Graph partialTree=mod.getGraph();
        String previous=ROOT.getData();        
        for(String uri:leafTopics) {
            LinkedList<String> ll=getRootToLeafPath(uri);            
            for(String node:ll) { 
                Integer count=topics.get(node);
                if(count==null) {
                    count=-1;
                }                
                if(!node.equals(previous) && !node.equals(ROOT.getData())) {
                    partialTree.add(new Triple(NodeFactory.createURI(node),
                            NodeFactory.createURI(COUNT),
                            NodeFactory.createLiteral(count.toString())));
                    partialTree.add(new Triple(NodeFactory.createURI(previous),NodeFactory.createURI(HASSUBCLASS),NodeFactory.createURI(node) ));
                }
                ExtendedIterator<Triple> label=modGraph.find(NodeFactory.createURI(node),NodeFactory.createURI(PREFLABEL),org.apache.jena.graph.Node.ANY);
                
                while(label.hasNext()){
                    partialTree.add(label.next());                    
                }
                previous=node;
            }
        }
        return partialTree;
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

}
