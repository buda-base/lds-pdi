package io.bdrc.ldspdi.service;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.bdrc.ldspdi.ontology.service.core.OntPolicy;

public class OntPolicies {

    private static final String CORE_TYPE = "core";
    private static final String SHAPES_TYPE = "shapes";

    public static final String policiesUrl = System.getProperty("user.dir") + "/owl-schema/ont-policy.rdf";
    public static final String shapesPoliciesUrl = System.getProperty("user.dir") + "/editor-templates/ont-policy.rdf";
    public static HashMap<String, OntPolicy> mapAll;
    public static HashMap<String, OntPolicy> mapCore;
    public static HashMap<String, OntPolicy> mapShapes;
    private static Model mod;
    public static String defaultCoreGraph;
    public static String defaultShapesGraph;
    public final static Logger log = LoggerFactory.getLogger(OntPolicies.class);

    private static OntPolicy loadPolicy(Resource r, FileManager fm, String type) {
        Statement st = r.getProperty(ResourceFactory.createProperty("http://jena.hpl.hp.com/schemas/2003/03/ont-manager#publicURI"));
        String baseUri = st.getObject().asResource().getURI();
        baseUri = baseUri.replace("purl.bdrc.io", ServiceConfig.SERVER_ROOT);
        String graph = null;
        st = r.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/admin/ontGraph"));
        if (st != null) {
            graph = st.getObject().asResource().getURI();
        }
        boolean visible = false;
        st = r.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/admin/ontVisible"));
        if (st != null) {
            visible = st.getObject().asLiteral().getBoolean();
        }
        st = r.getProperty(ResourceFactory.createProperty("http://jena.hpl.hp.com/schemas/2003/03/ont-manager#altURL"));
        String file = "";
        if (st != null) {
            file = st.getObject().toString();
        }
        if (type.equals(CORE_TYPE)) {
            if (graph == null) {
                graph = defaultCoreGraph;
            }
        }
        OntPolicy op = new OntPolicy(baseUri, graph, fm.mapURI(baseUri), file, visible);
        return op;
    }

    public static void init() {
        mapAll = new HashMap<>();
        set(CORE_TYPE);
        // We don't use shapes in china
        if (!ServiceConfig.isInChina()) {
            set(SHAPES_TYPE);
        }
    }

    public static void set(final String type) {
        try {
            final HashMap<String, OntPolicy> map = new HashMap<>();
            final FileManager fm = FileManager.get().clone(); // the global FileManager
            FileReader stream = null;
            if (type.equals(CORE_TYPE)) {
                stream = new FileReader(policiesUrl);
            }
            if (type.equals(SHAPES_TYPE)) {
                stream = new FileReader(shapesPoliciesUrl);
            }
            mod = ModelFactory.createDefaultModel();
            mod.read(stream, RDFLanguages.strLangRDFXML);
            stream.close();
            // mod.write(System.out, "TURTLE");
            final ResIterator it2 = mod.listResourcesWithProperty(RDF.type,
                    ResourceFactory.createResource("http://jena.hpl.hp.com/schemas/2003/03/ont-manager#DocumentManagerPolicy"));
            String defGraph = null;
            while (it2.hasNext()) {
                final Resource r = it2.next();
                final Statement defGraphS = r.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/admin/defaultOntGraph"));
                if (defGraphS == null) {
                	log.error("can't find adm:defaultOntGraph for {}", r.getURI());
                	continue;
                }
                defGraph = defGraphS.getObject().asResource().getURI();
            }
            if (type.equals(CORE_TYPE)) {
                mapCore = map;
                defaultCoreGraph = defGraph;
            }
            if (type.equals(SHAPES_TYPE)) {
                mapShapes = map;
                defaultShapesGraph = defGraph;
            }
            final ResIterator it1 = mod.listResourcesWithProperty(RDF.type,
                    ResourceFactory.createResource("http://jena.hpl.hp.com/schemas/2003/03/ont-manager#OntologySpec"));
            while (it1.hasNext()) {
                final Resource r = it1.next();
                final OntPolicy op = loadPolicy(r, fm, type);
                map.put(op.getBaseUri(), op);
                mapAll.put(op.getBaseUri(), op);
                log.info("loaded OntPolicy for uri {} >> {} ", op.getBaseUri(), op);
            }
        } catch (IOException e) {
            log.error("error setting ontology {}", type, e);
        }
    }

    public static ArrayList<String> getValidBaseUri() {
        Set<String> keys = mapAll.keySet();
        ArrayList<String> valid = new ArrayList<>();
        for (String s : keys) {
            OntPolicy p = mapAll.get(s);
            if (p.isVisible()) {
                valid.add(p.getBaseUri());
            }
        }
        return valid;
    }

    public static boolean isBaseUri(String s) {
        if (s.endsWith("/") || s.endsWith("#")) {
            s = s.substring(0, s.length() - 1);
        }
        return mapAll.containsKey(s);
    }

    public static OntPolicy getOntologyByBase(String name) {
        OntPolicy op = mapAll.get(name);
        if (op == null) {
            op = mapAll.get(name.substring(0, name.length() - 1));
        }
        return op;
    }

    public static OntPolicy getShapeOntologyByBase(String name) {
        OntPolicy op = mapShapes.get(name);
        if (op == null) {
            op = mapShapes.get(name.substring(0, name.length() - 1));
        }
        return op;
    }

    public static HashMap<String, OntPolicy> getMapShapes() {
        return mapShapes;
    }

    public static void main(String... arg) throws JsonParseException, JsonMappingException, IOException {
        ServiceConfig.init();
        OntPolicies.init();
    }

}
