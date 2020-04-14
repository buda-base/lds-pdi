package io.bdrc.ldspdi.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.jena.ontology.OntDocumentManager;
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

import io.bdrc.ldspdi.ontology.service.core.OntPolicy;

public class OntPolicies {

    public static final String policiesUrl = ServiceConfig.getProperty("ontPoliciesUrl");
    public static final String shapesPoliciesUrl = ServiceConfig.getProperty("ontShapesPoliciesUrl");
    public static HashMap<String, OntPolicy> map;
    private static Model mod;
    private static String defaultGraph;
    public final static Logger log = LoggerFactory.getLogger(OntPolicies.class);

    private static OntPolicy loadPolicy(Resource r, FileManager fm) {
        Statement st = r.getProperty(ResourceFactory.createProperty("http://jena.hpl.hp.com/schemas/2003/03/ont-manager#publicURI"));
        String baseUri = computeUri(st.getObject().asResource().getURI());
        String graph = defaultGraph;
        st = r.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/admin/ontGraph"));
        if (st != null) {
            graph = st.getObject().asResource().getURI();
        }
        boolean visible = false;
        st = r.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/admin/ontVisible"));
        if (st != null) {
            visible = st.getObject().asLiteral().getBoolean();
        }
        OntPolicy op = new OntPolicy(baseUri, graph, fm.mapURI(baseUri), visible);
        return op;
    }

    public static void init() {
        try {
            map = new HashMap<>();
            OntDocumentManager odm = new OntDocumentManager(policiesUrl);
            FileManager fm = odm.getFileManager();
            HttpURLConnection connection = (HttpURLConnection) new URL(policiesUrl).openConnection();
            InputStream stream = connection.getInputStream();
            mod = ModelFactory.createDefaultModel();
            mod.read(stream, RDFLanguages.strLangRDFXML);
            connection = (HttpURLConnection) new URL(shapesPoliciesUrl).openConnection();
            InputStream shapesStream = connection.getInputStream();
            Model shapesMod = ModelFactory.createDefaultModel();
            shapesMod.read(shapesStream, RDFLanguages.strLangRDFXML);
            mod.add(shapesMod);
            ResIterator it2 = mod.listResourcesWithProperty(RDF.type,
                    ResourceFactory.createResource("http://jena.hpl.hp.com/schemas/2003/03/ont-manager#DocumentManagerPolicy"));
            while (it2.hasNext()) {
                Resource r = it2.next();
                defaultGraph = r.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/admin/defaultOntGraph")).getObject()
                        .asResource().getURI();
            }
            ResIterator it1 = mod.listResourcesWithProperty(RDF.type,
                    ResourceFactory.createResource("http://jena.hpl.hp.com/schemas/2003/03/ont-manager#OntologySpec"));
            while (it1.hasNext()) {
                Resource r = it1.next();
                OntPolicy op = loadPolicy(r, fm);
                map.put(op.getBaseUri(), op);
                log.info("loaded OntPolicy >> {}", op);
            }
            stream.close();
            shapesStream.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static String computeUri(String uri) {
        return uri.replace("purl.bdrc.io", ServiceConfig.getProperty("serverRoot"));
    }

    public static ArrayList<String> getValidBaseUri() {
        Set<String> keys = map.keySet();
        ArrayList<String> valid = new ArrayList<>();
        for (String s : keys) {
            OntPolicy p = map.get(s);
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
        return map.containsKey(s);
    }

    public static OntPolicy getOntologyByBase(String name) {
        return map.get(name);
    }

}
