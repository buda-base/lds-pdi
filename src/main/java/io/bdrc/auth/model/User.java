package io.bdrc.auth.model;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class User {
    
    String id;
    String authId;
    String name;
    String email;
    String asJson;
    String isSocial;
    String provider;
    String connection;
    Model model;
    
    public User(JsonNode json) throws JsonProcessingException {        
        authId=getJsonValue(json,"user_id");
        name=getJsonValue(json,"name");
        email=getJsonValue(json,"email");
        JsonNode ids=json.findValue("identities");
        if(ids!=null) {
            isSocial=getJsonValue(ids,"isSocial");
            id=getJsonValue(ids,"user_id");
            provider=getJsonValue(ids,"provider");
            connection=getJsonValue(ids,"connection");
        }
        ObjectMapper mapper = new ObjectMapper();
        asJson=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        model=buildModel();
    }
    
    Model buildModel() {
        model = ModelFactory.createDefaultModel();
        model.getNsPrefixMap().put("foaf", "http://xmlns.com/foaf/0.1/");
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource-auth/"+id), 
                ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), 
                ResourceFactory.createResource("http://purl.bdrc.io/ontology/ext/auth/User")));
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource-auth/"+id), 
                ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/isSocial"), 
                ResourceFactory.createPlainLiteral(isSocial)));
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource-auth/"+id), 
                ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/provider"), 
                ResourceFactory.createPlainLiteral(provider)));
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource-auth/"+id), 
                ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/connection"), 
                ResourceFactory.createPlainLiteral(connection)));
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource-auth/"+id), 
                ResourceFactory.createProperty("http://xmlns.com/foaf/0.1/name"), 
                ResourceFactory.createPlainLiteral(name)));
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource-auth/"+id), 
                ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/id"), 
                ResourceFactory.createPlainLiteral(authId)));
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource-auth/"+id), 
                ResourceFactory.createProperty("http://xmlns.com/foaf/0.1/mbox"), 
                ResourceFactory.createPlainLiteral(email)));
        return model;
    }
    
    String getJsonValue(JsonNode json,String key) {
        JsonNode tmp=json.findValue(key);
        if(tmp!=null) {
            return tmp.asText();
        }
        return "";
    }
    
    public String getIsSocial() {
        return isSocial;
    }

    public String getProvider() {
        return provider;
    }

    public String getConnection() {
        return connection;
    }

    public Model getModel() {
        return model;
    }

    
    public String getAsJson() {
        return asJson;
    }
    
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return "User [id=" + id + ", name=" + name + ", email=" + email + ", asJson=" + asJson + ", model=" + model
                + "]";
    }

}
