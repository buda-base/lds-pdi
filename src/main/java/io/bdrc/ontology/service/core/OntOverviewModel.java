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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonGetter;

/**
 * A model class for the top-level of the ontology(s). The idea is to provide an entry point
 * to the ontology that provides some basic information and navigational links into to ontology
 * 
 * @author chris
 *
 */
public class OntOverviewModel {
    Logger log = LoggerFactory.getLogger(this.getClass());
    

    public Logger getLog() {
		return log;
	}

	@JsonGetter("name")
    public String getName() {
        return OntAccess.MODEL.listOntologies().toList().get(0).getLabel(null);
    }

    @JsonGetter("numPrefixes")
    public int getNumPrefixes() {
        return OntAccess.MODEL.numPrefixes();
    }

    @JsonGetter("numOntologies")
    public int getNumOntologies() {
        return OntAccess.MODEL.listOntologies().toList().size();
    }
    
    @JsonGetter
    public String getRootURL() {
        return OntAccess.getOwlURL();
    }
    
    @JsonGetter
    public int getNumClasses() {
        return OntAccess.MODEL.listClasses().toList().size();
    }
    
    @JsonGetter
    public int getNumAnnotationProperties() {
        return OntAccess.MODEL.listAnnotationProperties().toList().size();
    }
    
    @JsonGetter
    public int getNumObjectProperties() {
        return OntAccess.MODEL.listObjectProperties().toList().size();
    }
    
    @JsonGetter
    public int getNumDatatypeProperties() {
        return OntAccess.MODEL.listDatatypeProperties().toList().size();
    }
    
    @JsonGetter
    public int getNumRootClasses() {
        return OntAccess.getSimpleRootClasses().size();
    }
    
    @JsonGetter("rootClasses")
    public List<OntClassModel> getRootClasses() {
        List<OntClass> roots = OntAccess.getSimpleRootClasses();
        List<OntClassModel> models = new ArrayList<OntClassModel>();
       
        for (OntClass root : roots) {
            models.add(new OntClassModel(root));
        }
        
        return models;
    }
}

