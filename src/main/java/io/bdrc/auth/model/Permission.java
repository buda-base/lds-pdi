package io.bdrc.auth.model;


import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Permission {
    
    String id;
    String appId;
    String name;
    String desc;
    String asJson;
    Model model;
    
    public Permission(JsonNode json) throws JsonProcessingException {
        id=getJsonValue(json,"_id");
        name=getJsonValue(json,"name");
        desc=getJsonValue(json,"description");
        appId=getJsonValue(json,"applicationId");
        ObjectMapper mapper = new ObjectMapper();
        asJson=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        model=buildModel();
    }
    
    String getJsonValue(JsonNode json,String key) {
        JsonNode tmp=json.findValue(key);
        if(tmp!=null) {
            return tmp.asText();
        }
        return "";
    }
    
    Model buildModel() {
        model = ModelFactory.createDefaultModel();
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource-auth/"+id), 
                ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), 
                ResourceFactory.createResource("http://purl.bdrc.io/ontology/ext/auth/Permission")));
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource-auth/"+id), 
                ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#label"), 
                ResourceFactory.createPlainLiteral(name)));
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource-auth/"+id), 
                ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/desc"), 
                ResourceFactory.createPlainLiteral(desc)));
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource-auth/"+id), 
                ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/appId"), 
                ResourceFactory.createResource("http://purl.bdrc.io/resource-auth/"+appId)));        
        return model;
    }

    public Model getModel() {
        return model;
    }

    public String getAsJson() {
        return asJson;
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
    
    @Override
    public String toString() {
        return "Permission [ appId=" + appId + ", name=" + name + ", desc="
                + desc + ", asJson=" + asJson + ", model=" + model + "]";
    }

}
