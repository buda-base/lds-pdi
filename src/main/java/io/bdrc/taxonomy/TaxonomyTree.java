package io.bdrc.taxonomy;

import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.iterator.ExtendedIterator;

import io.bdrc.ldspdi.utils.Node;


public class TaxonomyTree {
    
    public final static String TYPE="http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    public final static String SUBCLASSOF="http://purl.bdrc.io/ontology/core/taxSubclassOf";
    public final static String TAXONOMY="http://purl.bdrc.io/ontology/core/Taxonomy";
    public static ArrayList<Node<String>> allNodes=new ArrayList<>(); 
    
      
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Node buildTree(Triple t,int x,Node root) {
        Model mod=TaxModel.getModel();
        Graph mGraph =mod.getGraph();        
        ExtendedIterator<Triple> ext=mGraph.find(t); 
        Triple tp=null;
        while(ext.hasNext()) {            
            tp=ext.next();
            Node nn=new Node(tp.getSubject().getLocalName());
            allNodes.add(nn);
            root.addChild(nn);
            Triple ttp=new Triple(org.apache.jena.graph.Node.ANY,NodeFactory.createURI(SUBCLASSOF),NodeFactory.createURI(tp.getSubject().getURI()));
            buildTree(ttp,x+1,nn);
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
    
    @SuppressWarnings("unchecked")
    public static LinkedList<String> getLeafToRootPath(Node<String> root,String data) {
        LinkedList<String> linkedList = new LinkedList<String>();
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

}
