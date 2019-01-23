package io.bdrc.shacl.validation;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.topbraid.shacl.model.SHFactory;
import org.topbraid.shacl.model.SHNodeShape;
import org.topbraid.shacl.vocabulary.SH;

import io.bdrc.ldspdi.ontology.service.core.OntData;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.Prefixes;
import io.bdrc.restapi.exceptions.RestException;

public class ShapeBuilder {

    public static String BDRC_SHAPE="http://purl.bdrc.io/shacl/core/shape";
    public static String BDRC_SHAPE_PREFIX="rsh";

    public SHNodeShape nodeShape;
    public String uri;

    public ShapeBuilder(String iri) {
        Model shapeModel=ModelFactory.createDefaultModel();
        shapeModel.setNsPrefixes(Prefixes.getPrefixMapping());
        this.uri=BDRC_SHAPE+"/"+iri.substring(iri.lastIndexOf('/')+1)+"Shape";
        Statement res = ResourceFactory.createStatement(ResourceFactory.createResource(uri),
                RDF.type, SH.NodeShape);
        shapeModel.add(res);
        shapeModel.add(ResourceFactory.createStatement(ResourceFactory.createResource(uri),
                SH.targetClass,
                ResourceFactory.createResource(iri)));
        nodeShape=SHFactory.asNodeShape(shapeModel.asRDFNode(ResourceFactory.createResource(uri).asNode()));
    }

    public SHNodeShape getNodeShape() {
        return nodeShape;
    }

    public String getUri() {
        return uri;
    }

    public static void main(String[] args) throws RestException {
        ServiceConfig.initForTests("http://buda1.bdrc.io:13180/fuseki/bdrcrw/query");
        OntData.init();
        String iri="http://purl.bdrc.io/ontology/core/Topic";
        ShapeBuilder builder=new ShapeBuilder(iri);
        builder.getNodeShape().getModel().write(System.out,"TURTLE");
    }

}
