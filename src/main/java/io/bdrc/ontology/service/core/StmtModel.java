package io.bdrc.ontology.service.core;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;

import com.fasterxml.jackson.annotation.JsonGetter;

public class StmtModel {

    Statement stmt;
    
    public StmtModel(Statement stmt) {
        this.stmt = stmt;
    }

    @JsonGetter("subjectUri")
    public String getSubjectUri() {
        return stmt.getSubject().getURI();
    }
    
    @JsonGetter("subjectId")
    public String getSubjectId() {
        return OntAccess.MODEL.shortForm(getSubjectUri());
    }

    @JsonGetter("propertyUri")
    public String getPropertyUri() {
        return stmt.getPredicate().getURI();
    }
    
    @JsonGetter("propertyId")
    public String getPropertyId() {
        return OntAccess.MODEL.shortForm(getPropertyUri());
    }
    
    @JsonGetter("object")
    public String getObject() {
        RDFNode obj = stmt.getObject();
        if (obj.isURIResource()) {
            return OntAccess.MODEL.shortForm(obj.asResource().getURI());
        } else {
            return obj.toString();
        }
    }
    
    @JsonGetter("objectUri")
    public String getObjectUri() {
        RDFNode obj = stmt.getObject();
        if (obj.isURIResource()) {
            return obj.asResource().getURI();
        } else {
            return "";
        }
    }
    
    @JsonGetter("objectHasUri")
    public boolean objectHasUri() {
        RDFNode obj = stmt.getObject();
        return obj.isURIResource();
    }
}

