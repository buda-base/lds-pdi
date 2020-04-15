package io.bdrc.ldspdi.ontology.service.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.jena.ontology.AnnotationProperty;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.service.OntPolicies;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.libraries.Prefixes;

public class OntData implements Runnable {

    public static InfModel infMod;
    public static OntModel ontMod;
    public static OntModel ontAllMod;
    public static OntModel ontAuthMod;
    public static OWLPropsCharacteristics owlCharacteristics;
    public final static Logger log = LoggerFactory.getLogger(OntData.class);
    public static String JSONLD_CONTEXT;
    static String update;
    static Date lastUpdated;
    public static HashMap<String, OntModel> modelsBase = new HashMap<>();
    final static Resource RDFPL = ResourceFactory.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral");
    final static String fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
    static List<AnnotationProperty> adminAnnotProps;

    public static void init() {
        try {
            modelsBase = new HashMap<>();
            Model md = ModelFactory.createDefaultModel();
            OntModelSpec oms = new OntModelSpec(OntModelSpec.OWL_MEM);
            OntDocumentManager odm = new OntDocumentManager(ServiceConfig.getProperty("ontPoliciesUrl"));
            odm.setProcessImports(false);
            ontAllMod = ModelFactory.createOntologyModel(oms, md);
            Iterator<String> it = odm.listDocuments();
            while (it.hasNext()) {
                String uri = it.next();
                log.info("OntManagerDoc : {}", uri);
                OntModel om = odm.getOntology(uri, oms);
                ontAllMod.add(om);
                OntData.addOntModelByBase(parseBaseUri(uri), om);
            }
            updateFusekiDataset();
            readGithubJsonLDContext();
            adminAnnotProps = OntData.getAdminAnnotProps();
            log.info("Done with OntData initialization !");
        } catch (Exception ex) {
            log.error("Error updating OntModel", ex);
        }
    }

    public static void addOntModelByBase(String baseUri, OntModel om) {
        modelsBase.put(baseUri, om);
    }

    public static void setOntModelWithBase(String baseUri) throws RestException {
        ontMod = getOntModelByBase(baseUri);
    }

    public static void setOntModel(OntModel mod) throws RestException {
        ontMod = mod;
    }

    public static OntModel getOntModelByBase(String baseUri) throws RestException {
        return modelsBase.get(baseUri);
    }

    public static void readGithubJsonLDContext() throws MalformedURLException, IOException {
        URL url = new URL(ServiceConfig.getProperty("jsonContextURL"));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder st = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            st.append(line + "\n");
        }
        in.close();
        JSONLD_CONTEXT = st.toString();
        lastUpdated = Calendar.getInstance().getTime();
        update = Long.toString(System.currentTimeMillis());
        // update = new EntityTag(Long.toString(System.currentTimeMillis()));
    }

    /*
     * public static EntityTag getEntityTag() { return update; }
     */

    public static String getEntityTag() {
        return update;
    }

    public static Date getLastUpdated() {
        return lastUpdated;
    }

    private static void updateFusekiDataset() throws RestException {
        log.info("FUSEKI URL IS >>" + fusekiUrl);
        QueryProcessor.updateOntology(ontAllMod, fusekiUrl.substring(0, fusekiUrl.lastIndexOf('/')) + "/data",
                OntPolicies.getOntologyByBase(parseBaseUri("http://" + ServiceConfig.SERVER_ROOT + "/ontology/core/")).getGraph());
        QueryProcessor.updateOntology(getOntModelByBase(parseBaseUri("http://" + ServiceConfig.SERVER_ROOT + "/ontology/ext/auth")),
                fusekiUrl.substring(0, fusekiUrl.lastIndexOf('/')) + "/data",
                OntPolicies.getOntologyByBase(parseBaseUri("http://" + ServiceConfig.SERVER_ROOT + "/ontology/ext/auth/")).getGraph());
    }

    @Override
    public void run() {
        try {
            modelsBase = new HashMap<>();
            Model md = ModelFactory.createDefaultModel();
            OntModelSpec oms = new OntModelSpec(OntModelSpec.OWL_MEM);
            OntDocumentManager odm = new OntDocumentManager(ServiceConfig.getProperty("ontPoliciesUrl"));
            odm.setProcessImports(false);
            ontAllMod = ModelFactory.createOntologyModel(oms, md);
            Iterator<String> it = odm.listDocuments();
            while (it.hasNext()) {
                String uri = it.next();
                log.info("OntManagerDoc :" + uri);
                OntModel om = odm.getOntology(uri, oms);
                ontAllMod.add(om);
                OntData.addOntModelByBase(parseBaseUri(uri), om);
            }
            log.info("Global model size :" + ontAllMod.size());
            QueryProcessor.updateOntology(ontAllMod, fusekiUrl.substring(0, fusekiUrl.lastIndexOf('/')) + "/data",
                    OntPolicies.getOntologyByBase(parseBaseUri("http://" + ServiceConfig.SERVER_ROOT + "/ontology/core/")).getGraph());
            log.info("Auth model size :" + getOntModelByBase(parseBaseUri("http://" + ServiceConfig.SERVER_ROOT + "/ontology/ext/auth")).size());
            log.info("Auth policy {}", OntPolicies.getOntologyByBase(parseBaseUri("http://" + ServiceConfig.SERVER_ROOT + "/ontology/ext/auth/")));
            QueryProcessor.updateOntology(getOntModelByBase(parseBaseUri("http://" + ServiceConfig.SERVER_ROOT + "/ontology/ext/auth")),
                    fusekiUrl.substring(0, fusekiUrl.lastIndexOf('/')) + "/data",
                    OntPolicies.getOntologyByBase(parseBaseUri("http://" + ServiceConfig.SERVER_ROOT + "/ontology/ext/auth/")).getGraph());
            readGithubJsonLDContext();
            updateFusekiDataset();

        } catch (Exception ex) {
            log.error("Error updating OntModel", ex);
        }
    }

    public static OWLPropsCharacteristics getOwlCharacteristics(boolean global) throws IOException, RestException {
        if (owlCharacteristics == null) {
            if (global) {
                owlCharacteristics = new OWLPropsCharacteristics(ontAllMod);
            } else {
                owlCharacteristics = new OWLPropsCharacteristics(ontMod);
            }
        }
        return owlCharacteristics;
    }

    public static Model describeUri(final String uri) {
        final String query = "describe  <" + uri + ">";
        final QueryExecution qexec = QueryExecutionFactory.create(query, ontAllMod);
        return qexec.execDescribe();
    }

    public static ArrayList<OntResource> getDomainUsages(final String uri, boolean global) throws RestException {
        final String query = "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" + " select distinct ?s where {" + "    ?s rdfs:domain <" + uri
                + "> ." + "} order by ?p ?s";
        QueryExecution qexec = null;
        if (global) {
            qexec = QueryExecutionFactory.create(query, ontAllMod);
        } else {
            qexec = QueryExecutionFactory.create(query, ontMod);
        }
        final ResultSet res = qexec.execSelect();
        final ArrayList<OntResource> list = new ArrayList<>();
        while (res.hasNext()) {
            final QuerySolution qs = res.next();
            final RDFNode node = qs.get("?s");
            if (global) {
                list.add(ontAllMod.getOntResource(node.asResource().getURI()));
            } else {
                list.add(ontMod.getOntResource(node.asResource().getURI()));
            }
        }
        return list;
    }

    public static ArrayList<OntResource> getRangeUsages(final String uri, boolean global) throws RestException {
        final String query = "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" + " select distinct ?s ?p where {" + "    ?s rdfs:range <"
                + uri + "> ." + "} order by ?p ?s";
        QueryExecution qexec = null;
        if (global) {
            qexec = QueryExecutionFactory.create(query, ontAllMod);
        } else {
            qexec = QueryExecutionFactory.create(query, ontMod);
        }
        final ResultSet res = qexec.execSelect();
        final ArrayList<OntResource> list = new ArrayList<>();
        while (res.hasNext()) {
            final QuerySolution qs = res.next();
            final RDFNode node = qs.get("?s");
            if (global) {
                list.add(ontAllMod.getOntResource(node.asResource().getURI()));
            } else {
                list.add(ontMod.getOntResource(node.asResource().getURI()));
            }
        }
        return list;
    }

    public static ArrayList<OntResource> getSubProps(final String uri, boolean global) throws RestException {
        final String query = "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" + " select distinct ?s ?p where {"
                + "    ?s rdfs:subPropertyOf <" + uri + "> ." + "} order by ?p ?s";
        QueryExecution qexec = null;
        if (global) {
            qexec = QueryExecutionFactory.create(query, ontAllMod);
        } else {
            qexec = QueryExecutionFactory.create(query, ontMod);
        }
        final ResultSet res = qexec.execSelect();
        final ArrayList<OntResource> list = new ArrayList<>();
        while (res.hasNext()) {
            final QuerySolution qs = res.next();
            final RDFNode node = qs.get("?s");
            if (global) {
                list.add(ontAllMod.getOntResource(node.asResource().getURI()));
            } else {
                list.add(ontMod.getOntResource(node.asResource().getURI()));
            }
        }
        return list;
    }

    public static ArrayList<OntResource> getParentProps(final String uri, boolean global) throws RestException {
        final String query = "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" + " select distinct ?s where {" + "   <" + uri
                + "> rdfs:subPropertyOf ?s ." + "} order by ?s";
        QueryExecution qexec = null;
        if (global) {
            qexec = QueryExecutionFactory.create(query, ontAllMod);
        } else {
            qexec = QueryExecutionFactory.create(query, ontMod);
        }
        final ResultSet res = qexec.execSelect();
        final ArrayList<OntResource> list = new ArrayList<>();
        while (res.hasNext()) {
            final QuerySolution qs = res.next();
            final RDFNode node = qs.get("?s");
            if (global) {
                list.add(ontAllMod.getOntResource(node.asResource().getURI()));
            } else {
                list.add(ontMod.getOntResource(node.asResource().getURI()));
            }
        }
        return list;
    }

    public static ArrayList<OntResource> getSubClassesOf(final String uri, boolean global) throws RestException {
        final String query = "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" + " select distinct ?s where {" + "   ?s rdfs:subClassOf <"
                + uri + "> ." + "} order by ?s";
        QueryExecution qexec = null;
        if (global) {
            qexec = QueryExecutionFactory.create(query, ontAllMod);
        } else {
            qexec = QueryExecutionFactory.create(query, ontMod);
        }
        final ResultSet res = qexec.execSelect();
        final ArrayList<OntResource> list = new ArrayList<>();
        while (res.hasNext()) {
            final QuerySolution qs = res.next();
            final RDFNode node = qs.get("?s");
            if (global) {
                list.add(ontAllMod.getOntResource(node.asResource().getURI()));
            } else {
                list.add(ontMod.getOntResource(node.asResource().getURI()));
            }
        }
        return list;
    }

    public static HashMap<String, ArrayList<OntResource>> getAllSubProps(final String uri, boolean global) throws RestException {
        final ArrayList<OntResource> props = OntData.getDomainUsages(uri, global);
        final HashMap<String, ArrayList<OntResource>> map = new HashMap<>();
        for (final OntResource rs : props) {
            final ArrayList<OntResource> l = OntData.getSubProps(rs.getURI(), global);
            if (l.size() > 1) {
                map.put(rs.getURI(), l);
            }
        }
        return map;
    }

    public static boolean isClass(final String uri, boolean global) {
        OntModel md = null;
        if (global) {
            md = ontAllMod;
        } else {
            md = ontMod;
        }
        if (md.getOntResource(uri) != null) {
            return md.getOntResource(uri).isClass();
        } else {
            return false;
        }
    }

    public static int getNumPrefixes(boolean global) {
        if (global) {
            return ontAllMod.numPrefixes();
        } else {
            return ontMod.numPrefixes();
        }

    }

    public static Map<String, String> getPrefixMap(boolean global) {
        if (global) {
            return ontAllMod.getNsPrefixMap();
        } else {
            return ontMod.getNsPrefixMap();
        }
    }

    public static int getNumRootClasses() {
        return getSimpleRootClasses().size();
    }

    /**
     * Returns a list of simple root OntClass(es). Simple means not defined as a
     * Union or Restriction and so on. The purpose is to provide the roots of a
     * traversal of classes defined in the ontology.
     *
     * @return list of simple root OntClass(es)
     */
    public static List<OntClass> getSimpleRootClasses() {
        // Iterator<OntClass> it = ontMod.listHierarchyRootClasses();
        Iterator<OntClass> it = ontMod.listClasses();
        final List<OntClass> rez = new ArrayList<>();
        while (it.hasNext()) {
            try {
                OntClass oc = it.next();
                if (oc.getURI() != null && oc.isHierarchyRoot()) {
                    rez.add(oc);
                }
            } catch (Exception ex) {

            }
        }
        Collections.sort(rez, OntData.ontClassComparator);
        return rez;
    }

    public static List<OntClassModel> getOntRootClasses() {
        final List<OntClass> roots = getSimpleRootClasses();
        final List<OntClassModel> models = new ArrayList<>();
        for (final OntClass root : roots) {
            if (!root.isAnon()) {
                models.add(new OntClassModel(root));
            }
        }
        Collections.sort(models, OntData.ontClassModelComparator);
        return models;
    }

    public static ArrayList<OntClass> getAllClasses() {
        final ExtendedIterator<OntClass> it = ontMod.listClasses();
        final ArrayList<OntClass> classes = new ArrayList<>();
        while (it.hasNext()) {
            final OntClass ocl = it.next();
            if (ocl != null && !ocl.isAnon()) {
                classes.add(ocl);
            }
        }
        Collections.sort(classes, OntData.ontClassComparator);
        return classes;
    }

    public static ArrayList<OntProperty> getAllProps() {
        final ExtendedIterator<OntProperty> it = ontMod.listAllOntProperties();
        final ArrayList<OntProperty> list = new ArrayList<>();
        while (it.hasNext()) {
            final OntProperty pr = it.next();
            if (pr != null && pr.isProperty()) {
                list.add(pr);
            }
        }
        Collections.sort(list, OntData.propComparator);
        return list;
    }

    public static List<Individual> getAllIndividuals() {
        final List<Individual> indv = ontMod.listIndividuals().toList();
        Collections.sort(indv, individualComparator);
        return indv;
    }

    public final static Comparator<OntClass> ontClassComparator = new Comparator<OntClass>() {
        @Override
        public int compare(OntClass class1, OntClass class2) {
            if (Prefixes.getPrefix(class1.getNameSpace()).equals(Prefixes.getPrefix(class2.getNameSpace()))) {
                return class1.getLocalName().compareTo(class2.getLocalName());
            }
            return Prefixes.getPrefix(class1.getNameSpace()).compareTo(Prefixes.getPrefix(class2.getNameSpace()));
        }

    };

    public final static Comparator<OntClassModel> ontClassModelComparator = new Comparator<OntClassModel>() {
        @Override
        public int compare(OntClassModel class1, OntClassModel class2) {
            return ontClassComparator.compare(class1.clazz, class2.clazz);
        }

    };

    public final static Comparator<Individual> individualComparator = new Comparator<Individual>() {
        @Override
        public int compare(Individual class1, Individual class2) {
            return class1.getLocalName().compareTo(class2.getLocalName());
        }

    };

    public final static Comparator<OntProperty> propComparator = new Comparator<OntProperty>() {
        @Override
        public int compare(OntProperty prop1, OntProperty prop2) {
            if (Prefixes.getPrefix(prop1.getNameSpace()).equals(Prefixes.getPrefix(prop2.getNameSpace()))) {
                return prop1.getLocalName().compareTo(prop2.getLocalName());
            }
            return Prefixes.getPrefix(prop1.getNameSpace()).compareTo(Prefixes.getPrefix(prop2.getNameSpace()));
        }

    };

    public static List<AnnotationProperty> getAdminAnnotProps() throws RestException {
        OntModel om = OntData.getOntModelByBase("http://" + ServiceConfig.SERVER_ROOT + "/ontology/admin");
        return om.listAnnotationProperties().toList();
    }

    public static boolean isAdminAnnotProp(Property op) {
        return OntData.adminAnnotProps.contains(op);
    }

    public static void rdf10tordf11(OntModel o) {
        final ExtendedIterator<DatatypeProperty> it = o.listDatatypeProperties();
        while (it.hasNext()) {
            final DatatypeProperty p = it.next();
            if (p.hasRange(RDFPL)) {
                p.removeRange(RDFPL);
                p.addRange(RDF.langString);
            }
        }
        final ExtendedIterator<Restriction> it2 = o.listRestrictions();
        while (it2.hasNext()) {
            final Restriction r = it2.next();
            final Statement s = r.getProperty(OWL2.onDataRange); // is that code obvious? no
            if (s != null && s.getObject().asResource().equals(RDFPL)) {
                s.changeObject(RDF.langString);

            }
        }
    }

    private static String parseBaseUri(String s) {
        if (s.endsWith("/") || s.endsWith("#")) {
            s = s.substring(0, s.length() - 1);
        }
        s = s.replace("purl.bdrc.io", ServiceConfig.SERVER_ROOT);
        return s;
    }

    public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException, RestException {
        // ServiceConfig.initForTests("http://buda1.bdrc.io:13180/fuseki/bdrcrw/query");
        // OntData.init();
        Model m = ReasonerRegistry.theRegistry().getAllDescriptions();
        m.write(System.out, "TURTLE");

    }

}
