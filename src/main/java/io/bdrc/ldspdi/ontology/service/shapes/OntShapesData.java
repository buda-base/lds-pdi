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
import io.bdrc.ldspdi.service.ServiceConfig;

public class OntShapesData {

    public final static Logger log = LoggerFactory.getLogger(OntShapesData.class);
    public static HashMap<String, OntModel> modelsBase = new HashMap<>();
    public static OntModel ontAllMod;

    public static void init() {
        try {
            modelsBase = new HashMap<>();
            Model md = ModelFactory.createDefaultModel();
            OntModelSpec oms = new OntModelSpec(OntModelSpec.OWL_MEM);
            OntDocumentManager odm = new OntDocumentManager(ServiceConfig.getProperty("ontShapesPoliciesUrl"));
            odm.setProcessImports(false);
            ontAllMod = ModelFactory.createOntologyModel(oms, md);
            Iterator<String> it = odm.listDocuments();
            while (it.hasNext()) {
                String uri = it.next();
                log.info("OntManagerDoc : {}", uri);
                OntModel om = odm.getOntology(uri, oms);
                ontAllMod.add(om);
                OntShapesData.addOntModelByBase(parseBaseUri(uri), om);
            }
            log.info("Done with OntShapesData initialization ! Uri set is {}", modelsBase.keySet());
        } catch (Exception ex) {
            log.error("Error updating OntShapesData Model", ex);
        }
    }

    private static String parseBaseUri(String s) {
        if (s.endsWith("/") || s.endsWith("#")) {
            s = s.substring(0, s.length() - 1);
        }
        s = s.replace("purl.bdrc.io", ServiceConfig.getProperty("serverRoot"));
        log.info("parseBaseUri returned {}", s);
        return s;
    }

    public static OntModel getOntModelByBase(String baseUri) throws RestException {
        log.info("In getOntModelByBase, Picking up shapes model for {} and keySet {}", baseUri, modelsBase.keySet());
        return modelsBase.get(baseUri);
    }

    public static void addOntModelByBase(String baseUri, OntModel om) {
        modelsBase.put(baseUri, om);
    }

    public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException, RestException {
        ServiceConfig.init();
        OntShapesData.init();
        System.out.println(OntShapesData.modelsBase.keySet());
    }

}
