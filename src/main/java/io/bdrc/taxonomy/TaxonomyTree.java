package io.bdrc.taxonomy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.util.iterator.ExtendedIterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.bdrc.ldspdi.results.Field;
import io.bdrc.ldspdi.results.LiteralStringField;
import io.bdrc.ldspdi.utils.Node;
import io.bdrc.restapi.exceptions.RestException;


public class TaxonomyTree {
    
    public final static String TYPE="http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    public final static String SUBCLASSOF="http://purl.bdrc.io/ontology/core/taxSubclassOf";
    public final static String TAXONOMY="http://purl.bdrc.io/ontology/core/Taxonomy";
    public final static String PREFLABEL="http://www.w3.org/2004/02/skos/core#prefLabel";
    public final static String COUNT="http://purl.bdrc.io/ontology/tmp/count";
    
    
    public static ArrayList<Node<String>> allNodes=new ArrayList<>();
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Node<String> ROOT=new Node("http://purl.bdrc.io/resource/O9TAXTBRC201605");
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
            ArrayList<Field> f=new ArrayList<>();
            while(label.hasNext()){
                Triple lb=label.next();
                //prefLabel=prefLabel+lb.getObject().getLiteral()+" , ";
                f.add(new LiteralStringField("literal",lb.getObject().getLiteralLanguage(),lb.getObject().getLiteral().getValue().toString()));
            }
            JsonNode newNode = mapper.createObjectNode();
            ((ObjectNode) newNode).putPOJO(tp.getSubject().getURI(), new TaxonomyItem(0,f));
            an1.add(newNode);
            ((ObjectNode) root).set("children", an1);
            Triple ttp=new Triple(org.apache.jena.graph.Node.ANY,NodeFactory.createURI(SUBCLASSOF),NodeFactory.createURI(tp.getSubject().getURI()));
            buildJsonTaxTree(ttp,x+1,newNode);
        }
        return root;
    }
    
    public static JsonNode buildJsonTaxTree(Triple t,int x,JsonNode root, Graph mGraph) {       
        Model mod=TaxModel.getModel();
        Graph modGraph=mod.getGraph();
        ExtendedIterator<Triple> ext=mGraph.find(t); 
        Triple tp=null;
        ArrayNode an1=new ArrayNode(JsonNodeFactory.instance);
        ObjectMapper mapper = new ObjectMapper();
        while(ext.hasNext()) {            
            tp=ext.next();
            System.out.println("TP >> "+tp);
                ExtendedIterator<Triple> label=modGraph.find(new Triple(tp.getSubject(),NodeFactory.createURI(PREFLABEL),org.apache.jena.graph.Node.ANY));
                ArrayList<Field> f=new ArrayList<>();
                while(label.hasNext()){
                    Triple lb=label.next();
                    //prefLabel=prefLabel+lb.getObject().getLiteral()+" , ";
                    f.add(new LiteralStringField("literal",lb.getObject().getLiteralLanguage(),lb.getObject().getLiteral().getValue().toString()));
                }
                JsonNode newNode = mapper.createObjectNode();
                //((ObjectNode) newNode).put(tp.getSubject().getURI(), prefLabel.substring(0,prefLabel.length()-3));
                ((ObjectNode) newNode).putPOJO(tp.getSubject().getURI(), new TaxonomyItem(0,f));
                an1.add(newNode);
                ((ObjectNode) root).set("children", an1);
                Triple ttp=new Triple(org.apache.jena.graph.Node.ANY,NodeFactory.createURI(SUBCLASSOF),NodeFactory.createURI(tp.getSubject().getURI()));
                buildJsonTaxTree(ttp,x+1,newNode,mGraph);
            
        }
        return root;
    }
    
    public static Graph getPartialTaxGraph(Triple t,Graph mGraph) {       
        Model mod=TaxModel.getModel();
        Graph modGraph=mod.getGraph();        
        ExtendedIterator<Triple> ext=mGraph.find(t); 
        Triple tp=null;
        ArrayNode an1=new ArrayNode(JsonNodeFactory.instance);
        ObjectMapper mapper = new ObjectMapper();
        while(ext.hasNext()) {            
            tp=ext.next();
            mGraph.add(new Triple(tp.getSubject(),NodeFactory.createURI(COUNT),NodeFactory.createLiteralByValue(0,XSDDatatype.XSDinteger)));
            System.out.println("TP >> "+tp);
                ExtendedIterator<Triple> label=modGraph.find(new Triple(tp.getSubject(),NodeFactory.createURI(PREFLABEL),org.apache.jena.graph.Node.ANY));
                ArrayList<Field> f=new ArrayList<>();
                while(label.hasNext()){
                    Triple lb=label.next();
                    //prefLabel=prefLabel+lb.getObject().getLiteral()+" , ";
                    mGraph.add(lb);
                }
                Triple ttp=new Triple(org.apache.jena.graph.Node.ANY,NodeFactory.createURI(SUBCLASSOF),NodeFactory.createURI(tp.getSubject().getURI()));
                getPartialTaxGraph(ttp,mGraph);
            
        }
        return mGraph;
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
    
    public static Graph getPartialTreeTriples(Node<String> root,ArrayList<String> leafTopics){
        Model mod=ModelFactory.createDefaultModel();
        Graph partialTree=mod.getGraph();
        String previous=ROOT.getData();        
        for(String uri:leafTopics) {
            LinkedList<String> ll=getRootToLeafPath(root,uri);
            //System.out.println("LINKED LIST >>"+ll);
            for(String node:ll) {
                if(!node.equals(previous) && !node.equals(ROOT.getData())) {
                    Triple t=new Triple(NodeFactory.createURI(node),NodeFactory.createURI(SUBCLASSOF),NodeFactory.createURI(previous));
                    //System.out.println("LINKED LIST TRIPLE >>"+t);
                    partialTree.add(t);
                }
                previous=node;
            }
        }
        return partialTree;
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void main(String[] args) throws RestException, InterruptedException {
        ArrayList<String> test=new ArrayList<>();
        test.add("http://purl.bdrc.io/resource/T3");
        test.add("http://purl.bdrc.io/resource/T1465");
        /*test.add("http://purl.bdrc.io/resource/T1");
        test.add("http://purl.bdrc.io/resource/T184");
        test.add("http://purl.bdrc.io/resource/T140");
        test.add("http://purl.bdrc.io/resource/T6");*/
        
        TaxModel.init();                     
        Triple t=new Triple(org.apache.jena.graph.Node.ANY,NodeFactory.createURI(SUBCLASSOF),NodeFactory.createURI("http://purl.bdrc.io/resource/O9TAXTBRC201605"));
        TaxonomyTree.buildTree(t, 0, ROOT);
        Graph partial=TaxonomyTree.getPartialTreeTriples(ROOT, test);
        /*Model m=ModelFactory.createModelForGraph(partial);
        m.write(System.out,RDFLanguages.strLangJSONLD);*/
        ExtendedIterator<Triple> ext=partial.find();
        while(ext.hasNext()) {
            System.out.println(ext.next());
        }
        //TaxonomyTree.printTaxTree(root, " ", 0);
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.createObjectNode();
        ((ObjectNode) node).putPOJO("http://purl.bdrc.io/resource/O9TAXTBRC201605", new TaxonomyItem(1,new LiteralStringField("literal","en","Root Taxonomy")));  
        JsonNode node1 = mapper.createObjectNode();
        ((ObjectNode) node1).putPOJO("http://purl.bdrc.io/resource/O9TAXTBRC201605", new TaxonomyItem(1,new LiteralStringField("literal","en","Root Taxonomy")));    
        //TaxonomyTree.buildJsonTaxTree(t, 0, node);
        //TaxonomyTree.buildJsonTaxTree(t,0,node1,partial);
        Graph ldGraph=getPartialTaxGraph(t,partial);
        Model m=ModelFactory.createModelForGraph(ldGraph);
        m.write(System.out,RDFLanguages.strLangJSONLD);
        /*try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(System.out, node1);
        } catch (IOException e) {            
            e.printStackTrace();
        }*/
        //System.out.println(TaxonomyTree.getRootToLeafPath(ROOT,"http://purl.bdrc.io/resource/T3"));
        ExtendedIterator<Triple> ext1=ldGraph.find();
        while(ext1.hasNext()) {
        System.out.println(ext1.next());
        }
        
    }
    
    

}
