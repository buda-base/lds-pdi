package io.bdrc.ontology.service.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.Prefixes;

public class OntData {
    
    public static InfModel infMod;    
    public static OntModel ontMod;
    public final static Logger log=LoggerFactory.getLogger(OntData.class.getName());
    
    public static void init() {
        try {
            
            log.info("URL >> "+ServiceConfig.getProperty("owlURL"));
            HttpURLConnection connection = (HttpURLConnection) new URL(ServiceConfig.getProperty("owlURL")).openConnection();
            InputStream stream=connection.getInputStream();
            Model m = ModelFactory.createDefaultModel();
            m.read(stream, "", "RDF/XML");
            stream.close();
            ontMod = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, m); 
            rdf10tordf11(ontMod);
    
        } catch (IOException io) {
            log.error("Error initializing OntModel", io);            
        }
    }
    
    public static ArrayList<OntResource> getDomainUsages(String uri) {        
        String query=ServiceConfig.getPrefixes()+ " select distinct ?s where {\n" + 
                "    ?s rdfs:domain <"+uri+"> .    \n" + 
                "} order by ?p ?s";
        QueryExecution qexec = QueryExecutionFactory.create(query, ontMod);
        ResultSet res = qexec.execSelect() ;        
        ArrayList<OntResource> list=new ArrayList<>();
        while(res.hasNext()) {
            QuerySolution qs=res.next();
            RDFNode node=qs.get("?s");
            list.add(ontMod.getOntResource(node.asResource().getURI()));            
        }        
        return list;
    }
    
    public static ArrayList<OntResource> getRangeUsages(String uri) {        
        String query=ServiceConfig.getPrefixes()+ " select distinct ?s ?p where {\n" + 
                "    ?s rdfs:range <"+uri+"> .    \n" + 
                "} order by ?p ?s";
        QueryExecution qexec = QueryExecutionFactory.create(query, ontMod);
        ResultSet res = qexec.execSelect() ;
        ArrayList<OntResource> list=new ArrayList<>();
        while(res.hasNext()) {
            QuerySolution qs=res.next();
            RDFNode node=qs.get("?s");
            list.add(ontMod.getOntResource(node.asResource().getURI()));            
        }        
        return list;
    }
    
    public static ArrayList<OntResource> getSubProps(String uri) {        
        String query=ServiceConfig.getPrefixes()+ " select distinct ?s ?p where {\n" + 
                "    ?s rdfs:subPropertyOf <"+uri+"> .    \n" + 
                "} order by ?p ?s";
        QueryExecution qexec = QueryExecutionFactory.create(query, ontMod);
        ResultSet res = qexec.execSelect() ;
        ArrayList<OntResource> list=new ArrayList<>();
        while(res.hasNext()) {
            QuerySolution qs=res.next();
            RDFNode node=qs.get("?s");
            list.add(ontMod.getOntResource(node.asResource().getURI()));            
        }        
        return list;
    }
    
    public static ArrayList<OntResource> getParentProps(String uri) {        
        String query=ServiceConfig.getPrefixes()+ " select distinct ?s where {\n" + 
                "   <"+uri+"> rdfs:subPropertyOf ?s .    \n" + 
                "} order by ?s";
        QueryExecution qexec = QueryExecutionFactory.create(query, ontMod);
        ResultSet res = qexec.execSelect() ;
        ArrayList<OntResource> list=new ArrayList<>();
        while(res.hasNext()) {
            QuerySolution qs=res.next();
            RDFNode node=qs.get("?s");
            list.add(ontMod.getOntResource(node.asResource().getURI()));            
        }        
        return list;
    }
    
    public static ArrayList<OntResource> getSubClassesOf(String uri) {        
        String query=ServiceConfig.getPrefixes()+ " select distinct ?s where {\n" + 
                "   <"+uri+"> rdfs:subClassOf ?s .    \n" + 
                "} order by ?s";
        QueryExecution qexec = QueryExecutionFactory.create(query, ontMod);
        ResultSet res = qexec.execSelect() ;
        ArrayList<OntResource> list=new ArrayList<>();
        while(res.hasNext()) {
            QuerySolution qs=res.next();
            RDFNode node=qs.get("?s");
            list.add(ontMod.getOntResource(node.asResource().getURI()));            
        }        
        return list;
    }
    
    public static HashMap<String,ArrayList<OntResource>> getAllSubProps(String uri) {        
        ArrayList<OntResource> props=OntData.getDomainUsages(uri);
        HashMap<String,ArrayList<OntResource>> map=new HashMap<>();
        for(OntResource rs:props) {
            ArrayList<OntResource> l=OntData.getSubProps(rs.getURI());
            if(l.size()>1) {
                map.put(rs.getURI(), l);
            }            
        }
        return map;
    }
    
    public static boolean isClass(String uri) {
        return ontMod.getOntResource(uri).isClass();
    }
    
    public static int getNumPrefixes() {
        return ontMod.numPrefixes();
    }
    
    public static Map<String,String> getPrefixMap(){
        return ontMod.getNsPrefixMap();
    }
    
    public static int getNumRootClasses() {
        return getSimpleRootClasses().size();
    }
    
    /**
     * Returns a list of simple root OntClass(es). Simple means not defined as a Union or Restriction
     * and so on. The purpose is to provide the roots of a traversal of classes defined in the ontology.
     * 
     * @return list of simple root OntClass(es)
     */    
    public static List<OntClass> getSimpleRootClasses() {
        List<OntClass> classes = ontMod.listHierarchyRootClasses().toList();
        List<OntClass> rez = new ArrayList<>();
        for (OntClass oc : classes) {
            if (oc.getURI() != null) {
                rez.add(oc);
            }
        }
        Collections.sort(rez, OntData.ontClassComparator);
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
        Collections.sort(models,OntData.ontClassModelComparator);
        return models;
    }
    
    public static ArrayList<OntClass> getAllClasses(){
        ExtendedIterator<OntClass> it=ontMod.listClasses();
        ArrayList<OntClass> classes=new ArrayList<>();
        while(it.hasNext()) {
            OntClass ocl=it.next();
            if(ocl !=null && !ocl.isAnon()) {
                classes.add(ocl);
            }
            ocl=null;
        }
        Collections.sort(classes, OntData.ontClassComparator);
        return classes;
    }
    
    public static ArrayList<OntProperty> getAllProps(){
        ExtendedIterator<OntProperty> it=ontMod.listAllOntProperties();
        ArrayList<OntProperty> list=new ArrayList<>();
        while(it.hasNext()) {
            OntProperty pr=it.next();
            if(pr!=null && pr.isProperty()) {
                list.add(pr);
            }
        }
        Collections.sort(list, OntData.propComparator);
        return list;
    }
    
    public static List<Individual> getAllIndividuals(){
        List<Individual> indv =ontMod.listIndividuals().toList();
        Collections.sort(indv, individualComparator);
        return indv;
    }
    
    public final static Comparator<OntClass> ontClassComparator = new Comparator<OntClass>() {

        public int compare(OntClass class1, OntClass class2) { 
            if(Prefixes.getPrefix(class1.getNameSpace()).equals(Prefixes.getPrefix(class2.getNameSpace()))) {
                return class1.getLocalName().compareTo(class2.getLocalName());
            }
            return Prefixes.getPrefix(class1.getNameSpace()).compareTo(Prefixes.getPrefix(class2.getNameSpace()));            
        }

    };
    
    public final static Comparator<OntClassModel> ontClassModelComparator = new Comparator<OntClassModel>() {

        public int compare(OntClassModel class1, OntClassModel class2) {
            return ontClassComparator.compare(class1.clazz,class2.clazz);
        }

    };
    
    public final static Comparator<Individual> individualComparator = new Comparator<Individual>() {

        public int compare(Individual class1, Individual class2) {
            return class1.getLocalName().compareTo(class2.getLocalName());
        }

    };
    
    public final static Comparator<OntProperty> propComparator = new Comparator<OntProperty>() {

        public int compare(OntProperty prop1, OntProperty prop2) {
            
            if(Prefixes.getPrefix(prop1.getNameSpace()).equals(Prefixes.getPrefix(prop2.getNameSpace()))) {
                return prop1.getLocalName().compareTo(prop2.getLocalName());
            }
            return Prefixes.getPrefix(prop1.getNameSpace()).compareTo(Prefixes.getPrefix(prop2.getNameSpace()));
        }

    };
    
    public static void rdf10tordf11(OntModel o) {
        Resource RDFPL = o.getResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral");
        Resource RDFLS = o.getResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString");
        ExtendedIterator<DatatypeProperty> it = o.listDatatypeProperties();
        while(it.hasNext()) {
            DatatypeProperty p = it.next();
            if (p.hasRange(RDFPL)) {
                p.removeRange(RDFPL);
                p.addRange(RDFLS);
            }
        }
        ExtendedIterator<Restriction> it2 = o.listRestrictions();
        while(it2.hasNext()) {
            Restriction r = it2.next();
            Statement s = r.getProperty(OWL2.onDataRange); // is that code obvious? no
            if (s != null && s.getObject().asResource().equals(RDFPL)) {
                s.changeObject(RDFLS);

            }
        }
    } 

}
