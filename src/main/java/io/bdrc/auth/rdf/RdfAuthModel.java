package io.bdrc.auth.rdf;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;

import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.model.Application;
import io.bdrc.auth.model.AuthDataModelBuilder;
import io.bdrc.auth.model.Endpoint;
import io.bdrc.auth.model.Group;
import io.bdrc.auth.model.Permission;
import io.bdrc.auth.model.Role;
import io.bdrc.auth.model.User;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.restapi.exceptions.RestException;

/*******************************************************************************
 * Copyright (c) 2018 Buddhist Digital Resource Center (BDRC)
 * 
 * If this file is a derivation of another work the license header will appear below; 
 * otherwise, this work is licensed under the Apache License, Version 2.0 
 * (the "License"); you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

public class RdfAuthModel implements Runnable{
    
    static Model authMod; 
    static ArrayList<User> users;
    static ArrayList<Group> groups;
    static ArrayList<Role> roles;
    static ArrayList<Permission> permissions;
    static ArrayList<Endpoint> endpoints;
    static ArrayList<Application> applications;
    static long updated;
    
    private static final int PERIOD_MS = Integer.parseInt(AuthProps.getPublicProperty("updatePeriod"));
    private static final int DELAY_MS = 5000;    
    public final static Logger log=LoggerFactory.getLogger(RdfAuthModel.class.getName());
        
    public static void init() throws RestException {        
        reloadModel();        
        ModelUpdate task = new ModelUpdate();
        Timer timer = new Timer();
        timer.schedule(task, DELAY_MS, PERIOD_MS);
    }
    
    
    public static long getUpdated() {
        return updated;
    }

    public static void update(long updatedTime) throws RestException {
        reloadModel();
        updated=updatedTime;
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
    
    static void reloadModel() {
        HttpURLConnection connection;
        Model m = ModelFactory.createDefaultModel();
        try {
            connection = (HttpURLConnection) new URL(AuthProps.getPublicProperty("updateModelUrl")).openConnection();
            InputStream stream=connection.getInputStream();
            m = ModelFactory.createDefaultModel();                      
            m.read(stream, "", "TURTLE");
            stream.close(); 
            authMod=m;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void updateAuthData(String fusekiUrl) {
        if(fusekiUrl == null) {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }
        fusekiUrl = fusekiUrl.substring(0, fusekiUrl.lastIndexOf("/"));
        log.info("Service fuseki >> "+fusekiUrl);
        log.info("authDataGraph >> "+ServiceConfig.getProperty("authDataGraph"));              
        DatasetAccessor access=DatasetAccessorFactory.createHTTP(fusekiUrl);
        try {
            AuthDataModelBuilder auth=new AuthDataModelBuilder();
            access.putModel(ServiceConfig.getProperty("authDataGraph"), auth.getModel());  
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }      
    }
        
    @Override
    public void run() {
        try {
            updateAuthData(null);
            update(System.currentTimeMillis());
        } catch (RestException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        log.info("Done loading and updating rdfAuth Model");
    }
    
    public static void main(String[] args) throws RestException {
        ServiceConfig.initForTests();
        reloadModel();
        getUsers();
        getGroups();
        getRoles();
        getPermissions();
        getEndpoints();
        getApplications();
      //Test
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) new URL("http://purl.bdrc.io/authmodel").openConnection();
            InputStream stream=connection.getInputStream();
            //InputStream stream=RdfAuthModel.class.getClassLoader().getResourceAsStream("policiesTest.ttl");  
            Model test = ModelFactory.createDefaultModel();                      
            test.read(stream, "", "TURTLE");
            stream.close();
            test.write(System.out, "TURTLE");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //end test
    }
    

}
