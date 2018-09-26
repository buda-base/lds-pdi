package io.bdrc.ldspdi.rest.resources;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;

import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

public class MarcExport {

    public static final MarcFactory factory = MarcFactory.newInstance();
    public static final String BDO = "http://purl.bdrc.io/ontology/core/";

    public static String fixIsbin(final String isbn) {
        if (isbn.length() < 10)
            return "0"+isbn;
        return isbn;
    }

    public static String getAuthorStr(final Model m) {
        return null;
    }

    public static Record marcFromMode(final Model m) {
        Record record = factory.newRecord("00000cam a2200000 a 4500");
        return record;
    }

    public static Model getMarcModel(final String resUri) throws RestException {
        Model model = QueryProcessor.getSimpleResourceGraph(resUri, "resForMarc.arq");
        if (model.size() < 1) {
            throw new RestException(404,new LdsError(LdsError.NO_GRAPH_ERR).setContext(resUri));
        }
        final Resource main = model.getResource(resUri);
        boolean test = main.hasLiteral(RDF.type, model.createResource(BDO+"Work"));
        System.out.println(test);
        return model;
    }

    public static Response getResponse(final MediaType mt, final String resUri) throws RestException {
        final Model m = getMarcModel(resUri);
        return null;
    }
}
