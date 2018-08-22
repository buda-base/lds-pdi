package io.bdrc.auth.model;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        model=buildModel();
    }
    
    public Application() {
        name="";
        desc="";
        appType="";
        appId="";
    }

    Model buildModel() {
        Resource app= ResourceFactory.createResource(RdfConstants.AUTH_RESOURCE+appId);
        model = ModelFactory.createDefaultModel();
        model.add(ResourceFactory.createStatement(app,RDF.type,ResourceFactory.createResource(RdfConstants.APPLICATION)));
        model.add(ResourceFactory.createStatement(app,RDFS.label,ResourceFactory.createPlainLiteral(name)));
        model.add(ResourceFactory.createStatement(
                app, 
                ResourceFactory.createProperty(RdfConstants.APPTYPE), 
                ResourceFactory.createPlainLiteral(appType)));
        model.add(ResourceFactory.createStatement(
                app, 
                ResourceFactory.createProperty(RdfConstants.DESC), 
                ResourceFactory.createPlainLiteral(desc)));
        return model;
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

    @Override
    public String toString() {
        return "Application [appType=" + appType + ", appId=" + appId + ", name=" + name + ", desc=" + desc
                + ", asJson=" + asJson + ", model=" + model + "]";
    }
    
    

}
