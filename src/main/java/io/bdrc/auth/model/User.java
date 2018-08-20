package io.bdrc.auth.model;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;

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
    
    public User() {
        authId="";
        name="";
        email="";
        isSocial="";
        id="";
        provider="";
        connection="";
        asJson="";
        model=null;
    }

    Model buildModel() {
        model = ModelFactory.createDefaultModel();
        model.getNsPrefixMap().put("foaf", "http://xmlns.com/foaf/0.1/");
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource-auth/"+id), 
                ResourceFactory.createProperty(RDF.type.getURI()), 
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
    
    public void setId(String id) {
        this.id = id;
    }

    public void setAuthId(String authId) {
        this.authId = authId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setIsSocial(String isSocial) {
        this.isSocial = isSocial;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setConnection(String connection) {
        this.connection = connection;
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
        return "User [id=" + id + ", authId=" + authId + ", name=" + name + ", email=" + email + ", asJson=" + asJson
                + ", isSocial=" + isSocial + ", provider=" + provider + ", connection=" + connection + ", model="
                + model + "]";
    }

}
