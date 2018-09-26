package io.bdrc.ldspdi.rest.resources;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.validator.routines.ISBNValidator;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Leader;
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
    public static final Property workIsbn = ResourceFactory.createProperty(BDO+"workIsbn");
    public static final Property workLccn = ResourceFactory.createProperty(BDO+"workLccn");
    // communicated by Columbia, XML leaders don't need addresses
    public static final String baseLeaderStr = "     nam a22    3ia 4500";
    static final Leader leader = factory.newLeader(baseLeaderStr);
    static final ISBNValidator isbnvalidator = ISBNValidator.getInstance(false);

    // initialize static fields:
    static final ControlField f006 = factory.newControlField("006", "m");
    static final ControlField f007 = factory.newControlField("007", "cr");
    static final DataField f040 = factory.newDataField("040", ' ', ' ');
    static final DataField f336 = factory.newDataField("336", ' ', ' ');
    static final DataField f337 = factory.newDataField("337", ' ', ' ');
    static final DataField f338 = factory.newDataField("338", ' ', ' ');
    static final DataField f533 = factory.newDataField("533", ' ', ' ');
    static final DataField f710_2 = factory.newDataField("710", '2', ' ');

    static {
        f040.addSubfield(factory.newSubfield('a', "NNC"));
        f040.addSubfield(factory.newSubfield('b', "eng"));
        f040.addSubfield(factory.newSubfield('e', "rda"));
        f040.addSubfield(factory.newSubfield('e', "NNC"));
        f336.addSubfield(factory.newSubfield('a', "text"));
        f336.addSubfield(factory.newSubfield('b', "txt"));
        f336.addSubfield(factory.newSubfield('2', "rdacontent"));
        f337.addSubfield(factory.newSubfield('a', "computer"));
        f337.addSubfield(factory.newSubfield('b', "c"));
        f337.addSubfield(factory.newSubfield('2', "rdamedia"));
        f338.addSubfield(factory.newSubfield('a', "online resource"));
        f338.addSubfield(factory.newSubfield('b', "er"));
        f338.addSubfield(factory.newSubfield('2', "rdacarrier"));
        f533.addSubfield(factory.newSubfield('a', "Electronic reproduction"));
        f533.addSubfield(factory.newSubfield('b', "Cambridge, Mass. :"));
        f533.addSubfield(factory.newSubfield('c', "Buddhist Digital Resource Center"));
        f710_2.addSubfield(factory.newSubfield('a', "Buddhist Digital Resource Center"));
    }

    public static boolean indent = true;

    public static String getAuthorStr(final Model m) {
        return null;
    }

    public static void addIsbn(final Model m, final Resource main, final Record r) {
        final StmtIterator si = main.listProperties(workIsbn);
        while (si.hasNext()) {
            final Statement s = si.next();
            final String isbn = s.getLiteral().getString();
            final String validIsbn = isbnvalidator.validate(isbn);
            final DataField df = factory.newDataField("020", ' ', ' ');
            if (validIsbn != null) {
                df.addSubfield(factory.newSubfield('a', validIsbn));
            } else {
                df.addSubfield(factory.newSubfield('z', isbn));
            }
            r.addVariableField(df);
        }
    }

    public static Record marcFromModel(final Model m, final Resource main) {
        final Record record = factory.newRecord(leader);
        // these are static and supplied by Columbia
        record.addVariableField(f006);
        record.addVariableField(f007);
        record.addVariableField(f040);
        record.addVariableField(f336);
        record.addVariableField(f337);
        record.addVariableField(f338);
        record.addVariableField(f533);
        record.addVariableField(f710_2);
        // maybe something like that could work?
        //record.addVariableField(factory.newControlField("003", "BDRC"));
        // these depend on the model
        final DataField f588 = factory.newDataField("588", ' ', ' ');
        // TODO: replace [date]
        f588.addSubfield(factory.newSubfield('a', "Description based on online resource viewed on [date]; title from title page."));
        record.addVariableField(f588);
        record.addVariableField(factory.newControlField("001", "(BDRC)"+main.getURI()));
        final DataField f856 = factory.newDataField("856", '4', '0');
        f856.addSubfield(factory.newSubfield('u', main.getURI()));
        f856.addSubfield(factory.newSubfield('z', "Available from BDRC"));
        record.addVariableField(f856);
        // lccn
        final StmtIterator si = main.listProperties(workLccn);
        while (si.hasNext()) {
            final String lccn = si.next().getLiteral().getString();
            final DataField f776_08 = factory.newDataField("776", '0', '8');
            f776_08.addSubfield(factory.newSubfield('w', "(DLC)   [LCCN] "+lccn));
            f776_08.addSubfield(factory.newSubfield('i', "Electronic reproduction of (manifestation)"));
            record.addVariableField(f776_08);
        }
        addIsbn(m, main, record);
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
