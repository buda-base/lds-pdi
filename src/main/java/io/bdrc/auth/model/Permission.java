package io.bdrc.auth.model;


import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Permission {
    
    String id;
    String appType;
    String appId;
    String name;
    String desc;
    String asJson;
    Model model;
    
    public Permission(JsonNode json) throws JsonProcessingException {
        id=getJsonValue(json,"_id");
        name=getJsonValue(json,"name");
        desc=getJsonValue(json,"description");
        appType=getJsonValue(json,"applicationType");
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
                ResourceFactory.createResource("http://purl.bdrc.io/resource/"+id), 
                ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), 
                ResourceFactory.createResource("http://purl.bdrc.io/ontology/core/Permission")));
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
                ResourceFactory.createProperty("http://purl.bdrc.io/auth/hasDesc"), 
                ResourceFactory.createPlainLiteral(desc)));
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource/"+id), 
                ResourceFactory.createProperty("http://purl.bdrc.io/auth/appType"), 
                ResourceFactory.createPlainLiteral(appType)));
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource/"+id), 
                ResourceFactory.createProperty("http://purl.bdrc.io/auth/appId"), 
                ResourceFactory.createPlainLiteral(appId)));        
        return model;
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
    
    @Override
    public String toString() {
        return "Permission [id=" + id + ", appType=" + appType + ", appId=" + appId + ", name=" + name + ", desc="
                + desc + ", asJson=" + asJson + ", model=" + model + "]";
    }

}
