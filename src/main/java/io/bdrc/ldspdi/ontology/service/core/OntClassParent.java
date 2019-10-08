package io.bdrc.ldspdi.ontology.service.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.UnionClass;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OntClassParent {

    /*
     * Wraps two types of OntClass parent class : normal superclass and anonymous
     * superclass being the union of several non anonymous classes
     */
    boolean isAnonymous;
    OntClass sup3r;
    String uri;
    boolean global;

    final static Logger log = LoggerFactory.getLogger("default");

    public OntClassParent(String uri, boolean global) {
        this.uri = uri;
        this.global = global;
        if (global) {
            sup3r = OntData.ontAllMod.getOntClass(uri).getSuperClass();
        } else {
            sup3r = OntData.ontMod.getOntClass(uri).getSuperClass();
        }
        isAnonymous = sup3r.isAnon();
    }

    public ArrayList<OntClassModel> getParents() {
        ArrayList<OntClassModel> list = new ArrayList<>();
        if (isAnonymous) {
            UnionClass uc = sup3r.asUnionClass();
            List<RDFNode> sups = uc.getOperands().asJavaList();
            for (RDFNode node : sups) {
                list.add(new OntClassModel(node.asResource().getURI(), global));
            }
        } else {
            list.add(new OntClassModel(sup3r));
        }
        return list;
    }

}
