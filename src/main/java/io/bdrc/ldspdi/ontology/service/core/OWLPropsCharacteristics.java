package io.bdrc.ldspdi.ontology.service.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.Prefixes;
import io.bdrc.restapi.exceptions.RestException;

public class OWLPropsCharacteristics {

    Model propsModel;
    public static final String OWL_FUNCTIONAL="http://www.w3.org/2002/07/owl#FunctionalProperty";
    public static final String OWL_SYMMETRIC="http://www.w3.org/2002/07/owl#SymmetricProperty";
    public static final String OWL_IRREFLEXIVE="http://www.w3.org/2002/07/owl#IrreflexiveProperty";
    public static final String OWL_INVERSE_FUNCTIONAL="http://www.w3.org/2002/07/owl#InverseFunctionalProperty";
    public static final String OWL_TRANSITIVE="http://www.w3.org/2002/07/owl#TransitiveProperty";
    public static final String OWL_ASYMMETRIC="http://www.w3.org/2002/07/owl#AsymmetricProperty";
    public static final String OWL_REFLEXIVE="http://www.w3.org/2002/07/owl#ReflexiveProperty";

    ArrayList<String> functionalProps;
    ArrayList<String> symmetricProps;
    ArrayList<String> irreflexiveProps;
    ArrayList<String> inverseFunctionalProps;
    ArrayList<String> transitiveProps;
    ArrayList<String> asymmetricProps;
    ArrayList<String> reflexiveProps;

    public OWLPropsCharacteristics(Model m) throws IOException, RestException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(ServiceConfig.class.getClassLoader().getResourceAsStream("arq/OwlProps.arq")));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
        }
        String query=Prefixes.getPrefixesString()+" "+out.toString();
        QueryExecution qexec = QueryExecutionFactory.create(query, m);
        propsModel=qexec.execConstruct();
        reflexiveProps=getReflexiveProps();
    }

    public ArrayList<String> getFunctionalProps(){
        if(functionalProps==null) {
            ResIterator it = propsModel.listResourcesWithProperty(RDF.type,ResourceFactory.createProperty(OWL_FUNCTIONAL));
            functionalProps=new ArrayList<>();
            while(it.hasNext()) {
                functionalProps.add(it.next().getURI());
            }
        }
        return functionalProps;
    }

    public ArrayList<String> getSymmetricProps(){
        if(symmetricProps==null) {
            ResIterator it = propsModel.listResourcesWithProperty(RDF.type,ResourceFactory.createProperty(OWL_SYMMETRIC));
            symmetricProps=new ArrayList<>();
            while(it.hasNext()) {
                symmetricProps.add(it.next().getURI());
            }
        }
        return symmetricProps;
    }

    public ArrayList<String> getIrreflexiveProps(){
        if(irreflexiveProps==null) {
            ResIterator it = propsModel.listResourcesWithProperty(RDF.type,ResourceFactory.createProperty(OWL_IRREFLEXIVE));
            irreflexiveProps=new ArrayList<>();
            while(it.hasNext()) {
                irreflexiveProps.add(it.next().getURI());
            }
        }
        return irreflexiveProps;
    }

    public ArrayList<String> getInverseFunctionalProps(){
        if(inverseFunctionalProps==null) {
            ResIterator it = propsModel.listResourcesWithProperty(RDF.type,ResourceFactory.createProperty(OWL_INVERSE_FUNCTIONAL));
            inverseFunctionalProps=new ArrayList<>();
            while(it.hasNext()) {
                inverseFunctionalProps.add(it.next().getURI());
            }
        }
        return inverseFunctionalProps;
    }

    public ArrayList<String> getTransitiveProps(){
        if(transitiveProps==null) {
            ResIterator it= propsModel.listResourcesWithProperty(RDF.type,ResourceFactory.createProperty(OWL_TRANSITIVE));
            transitiveProps=new ArrayList<>();
            while(it.hasNext()) {
                transitiveProps.add(it.next().getURI());
            }
        }
        return transitiveProps;
    }

    public ArrayList<String> getAsymmetricProps(){
        if(asymmetricProps==null) {
            ResIterator it= propsModel.listResourcesWithProperty(RDF.type,ResourceFactory.createProperty(OWL_ASYMMETRIC));
            asymmetricProps=new ArrayList<>();
            while(it.hasNext()) {
                asymmetricProps.add(it.next().getURI());
            }
        }
        return asymmetricProps;
    }

    public ArrayList<String> getReflexiveProps(){
        if(reflexiveProps==null) {
            ResIterator it=propsModel.listResourcesWithProperty(RDF.type,ResourceFactory.createProperty(OWL_REFLEXIVE));
            reflexiveProps=new ArrayList<>();
            while(it.hasNext()) {
                reflexiveProps.add(it.next().getURI());
            }
        }
        return reflexiveProps;
    }

    public boolean isFunctionalProp(String uri) {
        return getFunctionalProps().contains(uri);
    }

    public boolean isSymmetricProp(String uri) {
        return getSymmetricProps().contains(uri);
    }

    public boolean isIrreflexiveProp(String uri) {
        return getIrreflexiveProps().contains(uri);
    }

    public boolean isInverseFunctionalProp(String uri) {
        return getInverseFunctionalProps().contains(uri);
    }

    public boolean isTransitiveProp(String uri) {
        return getTransitiveProps().contains(uri);
    }

    public boolean isAsymmetricProp(String uri) {
        return getAsymmetricProps().contains(uri);
    }

    public boolean isReflexiveProp(String uri) {
        return getReflexiveProps().contains(uri);
    }

    public ArrayList<String> getOwlProps(String uri) throws RestException{
        ArrayList<String> owlProps=new ArrayList<>();
        if(isFunctionalProp(uri)) {
            owlProps.add(OWL_FUNCTIONAL);
        }
        if(isSymmetricProp(uri)) {
            owlProps.add(OWL_SYMMETRIC);
        }
        if(isIrreflexiveProp(uri)) {
            owlProps.add(OWL_IRREFLEXIVE);
        }
        if(isInverseFunctionalProp(uri)) {
            owlProps.add(OWL_INVERSE_FUNCTIONAL);
        }
        if(isTransitiveProp(uri)) {
            owlProps.add(OWL_TRANSITIVE);
        }
        if(isAsymmetricProp(uri)) {
            owlProps.add(OWL_ASYMMETRIC);
        }
        if(isReflexiveProp(uri)) {
            owlProps.add(OWL_REFLEXIVE);
        }
        return owlProps;
    }

    public String getPrefixed(String uri) {
        if(uri !=null) {
            return "owl:"+uri.substring(uri.lastIndexOf("#")+1);
        }
        return uri;
    }
}
