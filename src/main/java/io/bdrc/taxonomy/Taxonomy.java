package io.bdrc.taxonomy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.SKOS;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.bdrc.formatters.JSONLDFormatter;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.utils.TaxNode;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

public class Taxonomy {

    public static Map<String, TaxNode> allNodes = new HashMap<>();
    public static final String ROOTURI = ServiceConfig.getProperty("taxonomyRoot");
    public static TaxNode ROOT = new TaxNode(ROOTURI);
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
    public final static String ACCESS = "http://purl.bdrc.io/ontology/admin/access";
    public final static String LICENSE = "http://purl.bdrc.io/ontology/admin/license";
    public final static String STATUS = "http://purl.bdrc.io/ontology/admin/status";
    public final static String LANG_SCRIPT = "http://purl.bdrc.io/ontology/core/workLangScript";
    public final static String TOPIC = "http://purl.bdrc.io/ontology/core/Topic";
    public final static String ROLE = "http://purl.bdrc.io/ontology/core/Role";

    public final static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        Triple t = new Triple(NodeFactory.createURI(ROOTURI), hasSubClass, Node.ANY);
        Taxonomy.buildTree(t, Taxonomy.ROOT);
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

    public static Graph getPartialLDTreeTriples(TaxNode root, HashSet<String> leafTopics, HashMap<String, Integer> topics) {
        Model model = TaxModel.getModel();
        Graph modGraph = model.getGraph();
        Model mod = ModelFactory.createDefaultModel();
        Graph partialTree = mod.getGraph();
        String previous = ROOTURI;
        for (String uri : leafTopics) {
            LinkedList<String> ll = getRootToLeafPath(uri);
            for (String node : ll) {
                Integer count = topics.get(node);
                if (count == null) {
                    count = -1;
                }
                if (!node.equals(previous) && !node.equals(ROOTURI)) {
                    final Literal l = mod.createTypedLiteral(count, XSDDatatype.XSDinteger);
                    partialTree.add(new Triple(NodeFactory.createURI(node), countNode, l.asNode()));
                    partialTree.add(new Triple(NodeFactory.createURI(previous), hasSubClass, NodeFactory.createURI(node)));
                }
                // TODO: the labels should be added in the TaxNodes, this would save time at
                // each query
                final ExtendedIterator<Triple> label = modGraph.find(NodeFactory.createURI(node), SKOS.prefLabel.asNode(), Node.ANY);
                while (label.hasNext()) {
                    partialTree.add(label.next());
                }
                previous = node;
            }
        }
        return partialTree;
    }

    public static JsonNode buildFacetTree(HashSet<String> tops, HashMap<String, Integer> topics) throws RestException {
        JsonNode nn = null;
        if (tops.size() > 0) {
            try {
                Graph g = Taxonomy.getPartialLDTreeTriples(Taxonomy.ROOT, tops, topics);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                JSONLDFormatter.writeModelAsCompact(ModelFactory.createModelForGraph(g), baos);
                nn = mapper.readTree(baos.toString());
                baos.close();
            } catch (IOException ex) {
                throw new RestException(500, new LdsError(LdsError.JSON_ERR).setContext(" Taxonomy.buildFacetTree() was unable to write Taxonomy Tree : \"" + ex.getMessage() + "\"", ex));
            }
        }
        return nn;
    }

    public static void processTopicStatement(Statement st, HashSet<String> tops, HashMap<String, HashSet<String>> Wtopics, HashMap<String, HashSet<String>> WorkBranch, HashMap<String, Integer> topics) {
        tops.add(st.getObject().asNode().getURI());
        HashSet<String> tmp = Wtopics.get(st.getObject().asNode().getURI());
        if (tmp == null) {
            tmp = new HashSet<>();
        }
        final Resource sub = st.getSubject();
        final Node obj = st.getObject().asNode();
        if (sub.isURIResource()) {
            tmp.add(sub.getURI());
        } else {
            tmp.add(sub.toString());
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

            if (sub.isURIResource()) {
                bt.add(sub.getURI());
            } else {
                bt.add(sub.toString());
            }
            WorkBranch.put(s, bt);
            topics.put(s, bt.size());
        }
        topics.put(sub.getURI(), Wtopics.get(obj.getURI()).size());
    }

}
