package io.bdrc.ontology.service.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.composer.ClassProperties;
import io.bdrc.ldspdi.composer.ClassProperty;
import io.bdrc.ldspdi.rest.resources.PublicDataResource;
import io.bdrc.ldspdi.service.ServiceConfig;

public class OntAccess {
	
	public static OntModel MODEL;
    private static String OWL_URL;
    public static HashMap<String,ClassProperties> ontData;
    private static ArrayList<String> rootClassesUris;
    public static Logger log=LoggerFactory.getLogger(PublicDataResource.class.getName());

    public static void init() {
        
        OntModel ontModel = null;        
        
        try {
        	OWL_URL=ServiceConfig.getProperty("owlURL");
        	InputStream stream = HttpFile.stream(OWL_URL);
            
            Model m = ModelFactory.createDefaultModel();
            m.read(stream, "", "RDF/XML");
            stream.close();
            ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, m);
            //Utils.removeIndividuals(ontModel);
            Utils.rdf10tordf11(ontModel);

        } catch (IOException io) {
            log.error("Error initializing OntModel", io);            
        }        
        MODEL = ontModel;
        ontData=new HashMap<>();
        List<OntClass> list=MODEL.listClasses().toList();
		for(OntClass clazz:list) {			
			List<OntProperty> props=clazz.listDeclaredProperties(true).toList();
			for(OntProperty p:props) {
				if(clazz.getLocalName()!=null) {					
					addClassProperty(clazz.getURI(), p);
				}
			}
		}
		rootClassesUris = new ArrayList<>();
		List<OntClassModel> rootClasses=OntAccess.getOntRootClasses();
		for(OntClassModel m:rootClasses) {
			rootClassesUris.add(m.getUri());
		}
    }
    
    public static boolean isRootClass(String uri) {
    	return rootClassesUris.contains(uri);
    }
    
    private static void addClassProperty(String className,OntProperty prop) {
    	ClassProperties classProps=ontData.get(className);
    	if(classProps==null) {
    		classProps=new ClassProperties();
    	}
    	classProps.addClassProperty(new ClassProperty(prop,className));
    	ontData.put(className, classProps);
    }
    
    public static ArrayList<ClassProperty> listAnnotationProps(String classLocalName){
    	ArrayList<ClassProperty> list=new ArrayList<>(); 
    	ClassProperties clProps=ontData.get(classLocalName);
    	if(clProps != null) {
    		return clProps.getAnnotationProps();
    	}
    	return list;
    }
    
    public static ArrayList<ClassProperty> listObjectProps(String classLocalName){    	
    	ArrayList<ClassProperty> list=new ArrayList<>(); 
    	ClassProperties clProps=ontData.get(classLocalName);
    	if(clProps != null) {
    		list= clProps.getObjectProps();
    	}
    	return list;
    }
    
    public static ArrayList<ClassProperty> listDataTypeProps(String classLocalName){
    	ArrayList<ClassProperty> list=new ArrayList<>(); 
    	ClassProperties clProps=ontData.get(classLocalName);
    	if(clProps != null) {
    		return clProps.getDataTypeProps();
    	}
    	return list;
    }
    
    public static ArrayList<ClassProperty> listSymmetricProps(String classLocalName){
    	ArrayList<ClassProperty> list=new ArrayList<>(); 
    	ClassProperties clProps=ontData.get(classLocalName);
    	if(clProps != null) {
    		return clProps.getSymmetricProps();
    	}
    	return list;
    }
    
    public static ArrayList<ClassProperty> listFunctionalProps(String classLocalName){
    	ArrayList<ClassProperty> list=new ArrayList<>(); 
    	ClassProperties clProps=ontData.get(classLocalName);
    	if(clProps != null) {
    		return clProps.getFunctionalProps();
    	}
    	return list;
    }
    
    public static ArrayList<ClassProperty> listClassProps(String classLocalName){
    	ArrayList<ClassProperty> list=new ArrayList<>(); 
    	ClassProperties clProps=ontData.get(classLocalName);
    	if(clProps != null) {
    		return clProps.getClassProps();
    	}
    	return list;
    }
    
    public static ArrayList<ClassProperty> listIrreflexiveProps(String classLocalName){
    	ArrayList<ClassProperty> list=new ArrayList<>(); 
    	ClassProperties clProps=ontData.get(classLocalName);
    	if(clProps != null) {
    		return clProps.getIrreflexiveProps();
    	}
    	return list;
    }
    
    public static ArrayList<ClassProperty> listProps(String classLocalName,String rdfType){
    	ArrayList<ClassProperty> list=new ArrayList<>(); 
    	ClassProperties clProps=ontData.get(classLocalName);
    	if(clProps != null) {
    		return clProps.getProps(rdfType);
    	}
    	return list;
    }
    
    public static ClassProperties getClassAllProps(String classLocalName){
    	return ontData.get(classLocalName);    	
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
        Collections.sort(classes, OntClassComparator);
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
        List<OntClass> rez = new ArrayList<>();
        for (OntClass oc : classes) {
            if (oc.getURI() != null) {
                rez.add(oc);
            }
        }
        Collections.sort(rez, OntClassComparator);
        return rez;
    }
    
    public static List<OntClassModel> getOntRootClasses() {
        List<OntClass> roots = getSimpleRootClasses();
        List<OntClassModel> models = new ArrayList<>();
       
        for (OntClass root : roots) {
            if(!root.isAnon()) {
                models.add(new OntClassModel(root));
            }
        }
        Collections.sort(models,OntClassModelComparator);
        return models;
    }
    
    public static int getNumPrefixes() {
        return MODEL.numPrefixes();
    }
    
    public static String getName() {
        return MODEL.listOntologies().toList().get(0).getLabel(null);
    }
    
    public static int getNumClasses() {
        return MODEL.listClasses().toList().size();
    }
    
    public static int getNumObjectProperties() {
        return MODEL.listObjectProperties().toList().size();
    }
    
    public static int getNumDatatypeProperties() {
        return MODEL.listDatatypeProperties().toList().size();
    }
    
    public static int getNumAnnotationProperties() {
        return MODEL.listAnnotationProperties().toList().size();
    }
    
    public static int getNumRootClasses() {
        return getSimpleRootClasses().size();
    }
    
    public static Comparator<OntClass> OntClassComparator = new Comparator<OntClass>() {

        public int compare(OntClass class1, OntClass class2) {

            String cl1name = class1.getLocalName();
            String cl2name = class2.getLocalName();
            return cl1name.compareTo(cl2name);
        }

    };
    
    public static Comparator<OntClassModel> OntClassModelComparator = new Comparator<OntClassModel>() {

        public int compare(OntClassModel class1, OntClassModel class2) {
            return OntClassComparator.compare(class1.clazz,class2.clazz);
        }

    };
   
}

