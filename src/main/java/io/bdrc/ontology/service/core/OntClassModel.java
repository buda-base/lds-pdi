package io.bdrc.ontology.service.core;

/*******************************************************************************
 * Copyright (c) 2017 Buddhist Digital Resource Center (BDRC)
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

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This model is based on a URI for an OntClass in OntAccess.MODEL
 * 
 * @author chris
 *
 */
public class OntClassModel {
    static Logger log = LoggerFactory.getLogger(OntClassModel.class);

    protected String uri;
    protected OntClass clazz;
    
    public OntClassModel(String uri) {
        this.uri = uri;
        clazz = OntAccess.MODEL.getOntClass(uri);
        if (clazz == null) {
            log.info("***********>>  OntClassModel( " + uri + " )");
            clazz = OntClassNotPresent.INSTANCE;
        }
    }
    
    public OntClassModel(OntClass c) {
        
        this.uri = c.getURI();        
        clazz = c;
   }
    
    public boolean isPresent() {
        return !(OntClassNotPresent.class.equals(clazz.getClass()));
    }
    
    public boolean isRootClassModel() {
    	return OntAccess.getOntRootClasses().contains(this);
    }

    public String getUri() {
        return uri;
    }

    
    public String getId() {        
        return OntAccess.MODEL.shortForm(uri);
    }
    
    
    public boolean hasParent() {
        return clazz.getSuperClass() != null;
    }
    
    public OntClassModel getParent() {
        OntClass sup3r = clazz.getSuperClass();
        if (sup3r != null) {
            return new OntClassModel(sup3r);
        } else {
            return null;
        }
    }
    
    public List<OntClassModel> getSubclasses() {
        List<OntClass> subs = clazz.listSubClasses(true).toList();
        List<OntClassModel> models = new ArrayList<OntClassModel>();
        
        for (OntClass c : subs) {
            if(!c.isAnon()) {                
                models.add(new OntClassModel(c));
            }
        }
        
        return models;
    }
    
    public List<String> getLabels() {
        List<String> labels = new ArrayList<String>();
        
        for (RDFNode node : clazz.listLabels(null).toList()) {
            labels.add(node.toString());
        }
        
        return labels;
    }
    
    public List<String> getComments() {
        List<String> comments = new ArrayList<String>();
        
        for (RDFNode node : clazz.listComments(null).toList()) {
            comments.add(node.toString());
        }
        
        return comments; 
    }
    
    public List<StmtModel> getProperties() {
        List<StmtModel> properties = new ArrayList<StmtModel>();
        
        for (Statement stmt : clazz.listProperties().toList()) {
            properties.add(new StmtModel(stmt));
        }
        
        return properties;
    }
    
    public List<StmtModel> getOtherProperties() {
        List<StmtModel> properties = new ArrayList<StmtModel>();
        
        for (Statement stmt : clazz.listProperties().toList()) {
            String local = stmt.getPredicate().getLocalName();
            if (!local.equals("label") && !local.equals("comment")) {
              properties.add(new StmtModel(stmt));
            }
        }
        return properties;
    }
}
