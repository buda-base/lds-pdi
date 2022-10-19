package io.bdrc.ldspdi.ontology.service.shapes;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.ontology.service.core.OntPolicy;
import io.bdrc.ldspdi.service.OntPolicies;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.Helpers;

public class OntShapesData {

    public final static Logger log = LoggerFactory.getLogger(OntShapesData.class);
    public static HashMap<String, Model> modelsBase = new HashMap<>();
    private static Model fullMod;
    private String payload;
    private static String commitId;
    private static boolean writeDebugFiles = false;

    public OntShapesData(String payload, String commit) {
        super();
        this.payload = payload;
        this.commitId = commit;
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
                if (writeDebugFiles) {
	                final String tmp = uri.substring(0, uri.length() - 1);
	                File directory = new File("shapes/");
	                if (!directory.exists()) {
	                    directory.mkdir();
	                }
	                directory = new File("shapes/" + commitId);
	                if (!directory.exists()) {
	                    directory.mkdir();
	                }
	                String file = null;
	                try {
	                    file = "shapes/" + commitId + "/" + tmp.substring(tmp.lastIndexOf("/") + 1) + ".ttl";
	                    Helpers.writeModelToFile(om, file);
	                } catch (Exception ex) {
	                    // do absolutely nothing so the shapoes are loaded anyway - just log
	                    log.info("Could not write file {}", file);
	                }
                }
                OntShapesData.addOntModelByBase(parseBaseUri(uri), om);
                fullMod.add(om);
            }
            log.info("Done with OntShapesData initialization ! Uri set is {}", modelsBase.keySet());
            boolean readonly = "true".equals(ServiceConfig.getProperty("readOnly"));
            //if (!readonly)
            //    updateFusekiDataset();
        } catch (Exception ex) {
            log.error("Error updating OntShapesData Model", ex);
        }
    }
    
    public static void addLabelsAndDescriptions(Model shapesM, Model ontoModel) {
    	
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

    public static void addOntModelByBase(String baseUri, Model om) {
        log.info("add model for {} ({})", baseUri, om.size());
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
