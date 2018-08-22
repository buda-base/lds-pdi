package io.bdrc.auth.model;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;

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
        Resource usr= ResourceFactory.createResource(RdfConstants.AUTH_RESOURCE+id);
        model = ModelFactory.createDefaultModel();
        model.getNsPrefixMap().put("foaf", "http://xmlns.com/foaf/0.1/");
        model.add(ResourceFactory.createStatement(usr,RDF.type,ResourceFactory.createResource(RdfConstants.USER)));
        model.add(ResourceFactory.createStatement(
                usr, 
                ResourceFactory.createProperty(RdfConstants.IS_SOCIAL), 
                ResourceFactory.createPlainLiteral(isSocial)));
        model.add(ResourceFactory.createStatement(
                usr, 
                ResourceFactory.createProperty(RdfConstants.PROVIDER), 
                ResourceFactory.createPlainLiteral(provider)));
        model.add(ResourceFactory.createStatement(
                usr, 
                ResourceFactory.createProperty(RdfConstants.CONNECTION), 
                ResourceFactory.createPlainLiteral(connection)));
        model.add(ResourceFactory.createStatement(
                usr, 
                ResourceFactory.createProperty(RdfConstants.FOAF_NAME), 
                ResourceFactory.createPlainLiteral(name)));
        model.add(ResourceFactory.createStatement(
                usr, 
                ResourceFactory.createProperty(RdfConstants.ID), 
                ResourceFactory.createPlainLiteral(authId)));
        model.add(ResourceFactory.createStatement(
                usr, 
                ResourceFactory.createProperty(RdfConstants.FOAF_MBOX), 
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
