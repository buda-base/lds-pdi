package io.bdrc.taxonomy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.bdrc.formatters.JSONLDFormatter;
import io.bdrc.ldspdi.utils.Node;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

public class Taxonomy {
    
    public static ArrayList<Node<String>> allNodes=new ArrayList<>();
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Node<String> ROOT=new Node("http://purl.bdrc.io/resource/O9TAXTBRC201605");    
    public final static String HASSUBCLASS="http://purl.bdrc.io/ontology/core/taxHasSubClass";
    public final static String COUNT="http://purl.bdrc.io/ontology/tmp/count";
    public final static String PREFLABEL="http://www.w3.org/2004/02/skos/core#prefLabel";
    public final static String TYPE="http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    public final static String PERSON="http://purl.bdrc.io/ontology/core/Person";
    public final static String WORK="http://purl.bdrc.io/ontology/core/Work";
    public final static String PLACE="http://purl.bdrc.io/ontology/core/Place";
    public final static String LINEAGE="http://purl.bdrc.io/ontology/core/Lineage";
    public final static String WORK_GENRE="http://purl.bdrc.io/ontology/core/workGenre";
    public final static String WORK_IS_ABOUT="http://purl.bdrc.io/ontology/core/workIsAbout";
    public final static String PERSONGENDER="http://purl.bdrc.io/ontology/core/personGender";
    public final static String ACCESS="http://purl.bdrc.io/ontology/admin/access";
    public final static String LICENSE="http://purl.bdrc.io/ontology/admin/license";
    public final static String STATUS="http://purl.bdrc.io/ontology/admin/status";
    public final static String LANG_SCRIPT="http://purl.bdrc.io/ontology/core/workLangScript";
    public final static String TOPIC="http://purl.bdrc.io/ontology/core/Topic";
    public final static String ROLE="http://purl.bdrc.io/ontology/core/Role";
    
    static {
        Triple t=new Triple(NodeFactory.createURI("http://purl.bdrc.io/resource/O9TAXTBRC201605"),NodeFactory.createURI(Taxonomy.HASSUBCLASS),org.apache.jena.graph.Node.ANY);
        Taxonomy.buildTree(t, Taxonomy.ROOT);
    }
       
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Node buildTree(Triple t,Node root) {
        Model mod=TaxModel.getModel();
        Graph mGraph =mod.getGraph();        
        ExtendedIterator<Triple> ext=mGraph.find(t); 
        Triple tp=null;
        while(ext.hasNext()) {            
            tp=ext.next();            
            Node nn=new Node(tp.getObject().getURI());
            allNodes.add(nn);
            root.addChild(nn);
            Triple ttp=new Triple(NodeFactory.createURI(tp.getObject().getURI()),NodeFactory.createURI(HASSUBCLASS),org.apache.jena.graph.Node.ANY);
            buildTree(ttp,nn);
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
        
    public static LinkedList<String> getRootToLeafPath(String leaf) {
        LinkedList<String> tmp= getLeafToRootPath(leaf);
        Collections.reverse(tmp);
        return tmp;
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
    
    public static JsonNode buildFacetTree(HashSet<String> tops,HashMap<String,Integer> topics) throws RestException {
        JsonNode nn=null;
        if(tops.size()>0) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                Graph g=Taxonomy.getPartialLDTreeTriples(Taxonomy.ROOT, tops,topics);
                ByteArrayOutputStream baos=new ByteArrayOutputStream();        
                JSONLDFormatter.writeModelAsCompact(ModelFactory.createModelForGraph(g),baos);
                nn=mapper.readTree(baos.toString());
                baos.close();
            } catch (IOException ex) {
                throw new RestException(500,new LdsError(LdsError.JSON_ERR).setContext(" Taxonomy.buildFacetTree() was unable to write Taxonomy Tree : \""+ex.getMessage()+"\""));              
            }
        }
        return nn;
    }
    
    public static void processTopicStatement(Statement st,
                                      HashSet<String> tops,
                                      HashMap<String,HashSet<String>> Wtopics,
                                      HashMap<String,HashSet<String>> WorkBranch,
                                      HashMap<String,Integer> topics) {
        tops.add(st.getObject().asNode().getURI());
        HashSet<String> tmp=Wtopics.get(st.getObject().asNode().getURI());
        if (tmp==null) {
            tmp=new HashSet<>();
        }
        tmp.add(st.getSubject().asNode().getURI());
        Wtopics.put(st.getObject().asNode().getURI(), tmp); 
        LinkedList<String> nodes=Taxonomy.getRootToLeafPath(st.getObject().asNode().getURI());
        if(!nodes.isEmpty()) {
            nodes.removeFirst();
            nodes.removeLast();
        }
        for(String s:nodes) {
            HashSet<String> bt=WorkBranch.get(s);
            if (bt==null) {
                bt=new HashSet<>();
            }
            bt.add(st.getSubject().asNode().getURI());
            WorkBranch.put(s, bt);
            topics.put(s, bt.size());
        }
        topics.put(st.getObject().asNode().getURI(), Wtopics.get(st.getObject().asNode().getURI()).size());
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
