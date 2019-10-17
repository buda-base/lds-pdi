package io.bdrc.ldspdi.users;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.model.User;
import io.bdrc.ldspdi.sparql.Prefixes;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.restapi.exceptions.RestException;

public class BudaUser {

    public final static Property SKOS_PREF_LABEL = ResourceFactory.createProperty("http://www.w3.org/2004/02/skos/core#prefLabel");

    public final static Logger log = LoggerFactory.getLogger(BudaUser.class);
    private static String adminGroupId = "f0f95a54-56cf-4bce-bf9d-8d2df6779b60";
    public static final String PRIVATE_PFX = "http://purl.bdrc.io/graph-nc/user-private/";
    public static final String PUBLIC_PFX = "http://purl.bdrc.io/graph-nc/user/";
    public static final String BDOU_PFX = "http://purl.bdrc.io/ontology/ext/user/";
    public static final String BDU_PFX = "http://purl.bdrc.io/resource-nc/user/";
    public static final String BDO = "http://purl.bdrc.io/ontology/core/";
    public static final String FOAF = "http://xmlns.com/foaf/0.1/";
    public static final String ADR_PFX = "http://purl.bdrc.io/resource-nc/auth/";

    public static Resource getRdfProfile(String auth0Id) throws IOException, RestException {
        Dataset ds = DatasetFactory.wrap(QueryProcessor.buildRdfUserDataset());
        Model m = ds.getUnionModel();
        ResIterator ri = m.listSubjectsWithProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/user/hasUserProfile"), ResourceFactory.createResource("http://purl.bdrc.io/resource-nc/auth/" + auth0Id));
        if (ri.hasNext()) {
            Resource r = ri.next();
            log.info("RESOURCE >> {} and rdfId= {} ", r);
            return r;
        }
        return null;
    }

    public static RDFNode getAuth0IdFromUserId(String userId) throws IOException, RestException {
        Dataset ds = DatasetFactory.wrap(QueryProcessor.buildRdfUserDataset());
        Model m = ds.getUnionModel();
        NodeIterator ni = m.listObjectsOfProperty(ResourceFactory.createResource("http://purl.bdrc.io/resource-nc/user/" + userId), ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/user/hasUserProfile"));
        if (ni.hasNext()) {
            RDFNode n = ni.next();
            log.info("NODE >> {} and rdfId= {} ", n);
            return n;
        }
        return null;
    }

    public static boolean isAdmin(User usr) throws IOException, RestException {
        ArrayList<String> gp = usr.getGroups();
        log.info("user groups {}", gp);
        return gp.contains(BudaUser.adminGroupId);
    }

    public static Model getUserModel(boolean full, Resource r) throws IOException, RestException {
        if (r == null) {
            return null;
        }
        Model mod = ModelFactory.createDefaultModel();
        String rdfId = r.getURI().substring(r.getURI().lastIndexOf("/") + 1);
        Dataset ds = DatasetFactory.wrap(QueryProcessor.buildRdfUserDataset());
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
        Model mod = ModelFactory.createDefaultModel();
        Dataset ds = DatasetFactory.wrap(QueryProcessor.buildRdfUserDataset());
        mod.add(ds.getNamedModel(PUBLIC_PFX + resId));
        if (full) {
            mod.add(ds.getNamedModel(PRIVATE_PFX + resId));
        }
        return mod;
    }

    public static Model[] createBudaUserModels(User usr) {
        Model[] mods = new Model[2];
        Model publicModel = ModelFactory.createDefaultModel();
        String userId = "U" + Integer.toString(Math.abs(usr.getName().hashCode()));
        System.out.println(userId);
        Resource bUser = ResourceFactory.createResource(BDU_PFX + userId);
        publicModel.setNsPrefixes(Prefixes.getPrefixMapping());
        publicModel.add(bUser, RDF.type, ResourceFactory.createResource(FOAF + "Person"));
        publicModel.add(bUser, RDF.type, ResourceFactory.createResource(BDOU_PFX + "User"));
        publicModel.add(bUser, RDF.type, ResourceFactory.createResource(BDO + "Person"));
        publicModel.add(bUser, SKOS_PREF_LABEL, usr.getName());
        publicModel.write(System.out, "TURTLE");
        mods[0] = publicModel;
        Model privateModel = ModelFactory.createDefaultModel();
        privateModel.setNsPrefixes(Prefixes.getPrefixMapping());
        privateModel.add(bUser, RDF.type, ResourceFactory.createResource(FOAF + "Person"));
        privateModel.add(bUser, RDF.type, ResourceFactory.createResource(BDOU_PFX + "User"));
        privateModel.add(bUser, RDF.type, ResourceFactory.createResource(BDO + "Person"));
        privateModel.add(bUser, ResourceFactory.createProperty(BDOU_PFX + "hasUserProfile"), ResourceFactory.createResource(ADR_PFX + usr.getAuthId().substring(usr.getAuthId().lastIndexOf("/") + 1)));
        privateModel.write(System.out, "TURTLE");
        mods[0] = publicModel;
        mods[1] = privateModel;
        return mods;
    }

    public static void main(String[] args) throws IOException, RestException {
        System.out.println("Auth0Id >> " + BudaUser.getAuth0IdFromUserId("U456"));
        BudaUser.createBudaUserModels(null);
    }

}
