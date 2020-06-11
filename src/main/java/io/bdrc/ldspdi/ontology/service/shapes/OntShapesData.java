package io.bdrc.ldspdi.ontology.service.shapes;

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

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.ontology.service.core.OntPolicy;
import io.bdrc.ldspdi.service.OntPolicies;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;

public class OntShapesData implements Runnable {

    public final static Logger log = LoggerFactory.getLogger(OntShapesData.class);
    public static HashMap<String, Model> modelsBase = new HashMap<>();
    private static Model fullMod;

    public static void init() {
        try {
            OntPolicies.init();
            modelsBase = new HashMap<>();
            fullMod = ModelFactory.createDefaultModel();
            OntModelSpec oms = new OntModelSpec(OntModelSpec.OWL_MEM);
            OntDocumentManager odm = new OntDocumentManager(ServiceConfig.getProperty("ontShapesPoliciesUrl"));
            odm.setProcessImports(true);
            Iterator<String> it = odm.listDocuments();
            while (it.hasNext()) {
                String uri = it.next();
                log.info("OntManagerDoc : {}", uri);
                OntModel om = odm.getOntology(uri, oms);
                OntShapesData.addOntModelByBase(parseBaseUri(uri), om);
                fullMod.add(om);
            }
            log.info("Done with OntShapesData initialization ! Uri set is {}", modelsBase.keySet());
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
        Model global = ModelFactory.createDefaultModel();
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
            global.add(m);
        }
        // Individuals graphs
        for (String st : updates.keySet()) {
            QueryProcessor.updateOntology(updates.get(st), fusekiUrl.substring(0, fusekiUrl.lastIndexOf('/')) + "/data", st, st + " update");
        }
        // Global shapes model
        QueryProcessor.updateOntology(global, fusekiUrl.substring(0, fusekiUrl.lastIndexOf('/')) + "/data", OntPolicies.defaultShapesGraph,
                " update");
    }

    public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException, RestException {
        ServiceConfig.init();
        // OntData.init();
        // OntShapesData.init();
        // fullMod.write(System.out, "TURTLE");
        // updateFusekiDataset();
    }

    @Override
    public void run() {
        init();
        try {
            updateFusekiDataset();
        } catch (RestException e) {
            // TODO Auto-generated catch block
            log.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }

}
