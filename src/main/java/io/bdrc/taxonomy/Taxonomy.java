package io.bdrc.taxonomy;

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
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.results.library.WorkResults;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.LdsQuery;
import io.bdrc.ldspdi.sparql.LdsQueryService;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.TaxNode;

public class Taxonomy {

    public Map<String, TaxNode> allNodes = new HashMap<>();
    public String ROOTURI = null;
    public TaxNode ROOT = null;
    public Model model = null;
    
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
    public final static String TOPIC = "http://purl.bdrc.io/ontology/core/Topic";
    public final static String ROLE = "http://purl.bdrc.io/ontology/core/Role";

    public final static ObjectMapper mapper = new ObjectMapper();
    
    public final static Logger log = LoggerFactory.getLogger(Taxonomy.class);

    static {
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }
    
    public Taxonomy(final String rootUri) {
        ROOTURI = rootUri;
        try {
            final LdsQuery qfp = LdsQueryService.get(ServiceConfig.getProperty("taxtreeArqFile"), "library");
            final Map<String, String> map = new HashMap<>();
            map.put("R_RES", rootUri);
            final String query = qfp.getParametizedQuery(map, true);
            model = QueryProcessor.getGraph(query);
            ROOT = new TaxNode(rootUri, model);
            Triple t = new Triple(NodeFactory.createURI(ROOTURI), hasSubClass, Node.ANY);
            this.buildTree(t, ROOT);
        } catch (Exception e) {
            log.error("error when building taxonomy "+rootUri, e);
        }
    }
    
    public Taxonomy(final String rootUri, final Model model) {
        ROOTURI = rootUri;
        try {
            this.model = model;
            ROOT = new TaxNode(rootUri, model);
            Triple t = new Triple(NodeFactory.createURI(ROOTURI), hasSubClass, Node.ANY);
            this.buildTree(t, ROOT);
        } catch (Exception e) {
            log.error("error when building taxonomy "+rootUri, e);
        }
    }

    public TaxNode buildTree(Triple t, TaxNode root) {
        final Graph mGraph = model.getGraph();
        ExtendedIterator<Triple> ext = mGraph.find(t);
        while (ext.hasNext()) {
            final Triple tp = ext.next();
            final String uri = tp.getObject().getURI();
            final TaxNode nn = new TaxNode(uri, model);
            allNodes.put(uri, nn);
            root.addChild(nn);
            final Triple ttp = new Triple(tp.getObject(), hasSubClass, Node.ANY);
            buildTree(ttp, nn);
        }
        return root;
    }
    
    public Map<String, Map<String, Object>> buildFacetTree(HashSet<String> leafTopics, Map<String, Integer> topics) throws RestException {
        if (leafTopics.size() == 0) {
            return null;
        }
        long start = System.nanoTime();
        WorkResults.log.error("WorkResults.getResultMap(), checkpoint1: {}", (System.nanoTime()-start)/1000);
        Map<String,Map<String, Object>> res = new HashMap<>();
        String previous = ROOTURI;
        for (String uri : leafTopics) {
            TaxNode leaf = allNodes.get(uri);
            if (leaf == null) // shouldn't happen, just a safeguard
                continue;
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
    
    public boolean processTopicStatement(Statement st, HashSet<String> tops, Map<String, HashSet<String>> Wtopics, Map<String, HashSet<String>> WorkBranch, Map<String, Integer> topics, boolean putIfAbsent) {
        final Resource wa = st.getSubject();
        final Node obj = st.getObject().asNode();
        TaxNode n = allNodes.get(obj.getURI());
        if (n == null) {
            if (putIfAbsent) {
                tops.add(st.getObject().asNode().getURI());
                topics.put(wa.getURI(), Wtopics.get(obj.getURI()).size());
            }
            return false;
        }
        tops.add(st.getObject().asNode().getURI());
        HashSet<String> tmp = Wtopics.get(obj.getURI());
        if (tmp == null) {
            tmp = new HashSet<>();
        }        
        tmp.add(wa.getURI());
        Wtopics.put(obj.getURI(), tmp);
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
            bt.add(wa.getURI());
            WorkBranch.put(suri, bt);
            topics.put(suri, bt.size());
        }
        topics.put(wa.getURI(), Wtopics.get(obj.getURI()).size());
        return true;
    }

}
