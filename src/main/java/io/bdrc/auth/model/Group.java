package io.bdrc.auth.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.bdrc.auth.rdf.RdfConstants;

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

public class Group {
    
    String id;
    String name;
    String desc;
    ArrayList<String> members;
    ArrayList<String> roles;
    String asJson;
    Model model;
    
    public Group(JsonNode json) throws IOException {        
        id=getJsonValue(json,"_id");
        name=getJsonValue(json,"name");
        desc=getJsonValue(json,"description");
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
    
    public Group() {
        id="";
        name="";
        desc="";
        asJson="";
        members=new ArrayList<>();
        roles=new ArrayList<>();
    }

    String getJsonValue(JsonNode json,String key) {
        JsonNode tmp=json.findValue(key);
        if(tmp!=null) {
            return tmp.asText();
        }
        return "";
    }
    
    Model buildModel() {
        Resource gp= ResourceFactory.createResource(RdfConstants.AUTH_RESOURCE+id);
        model = ModelFactory.createDefaultModel();
        model.add(ResourceFactory.createStatement(gp,RDF.type,ResourceFactory.createResource(RdfConstants.GROUP)));
        model.add(ResourceFactory.createStatement(gp,RDFS.label,ResourceFactory.createPlainLiteral(name)));
        model.add(ResourceFactory.createStatement(
                gp, 
                ResourceFactory.createProperty(RdfConstants.DESC), 
                ResourceFactory.createPlainLiteral(desc)));
        for(String memb: members) {
            model.add(ResourceFactory.createStatement(
                    gp, 
                    ResourceFactory.createProperty(RdfConstants.HAS_MEMBER), 
                    ResourceFactory.createResource(RdfConstants.AUTH_RESOURCE+memb.substring(memb.indexOf("|")+1))));
            model.add(ResourceFactory.createStatement(
                    ResourceFactory.createResource(RdfConstants.AUTH_RESOURCE+memb.substring(memb.indexOf("|")+1)), 
                    ResourceFactory.createProperty(RdfConstants.FOR_GROUP), 
                    gp));
        }
        for(String role: roles) {
            model.add(ResourceFactory.createStatement(
                    gp, 
                    ResourceFactory.createProperty(RdfConstants.HAS_ROLE), 
                    ResourceFactory.createResource(RdfConstants.AUTH_RESOURCE+role)));
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

    public void setMembers(ArrayList<String> members) {
        this.members = members;
    }

    public void setRoles(ArrayList<String> roles) {
        this.roles = roles;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDesc(String desc) {
        this.desc = desc;
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
