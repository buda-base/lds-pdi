package io.bdrc.auth.model;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class User {
    
    String id;
    String name;
    String email;
    String asJson;
    Model model;
    
    public User(JsonNode json) throws JsonProcessingException {
        id=getJsonValue(json,"user_id");
        name=getJsonValue(json,"name");
        email=getJsonValue(json,"email");
        ObjectMapper mapper = new ObjectMapper();
        asJson=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        model=buildModel();
    }
    
    Model buildModel() {
        model = ModelFactory.createDefaultModel();
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource/"+id), 
                ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), 
                ResourceFactory.createResource("http://purl.bdrc.io/ontology/core/User")));
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource/"+id), 
                ResourceFactory.createProperty("http://purl.bdrc.io/auth/id"), 
                ResourceFactory.createPlainLiteral(id)));
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource/"+id), 
                ResourceFactory.createProperty("http://purl.bdrc.io/auth/hasName"), 
                ResourceFactory.createPlainLiteral(name)));
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource/"+id), 
                ResourceFactory.createProperty("http://purl.bdrc.io/auth/hasEmail"), 
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
