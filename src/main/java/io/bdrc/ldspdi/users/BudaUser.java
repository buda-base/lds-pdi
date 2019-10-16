package io.bdrc.ldspdi.users;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.model.User;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.restapi.exceptions.RestException;

public class BudaUser {

    public final static Logger log = LoggerFactory.getLogger(BudaUser.class);
    private static String adminGroupId = "f0f95a54-56cf-4bce-bf9d-8d2df6779b60";
    public static final String PRIVATE_PFX = "http://purl.bdrc.io/graph-nc/user-private/";
    public static final String PUBLIC_PFX = "http://purl.bdrc.io/graph-nc/user/";

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

    public static void main(String[] args) throws IOException, RestException {
        System.out.println("Auth0Id >> " + BudaUser.getAuth0IdFromUserId("U456"));
    }

}
