package io.bdrc.ldspdi.users;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
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
        Dataset ds = QueryProcessor.buildRdfUserDataset();
        Model m = ds.getUnionModel();
        ResIterator ri = m.listSubjectsWithProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/user/hasUserProfile"), ResourceFactory.createResource("http://purl.bdrc.io/resource-nc/auth/" + auth0Id));
        if (ri.hasNext()) {
            Resource r = ri.next();
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

    public static Model getUserModel(boolean full, Resource r) throws IOException, RestException {
        if (r == null) {
            return null;
        }
        Model mod = ModelFactory.createDefaultModel();
        String rdfId = r.getURI().substring(r.getURI().lastIndexOf("/") + 1);
        Dataset ds = QueryProcessor.buildRdfUserDataset();
        mod.add(ds.getNamedModel(PUBLIC_PFX + rdfId));
        if (full) {
            mod.add(ds.getNamedModel(PRIVATE_PFX + rdfId));
        }
        // log.info("USER MODEL for {} and full={}", r, full);
        // mod.write(System.out, "JSONLD");
        return mod;
    }

}
