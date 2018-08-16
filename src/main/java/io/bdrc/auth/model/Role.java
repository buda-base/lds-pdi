package io.bdrc.auth.model;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class Role {
    
    String id;
    String appType;
    String appId;
    String name;
    String desc;
    ArrayList<String> permissions;
    String asJson;
    Model model;
    
    public Role(JsonNode json) throws JsonProcessingException {
        id=getJsonValue(json,"_id");
        name=getJsonValue(json,"name");
        desc=getJsonValue(json,"description");
        appType=getJsonValue(json,"applicationType");
        appId=getJsonValue(json,"applicationId");
        permissions=new ArrayList<>();
        ArrayNode array=(ArrayNode)json.findValue("permissions");
        if(array!=null) {
            Iterator<JsonNode> it=array.iterator();
            while(it.hasNext()) {
                permissions.add(it.next().asText());
            }
        }
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
                ResourceFactory.createResource("http://purl.bdrc.io/ontology/ext/auth/Role")));        
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
                ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/appType"), 
                ResourceFactory.createPlainLiteral(appType)));
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource-auth/"+id), 
                ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/appId"), 
                ResourceFactory.createPlainLiteral(appId)));
        for(String perm: permissions) {
            model.add(ResourceFactory.createStatement(
                    ResourceFactory.createResource("http://purl.bdrc.io/resource-auth/"+id), 
                    ResourceFactory.createProperty("http://purl.bdrc.io/ontology/ext/auth/hasPermission"), 
                    ResourceFactory.createPlainLiteral("http://purl.bdrc.io/resource-auth/"+perm)));
        }
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

    public ArrayList<String> getPermissions() {
        return permissions;
    }

    @Override
    public String toString() {
        return "Role [id=" + id + ", appType=" + appType + ", appId=" + appId + ", name=" + name + ", desc=" + desc
                + ", permissions=" + permissions + ", asJson=" + asJson + ", model=" + model + "]";
    }

}
