package io.bdrc.auth.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class Group {
    
    String id;
    String name;
    String desc;
    ArrayList<String> members;
    ArrayList<String> roles;
    String asJson;
    Model model;
    
    public Group(JsonNode json) throws IOException {        
        id=json.findValue("_id").asText();
        name=json.findValue("name").asText();
        desc=json.findValue("description").asText();
        members=new ArrayList<>();
        ArrayNode array=(ArrayNode)json.findValue("members");
        if(array!=null) {
            Iterator<JsonNode> it=((ArrayNode)json.findValue("members")).iterator();
            while(it.hasNext()) {
                members.add(it.next().asText());
            }
        }
        roles=new ArrayList<>();
        array=(ArrayNode)json.findValue("roles");
        if(array!=null) {
            Iterator<JsonNode> it=(array).iterator();
            while(it.hasNext()) {
                roles.add(it.next().asText());
            }
        }
        ObjectMapper mapper = new ObjectMapper();
        asJson=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        model=buildModel();
    }
    
    Model buildModel() {
        model = ModelFactory.createDefaultModel();
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://purl.bdrc.io/resource/"+id), 
                ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), 
                ResourceFactory.createResource("http://purl.bdrc.io/ontology/core/Group")));
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
        for(String memb: members) {
            model.add(ResourceFactory.createStatement(
                    ResourceFactory.createResource("http://purl.bdrc.io/resource/"+id), 
                    ResourceFactory.createProperty("http://purl.bdrc.io/auth/hasMember"), 
                    ResourceFactory.createPlainLiteral(memb)));
        }
        for(String role: roles) {
            model.add(ResourceFactory.createStatement(
                    ResourceFactory.createResource("http://purl.bdrc.io/resource/"+id), 
                    ResourceFactory.createProperty("http://purl.bdrc.io/auth/hasRole"), 
                    ResourceFactory.createPlainLiteral(role)));
        }
        return model;
    }
    
    public String getAsJson() {
        return asJson;
    }

    @Override
    public String toString() {
        return "Group [id=" + id + ", name=" + name + ", desc=" + desc + ", members=" + members + ", roles=" + roles
                + ", asJson=" + asJson + ", model=" + model + "]";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public ArrayList<String> getMembers() {
        return members;
    }

    public ArrayList<String> getRoles() {
        return roles;
    }
    
    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public boolean isValidRole(String role) {
        return roles.contains(role);
    }
    
    public boolean isMember(String member) {
        return members.contains(member);
    }

}
