package io.bdrc.ontology.service.core;

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
        } else {
            return obj.toString();
        }
    }
    
    public String getObjectUri() {
        RDFNode obj = stmt.getObject();
        if (obj.isURIResource()) {
            return obj.asResource().getURI();
        } else {
            return "";
        }
    }
    
    public boolean objectHasUri() {
        RDFNode obj = stmt.getObject();
        return obj.isURIResource();
    }
}

