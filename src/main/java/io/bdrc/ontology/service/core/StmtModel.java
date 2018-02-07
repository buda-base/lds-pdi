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

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;


public class StmtModel {

    Statement stmt;
    
    public StmtModel(Statement stmt) {
        this.stmt = stmt;
    }

    public String getSubjectUri() {
        return stmt.getSubject().getURI();
    }
    
    public String getSubjectId() {
        return OntAccess.MODEL.shortForm(getSubjectUri());
    }

    public String getPropertyUri() {
        return stmt.getPredicate().getURI();
    }
    
    public String getPropertyId() {
        return OntAccess.MODEL.shortForm(getPropertyUri());
    }
    
    public String getObject() {
        RDFNode obj = stmt.getObject();
        if (obj.isURIResource()) {
            return OntAccess.MODEL.shortForm(obj.asResource().getURI());
        } 
        return obj.toString();
        
    }
    
    public String getObjectUri() {
        RDFNode obj = stmt.getObject();
        if (obj.isURIResource()) {
            return obj.asResource().getURI();
        } 
        return "";        
    }
    
       
    public boolean objectHasUri() {
        RDFNode obj = stmt.getObject();
        return obj.isURIResource();
    }
}

