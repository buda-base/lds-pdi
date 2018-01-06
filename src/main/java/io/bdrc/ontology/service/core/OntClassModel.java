package io.bdrc.ontology.service.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonGetter;

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
    
    @JsonGetter("isPresent")
    public boolean isPresent() {
        return !(OntClassNotPresent.class.equals(clazz.getClass()));
    }

    @JsonGetter("uri")
    public String getUri() {
        return uri;
    }

    @JsonGetter("id")
    public String getId() {
        return OntAccess.MODEL.shortForm(uri);
    }
    
    @JsonGetter("hasParent")
    public boolean hasParent() {
        return clazz.getSuperClass() != null;
    }
    @JsonGetter("parent")
    public OntClassModel getParent() {
        OntClass sup3r = clazz.getSuperClass();
        if (sup3r != null) {
            return new OntClassModel(sup3r);
        } else {
            return null;
        }
    }
    
    @JsonGetter("subclasses")
    public List<OntClassModel> getSubclasses() {
        List<OntClass> subs = clazz.listSubClasses(true).toList();
        List<OntClassModel> models = new ArrayList<OntClassModel>();
        
        for (OntClass c : subs) {
            models.add(new OntClassModel(c));
        }
        
        return models;
    }
    
    @JsonGetter("labels")
    public List<String> getLabels() {
        List<String> labels = new ArrayList<String>();
        
        for (RDFNode node : clazz.listLabels(null).toList()) {
            labels.add(node.toString());
        }
        
        return labels;
    }
    
    @JsonGetter("comments")
    public List<String> getComments() {
        List<String> comments = new ArrayList<String>();
        
        for (RDFNode node : clazz.listComments(null).toList()) {
            comments.add(node.toString());
        }
        
        return comments;
    }
    
    @JsonGetter("properties")
    public List<StmtModel> getProperties() {
        List<StmtModel> properties = new ArrayList<StmtModel>();
        
        for (Statement stmt : clazz.listProperties().toList()) {
            properties.add(new StmtModel(stmt));
        }
        
        return properties;
    }
    
    @JsonGetter("otherProperties")
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
