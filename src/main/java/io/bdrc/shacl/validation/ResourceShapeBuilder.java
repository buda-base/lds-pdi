package io.bdrc.shacl.validation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;

import io.bdrc.ldspdi.ontology.service.core.OWLPropsCharacteristics;
import io.bdrc.ldspdi.ontology.service.core.OntData;
import io.bdrc.ldspdi.ontology.service.core.OntPropModel;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.Prefixes;
import io.bdrc.restapi.exceptions.RestException;

public class ResourceShapeBuilder {

    String uri;
    Model shapeModel;

    public ResourceShapeBuilder(String iri) throws RestException {
        shapeModel=ModelFactory.createDefaultModel();
        shapeModel.setNsPrefixes(Prefixes.getPrefixMapping());
        this.uri=SHACL.BDRC_SHAPE+"/"+iri.substring(iri.lastIndexOf('/')+1)+"Shape";
        Statement res = ResourceFactory.createStatement(ResourceFactory.createResource(uri),
                RDF.type, ResourceFactory.createResource(SHACL.URL_ROOT+SHACL.NODE_SHAPE));
        shapeModel.add(res);
        shapeModel.add(ResourceFactory.createStatement(ResourceFactory.createResource(uri),
                ResourceFactory.createProperty(SHACL.URL_ROOT+SHACL.TARGET_CLASS),
                ResourceFactory.createResource(iri)));
        shapeModel.add(ResourceFactory.createStatement(ResourceFactory.createResource(uri),
                ResourceFactory.createProperty(SHACL.URL_ROOT+SHACL.CLOSED),
                ResourceFactory.createTypedLiteral("true", XSDDatatype.XSDboolean)));
        List<OntProperty> props=OntData.getAllClassProps(iri);
        for(OntProperty pr:props) {
            Resource rss=null;
            ExtendedIterator<? extends OntProperty> it=pr.listSubProperties();
            if(it.hasNext()) {
                String prUri=pr.getURI();
                OntPropModel prMod=new OntPropModel(prUri);
                //System.out.println("!!!!!!!!!!! >> "+pr+ " PR >>> "+prMod.getRange()+" <<isObject >>"+pr.isObjectProperty()+" <<isDataType >>"+pr.isDatatypeProperty());
                rss= ResourceFactory.createResource(SHACL.BDRC_SHAPE+"/"+prUri.substring(prUri.lastIndexOf('/')+1));
                Statement stmp = ResourceFactory.createStatement(rss,
                        RDF.type,
                        ResourceFactory.createProperty(SHACL.URL_ROOT+SHACL.PROPERTY_GROUP));
                shapeModel.add(stmp);
                //System.out.println("PROP GROUP >>"+stmp);
                while(it.hasNext()) {
                    //System.out.println("NEXT >>"+it.next());
                    OntProperty opp=it.next();
                    OntPropModel prMod1=new OntPropModel(opp.getURI());
                    Resource rs= ResourceFactory.createResource();
                    Statement tmp = ResourceFactory.createStatement(ResourceFactory.createResource(uri),
                            ResourceFactory.createProperty(SHACL.URL_ROOT+SHACL.PROPERTY),
                            rs);
                    Statement tmp1 = ResourceFactory.createStatement(rs,
                            ResourceFactory.createProperty(SHACL.URL_ROOT+SHACL.PATH),
                            ResourceFactory.createResource(opp.getURI()));
                    Statement tmp2 = ResourceFactory.createStatement(rs,
                            ResourceFactory.createProperty(SHACL.URL_ROOT+SHACL.GROUP),
                            rss);
                    shapeModel.add(tmp);
                    shapeModel.add(tmp1);
                    shapeModel.add(tmp2);
                    ArrayList<String> range=prMod1.getRange();
                    if(prMod1.isRangeInherited()) {
                        range=prMod.getRange();
                    }
                    for(String r:range) {
                        Statement stmt=null;
                        //System.out.println("RANGE STRING >> for pr "+prMod1.getUri()+ " >>" +r);
                        OntClass cl=OntData.ontMod.getOntClass(r);
                        // System.out.println("RANGE SUB Object >> for pr "+prMod1+ " >>" +cl);
                        if(cl!=null) {
                            stmt = ResourceFactory.createStatement(rs,
                                    ResourceFactory.createProperty(SHACL.URL_ROOT+SHACL.CLASS),
                                    ResourceFactory.createResource(cl.getURI()));

                        }else {
                            stmt = ResourceFactory.createStatement(rs,
                                    ResourceFactory.createProperty(SHACL.URL_ROOT+SHACL.DATATYPE),
                                    ResourceFactory.createResource(r));
                        }
                        shapeModel.add(stmt);
                    }
                }
           }else
               {
                    OntPropModel prMod=new OntPropModel(pr.getURI());
                    Resource rs= ResourceFactory.createResource();
                    Statement tmp = ResourceFactory.createStatement(ResourceFactory.createResource(uri),
                            ResourceFactory.createProperty(SHACL.URL_ROOT+SHACL.PROPERTY),
                            rs);
                    Statement tmp1 = ResourceFactory.createStatement(rs,
                            ResourceFactory.createProperty(SHACL.URL_ROOT+SHACL.PATH),
                            ResourceFactory.createResource(pr.getURI()));
                    shapeModel.add(tmp);
                    shapeModel.add(tmp1);
                    ArrayList<String> range=prMod.getRange();
                    //System.out.println("RANGE INHERITED BASIC >> "+prMod.isRangeInherited());
                    for(String r:range) {
                        Statement stmt=null;
                        if(pr.isAnnotationProperty()&& prMod.isRangeInherited()) {
                            stmt = ResourceFactory.createStatement(rs,
                                    ResourceFactory.createProperty(SHACL.URL_ROOT+SHACL.DATATYPE),
                                    ResourceFactory.createResource(OWLPropsCharacteristics.OWL_ANNOTATION_PROP));
                            shapeModel.add(stmt);
                            break;
                        }
                        //System.out.println("RANGE Object >> for pr: "+prMod+ "  >>> "+OntData.ontMod.getOntClass(r));
                        OntClass cl=OntData.ontMod.getOntClass(r);
                        if(cl!=null) {
                            stmt = ResourceFactory.createStatement(rs,
                                    ResourceFactory.createProperty(SHACL.URL_ROOT+SHACL.CLASS),
                                    ResourceFactory.createResource(r));

                        }else {
                            stmt = ResourceFactory.createStatement(rs,
                                    ResourceFactory.createProperty(SHACL.URL_ROOT+SHACL.DATATYPE),
                                    ResourceFactory.createResource(r));
                        }
                        shapeModel.add(stmt);
                    }
            }
        }
    }

    public String getUri() {
        return uri;
    }

    public Model getShapeModel() {
        return shapeModel;
    }

    public static void main(String[] args) throws RestException, IOException {
        ServiceConfig.initForTests("http://buda1.bdrc.io:13180/fuseki/bdrcrw/query");
        OntData.init(null);
        List<OntResource> classes=OntData.getSubClassesOf("http://purl.bdrc.io/ontology/core/Entity");
        for(OntResource om:classes) {
            System.out.println("ROOT >>"+om.getURI());
            String iri=om.getURI();
            ResourceShapeBuilder builder=new ResourceShapeBuilder(iri);
            builder.getShapeModel().write(System.out, "TURTLE");
            //System.out.println(OntData.getAllClassProps(iri));
            File f=new File("src/main/resources/"+iri.substring(iri.lastIndexOf("/")+1)+"Shape.ttl");
            FileOutputStream fos=new FileOutputStream(f);
            builder.getShapeModel().write(fos, "TURTLE");
            System.out.println("write ?"+f.getAbsolutePath());
            fos.close();
        }

    }

}
