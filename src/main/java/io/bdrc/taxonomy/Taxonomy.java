package io.bdrc.taxonomy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.SKOS;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.results.library.WorkResults;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.utils.TaxNode;
import io.bdrc.libraries.Models;
import io.bdrc.libraries.formatters.JSONLDFormatter;

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
        ROOT = new TaxNode(rootUri);
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
            final TaxNode nn = new TaxNode(uri);
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
        final Graph modGraph = TaxModel.getModel().getGraph();
        String previous = ROOTURI;
        for (String uri : leafTopics) {
            LinkedList<String> ll = getRootToLeafPath(uri);
            for (String node : ll) {
                Integer count = topics.get(node);
                if (count == null) {
                    count = -1;
                }
                final Map<String,Object> nodeMap = res.computeIfAbsent(node, x -> new HashMap<>());
                if (!node.equals(previous) && !node.equals(ROOTURI)) {
                    nodeMap.put("count", count);
                    final Map<String,Object> previousNodeMap = res.computeIfAbsent(previous, x -> new HashMap<>());
                    @SuppressWarnings("unchecked")
                    final Set<String> previousSubclasses = (Set<String>) previousNodeMap.computeIfAbsent("subclasses", x -> new HashSet<>());
                    previousSubclasses.add(node);
                }

                // TODO: the labels should be added in the TaxNodes, this would save time at
                // each query
                if (!nodeMap.containsKey("skos:prefLabel")) {
                    final List<Map<String,String>> labels = new ArrayList<>();
                    nodeMap.put("skos:prefLabel", labels);
                 
                    final ExtendedIterator<Triple> labelIt = modGraph.find(NodeFactory.createURI(node), SKOS.prefLabel.asNode(), Node.ANY);
                    while (labelIt.hasNext()) {
                        LiteralLabel l = labelIt.next().getObject().getLiteral();
                        final Map<String,String> label = new HashMap<>();
                        labels.add(label);
                        label.put("@language", l.language());
                        label.put("@value", (String) l.getValue());
                    }
                }
                previous = node;
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
        LinkedList<String> nodes = Taxonomy.getRootToLeafPath(obj.getURI());
        if (!nodes.isEmpty()) {
            nodes.removeFirst();
            // nodes.removeLast();
        }
        for (String s : nodes) {
            HashSet<String> bt = WorkBranch.get(s);
            if (bt == null) {
                bt = new HashSet<>();
            }

            if (wa.isURIResource()) {
                bt.add(wa.getURI());
            } else {
                bt.add(wa.toString());
            }
            WorkBranch.put(s, bt);
            topics.put(s, bt.size());
        }
        topics.put(wa.getURI(), Wtopics.get(obj.getURI()).size());
    }

}
