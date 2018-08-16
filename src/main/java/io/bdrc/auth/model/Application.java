package io.bdrc.auth.model;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Application {
    
    String appType;
    String appId;
    String name;
    String desc;
    String asJson;
    Model model;
    
    public Application(JsonNode json) throws JsonProcessingException {        
        name=getJsonValue(json,"name");
        desc=getJsonValue(json,"description");
        appType=getJsonValue(json,"app_type");
        appId=getJsonValue(json,"client_id");
        ObjectMapper mapper = new ObjectMapper();
        asJson=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        System.out.println("APPS >>"+asJson);
        model=buildModel();
    }
    
    Model buildModel() {
        model = ModelFactory.createDefaultModel();
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource-auth/"+appId), 
                ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), 
                ResourceFactory.createResource("http://purl.bdrc.io/ontology/ext/auth/Application")));
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource-auth/"+appId), 
                ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#label"), 
                ResourceFactory.createPlainLiteral(name)));
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource-auth/"+appId), 
                ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/appType"), 
                ResourceFactory.createPlainLiteral(appType)));
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource-auth/"+appId), 
                ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/desc"), 
                ResourceFactory.createPlainLiteral(desc)));
        return model;
    }
    
    
    
    public String getAppType() {
        return appType;
    }

    public String getAppId() {
        return appId;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public String getAsJson() {
        return asJson;
    }

    public Model getModel() {
        return model;
    }

    String getJsonValue(JsonNode json,String key) {
        JsonNode tmp=json.findValue(key);
        if(tmp!=null) {
            return tmp.asText();
        }
        return "";
    }

}
