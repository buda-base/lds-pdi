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
import io.bdrc.ldspdi.service.GitService;
import io.bdrc.ldspdi.service.OntPolicies;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.Helpers;

public class OntShapesData {

    public final static Logger log = LoggerFactory.getLogger(OntShapesData.class);
    public static HashMap<String, Model> modelsBase = new HashMap<>();
    private static Model fullMod;
    private String payload;
    private String commitId;

    public OntShapesData(String payload, String commitId) {
        super();
        this.payload = payload;
        this.commitId = commitId;
    }

    public void update() {
        try {
            if (commitId == null) {
                JsonNode node = new ObjectMapper().readTree(payload);
                commitId = node.get("commits").elements().next().get("id").asText();
            }
            OntPolicies.init();
            modelsBase = new HashMap<>();
            OntModelSpec oms = new OntModelSpec(OntModelSpec.OWL_MEM);
            OntDocumentManager odm = new OntDocumentManager(System.getProperty("user.dir") + "/editor-templates/ont-policy.rdf");
            odm.setProcessImports(true);
            odm.setCacheModels(true);
            Iterator<String> it = odm.listDocuments();
            fullMod = ModelFactory.createDefaultModel();
            while (it.hasNext()) {
                String uri = it.next();
                log.info("OntManagerDoc : {}", uri);
                OntModel om = odm.getOntology(uri, oms);
                String tmp = uri.substring(0, uri.length() - 1);
                File directory = new File(commitId);
                if (!directory.exists()) {
                    directory.mkdir();
                }
                Helpers.writeModelToFile(om, commitId + "/" + tmp.substring(tmp.lastIndexOf("/") + 1) + ".ttl");
                OntShapesData.addOntModelByBase(parseBaseUri(uri), om);
                fullMod.add(om);
            }
            log.info("Done with OntShapesData initialization ! Uri set is {}", modelsBase.keySet());
            updateFusekiDataset();
        } catch (Exception ex) {
            log.error("Error updating OntShapesData Model", ex);
        }
    }

    public static Model getFullModel() {
        return fullMod;
    }

    private static String parseBaseUri(String s) {
        if (s.endsWith("/") || s.endsWith("#")) {
            s = s.substring(0, s.length() - 1);
        }
        s = s.replace("purl.bdrc.io", ServiceConfig.SERVER_ROOT);
        return s;
    }

    public static void addOntModelByBase(String baseUri, Model om) {
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
        GitService gs = new GitService();
        gs.setMode(GitService.SHAPES);
        Thread t = new Thread(gs);
        t.start();
        // fullMod.write(System.out, "TURTLE");
        // updateFusekiDataset();
    }

    /*
     * @Override public void run() { try { init(); } catch (Exception e) { // TODO
     * Auto-generated catch block log.error(e.getMessage(), e); e.printStackTrace();
     * } }
     */

}
