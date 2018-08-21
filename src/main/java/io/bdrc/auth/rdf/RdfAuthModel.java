package io.bdrc.auth.rdf;


import java.util.ArrayList;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.model.Application;
import io.bdrc.auth.model.Endpoint;
import io.bdrc.auth.model.Group;
import io.bdrc.auth.model.Permission;
import io.bdrc.auth.model.Role;
import io.bdrc.auth.model.User;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.restapi.exceptions.RestException;

public class RdfAuthModel implements Runnable{
    
    static Model authMod; 
    static ArrayList<User> users;
    static ArrayList<Group> groups;
    static ArrayList<Role> roles;
    static ArrayList<Permission> permissions;
    static ArrayList<Endpoint> endpoints;
    static ArrayList<Application> applications;
    
    public final static Logger log=LoggerFactory.getLogger(RdfAuthModel.class.getName());
    public final static String query="\n SELECT ?s ?p ?o \n" + 
            "WHERE {\n" + 
            "  GRAPH <http://purl.bdrc.io/ontology/ext/authData> { \n" + 
            "    ?s ?p ?o .\n" + 
            "    }\n" + 
            "}";
        
    public static void init() throws RestException {        
        authMod=QueryProcessor.getAuthDataGraph(query,null);        
    }
    
    public static Model getFullModel() {
        return authMod;
    }
    
    public static ArrayList<User> getUsers(){
        if(users!=null) {
            return users;
        }
        users=new ArrayList<>();
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
            gp.setId(id.substring(id.lastIndexOf("/")+1));
            gp.setName(rs.getProperty(ResourceFactory.createProperty(RDFS.label.getURI())).getObject().toString());
            gp.setDesc(rs.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/desc")).getObject().toString());
            groups.add(gp);
            System.out.println(gp);
        }
        return groups;
    }
    
    public static ArrayList<Role> getRoles(){
        if(roles!=null) {
            return roles;
        }
        roles=new ArrayList<>();
        ResIterator it=authMod.listResourcesWithProperty(ResourceFactory.createProperty(RDF.type.getURI()),
                ResourceFactory.createResource("http://purl.bdrc.io/ontology/ext/auth/Role"));
        while(it.hasNext()) {
            Role role=new Role();
            Resource rs=it.next();
            StmtIterator sit=rs.listProperties(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/hasPermission"));
            while(sit.hasNext()) {
                String perm=sit.next().getObject().toString();
                role.getPermissions().add(perm.substring(perm.lastIndexOf("/")+1));                   
            }
            String id=rs.getURI();
            role.setId(id.substring(id.lastIndexOf("/")+1));
            role.setName(rs.getProperty(ResourceFactory.createProperty(RDFS.label.getURI())).getObject().toString());
            role.setDesc(rs.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/desc")).getObject().toString());
            String appId=rs.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/appId")).getObject().toString();
            role.setAppId(appId.substring(id.lastIndexOf("/")+1));
            role.setAppType(rs.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/appType")).getObject().toString());
            
            roles.add(role);
            System.out.println(role);
        }
        return roles;
    }
    
    public static ArrayList<Permission> getPermissions(){
        if(permissions!=null) {
            return permissions;
        }
        permissions=new ArrayList<>();
        ResIterator it=authMod.listResourcesWithProperty(ResourceFactory.createProperty(RDF.type.getURI()),
                ResourceFactory.createResource("http://purl.bdrc.io/ontology/ext/auth/Permission"));
        while(it.hasNext()) {
            Permission perm=new Permission();
            Resource rs=it.next();            
            String id=rs.getURI();
            perm.setId(id.substring(id.lastIndexOf("/")+1));
            perm.setName(rs.getProperty(ResourceFactory.createProperty(RDFS.label.getURI())).getObject().toString());
            perm.setDesc(rs.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/desc")).getObject().toString());
            String appId=rs.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/appId")).getObject().toString();
            perm.setAppId(appId.substring(id.lastIndexOf("/")+1));
            permissions.add(perm);
            System.out.println(perm);
        }
        return permissions;
    }
    
    public static ArrayList<Endpoint> getEndpoints(){
        if(endpoints!=null) {
            return endpoints;
        }
        endpoints=new ArrayList<>();
        ResIterator it=authMod.listResourcesWithProperty(ResourceFactory.createProperty(RDF.type.getURI()),
                ResourceFactory.createResource("http://purl.bdrc.io/ontology/ext/auth/Endpoint"));
        while(it.hasNext()) {
            Endpoint endp=new Endpoint();
            Resource rs=it.next();
            StmtIterator sit=rs.listProperties(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/forGroup"));
            while(sit.hasNext()) {
                String gp=sit.next().getObject().toString();
               endp.getGroups().add(gp.substring(gp.lastIndexOf("/")+1));                   
            }
            sit=rs.listProperties(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/forRole"));
            while(sit.hasNext()) {
                String role=sit.next().getObject().toString();
                endp.getRoles().add(role.substring(role.lastIndexOf("/")+1));                   
            }
            sit=rs.listProperties(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/forPermission"));
            while(sit.hasNext()) {
                String perm=sit.next().getObject().toString();
                endp.getPermissions().add(perm.substring(perm.lastIndexOf("/")+1));                   
            }
            endp.setPath(rs.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/path")).getObject().toString());
            String appId=rs.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/appId")).getObject().toString();
            endp.setAppId(appId.substring(appId.lastIndexOf("/")+1));
            endpoints.add(endp);
            System.out.println(endp);
        }
        return endpoints;
    }
    
    public static ArrayList<Application> getApplications(){
        if(applications!=null) {
            return applications;
        }
        applications=new ArrayList<>();
        ResIterator it=authMod.listResourcesWithProperty(ResourceFactory.createProperty(RDF.type.getURI()),
                ResourceFactory.createResource("http://purl.bdrc.io/ontology/ext/auth/Application"));
        while(it.hasNext()) {
            Resource rs=it.next();
            Application app=new Application();
            app.setName(rs.getProperty(ResourceFactory.createProperty(RDFS.label.getURI())).getObject().toString());
            String id=rs.getURI();
            app.setAppId(id.substring(0, id.lastIndexOf("/")));
            app.setAppType(rs.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/appType")).getObject().toString());
            app.setDesc(rs.getProperty(ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/desc")).getObject().toString());
            applications.add(app);
            System.out.println(app);
        }
        return applications;
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
        getUsers();
        getGroups();
        getRoles();
        getPermissions();
        getEndpoints();
        getApplications();
    }
    

}
