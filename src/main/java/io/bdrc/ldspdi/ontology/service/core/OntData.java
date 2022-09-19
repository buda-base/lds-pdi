package io.bdrc.ldspdi.ontology.service.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
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
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.ReasonerVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.service.OntPolicies;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.Helpers;

public class OntData {

    public static InfModel infMod;
    public static OntModel ontAllMod;
    public static OntModel ontAuthMod;
    public static OWLPropsCharacteristics owlCharacteristics;
    public final static Logger log = LoggerFactory.getLogger(OntData.class);
    public static String JSONLD_CONTEXT;
    static String update;
    static Date lastUpdated;
    public static HashMap<String, OntModel> modelsBase = new HashMap<>();
    final static Resource RDFPL = ResourceFactory.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral");
    static String fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
    static List<AnnotationProperty> adminAnnotProps;
    private String payload;
    private static String commitId;
    private static String fusekidataUrl = fusekiUrl.substring(0, fusekiUrl.lastIndexOf('/')) + "/data"; 
    
    public OntData(String payload, String commit) {
        this.payload = payload;
        commitId = commit;
    }

    public void init() {
        try {
            if (commitId == null && payload != null) {
                JsonNode node = new ObjectMapper().readTree(payload);
                commitId = node.get("commits").elements().next().get("id").asText();
            }
            OntPolicies.init();
            modelsBase = new HashMap<>();
            Model md = ModelFactory.createDefaultModel();
            OntModelSpec oms = new OntModelSpec(OntModelSpec.OWL_MEM);
            OntDocumentManager odm = null;
            odm = new OntDocumentManager(System.getProperty("user.dir") + "/owl-schema/ont-policy.rdf");
            odm.getFileManager().resetCache();
            odm.setCacheModels(false);
            odm.setProcessImports(false);
            oms.setDocumentManager(odm);
            ontAllMod = ModelFactory.createOntologyModel(oms, md);
            Iterator<String> it = odm.listDocuments();
            while (it.hasNext()) {
                String uri = it.next();
                log.info("OntManagerDoc : {}", uri);
                OntModel om = odm.getOntology(uri, oms);
                String tmp = uri.substring(0, uri.length() - 1);
                File directory = new File("ontologies/");
                if (!directory.exists()) {
                    directory.mkdir();
                }
                directory = new File("ontologies/" + commitId);
                if (!directory.exists()) {
                    directory.mkdir();
                }
                String file = null;
                try {
                    file = "ontologies/" + commitId + "/" + tmp.substring(tmp.lastIndexOf("/") + 1) + ".ttl";
                    Helpers.writeModelToFile(om, file);
                } catch (Exception ex) {
                    // do absolutely nothing so the shapoes are loaded anyway - just log
                    log.info("Could not write file {}", file);
                }
                ontAllMod.add(om);
                OntData.addOntModelByBase(parseBaseUri(uri), om);
            }
            boolean readonly = "true".equals(ServiceConfig.getProperty("readOnly"));
            if (!readonly) {
                updateFusekiDataset();
            } else {
                log.error("read only mode, don't update Fuseki with the ontology");
            }
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

    public static OntModel getOntModelByBase(String baseUri) throws RestException {
        return modelsBase.get(baseUri);
    }

    public static void readGithubJsonLDContext() throws MalformedURLException, IOException {
        InputStream stream = null;
        stream = new FileInputStream(ServiceConfig.LOCAL_ONT_DIR + "context.jsonld");
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        StringBuilder st = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            st.append(line + "\n");
        }
        in.close();
        JSONLD_CONTEXT = st.toString();
        lastUpdated = Calendar.getInstance().getTime();
        update = Long.toString(System.currentTimeMillis());
    }

    /*
     * public static String getEntityTag() { return update; }
     */

    public static String getOntCommitId() {
        return commitId;
    }

    public static Date getLastUpdated() {
        return lastUpdated;
    }

    private static void updateFusekiDataset() throws RestException {
        OntPolicies.init();
        log.error("FUSEKI URL IS >>" + fusekidataUrl);
        log.error("updateFusekiDataset() is ontAllModNull {} ont graph uri {} ", (ontAllMod == null),
                parseBaseUri("http://purl.bdrc.io/ontology/core/"));
        log.error("updateFusekiDataset() is ontAllModByBaseNull {}",
                (OntPolicies.getOntologyByBase(parseBaseUri("http://purl.bdrc.io/ontology/core/")) == null));
        log.error("updateFusekiDataset() is ontAllMod by base graph uri {}",
                (OntPolicies.getOntologyByBase(parseBaseUri("http://purl.bdrc.io/ontology/core/")).getGraph()));
        String rule = "[inverseOf1: (?P owl:inverseOf ?Q) -> (?Q owl:inverseOf ?P) ]";
        List<Rule> miniRules = Rule.parseRules(rule);
        Reasoner reasoner = new GenericRuleReasoner(miniRules);
        reasoner.setParameter(ReasonerVocabulary.PROPruleMode, "forward");
        InfModel core = ModelFactory.createInfModel(reasoner, ontAllMod);
        QueryProcessor.updateOntology(core, fusekidataUrl,
                OntPolicies.getOntologyByBase(parseBaseUri("http://purl.bdrc.io/ontology/core/")).getGraph(), "update 1");
        QueryProcessor.updateOntology(getOntModelByBase(parseBaseUri("http://purl.bdrc.io/ontology/ext/auth")),
                fusekidataUrl,
                OntPolicies.getOntologyByBase(parseBaseUri("http://purl.bdrc.io/ontology/ext/auth/")).getGraph(), "update 2");
    }

    public static OWLPropsCharacteristics getOwlCharacteristics() throws IOException, RestException {
        if (owlCharacteristics == null) {
            owlCharacteristics = new OWLPropsCharacteristics(ontAllMod);
        }
        return owlCharacteristics;
    }

    public static Model describeUri(final String uri) {
        final String query = "describe  <" + uri + ">";
        final QueryExecution qexec = QueryExecutionFactory.create(query, ontAllMod);
        return qexec.execDescribe();
    }

    public static ArrayList<OntResource> getDomainUsages(final String uri) throws RestException {
        final String query = "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" + " select distinct ?s where {" + "    ?s rdfs:domain <" + uri
                + "> ." + "} order by ?p ?s";
        QueryExecution qexec = null;

        qexec = QueryExecutionFactory.create(query, ontAllMod);

        final ResultSet res = qexec.execSelect();
        final ArrayList<OntResource> list = new ArrayList<>();
        while (res.hasNext()) {
            final QuerySolution qs = res.next();
            final RDFNode node = qs.get("?s");

            list.add(ontAllMod.getOntResource(node.asResource().getURI()));

        }
        return list;
    }

    public static ArrayList<OntResource> getRangeUsages(final String uri) throws RestException {
        final String query = "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" + " select distinct ?s ?p where {" + "    ?s rdfs:range <"
                + uri + "> ." + "} order by ?p ?s";
        QueryExecution qexec = null;
        qexec = QueryExecutionFactory.create(query, ontAllMod);
        final ResultSet res = qexec.execSelect();
        final ArrayList<OntResource> list = new ArrayList<>();
        while (res.hasNext()) {
            final QuerySolution qs = res.next();
            final RDFNode node = qs.get("?s");
            list.add(ontAllMod.getOntResource(node.asResource().getURI()));
        }
        return list;
    }

    public static ArrayList<OntResource> getSubProps(final String uri) throws RestException {
        final String query = "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" + " select distinct ?s ?p where {"
                + "    ?s rdfs:subPropertyOf <" + uri + "> ." + "} order by ?p ?s";
        QueryExecution qexec = null;
        qexec = QueryExecutionFactory.create(query, ontAllMod);
        final ResultSet res = qexec.execSelect();
        final ArrayList<OntResource> list = new ArrayList<>();
        while (res.hasNext()) {
            final QuerySolution qs = res.next();
            final RDFNode node = qs.get("?s");
            list.add(ontAllMod.getOntResource(node.asResource().getURI()));
        }
        return list;
    }

    public static ArrayList<OntResource> getParentProps(final String uri) throws RestException {
        final String query = "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" + " select distinct ?s where {" + "   <" + uri
                + "> rdfs:subPropertyOf ?s ." + "} order by ?s";
        QueryExecution qexec = null;
        qexec = QueryExecutionFactory.create(query, ontAllMod);
        final ResultSet res = qexec.execSelect();
        final ArrayList<OntResource> list = new ArrayList<>();
        while (res.hasNext()) {
            final QuerySolution qs = res.next();
            final RDFNode node = qs.get("?s");
            list.add(ontAllMod.getOntResource(node.asResource().getURI()));
        }
        return list;
    }

    public static HashMap<String, ArrayList<OntResource>> getAllSubProps(final String uri) throws RestException {
        final ArrayList<OntResource> props = OntData.getDomainUsages(uri);
        final HashMap<String, ArrayList<OntResource>> map = new HashMap<>();
        for (final OntResource rs : props) {
            final ArrayList<OntResource> l = OntData.getSubProps(rs.getURI());
            if (l.size() > 1) {
                map.put(rs.getURI(), l);
            }
        }
        return map;
    }

    public static boolean isClass(final String uri) {
        OntModel md = null;
        md = ontAllMod;
        if (md.getOntResource(uri) != null) {
            return md.getOntResource(uri).isClass();
        } else {
            return false;
        }
    }

    public static int getNumPrefixes(String baseUri) throws RestException {
        return OntData.getOntModelByBase(baseUri).numPrefixes();
    }

    public static Map<String, String> getPrefixMap(String baseUri) throws RestException {
        return OntData.getOntModelByBase(baseUri).getNsPrefixMap();
    }

    public static int getNumRootClasses(String baseuri) throws RestException {
        return getSimpleRootClasses(baseuri).size();
    }

    /**
     * Returns a list of simple root OntClass(es). Simple means not defined as a
     * Union or Restriction and so on. The purpose is to provide the roots of a
     * traversal of classes defined in the ontology.
     *
     * @return list of simple root OntClass(es)
     * @throws RestException
     */
    public static List<OntClass> getSimpleRootClasses(String baseUri) throws RestException {
        // Iterator<OntClass> it = ontMod.listHierarchyRootClasses();
        Iterator<OntClass> it = OntData.getOntModelByBase(baseUri).listClasses();
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

    public static List<OntClassModel> getOntRootClasses(String baseUri) throws RestException {
        final List<OntClass> roots = getSimpleRootClasses(baseUri);
        final List<OntClassModel> models = new ArrayList<>();
        for (final OntClass root : roots) {
            if (!root.isAnon()) {
                models.add(new OntClassModel(root));
            }
        }
        Collections.sort(models, OntData.ontClassModelComparator);
        return models;
    }

    public static ArrayList<OntClass> getAllClasses(String baseUri) throws RestException {
        final ExtendedIterator<OntClass> it = OntData.getOntModelByBase(baseUri).listClasses();
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

    public static ArrayList<OntProperty> getAllProps(String baseUri) throws RestException {
        final ExtendedIterator<OntProperty> it = OntData.getOntModelByBase(baseUri).listAllOntProperties();
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

    public static List<Individual> getAllIndividuals(String baseUri) throws RestException {
        final List<Individual> indv = OntData.getOntModelByBase(baseUri).listIndividuals().toList();
        Collections.sort(indv, individualComparator);
        return indv;
    }

    public final static Comparator<OntClass> ontClassComparator = new Comparator<OntClass>() {
        @Override
        public int compare(OntClass class1, OntClass class2) {
            if (ServiceConfig.PREFIX.getPrefix(class1.getNameSpace()).equals(ServiceConfig.PREFIX.getPrefix(class2.getNameSpace()))) {
                return class1.getLocalName().compareTo(class2.getLocalName());
            }
            return ServiceConfig.PREFIX.getPrefix(class1.getNameSpace()).compareTo(ServiceConfig.PREFIX.getPrefix(class2.getNameSpace()));
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
            if (ServiceConfig.PREFIX.getPrefix(prop1.getNameSpace()).equals(ServiceConfig.PREFIX.getPrefix(prop2.getNameSpace()))) {
                return prop1.getLocalName().compareTo(prop2.getLocalName());
            }
            return ServiceConfig.PREFIX.getPrefix(prop1.getNameSpace()).compareTo(ServiceConfig.PREFIX.getPrefix(prop2.getNameSpace()));
        }

    };

    public static List<AnnotationProperty> getAdminAnnotProps() throws RestException {
        OntModel om = OntData.getOntModelByBase(parseBaseUri("http://purl.bdrc.io/ontology/admin"));
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
        // ServiceConfig.initForTests("http://buda1.bdrc.io:13180/fuseki/newcorerw/query");
        // OntData.init();
        // Model m = ReasonerRegistry.theRegistry().getAllDescriptions();
        // m.write(System.out, "TURTLE");

    }

}
