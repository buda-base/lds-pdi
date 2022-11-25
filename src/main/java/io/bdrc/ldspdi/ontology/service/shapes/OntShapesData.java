package io.bdrc.ldspdi.ontology.service.shapes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.ontology.service.core.OntData;
import io.bdrc.ldspdi.ontology.service.core.OntPolicy;
import io.bdrc.ldspdi.service.OntPolicies;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;

public class OntShapesData {

    public final static Logger log = LoggerFactory.getLogger(OntShapesData.class);
    public static HashMap<String, Model> modelsBase = new HashMap<>();
    private static Model fullMod;
    private String payload;
    private static String commitId = null;

    public OntShapesData(String payload, String commit) {
        super();
        this.payload = payload;
        commitId = commit;
    }

    public static String getCommitId() {
        return commitId;
    }

    public void update() {
        try {
            if (commitId == null && payload != null) {
                final JsonNode node = new ObjectMapper().readTree(payload);
                commitId = node.get("commits").elements().next().get("id").asText();
            }
            OntPolicies.init();
            log.info("reinitialize modelsBase");
            modelsBase = new HashMap<>();
            final OntModelSpec oms = new OntModelSpec(OntModelSpec.OWL_MEM);
            OntDocumentManager odm = new OntDocumentManager(System.getProperty("user.dir") + "/editor-templates/ont-policy.rdf");
            odm.setProcessImports(true);
            odm.setCacheModels(false);
            odm.getFileManager().resetCache();
            oms.setDocumentManager(odm);
            final Iterator<String> it = odm.listDocuments();
            fullMod = ModelFactory.createDefaultModel();
            while (it.hasNext()) {
                final String uri = it.next();
                log.info("OntManagerDoc : {}", uri);
                final OntModel om = odm.getOntology(uri, oms);
                OntShapesData.addOntModelByBase(parseBaseUri(uri), om);
                fullMod.add(om);
            }
            log.info("Done with OntShapesData initialization ! Uri set is {}", modelsBase.keySet());
        } catch (Exception ex) {
            log.error("Error updating OntShapesData Model", ex);
        }
    }
    
    public static final Property shPath = ResourceFactory.createProperty("http://www.w3.org/ns/shacl#path");
    public static final Property shInversePath = ResourceFactory.createProperty("http://www.w3.org/ns/shacl#inversePath");
    public static final Property shTargetObjectsOf = ResourceFactory.createProperty("http://www.w3.org/ns/shacl#targetObjectsOf");

    // RDF.first is kind of a dirty trick to account for the resources listed in sh:in that takes
    // a list as its object.
    public static final List<Property> propertiesForLabels = Arrays.asList(new Property[]{shPath, shInversePath, shTargetObjectsOf, RDF.first});
    
    public static void addLabelsAndDescriptions(final Model shapesM, final Model ontoModel) {
        final List<Resource> resources = new ArrayList<>();
    	for (final Property p : propertiesForLabels) {
    	    final NodeIterator ni = shapesM.listObjectsOfProperty(p);
    	    while (ni.hasNext()) {
    	        final RDFNode n = ni.next();
    	        if (n.isResource())
    	            resources.add(n.asResource());
    	    }
    	}
    	for (final Resource r : resources) {
    	    shapesM.add(ontoModel.listStatements(r, null, (RDFNode) null));
    	}
    }

    public static Model getFullModel() {
        return fullMod;
    }

    private static String parseBaseUri(String s) {
        if (s.endsWith("/") || s.endsWith("#")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    public static void addOntModelByBase(final String baseUri, final Model om) {
        if (OntData.ontAllMod == null) {
            log.error("OntData.ontAllMod is null for {}", baseUri);
            modelsBase.put(baseUri, om);
            return;
        }
        log.info("add model for {} ({}) with labels from ontology ({})", baseUri, om.size(), OntData.ontAllMod.size());
        addLabelsAndDescriptions(om, OntData.ontAllMod);
        modelsBase.put(baseUri, om);
    }

    public static Model getOntModelByBase(String baseUri) {
        return modelsBase.get(baseUri);
    }

    private static void updateFusekiDataset() throws RestException {
        String fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        HashMap<String, OntPolicy> policies = OntPolicies.getMapShapes();
        HashMap<String, Model> updates = new HashMap<>();
        for (String s : policies.keySet()) {
            OntPolicy op = policies.get(s);
            String graph = op.getGraph();
            Model m = updates.get(graph);
            if (m != null) {
                m.add(getOntModelByBase(op.getBaseUri()));
            } else {
                m = getOntModelByBase(op.getBaseUri());
            }
            if (graph != null) {
                updates.put(graph, m);
            }
        }
        // Individuals graphs
        for (String st : updates.keySet()) {
            QueryProcessor.updateOntology(updates.get(st), fusekiUrl.substring(0, fusekiUrl.lastIndexOf('/')) + "/data", st, st + " update");
        }
        // Global shapes model
        QueryProcessor.updateOntology(getFullModel(), fusekiUrl.substring(0, fusekiUrl.lastIndexOf('/')) + "/data", OntPolicies.defaultShapesGraph,
                " update");
    }

    public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException, RestException {
        ServiceConfig.init();
        // OntData.init();
        // OntShapesData.init();

        // fullMod.write(System.out, "TURTLE");
        // updateFusekiDataset();
    }

}
