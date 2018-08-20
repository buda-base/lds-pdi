package io.bdrc.auth.rdf;


import java.util.ArrayList;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.model.Group;
import io.bdrc.auth.model.User;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.restapi.exceptions.RestException;

public class RdfAuthModel implements Runnable{
    
    static Model authMod; 
    static ArrayList<User> users;
    static ArrayList<Group> groups;
    
    public final static Logger log=LoggerFactory.getLogger(RdfAuthModel.class.getName());
    public final static String query="\n SELECT ?s ?p ?o \n" + 
            "WHERE {\n" + 
            "  GRAPH <http://purl.bdrc.io/ontology/ext/authData> { \n" + 
            "    ?s ?p ?o .\n" + 
            "    }\n" + 
            "}";
        
    public static void init() throws RestException {        
        authMod=QueryProcessor.getAuthDataGraph(query,null);
        authMod.write(System.out,"TURTLE");
    }
    
    public static Model getFullModel() {
        return authMod;
    }
    
    public static ArrayList<User> getUsers(){
        if(users!=null) {
            return users;
        }
        users=new ArrayList<>();
        SimpleSelector s=new SimpleSelector(null, null,ResourceFactory.createResource("http://purl.bdrc.io/ontology/ext/auth/User"));
        ResIterator it=authMod.listResourcesWithProperty(ResourceFactory.createProperty(RDF.type.getURI()),
                ResourceFactory.createResource("http://purl.bdrc.io/ontology/ext/auth/User"));
        while(it.hasNext()) {
            Resource rs=it.next();
            User user=new User();
            user.setAuthId(rs.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/id")).getObject().toString());
            user.setName(rs.getProperty(ResourceFactory.createProperty("http://xmlns.com/foaf/0.1/name")).getObject().toString());
            user.setEmail(rs.getProperty(ResourceFactory.createProperty("http://xmlns.com/foaf/0.1/mbox")).getObject().toString());
            user.setIsSocial(rs.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/isSocial")).getObject().toString());
            String id=rs.getURI();
            user.setId(id.substring(0, id.lastIndexOf("/")));
            user.setProvider(rs.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/provider")).getObject().toString());
            user.setConnection(rs.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/connection")).getObject().toString());
            users.add(user);
            System.out.println(user);
        }
        return users;
    }
    
    public static ArrayList<Group> getGroups(){
        if(groups!=null) {
            return groups;
        }
        groups=new ArrayList<>();
        SimpleSelector s=new SimpleSelector(null, null,ResourceFactory.createResource("http://purl.bdrc.io/ontology/ext/auth/Group"));
        ResIterator it=authMod.listResourcesWithProperty(ResourceFactory.createProperty(RDF.type.getURI()),
                ResourceFactory.createResource("http://purl.bdrc.io/ontology/ext/auth/Group"));
        while(it.hasNext()) {
            Group gp=new Group();
            Resource rs=it.next();
            StmtIterator sit=rs.listProperties(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/hasMember"));
            while(sit.hasNext()) {
                String member=sit.next().getObject().toString();
                gp.getMembers().add(member.substring(member.lastIndexOf("/")+1));                   
            }
            sit=rs.listProperties(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/hasRole"));
            while(sit.hasNext()) {
                String role=sit.next().getObject().toString();
                gp.getRoles().add(role.substring(role.lastIndexOf("/")+1));                   
            }
            String id=rs.getURI();
            gp.setId(id.substring(0, id.lastIndexOf("/")));
            gp.setName(rs.getProperty(ResourceFactory.createProperty(RDFS.label.getURI())).getObject().toString());
            gp.setDesc(rs.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/desc")).getObject().toString());
            groups.add(gp);
            System.out.println(gp);
        }
        return groups;
    }
        
    @Override
    public void run() {
        QueryProcessor.updateAuthData(null);
        try {
            init();
        } catch (RestException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        log.info("Done loading rdfAuth Model");
    }
    
    public static void main(String[] args) throws RestException {
        ServiceConfig.initForTests();
        authMod=QueryProcessor.getAuthDataGraph(query,"http://buda1.bdrc.io:13180/fuseki/bdrcrw/query");
        getGroups();
    }
    

}
