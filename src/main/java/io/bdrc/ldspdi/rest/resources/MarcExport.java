package io.bdrc.ldspdi.rest.resources;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;

import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

public class MarcExport {

    public static final MarcFactory factory = MarcFactory.newInstance();
    public static final String BDO = "http://purl.bdrc.io/ontology/core/";
    public static final Property partOf = ResourceFactory.createProperty(BDO+"workPartOf");
    public static final Property hasExpression = ResourceFactory.createProperty(BDO+"workHasExpression");

    public static boolean indent = true;

    public static String fixIsbin(final String isbn) {
        if (isbn.length() < 10)
            return "0"+isbn;
        return isbn;
    }

    public static String getAuthorStr(final Model m) {
        return null;
    }

    public static Record marcFromModel(final Model m, final Resource main) {
        Record record = factory.newRecord();
        return record;
    }

    public static Model getMarcModel(final String resUri) throws RestException {
        Model model = QueryProcessor.getSimpleResourceGraph(resUri, "resForMarc.arq");
        if (model.size() < 1) {
            throw new RestException(404,new LdsError(LdsError.NO_GRAPH_ERR).setContext(resUri));
        }
        final Resource main = model.getResource(resUri);
        final Resource type = main.getPropertyResourceValue(RDF.type);
        if (!type.getLocalName().equals("Work")) {
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setMsg("Resource is not a Work"));
        }
        if (main.hasProperty(partOf)) {
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setMsg("Resource is part of another Work"));
        }
        if (main.hasProperty(hasExpression)) {
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setMsg("Resource is an abstract Work"));
        }
        return model;
    }

    public static Response getResponse(final MediaType mt, final String resUri) throws RestException {
        final Model m = getMarcModel(resUri);
        final Resource main = m.getResource(resUri);
        final Record r = marcFromModel(m, main);
        final StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(final OutputStream os) throws IOException, WebApplicationException {
                final MarcXmlWriter writer = new MarcXmlWriter(os, indent);
                writer.write(r);
                writer.close();
            }
        };
        final ResponseBuilder builder = Response.ok(stream);
        builder.header("Allow", "GET, OPTIONS, HEAD");
        builder.header("Content-Type", mt);
        builder.header("Vary", "Negotiate, Accept");
        return builder.build();
    }
}
