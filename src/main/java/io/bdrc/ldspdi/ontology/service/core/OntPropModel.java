package io.bdrc.ldspdi.ontology.service.core;

import java.util.ArrayList;

import org.apache.jena.ontology.InverseFunctionalProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.SymmetricProperty;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.restapi.exceptions.RestException;

public class OntPropModel {

    final static Logger log = LoggerFactory.getLogger(OntPropModel.class.getName());

    public static final String DOMAIN="http://www.w3.org/2000/01/rdf-schema#domain";
    public static final String RANGE="http://www.w3.org/2000/01/rdf-schema#range";
    public static final String LABEL="http://www.w3.org/2000/01/rdf-schema#label";
    public static final String COMMENT="http://www.w3.org/2000/01/rdf-schema#comment";
    public static final String TYPE="http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    public String uri;
    public String name;
    public String rdfType;
    public String rdfTypeUri;
    public String label;
    public String labelLang;
    public String range;
    public String rangeUri;
    public String domain;
    public String domainUri;
    public String comment;
    public String commentLang;

    public OntPropModel(String uri) {
        this.uri=uri;
        this.name=OntData.ontMod.shortForm(uri);
        StmtIterator it=((Model)OntData.ontMod).listStatements(
        ResourceFactory.createResource(uri),(Property)null,(RDFNode)null);
        while(it.hasNext()) {
            Statement st=it.next();
            String pred=st.getPredicate().getURI();
            switch(pred) {
                case DOMAIN:
                    this.domainUri=st.getObject().asNode().getURI();
                    this.domain=OntData.ontMod.shortForm(domainUri);
                    break;
                case RANGE:
                    this.rangeUri=st.getObject().asNode().getURI();
                    this.range=OntData.ontMod.shortForm(rangeUri);
                    break;
                case LABEL:
                    this.label=st.getObject().asLiteral().getString();
                    this.labelLang=st.getObject().asLiteral().getLanguage();
                    break;
                case COMMENT:
                    this.comment=st.getObject().asLiteral().getString();
                    this.commentLang=st.getObject().asLiteral().getLanguage();
                    break;
                case TYPE:
                    this.rdfTypeUri=st.getObject().asNode().getURI();
                    this.rdfType=OntData.ontMod.shortForm(rdfTypeUri);
                    break;
            }
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

    public String getSymmetricProp() {
        SymmetricProperty prop=OntData.ontMod.getSymmetricProperty(getUri());
        System.out.println("Sym Prop >< "+prop);
        System.out.println("Sym Prop ? >< "+prop.isSymmetricProperty());
        System.out.println("Sym Prop datatype ? >< "+prop.asProperty().getRDFType().getURI());
        if(prop!=null) {
            return prop.asProperty().getRDFType().getURI();
        }
        return null;
    }

    public String getInverseProp() {
        InverseFunctionalProperty prop=OntData.ontMod.getInverseFunctionalProperty(getUri());
        if(prop!=null) {
            System.out.println("Sym Prop >< "+prop);
            System.out.println("Sym Prop ? >< "+prop.isInverseFunctionalProperty());
            System.out.println("Sym Prop datatype ? >< "+prop.getInverse().getURI());
            return prop.asProperty().getRDFType().getURI();
        }
        return null;
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
        if(range==null) {
            return "Inherited";
        }
        return range;
    }

    public String getDomain() {
        if(domain==null) {
            return "Inherited";
        }
        return domain;
    }

    public boolean isDomainInherited() {
        return getDomain().equals("Inherited");
    }

    public boolean isRangeInherited() {
        return getRange().equals("Inherited");
    }

    public String getComment() {
        return comment;
    }

    public String getLabelLang() {
        return labelLang;
    }

    public String getCommentLang() {
        return commentLang;
    }

    public String getRdfTypeUri() {
        return rdfTypeUri;
    }

    public String getRangeUri() {
        return rangeUri;
    }

    public String getDomainUri() {
        return domainUri;
    }

    @Override
    public String toString() {
        return "OntPropModel [uri=" + uri + ", name=" + name + ", rdfType=" + rdfType + ", label=" + label + ", range="
                + range + ", domain=" + domain + "]";
    }

}
