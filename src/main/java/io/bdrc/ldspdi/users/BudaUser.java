package io.bdrc.ldspdi.users;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.vocabulary.RDF;
import org.seaborne.patch.changes.RDFChangesApply;
import org.seaborne.patch.text.RDFPatchReaderText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.model.User;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.Prefixes;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.restapi.exceptions.RestException;

public class BudaUser {

    public final static Property SKOS_PREF_LABEL = ResourceFactory.createProperty("http://www.w3.org/2004/02/skos/core#prefLabel");

    public final static Logger log = LoggerFactory.getLogger(BudaUser.class);
    // TODO this should come from the auth library
    private static String adminGroupId = "f0f95a54-56cf-4bce-bf9d-8d2df6779b60";
    public static final String PRIVATE_PFX = "http://purl.bdrc.io/graph-nc/user-private/";
    public static final String PUBLIC_PFX = "http://purl.bdrc.io/graph-nc/user/";
    public static final String BDOU_PFX = "http://purl.bdrc.io/ontology/ext/user/";
    public static final String BDU_PFX = "http://purl.bdrc.io/resource-nc/user/";
    public static final String BDO = "http://purl.bdrc.io/ontology/core/";
    public static final String FOAF = "http://xmlns.com/foaf/0.1/";
    public static final String ADR_PFX = "http://purl.bdrc.io/resource-nc/auth/";

    // TODO give an explicit name
    public static Dataset ds;

    static {
        try {
            ds = DatasetFactory.wrap(QueryProcessor.buildRdfUserDataset());
        } catch (Exception e) {
            // TODO log the error instead of writing to stdout
            e.printStackTrace();
        }
    }

    public static Resource getRdfProfile(String auth0Id) throws IOException, RestException {
        String query = "select distinct ?s where  {  ?s <http://purl.bdrc.io/ontology/ext/user/hasUserProfile> <http://purl.bdrc.io/resource-nc/auth/" + auth0Id + "> }";
        log.info("QUERY >> {} and service: {} ", query, ServiceConfig.getProperty("fusekiAuthData") + "query");
        // TODO this should be cached (like the other sparql queries)
        // perhaps in a special cache that gets cleared when there's a change in auth0
        // model
        QueryExecution qe = QueryProcessor.getResultSet(query, ServiceConfig.getProperty("fusekiAuthData") + "query");
        ResultSet rs = qe.execSelect();
        if (rs.hasNext()) {
            Resource r = rs.next().getResource("?s");
            log.info("RESOURCE >> {} and rdfId= {} ", r);
            return r;
        }
        qe.close();
        return null;
    }

    public static RDFNode getAuth0IdFromUserId(String userId) throws IOException, RestException {
        String query = "select distinct ?o where  {  <" + BDU_PFX + userId + "> <http://purl.bdrc.io/ontology/ext/user/hasUserProfile> ?o }";
        log.info("QUERY >> {} and service: {} ", query, ServiceConfig.getProperty("fusekiAuthData") + "query");
        QueryExecution qe = QueryProcessor.getResultSet(query, ServiceConfig.getProperty("fusekiAuthData") + "query");
        ResultSet rs = qe.execSelect();
        if (rs.hasNext()) {
            Resource r = rs.next().getResource("?o");
            log.info("RESOURCE >> {} and rdfId= {} ", r);
            return r;
        }
        return null;
    }

    public static boolean isAdmin(User usr) throws IOException, RestException {
        ArrayList<String> gp = usr.getGroups();
        log.info("user groups {}", gp);
        return gp.contains(BudaUser.adminGroupId);
    }

    public static boolean isActive(String userId) throws IOException, RestException {
        String query = "select distinct ?o where  {  <" + BDU_PFX + userId + "> <http://purl.bdrc.io/ontology/ext/user/isActive> ?o }";
        log.info("QUERY >> {} and service: {} ", query, ServiceConfig.getProperty("fusekiAuthData") + "query");
        QueryExecution qe = QueryProcessor.getResultSet(query, ServiceConfig.getProperty("fusekiAuthData") + "query");
        ResultSet rs = qe.execSelect();
        if (rs.hasNext()) {
            Literal r = rs.next().getLiteral("?o");
            log.info("RESOURCE >> {} and rdfId= {} ", r);
            return r.getBoolean();
        }
        return false;
    }

    public static void update(String userId, String patch) throws Exception {
        RDFConnectionRemoteBuilder builder = RDFConnectionFuseki.create().destination(ServiceConfig.getProperty("fusekiAuthData"));
        RDFConnectionFuseki fusConn = ((RDFConnectionFuseki) builder.build());
        InputStream ptc = new ByteArrayInputStream(patch.getBytes());
        RDFPatchReaderText rdf = new RDFPatchReaderText(ptc);
        Model m = fusConn.fetch("<" + PRIVATE_PFX + userId + ">");
        DatasetGraph dsg = DatasetFactory.wrap(m).asDatasetGraph();
        RDFChangesApply apply = new RDFChangesApply(dsg);
        rdf.apply(apply);
        Model m1 = ModelFactory.createModelForGraph(dsg.getGraph(NodeFactory.createURI("<" + PRIVATE_PFX + userId + ">")));
        QueryProcessor.putModel(fusConn, "<" + PRIVATE_PFX + userId + ">", m1);
        fusConn.close();
    }

    public static Model getUserModel(boolean full, Resource r) throws IOException, RestException {
        if (r == null) {
            return null;
        }
        Model mod = ModelFactory.createDefaultModel();
        String rdfId = r.getURI().substring(r.getURI().lastIndexOf("/") + 1);
        // Dataset ds = DatasetFactory.wrap(QueryProcessor.buildRdfUserDataset());
        log.info("DATASET as TRIG >>");
        RDFDataMgr.write(System.out, ds, Lang.TRIG);
        mod.add(ds.getNamedModel(PUBLIC_PFX + rdfId));
        if (full) {
            mod.add(ds.getNamedModel(PRIVATE_PFX + rdfId));
        }
        return mod;
    }

    public static Model getUserModelFromUserId(boolean full, String resId) throws IOException, RestException {
        if (resId == null) {
            return null;
        }
        // TODO I'm not sure I understand: do we keep all rdf data about users in memory
        // all the time?
        // why not do what we usually do and cache sparql query results for some time?
        Model mod = ModelFactory.createDefaultModel();
        // Dataset ds = DatasetFactory.wrap(QueryProcessor.buildRdfUserDataset());
        mod.add(ds.getNamedModel(PUBLIC_PFX + resId));
        if (full) {
            mod.add(ds.getNamedModel(PRIVATE_PFX + resId));
        }
        return mod;
    }

    public static Model[] createBudaUserModels(User usr) {
        log.info("createBudaUserModels for user {}", usr);
        Model[] mods = new Model[2];
        Model publicModel = ModelFactory.createDefaultModel();
        String userId = "U" + Integer.toString(Math.abs(usr.getName().hashCode()));
        log.debug("createBudaUserModel for >> {}", userId);
        Resource bUser = ResourceFactory.createResource(BDU_PFX + userId);
        publicModel.setNsPrefixes(Prefixes.getPrefixMapping());
        publicModel.add(bUser, RDF.type, ResourceFactory.createResource(FOAF + "Person"));
        publicModel.add(bUser, RDF.type, ResourceFactory.createResource(BDOU_PFX + "User"));
        publicModel.add(bUser, RDF.type, ResourceFactory.createResource(BDO + "Person"));
        // TODO there should be some language detection based on the first character:
        // if Chinese, then @zh-hani, if Tibetan then @bo, else no lang tag
        publicModel.add(bUser, SKOS_PREF_LABEL, usr.getName());
        // TODO don't write on System.out
        publicModel.write(System.out, "TURTLE");
        mods[0] = publicModel;

        Model privateModel = ModelFactory.createDefaultModel();
        privateModel.setNsPrefixes(Prefixes.getPrefixMapping());
        privateModel.add(bUser, RDF.type, ResourceFactory.createResource(FOAF + "Person"));
        privateModel.add(bUser, RDF.type, ResourceFactory.createResource(BDOU_PFX + "User"));
        privateModel.add(bUser, RDF.type, ResourceFactory.createResource(BDO + "Person"));
        log.info("hasUserProfile in createBudaUserModels = {}", usr.getUserId());
        String auth0Id = usr.getUserId();

        privateModel.add(bUser, ResourceFactory.createProperty(BDOU_PFX + "hasUserProfile"), ResourceFactory.createResource(ADR_PFX + auth0Id));
        // TODO don't write on system.out
        privateModel.write(System.out, "TURTLE");
        // TODO also include email from auth profile in private model
        // TODO include name

        mods[0] = publicModel;
        mods[1] = privateModel;
        ds.addNamedModel(PUBLIC_PFX + userId, publicModel);
        ds.addNamedModel(PRIVATE_PFX + userId, publicModel);
        return mods;
    }

    public static void main(String[] args) throws IOException, RestException {
        ServiceConfig.initForTests(null);
        RdfAuthModel.initForTest(false, true);
        String auth0Id = BudaUser.getAuth0IdFromUserId("U456").asNode().getURI();
        System.out.println("Is active  >> " + BudaUser.isActive("U456"));
        // System.out.println("Auth0Id >> " + auth0Id);
        // System.out.println("USERS >> " + RdfAuthModel.getUsers());
        // System.out.println("USER >> " +
        // RdfAuthModel.getUser(auth0Id.substring(auth0Id.lastIndexOf("/") + 1)));
    }

}
