package io.bdrc.ontology.service.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import io.bdrc.ldspdi.service.ServiceConfig;

public class OntAccess {
	
	public static OntModel MODEL;
    private static String OWL_URL;

    public static void init() {
        Logger log = LoggerFactory.getLogger(OntAccess.class);
        
        OntModel ontModel = null;
        //CONFIG = config;
        
        try {
        	OWL_URL=ServiceConfig.getProperty("owlURL");
            InputStream stream = HttpFile.stream(OWL_URL);

            log.info("got stream for " + OWL_URL);
            
            Model m = ModelFactory.createDefaultModel();
            m.read(stream, "", "RDF/XML");
            stream.close();
            ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, m);
            Utils.removeIndividuals(ontModel);
            Utils.rdf10tordf11(ontModel);
            
            log.info("got OntModel for " + OWL_URL);

        } catch (IOException io) {
            log.error("Error initializing OntModel", io);
        }
        
        MODEL = ontModel;
    }
    
    public static String getOwlURL() {
        return OWL_URL;
    }
    
    /**
     * Answer the prefix for the given URI, or null if there isn't one. If there is more than one, 
     * one of them will be picked. If possible, it will be the most recently added prefix. 
     * (The cases where it's not possible is when a binding has been removed.)
     * 
     * @param uri
     * @return
     */
    public static String getNsURIPrefix(String uri) {
        return MODEL.getNsURIPrefix(uri);
    }
    
    /**
     * Get the URI bound to a specific prefix, null if there isn't one.
     * 
     * @param pfx
     * @return
     */
    public static String getNsPrefixURI(String pfx) {
        return MODEL.getNsPrefixURI(pfx);
    }
    
    /**
     * List of all of the root OntClass(es) in the ontology - includes unions and so on that may have
     * been defined for object property domain or range purposes. These latter are blank nodes.
     * 
     * @return list of all of the root OntClass(es)
     */
    public static List<OntClass> getRootClasses() {
        List<OntClass> classes = MODEL.listHierarchyRootClasses().toList();
        return classes;
    }
    
    /**
     * Returns a list of simple root OntClass(es). Simple means not defined as a Union or Restriction
     * and so on. The purpose is to provide the roots of a traversal of classes defined in the ontology.
     * 
     * @return list of simple root OntClass(es)
     */    
    public static List<OntClass> getSimpleRootClasses() {
        List<OntClass> classes = MODEL.listHierarchyRootClasses().toList();
        List<OntClass> rez = new ArrayList<OntClass>();
        for (OntClass oc : classes) {
            if (oc.getURI() != null) {
                rez.add(oc);
            }
        }
        
        return rez;
    }
    
    //@JsonGetter("rootClasses")
    public static List<OntClassModel> getOntRootClasses() {
        List<OntClass> roots = getSimpleRootClasses();
        List<OntClassModel> models = new ArrayList<OntClassModel>();
       
        for (OntClass root : roots) {
            models.add(new OntClassModel(root));
        }
        
        return models;
    }
    
    //@JsonGetter("numPrefixes")
    public static int getNumPrefixes() {
        return MODEL.numPrefixes();
    }
    
    //@JsonGetter("name")
    public static String getName() {
        return MODEL.listOntologies().toList().get(0).getLabel(null);
    }
    
    //@JsonGetter
    public static int getNumClasses() {
        return MODEL.listClasses().toList().size();
    }
    
    //@JsonGetter
    public static int getNumObjectProperties() {
        return MODEL.listObjectProperties().toList().size();
    }
    
    //@JsonGetter
    public static int getNumDatatypeProperties() {
        return MODEL.listDatatypeProperties().toList().size();
    }
    
    //@JsonGetter
    public static int getNumAnnotationProperties() {
        return MODEL.listAnnotationProperties().toList().size();
    }
    
    //@JsonGetter
    public static int getNumRootClasses() {
        return getSimpleRootClasses().size();
    }
}

