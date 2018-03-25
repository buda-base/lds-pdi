package io.bdrc.ontology.service.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.UnionClass;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OntClassParent {
    
    /*
     * Wraps two types of OntClass parent class : normal superclass and anonymous superclass being
     * the union of several non anonymous classes 
     */
    boolean isAnonymous;
    OntClass sup3r;
    String uri;
    
    final static Logger log = LoggerFactory.getLogger(OntClassModel.class.getName());
        
    public OntClassParent(String uri) {
        this.uri=uri;
        sup3r = OntData.ontMod.getOntClass(uri).getSuperClass();
        log.info("Get Super Class >>"+OntData.ontMod.getOntClass(uri).listSuperClasses(false).toList());
        isAnonymous=sup3r.isAnon();
    }
    
    public ArrayList<OntClassModel> getParents(){
        ArrayList<OntClassModel> list=new ArrayList<>();
        if(isAnonymous) {
            UnionClass uc=sup3r.asUnionClass();
            List<RDFNode> sups = uc.getOperands().asJavaList();
            for(RDFNode node:sups) {
                list.add(new OntClassModel(node.asResource().getURI()));
            }            
        }else {
            list.add(new OntClassModel(sup3r));
        }
        return list;
    }

}
