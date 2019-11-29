package io.bdrc.ldspdi.users;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.jena.datatypes.xsd.XSDDatatype;
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
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.vocabulary.RDF;
import org.seaborne.patch.changes.RDFChangesApply;
import org.seaborne.patch.text.RDFPatchReaderText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.model.User;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.service.UserDataService;
import io.bdrc.ldspdi.sparql.Prefixes;
import io.bdrc.ldspdi.sparql.QueryProcessor;

public class BudaUser {

    public final static Property SKOS_PREF_LABEL = ResourceFactory.createProperty("http://www.w3.org/2004/02/skos/core#prefLabel");

    public final static Logger log = LoggerFactory.getLogger(BudaUser.class);
    // TODO this should come from the auth library
    public static final String PRIVATE_PFX = "http://purl.bdrc.io/graph-nc/user-private/";
    public static final String PUBLIC_PFX = "http://purl.bdrc.io/graph-nc/user/";
    public static final String BDOU_PFX = "http://purl.bdrc.io/ontology/ext/user/";
    public static final String BDU_PFX = "http://purl.bdrc.io/resource-nc/user/";
    public static final String BDO = "http://purl.bdrc.io/ontology/core/";
    public static final String FOAF = "http://xmlns.com/foaf/0.1/";
    public static final String ADR_PFX = "http://purl.bdrc.io/resource-nc/auth/";

    public static final String PUBLIC_PROPS_KEY = "publicProps";
    public static final String ADMIN_PROPS_KEY = "adminEditProps";
    public static final String USER_PROPS_KEY = "userEditProps";

    public static HashMap<String, List<String>> propsPolicies;

    public static Resource getRdfProfile(String auth0Id) throws IOException, RestException {
        Resource r = (Resource) UsersCache.getObjectFromCache(auth0Id.hashCode());
        if (r != null) {
            return r;
        }
        String query = "select distinct ?s where  {  ?s <http://purl.bdrc.io/ontology/ext/user/hasUserProfile> <http://purl.bdrc.io/resource-nc/auth/" + auth0Id + "> }";
        log.info("QUERY >> {} and service: {} ", query, ServiceConfig.getProperty("fusekiAuthData") + "query");
        QueryExecution qe = QueryProcessor.getResultSet(query, ServiceConfig.getProperty("fusekiAuthData") + "query");
        ResultSet rs = qe.execSelect();
        if (rs.hasNext()) {
            r = rs.next().getResource("?s");
            log.info("RESOURCE >> {} and rdfId= {} ", r);
            UsersCache.addToCache(r, auth0Id.hashCode());
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
        Dataset ds = DatasetFactory.create();
        DatasetGraph dsg = ds.asDatasetGraph();
        Model m = fusConn.fetch(PRIVATE_PFX + userId);
        Model pub = fusConn.fetch(PUBLIC_PFX + userId);
        dsg.addGraph(NodeFactory.createURI(PRIVATE_PFX + userId), m.getGraph());
        RDFChangesApply apply = new RDFChangesApply(dsg);
        rdf.apply(apply);
        Model m1 = ModelFactory.createModelForGraph(dsg.getGraph(NodeFactory.createURI(PRIVATE_PFX + userId)));
        QueryProcessor.putModel(fusConn, PRIVATE_PFX + userId, m1);
        ptc.close();
        fusConn.close();
        UserDataService.update(userId, pub, m1);
    }

    public static Model getUserModel(boolean full, Resource r) throws IOException, RestException {
        if (r == null) {
            return null;
        }
        RDFConnectionRemoteBuilder builder = RDFConnectionFuseki.create().destination(ServiceConfig.getProperty("fusekiAuthData"));
        RDFConnectionFuseki fusConn = ((RDFConnectionFuseki) builder.build());
        Model mod = ModelFactory.createDefaultModel();
        String rdfId = r.getURI().substring(r.getURI().lastIndexOf("/") + 1);
        mod.add(fusConn.fetch(PUBLIC_PFX + rdfId));
        if (full) {
            mod.add(fusConn.fetch(PRIVATE_PFX + rdfId));
        }
        fusConn.close();
        return mod;
    }

    public static Model getUserModelFromUserId(boolean full, String resId) throws IOException, RestException {
        if (resId == null) {
            return null;
        }
        RDFConnectionRemoteBuilder builder = RDFConnectionFuseki.create().destination(ServiceConfig.getProperty("fusekiAuthData"));
        RDFConnectionFuseki fusConn = ((RDFConnectionFuseki) builder.build());
        Model mod = ModelFactory.createDefaultModel();
        mod.add(fusConn.fetch(PUBLIC_PFX + resId));
        if (full) {
            mod.add(fusConn.fetch(PRIVATE_PFX + resId));
        }
        fusConn.close();
        return mod;
    }

    public static Model[] createBudaUserModels(User usr) {
        RDFConnectionRemoteBuilder builder = RDFConnectionFuseki.create().destination(ServiceConfig.getProperty("fusekiAuthData"));
        RDFConnectionFuseki fusConn = ((RDFConnectionFuseki) builder.build());
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
        publicModel.add(bUser, SKOS_PREF_LABEL, ResourceFactory.createPlainLiteral(usr.getName()));
        // TODO don't write on System.out
        // for development purpose only
        publicModel.write(System.out, "TURTLE");
        mods[0] = publicModel;

        Model privateModel = ModelFactory.createDefaultModel();
        privateModel.setNsPrefixes(Prefixes.getPrefixMapping());
        privateModel.add(bUser, RDF.type, ResourceFactory.createResource(FOAF + "Person"));
        privateModel.add(bUser, RDF.type, ResourceFactory.createResource(BDOU_PFX + "User"));
        privateModel.add(bUser, RDF.type, ResourceFactory.createResource(BDO + "Person"));
        log.info("hasUserProfile in createBudaUserModels = {}", usr.getUserId());
        String auth0Id = usr.getUserId();
        privateModel.add(bUser, ResourceFactory.createProperty(BDOU_PFX + "isActive"), ResourceFactory.createPlainLiteral("true"));
        privateModel.add(bUser, ResourceFactory.createProperty(BDOU_PFX + "hasUserProfile"), ResourceFactory.createResource(ADR_PFX + auth0Id));
        privateModel.add(bUser, ResourceFactory.createProperty(FOAF + "mbox"), ResourceFactory.createPlainLiteral(usr.getEmail()));
        privateModel.add(bUser, SKOS_PREF_LABEL, ResourceFactory.createPlainLiteral(usr.getName()));
        // TODO don't write on system.out
        // for development purpose only
        privateModel.write(System.out, "TURTLE");

        mods[0] = publicModel;
        mods[1] = privateModel;
        fusConn.put(PUBLIC_PFX + userId, publicModel);
        fusConn.put(PRIVATE_PFX + userId, publicModel);
        fusConn.close();
        return mods;
    }

    // for admin or testing purpose only
    public static Model[] createBudaUserModels(String userName, String usrId, String userEmail) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        RDFConnectionRemoteBuilder builder = RDFConnectionFuseki.create().destination(ServiceConfig.getProperty("fusekiAuthData"));
        RDFConnectionFuseki fusConn = ((RDFConnectionFuseki) builder.build());
        log.info("createBudaUserModels for user {}", userName);
        Model[] mods = new Model[2];
        Model publicModel = ModelFactory.createDefaultModel();
        String userId = "U" + Integer.toString(Math.abs(userName.hashCode()));
        log.debug("createBudaUserModel for >> {}", userId);
        Resource bUser = ResourceFactory.createResource(BDU_PFX + userId);
        publicModel.setNsPrefixes(Prefixes.getPrefixMapping());
        publicModel.add(bUser, RDF.type, ResourceFactory.createResource(FOAF + "Person"));
        publicModel.add(bUser, RDF.type, ResourceFactory.createResource(BDOU_PFX + "User"));
        publicModel.add(bUser, RDF.type, ResourceFactory.createResource(BDO + "Person"));
        // TODO there should be some language detection based on the first character:
        // if Chinese, then @zh-hani, if Tibetan then @bo, else no lang tag
        publicModel.add(bUser, SKOS_PREF_LABEL, ResourceFactory.createPlainLiteral(userName));
        // TODO don't write on System.out
        // for development purpose only
        publicModel.write(System.out, "TURTLE");
        mods[0] = publicModel;

        Model privateModel = ModelFactory.createDefaultModel();
        privateModel.setNsPrefixes(Prefixes.getPrefixMapping());
        privateModel.add(bUser, RDF.type, ResourceFactory.createResource(FOAF + "Person"));
        privateModel.add(bUser, RDF.type, ResourceFactory.createResource(BDOU_PFX + "User"));
        privateModel.add(bUser, RDF.type, ResourceFactory.createResource(BDO + "Person"));
        log.info("hasUserProfile in createBudaUserModels = {}", userId);
        String auth0Id = usrId;
        privateModel.add(bUser, ResourceFactory.createProperty(BDOU_PFX + "isActive"), ResourceFactory.createPlainLiteral("true"));
        privateModel.add(bUser, ResourceFactory.createProperty(BDOU_PFX + "hasUserProfile"), ResourceFactory.createResource(ADR_PFX + auth0Id));
        privateModel.add(bUser, ResourceFactory.createProperty(FOAF + "mbox"), ResourceFactory.createPlainLiteral(userEmail));
        privateModel.add(bUser, ResourceFactory.createProperty(BDOU_PFX + "accountCreation"), ResourceFactory.createTypedLiteral(sdf.format(new Date()), XSDDatatype.XSDdateTime));
        privateModel.add(bUser, ResourceFactory.createProperty(BDOU_PFX + "preferredLangTags"), ResourceFactory.createPlainLiteral("eng"));
        privateModel.add(bUser, SKOS_PREF_LABEL, ResourceFactory.createPlainLiteral(userName));
        // TODO don't write on system.out
        // for development purpose only
        privateModel.write(System.out, "TURTLE");

        mods[0] = publicModel;
        mods[1] = privateModel;
        fusConn.put(PUBLIC_PFX + userId, publicModel);
        fusConn.put(PRIVATE_PFX + userId, publicModel);
        fusConn.close();
        return mods;
    }

    public static HashMap<String, List<String>> getUserPropsEditPolicies() {
        if (propsPolicies == null) {
            propsPolicies = new HashMap<>();
            propsPolicies.put(BudaUser.PUBLIC_PROPS_KEY, Arrays.asList(ServiceConfig.getProperty(BudaUser.PUBLIC_PROPS_KEY).split(",")));
            propsPolicies.put(BudaUser.ADMIN_PROPS_KEY, Arrays.asList(ServiceConfig.getProperty(BudaUser.ADMIN_PROPS_KEY).split(",")));
            propsPolicies.put(BudaUser.USER_PROPS_KEY, Arrays.asList(ServiceConfig.getProperty(BudaUser.USER_PROPS_KEY).split(",")));
        }
        return propsPolicies;
    }

    public static void main(String[] args) throws IOException, RestException {
        ServiceConfig.initForTests(null);
        RdfAuthModel.initForTest(false, true);
        // String auth0Id = BudaUser.getAuth0IdFromUserId("U456").asNode().getURI();
        // System.out.println("Is active >> " + BudaUser.isActive("U456"));
        // System.out.println("Auth0Id >> " + auth0Id);
        // System.out.println("USERS >> " + RdfAuthModel.getUsers());
        // System.out.println("USER >> " +
        // RdfAuthModel.getUser(auth0Id.substring(auth0Id.lastIndexOf("/") + 1)));
        System.out.println(BudaUser.createBudaUserModels("Nicolas Berger", "103776618189565648628", "quai.ledrurollin@gmail.com")[0]);
        System.out.println(BudaUser.createBudaUserModels("Nicolas Berger", "103776618189565648628", "quai.ledrurollin@gmail.com")[1]);
    }

}
