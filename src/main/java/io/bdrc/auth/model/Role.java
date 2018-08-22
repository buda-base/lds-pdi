package io.bdrc.auth.model;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import com.fasterxml.jackson.core.JsonProcessingException;
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
    
    public Role() {
        id="";
        name="";
        desc="";
        appType="";
        appId="";
        permissions=new ArrayList<>();
    }

    String getJsonValue(JsonNode json,String key) {
        JsonNode tmp=json.findValue(key);
        if(tmp!=null) {
            return tmp.asText();
        }
        return "";
    }
    
    Model buildModel() {
        Resource role= ResourceFactory.createResource(RdfConstants.AUTH_RESOURCE+id);
        model = ModelFactory.createDefaultModel();
        model.add(ResourceFactory.createStatement(
                role, 
                ResourceFactory.createProperty(RDF.type.getURI()), 
                ResourceFactory.createResource(RdfConstants.ROLE)));        
        model.add(ResourceFactory.createStatement(
                role, 
                ResourceFactory.createProperty(RDFS.label.getURI()), 
                ResourceFactory.createPlainLiteral(name)));
        model.add(ResourceFactory.createStatement(
                role, 
                ResourceFactory.createProperty(RdfConstants.DESC), 
                ResourceFactory.createPlainLiteral(desc)));
        model.add(ResourceFactory.createStatement(
                role, 
                ResourceFactory.createProperty(RdfConstants.APPTYPE), 
                ResourceFactory.createPlainLiteral(appType)));
        model.add(ResourceFactory.createStatement(
                role, 
                ResourceFactory.createProperty(RdfConstants.APPID), 
                ResourceFactory.createResource(RdfConstants.AUTH_RESOURCE+appId)));
        for(String perm: permissions) {
            model.add(ResourceFactory.createStatement(
                    role, 
                    ResourceFactory.createProperty(RdfConstants.HAS_PERMISSION), 
                    ResourceFactory.createResource(RdfConstants.AUTH_RESOURCE+perm)));
        }
        return model;
    }
    
    
    public void setId(String id) {
        this.id = id;
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setPermissions(ArrayList<String> permissions) {
        this.permissions = permissions;
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
