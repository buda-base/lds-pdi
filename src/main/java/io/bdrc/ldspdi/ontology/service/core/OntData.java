package io.bdrc.ldspdi.ontology.service.core;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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

import javax.ws.rs.core.EntityTag;

import org.apache.commons.io.IOUtils;
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
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.RDFSRuleReasonerFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.ReasonerVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.results.ResultsCache;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.Prefixes;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.restapi.exceptions.RestException;

public class OntData implements Runnable {

    public static InfModel infMod;
    public static OntModel ontMod;
    public static OntModel ontAllMod;
    public static OntModel ontAuthMod;
    public static OWLPropsCharacteristics owlCharacteristics;
    public final static Logger log = LoggerFactory.getLogger(OntData.class.getName());
    public static String JSONLD_CONTEXT;
    static EntityTag update;
    static Date lastUpdated;
    static String ont;
    public static HashMap<String, OntModel> models = new HashMap<>();
    public static HashMap<String, OntModel> modelsBase = new HashMap<>();
    final static Resource RDFPL = ResourceFactory.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral");
    final static String fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);

    public static void init() {
        try {
            models = new HashMap<>();
            modelsBase = new HashMap<>();
            ArrayList<String> names = ServiceConfig.getConfig().getValidNames();
            Model md = ModelFactory.createDefaultModel();
            OntModelSpec oms = new OntModelSpec(OntModelSpec.OWL_MEM);
            OntDocumentManager odm = new OntDocumentManager();
            odm.setProcessImports(false);
            oms.setDocumentManager(odm);
            ontAllMod = ModelFactory.createOntologyModel(oms, md);
            for (String name : names) {
                String url = ServiceConfig.getConfig().getOntology(name).fileurl;
                log.info("URL >> " + url);
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                InputStream stream = connection.getInputStream();
                Model tmp = ModelFactory.createDefaultModel();
                OntModel om = ModelFactory.createOntologyModel(oms, tmp);
                byte[] byteArr = IOUtils.toByteArray(stream);
                om.read(new ByteArrayInputStream(byteArr), ServiceConfig.getConfig().getOntology(name).getBaseuri(), "TURTLE");
                // caching ttl file as byte array
                ResultsCache.addToCache(byteArr, url.hashCode());
                ontAllMod.add(om);
                OntData.addOntModelByName(name, om);
                OntData.addOntModelByBase(ServiceConfig.getConfig().getOntology(name).getBaseuri(), om);
            }
            readGithubJsonLDContext();
        } catch (Exception ex) {
            log.error("Error updating OntModel", ex);
        }
    }

    public static void addOntModelByName(String name, OntModel om) {
        models.put(name, om);
    }

    public static void addOntModelByBase(String baseUri, OntModel om) {
        modelsBase.put(baseUri, om);
    }

    public static void setOntModelWithName(String name) throws RestException {
        ontMod = getOntModelByName(name);
    }

    public static void setOntModelWithBase(String baseUri) throws RestException {
        ontMod = getOntModelByBase(baseUri);
    }

    public static OntModel getOntModelByBase(String baseUri) throws RestException {
        return modelsBase.get(baseUri);
    }

    public static OntModel getOntModelByName(String name) throws RestException {
        return models.get(name);
    }

    public static void readGithubJsonLDContext() throws MalformedURLException, IOException {
        URL url = new URL("https://raw.githubusercontent.com/buda-base/owl-schema/master/context.jsonld");
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
        update = new EntityTag(Long.toString(System.currentTimeMillis()));
    }

    public static EntityTag getEntityTag() {
        return update;
    }

    public static Date getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public void run() {
        try {
            models = new HashMap<>();
            modelsBase = new HashMap<>();
            ArrayList<String> names = ServiceConfig.getConfig().getValidNames();
            Model md = ModelFactory.createDefaultModel();
            OntModelSpec oms = new OntModelSpec(OntModelSpec.OWL_MEM);
            OntDocumentManager odm = new OntDocumentManager();
            odm.setProcessImports(false);
            oms.setDocumentManager(odm);
            ontAllMod = ModelFactory.createOntologyModel(oms, md);
            for (String name : names) {
                String url = ServiceConfig.getConfig().getOntology(name).fileurl;
                log.info("URL >> " + ServiceConfig.getConfig().getOntology(name).fileurl);
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                InputStream stream = connection.getInputStream();
                Model tmp = ModelFactory.createDefaultModel();
                OntModel om = ModelFactory.createOntologyModel(oms, tmp);
                byte[] byteArr = IOUtils.toByteArray(stream);
                om.read(new ByteArrayInputStream(byteArr), ServiceConfig.getConfig().getOntology(name).getBaseuri(), "TURTLE");
                // caching ttl file as byte array
                ResultsCache.addToCache(byteArr, url.hashCode());
                if (!name.equals("auth")) {
                    ontAllMod.add(om);
                }
                OntData.addOntModelByName(name, om);
                OntData.addOntModelByBase(ServiceConfig.getConfig().getOntology(name).getBaseuri(), om);
                stream.close();
            }
            Resource config = ModelFactory.createDefaultModel().createResource().addProperty(ReasonerVocabulary.PROPsetRDFSLevel, "simple");
            Reasoner reasoner = RDFSRuleReasonerFactory.theInstance().create(config);
            log.info("Global model size :" + ontAllMod.size());
            infMod = ModelFactory.createInfModel(reasoner, ontAllMod);
            log.info("Inferred Global model size :" + infMod.listStatements().toList().size());
            QueryProcessor.updateOntology(infMod, fusekiUrl.substring(0, fusekiUrl.lastIndexOf('/')) + "/data", ServiceConfig.getConfig().getOntology("core").getGraph());
            log.info("Auth model size :" + getOntModelByName("auth").size());
            InfModel infModAuth = ModelFactory.createInfModel(reasoner, getOntModelByName("auth"));
            log.info("Inferred Auth model size :" + infModAuth.listStatements().toList().size());
            QueryProcessor.updateOntology(infModAuth, fusekiUrl.substring(0, fusekiUrl.lastIndexOf('/')) + "/data", ServiceConfig.getConfig().getOntology("auth").getGraph());
            readGithubJsonLDContext();

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

    public static ArrayList<OntProperty> getAllClassProps(String iri, boolean global) {
        OntClassModel clModel = new OntClassModel(iri, global);
        final ArrayList<OntProperty> list = new ArrayList<>();
        if (clModel.clazz != null) {
            String qy = "";
            try {
                qy = Prefixes.getPrefixesString() + " select distinct  ?clazz ?p\n" + "where {\n" + "bind (<" + iri + "> as ?base)\n" + "graph <" + ServiceConfig.getConfig().getOntology(ont).getGraph() + ">{\n" + "?base rdfs:subClassOf+ ?clazz .\n"
                        + "{ ?p rdfs:domain/(owl:unionOf/rdf:rest*/rdf:first)* ?clazz . }\n" + "}\n" + "}";
            } catch (RestException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            final QueryExecution qexec = QueryProcessor.getResultSet(qy, null);
            final ResultSet res = qexec.execSelect();
            while (res.hasNext()) {
                final QuerySolution qs = res.next();
                final RDFNode node = qs.get("?p");
                OntResource rsc = null;
                if (global) {
                    rsc = ontAllMod.getOntResource(node.asResource().getURI());
                } else {
                    rsc = ontMod.getOntResource(node.asResource().getURI());
                }
                if (rsc.isProperty()) {
                    list.add(rsc.asProperty());
                } else {
                    System.out.println("Skipped " + node.asResource().getURI() + " property in getAllClassProps(" + iri + ")");
                }
            }
        }
        return list;
    }

    public static ArrayList<OntResource> getDomainUsages(final String uri, boolean global) throws RestException {
        final String query = "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" + " select distinct ?s where {" + "    ?s rdfs:domain <" + uri + "> ." + "} order by ?p ?s";
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
        final String query = "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" + " select distinct ?s ?p where {" + "    ?s rdfs:range <" + uri + "> ." + "} order by ?p ?s";
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
        final String query = "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" + " select distinct ?s ?p where {" + "    ?s rdfs:subPropertyOf <" + uri + "> ." + "} order by ?p ?s";
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
        final String query = "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" + " select distinct ?s where {" + "   <" + uri + "> rdfs:subPropertyOf ?s ." + "} order by ?s";
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
        final String query = "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" + " select distinct ?s where {" + "   ?s rdfs:subClassOf <" + uri + "> ." + "} order by ?s";
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
}
