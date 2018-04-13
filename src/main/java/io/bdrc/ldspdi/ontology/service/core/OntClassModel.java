package io.bdrc.ldspdi.ontology.service.core;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This model is based on a URI for an OntClass in OntAccess.MODEL
 * 
 * @author chris, marc
 *
 */
public class OntClassModel {
    
    final static Logger log = LoggerFactory.getLogger(OntClassModel.class.getName());

    protected String uri;
    protected OntClass clazz;
    
    public OntClassModel(String uri) {
        log.info("Instanciated  OntClassModel >> "+uri);
        this.uri = uri;
        clazz = OntData.ontMod.getOntClass(uri);        
    }
    
    public OntClassModel(OntClass c) {
        
        this.uri = c.getURI();        
        clazz = c;
   }
    
    public boolean isPresent() {
        //return !(OntClassNotPresent.class.equals(clazz.getClass()));
        return clazz!=null;
    }
    
    public boolean isRootClassModel() {
    	return OntData.getOntRootClasses().contains(this);
    }

    public String getUri() {
        return uri;
    }

    
    public String getId() {        
        return OntData.ontMod.shortForm(uri);
    }
    
    
    public boolean hasParent() {        
        return clazz.getSuperClass() != null;        
    }
    
    public ArrayList<OntClassModel> getParent() {               
        if (clazz.getSuperClass() != null) {                      
            return new OntClassParent(uri).getParents();
        }
        return null;
    }
    
    public List<OntClassModel> getSubclasses() {
        List<OntClass> subs = clazz.listSubClasses(true).toList();
        List<OntClassModel> models = new ArrayList<>();
        
        for (OntClass c : subs) {
            if(!c.isAnon()) {                
                models.add(new OntClassModel(c));
            }
        }
        Collections.sort(models,OntData.ontClassModelComparator);
        return models;
    }
    
    public List<OntClassModel> getSuperClasses() {
        List<OntClass> sups = clazz.listSuperClasses(true).toList();
        List<OntClassModel> models = new ArrayList<>();        
        for (OntClass c : sups) {
            if(!c.isAnon()) {                
                models.add(new OntClassModel(c));
            }
        }
        Collections.sort(models,OntData.ontClassModelComparator);
        return models;
    }
    
    @SuppressWarnings("unchecked")
    public List<Individual> getIndividuals() {
        ExtendedIterator<Individual> it=(ExtendedIterator<Individual>)clazz.listInstances(true);
        
        List<Individual> inds = it.toList();        
        Collections.sort(inds,OntData.individualComparator);
        return inds;
    }
    
    public List<String> getLabels() {
        List<String> labels = new ArrayList<>();
        
        for (RDFNode node : clazz.listLabels(null).toList()) {
            labels.add(node.toString());
        }
        
        return labels;
    }
    
    public List<String> getComments() {
        List<String> comments = new ArrayList<>();
        
        for (RDFNode node : clazz.listComments(null).toList()) {
            comments.add(node.toString());
        }
        
        return comments; 
    }
    
}