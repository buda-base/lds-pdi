package io.bdrc.taxonomy;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.results.library.WorkResults;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.utils.TaxNode;
import io.bdrc.libraries.Models;

public class Taxonomy {

    public static Map<String, TaxNode> allNodes = new HashMap<>();
    public static String ROOTURI = null;
    public static TaxNode ROOT = null;
    public final static String HASSUBCLASS = "http://purl.bdrc.io/ontology/core/taxHasSubClass";
    public final static Node hasSubClass = ResourceFactory.createProperty(HASSUBCLASS).asNode();
    public final static String COUNT = "http://purl.bdrc.io/ontology/tmp/count";
    public final static Node countNode = ResourceFactory.createProperty(COUNT).asNode();
    public final static String PERSON = "http://purl.bdrc.io/ontology/core/Person";
    public final static String TAXONOMY_R = "http://purl.bdrc.io/ontology/core/Taxonomy";
    public final static String ETEXT_R = "http://purl.bdrc.io/ontology/core/Etext";
    public final static String WORK = "http://purl.bdrc.io/ontology/core/Work";
    public final static String PUBLISHED_WORK = "http://purl.bdrc.io/ontology/core/PublishedWork";
    public final static String UNICODE_WORK = "http://purl.bdrc.io/ontology/core/UnicodeWork";
    public final static String ABSTRACT_WORK = "http://purl.bdrc.io/ontology/core/AbstractWork";
    public final static String UNSPEC_WORK = "http://purl.bdrc.io/ontology/core/UnspecifiedWorkClass";
    public final static String VIRTUAL_WORK = "http://purl.bdrc.io/ontology/core/VirtualWork";
    public final static String ETEXT = "http://purl.bdrc.io/ontology/core/Etext";
    public final static String ETEXT_CHUNK = "http://purl.bdrc.io/ontology/core/EtextChunk";
    public final static String PLACE = "http://purl.bdrc.io/ontology/core/Place";
    public final static String LINEAGE = "http://purl.bdrc.io/ontology/core/Lineage";
    public final static String WORK_GENRE = "http://purl.bdrc.io/ontology/core/workGenre";
    public final static String WORK_IS_ABOUT = "http://purl.bdrc.io/ontology/core/workIsAbout";
    public final static String WORK_MAIN_AUTHOR = "http://purl.bdrc.io/ontology/core/creatorMainAuthor";
    public final static String PERSONGENDER = "http://purl.bdrc.io/ontology/core/personGender";
    //public final static String ACCESS = "http://purl.bdrc.io/ontology/admin/access";
    public final static String INSTANCEACCESS = "http://purl.bdrc.io/ontology/tmp/instanceAccess";
    public final static String AUTHOR = "http://purl.bdrc.io/ontology/tmp/author";
    public final static String INSTANCETYPE = "http://purl.bdrc.io/ontology/tmp/instanceType";
    public final static String LICENSE = "http://purl.bdrc.io/ontology/admin/license";
    public final static String STATUS = "http://purl.bdrc.io/ontology/admin/status";
    //public final static String LANG_SCRIPT = "http://purl.bdrc.io/ontology/core/workLangScript";
    public final static String LANGUAGE = "http://purl.bdrc.io/ontology/core/workLangScript";
    public final static String TOPIC = "http://purl.bdrc.io/ontology/core/Topic";
    public final static String ROLE = "http://purl.bdrc.io/ontology/core/Role";

    public final static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        String sysProp = ServiceConfig.getProperty("taxonomyRoot");
        if (sysProp != null)
            init(ServiceConfig.getProperty("taxonomyRoot"));
    }
    
    public static void init(String rootUri) {
        ROOTURI = rootUri;
        //ROOT = new TaxNode(rootUri);
        Triple t = new Triple(NodeFactory.createURI(ROOTURI), hasSubClass, Node.ANY);
        Taxonomy.buildTree(t, ROOT);
    }

    public static TaxNode buildTree(Triple t, TaxNode root) {
        final Model mod = TaxModel.getModel();
        final Graph mGraph = mod.getGraph();
        ExtendedIterator<Triple> ext = mGraph.find(t);
        while (ext.hasNext()) {
            final Triple tp = ext.next();
            final String uri = tp.getObject().getURI();
            final TaxNode nn = new TaxNode(uri, mod);
            allNodes.put(uri, nn);
            root.addChild(nn);
            final Triple ttp = new Triple(tp.getObject(), hasSubClass, Node.ANY);
            buildTree(ttp, nn);
        }
        return root;
    }

    public static LinkedList<String> getLeafToRootPath(String leaf) {
        LinkedList<String> linkedList = new LinkedList<String>();
        TaxNode node = allNodes.get(leaf);
        if (node == null) {
            return linkedList;
        }
        linkedList.addLast(node.getUri());
        while (node != null) {
            node = node.getParent();
            if (node != null)
                linkedList.addLast(node.getUri());
        }
        return linkedList;
    }

    public static LinkedList<String> getRootToLeafPath(String leaf) {
        LinkedList<String> tmp = getLeafToRootPath(leaf);
        Collections.reverse(tmp);
        return tmp;
    }
    
    public static Map<String, Map<String, Object>> buildFacetTree(HashSet<String> leafTopics, Map<String, Integer> topics) throws RestException {
        if (leafTopics.size() == 0) {
            return null;
        }
        long start = System.nanoTime();
        WorkResults.log.error("WorkResults.getResultMap(), checkpoint1: {}", (System.nanoTime()-start)/1000);
        Map<String,Map<String, Object>> res = new HashMap<>();
        String previous = ROOTURI;
        for (String uri : leafTopics) {
            TaxNode leaf = allNodes.get(uri);
            LinkedList<TaxNode> ll = leaf.getPathFromRoot();
            for (TaxNode n : ll) {
                String nodeUri = n.getUri();
                Integer count = topics.get(nodeUri);
                if (count == null) {
                    count = -1;
                }
                final Map<String,Object> nodeMap = res.computeIfAbsent(nodeUri, x -> new HashMap<>());
                if (!nodeUri.equals(previous) && !nodeUri.equals(ROOTURI)) {
                    nodeMap.put("count", count);
                    final Map<String,Object> previousNodeMap = res.computeIfAbsent(previous, x -> new HashMap<>());
                    @SuppressWarnings("unchecked")
                    final Set<String> previousSubclasses = (Set<String>) previousNodeMap.computeIfAbsent("subclasses", x -> new HashSet<>());
                    previousSubclasses.add(nodeUri);
                }

                if (!nodeMap.containsKey("skos:prefLabel")) {
                    nodeMap.put("skos:prefLabel", n.getLabels());
                }
                previous = nodeUri;
            }
        }
        simplifySubnodes(res, ROOTURI);
        WorkResults.log.error("WorkResults.getResultMap(), checkpoint3: {}", (System.nanoTime()-start)/1000);
        return res;
    }
    
    public static String simplifySubnodes (final Map<String, Map<String, Object>> res, final String key) {
        // simplifies the subnodes and returns the URI that should be substituted for this one, or null if it's fine
        // see https://github.com/buda-base/lds-pdi/issues/220
        final Map<String, Object> node = res.get(key);
        @SuppressWarnings("unchecked")
        Set<String> subnodes = (Set<String>) node.get("subclasses");
        if (subnodes == null || subnodes.size() == 0)
            return key;
        if (subnodes.size() == 1) {
            final String currentSubclass = subnodes.iterator().next();
            final String newSubclass = simplifySubnodes(res, currentSubclass);
            res.remove(key);
            return newSubclass;
        }
        Set<String> newSubclasses = new HashSet<>();
        for (final String subclass : subnodes) {
            newSubclasses.add(simplifySubnodes(res, subclass));
        }
        node.put("subclasses", newSubclasses);
        return key;
        
    }

    public static final Property tmpStatus = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/tmp/status");
    public static final Resource released = ResourceFactory.createProperty(Models.BDA+"StatusReleased");
    
    public static void processTopicStatement(Statement st, HashSet<String> tops, Map<String, HashSet<String>> Wtopics, Map<String, HashSet<String>> WorkBranch, Map<String, Integer> topics) {
        // check if work is released first: https://github.com/buda-base/lds-pdi/issues/221
        final Resource wa = st.getSubject();
        if (!wa.hasProperty(tmpStatus, released))
            return;
        tops.add(st.getObject().asNode().getURI());
        HashSet<String> tmp = Wtopics.get(st.getObject().asNode().getURI());
        if (tmp == null) {
            tmp = new HashSet<>();
        }
        final Node obj = st.getObject().asNode();
        if (wa.isURIResource()) {
            tmp.add(wa.getURI());
        } else {
            tmp.add(wa.toString());
        }
        Wtopics.put(obj.getURI(), tmp);
        TaxNode n = Taxonomy.allNodes.get(obj.getURI());
        LinkedList<TaxNode> nodes = n.getPathFromRoot();
        boolean first = true;
        for (TaxNode s : nodes) {
            if (first) {
                first = false;
                continue;
            }
            String suri = s.getUri();
            HashSet<String> bt = WorkBranch.get(suri);
            if (bt == null) {
                bt = new HashSet<>();
            }

            if (wa.isURIResource()) {
                bt.add(wa.getURI());
            } else {
                bt.add(wa.toString());
            }
            WorkBranch.put(suri, bt);
            topics.put(suri, bt.size());
        }
        topics.put(wa.getURI(), Wtopics.get(obj.getURI()).size());
    }

}
