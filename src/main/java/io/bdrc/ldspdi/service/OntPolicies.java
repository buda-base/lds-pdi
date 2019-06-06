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

import io.bdrc.ldspdi.ontology.service.core.OntPolicy;

public class OntPolicies {

    public static final String policiesUrl = ServiceConfig.getProperty("ontPoliciesUrl");
    public static HashMap<String, OntPolicy> map;
    private static Model mod;
    private static String defaultGraph;

    private static OntPolicy loadPolicy(Resource r, FileManager fm) {
        Statement st = r.getProperty(ResourceFactory.createProperty("http://jena.hpl.hp.com/schemas/2003/03/ont-manager#publicURI"));
        String baseUri = st.getObject().asResource().getURI();
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
            OntDocumentManager odm = new OntDocumentManager("https://raw.githubusercontent.com/buda-base/owl-schema/master/ont-policy.rdf");
            FileManager fm = odm.getFileManager();
            HttpURLConnection connection = (HttpURLConnection) new URL(policiesUrl).openConnection();
            InputStream stream = connection.getInputStream();
            mod = ModelFactory.createDefaultModel();
            mod.read(stream, RDFLanguages.strLangRDFXML);
            ResIterator it2 = mod.listResourcesWithProperty(RDF.type, ResourceFactory.createResource("http://jena.hpl.hp.com/schemas/2003/03/ont-manager#DocumentManagerPolicy"));
            while (it2.hasNext()) {
                Resource r = it2.next();
                defaultGraph = r.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/admin/defaultOntGraph")).getObject().asResource().getURI();
            }
            ResIterator it1 = mod.listResourcesWithProperty(RDF.type, ResourceFactory.createResource("http://jena.hpl.hp.com/schemas/2003/03/ont-manager#OntologySpec"));
            while (it1.hasNext()) {
                Resource r = it1.next();
                OntPolicy op = loadPolicy(r, fm);
                map.put(op.getBaseUri(), op);
            }
            stream.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
