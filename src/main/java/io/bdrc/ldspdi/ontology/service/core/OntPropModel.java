package io.bdrc.ldspdi.ontology.service.core;

import java.util.ArrayList;

import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.restapi.exceptions.RestException;

public class OntPropModel {
    
    final static Logger log = LoggerFactory.getLogger(OntPropModel.class.getName());
    
    public String uri;
    public String name;
    public String rdfType;
    public String label;
    public String range;
    public String domain;
    
    public OntPropModel(String uri) {
        this.uri=uri;
        OntProperty prop=OntData.ontMod.getOntResource(uri).asProperty();
        this.rdfType=prop.getRDFType().getLocalName();
        this.name=prop.getLocalName();
        String lab=prop.getLabel(null);
        if(lab!=null) {
            this.label=prop.getLabel(null);
        }else {
            this.label=name;
        }
        OntResource dom=prop.getDomain();
        if(dom!=null) {
            this.domain=dom.getLocalName();
        }else {
            this.domain="Inherited";
        }        
        OntResource ontRes =prop.getRange();
        if(ontRes!=null) {
            this.range=ontRes.getLocalName();
        }else {
            this.range="Inherited";
        }
    }
    
    public ArrayList<OntPropModel> getAllSubProps() throws RestException{
        ArrayList<OntResource> res= OntData.getSubProps(uri);
        ArrayList<OntPropModel> list=new ArrayList<>();
        for(OntResource r:res) {
            list.add(new OntPropModel(r.getURI()));
        }
        return list;
    }
    
    public ArrayList<OntPropModel> getParentProps() throws RestException{
        ArrayList<OntResource> res= OntData.getParentProps(uri);
        ArrayList<OntPropModel> list=new ArrayList<>();
        for(OntResource r:res) {
            list.add(new OntPropModel(r.getURI()));
        }
        return list;
    }

    public String getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public String getRdfType() {
        return rdfType;
    }

    public String getLabel() {
        return label;
    }

    public String getRange() {
        return range;
    }

    public String getDomain() {
        return domain;
    }
    
    
}
