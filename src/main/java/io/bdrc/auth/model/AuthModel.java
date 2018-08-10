package io.bdrc.auth.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.iterator.ExtendedIterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.auth.AuthProps;


public class AuthModel {
    
    ArrayList<Group> groups;
    ArrayList<Role> roles;
    ArrayList<Permission> permissions;
    ArrayList<User> users;
    ArrayList<Endpoint> endpoints;
    ArrayList<String> paths;
    Model model;
    
    public AuthModel(Model authMod) throws ClientProtocolException, IOException {
        HttpClient client=HttpClientBuilder.create().build();
        HttpPost post=new HttpPost("https://bdrc-io.auth0.com/oauth/token");
        HashMap<String,String> json = new HashMap<>();
        json.put("grant_type","client_credentials");
        json.put("client_id",AuthProps.getProperty("lds-pdiClientID"));
        json.put("client_secret",AuthProps.getProperty("lds-pdiClientSecret"));
        json.put("audience","urn:auth0-authz-api"); 
        ObjectMapper mapper=new ObjectMapper();
        String post_data=mapper.writer().writeValueAsString(json);
        StringEntity se = new StringEntity(post_data);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(se);
        HttpResponse response = client.execute(post);
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        response.getEntity().writeTo(baos);
        String json_resp=baos.toString();        
        baos.close();
        JsonNode node=mapper.readTree(json_resp);
        String token=node.findValue("access_token").asText();
        model=ModelFactory.createDefaultModel();
        setGroups(token);
        setRoles(token);
        setPermissions(token);
        setUsers(token);
        setEndpoints(authMod);
    }
    
    private void setGroups(String token) throws ClientProtocolException, IOException {
        groups=new ArrayList<>();
        ObjectMapper mapper=new ObjectMapper();
        HttpClient client=HttpClientBuilder.create().build();
        HttpGet get=new HttpGet("https://bdrc-io.us.webtask.io/adf6e2f2b84784b57522e3b19dfc9201/api/groups");
        get.addHeader("Authorization", "Bearer "+token);
        HttpResponse resp=client.execute(get);
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        resp.getEntity().writeTo(baos);
        JsonNode node1=mapper.readTree(baos.toString());
        baos.close();
        Iterator<JsonNode> it=node1.at("/groups").elements();
        while(it.hasNext()) { 
            Group gp=new Group(it.next());
            groups.add(gp);
            model.add(gp.getModel());          
        }
    }
    
    private void setRoles(String token) throws ClientProtocolException, IOException {
        roles=new ArrayList<>();
        ObjectMapper mapper=new ObjectMapper();
        HttpClient client=HttpClientBuilder.create().build();
        HttpGet get=new HttpGet("https://bdrc-io.us.webtask.io/adf6e2f2b84784b57522e3b19dfc9201/api/roles");
        get.addHeader("Authorization", "Bearer "+token);
        HttpResponse resp=client.execute(get);
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        resp.getEntity().writeTo(baos);
        JsonNode node1=mapper.readTree(baos.toString());
        baos.close();
        Iterator<JsonNode> it=node1.at("/roles").elements();
        while(it.hasNext()) { 
            Role role=new Role(it.next());
            roles.add(role);
            model.add(role.getModel());          
        }
    }
    
    private void setPermissions(String token) throws ClientProtocolException, IOException {
        permissions=new ArrayList<>();
        ObjectMapper mapper=new ObjectMapper();
        HttpClient client=HttpClientBuilder.create().build();
        HttpGet get=new HttpGet("https://bdrc-io.us.webtask.io/adf6e2f2b84784b57522e3b19dfc9201/api/permissions");
        get.addHeader("Authorization", "Bearer "+token);
        HttpResponse resp=client.execute(get);
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        resp.getEntity().writeTo(baos);
        JsonNode node1=mapper.readTree(baos.toString());
        baos.close();
        Iterator<JsonNode> it=node1.at("/permissions").elements();
        while(it.hasNext()) {  
            Permission perm=new Permission(it.next());
            permissions.add(perm);
            model.add(perm.getModel());          
        }
    }
    
    private void setUsers(String token) throws ClientProtocolException, IOException {
        users=new ArrayList<>();
        ObjectMapper mapper=new ObjectMapper();
        HttpClient client=HttpClientBuilder.create().build();
        HttpGet get=new HttpGet("https://bdrc-io.us.webtask.io/adf6e2f2b84784b57522e3b19dfc9201/api/users");
        get.addHeader("Authorization", "Bearer "+token);
        HttpResponse resp=client.execute(get);
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        resp.getEntity().writeTo(baos);
        JsonNode node1=mapper.readTree(baos.toString());
        baos.close();
        Iterator<JsonNode> it=node1.at("/users").elements();
        while(it.hasNext()) { 
            User user=new User(it.next());
            users.add(user);
            model.add(user.getModel());       
        }
    }
    
    private void setEndpoints(Model authMod) throws ClientProtocolException, IOException {
        endpoints=new ArrayList<>();
        paths=new ArrayList<>();
        Triple t=new Triple(org.apache.jena.graph.Node.ANY,
                NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                NodeFactory.createURI("http://purl.bdrc.io/ontology/core/Endpoint"));
        ExtendedIterator<Triple> ext=authMod.getGraph().find(t);
        //System.out.println("TRIPLE LIST >>> "+ext.toList());
        while(ext.hasNext()) {
            String st=ext.next().getSubject().getURI();
            Endpoint end=new Endpoint(authMod,st);
            endpoints.add(end); 
            paths.add(end.getPath());
        }
    }
    
    public ArrayList<Group> getGroups() {
        return groups;
    }

    public ArrayList<Role> getRoles() {
        return roles;
    }

    public ArrayList<Permission> getPermissions() {
        return permissions;
    }

    public ArrayList<User> getUsers() {
        return users;
    }

    public ArrayList<Endpoint> getEndpoints() {
        return endpoints;
    }
    
    public ArrayList<String> getPaths() {
        return paths;
    }
    
    public Endpoint getEndpoint(String path) {
        for(Endpoint e:endpoints) {
            if(e.getPath().equals(path)) {
                return e;
            }
        }
        return null;
    }

    public Model getModel() {
        return model;
    }
    
    public boolean isSecuredEndpoint(String path) {
        return paths.contains(path);
    }

    @Override
    public String toString() {
        return "AuthModel [groups=" + groups + ", roles=" + roles + ", permissions=" + permissions + ", users=" + users
                + ", endpoints=" + endpoints + ", model=" + model + "]";
    }

}
