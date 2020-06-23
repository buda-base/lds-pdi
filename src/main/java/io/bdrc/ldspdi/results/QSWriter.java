package io.bdrc.ldspdi.results;

import java.util.List;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;

import com.opencsv.CSVWriter;

import io.bdrc.ldspdi.service.ServiceConfig;

public class QSWriter {

    public static void writeCsvSimple(final QuerySolution qs, final List<String> vars, final CSVWriter writer) {
        final int nbVars = vars.size();
        String[] csvrow = new String[nbVars];
        int varIdx = 0;
        for (String key : vars) {
            RDFNode node = qs.get(key);
            if (node == null) {
                csvrow[varIdx] = "";
            } else if (node.isURIResource()) {
                csvrow[varIdx] = ServiceConfig.PREFIX.getPrefixedIRI(node.asResource().getURI());
            } else if (node.isLiteral()) {
                csvrow[varIdx] = node.asLiteral().getValue().toString();
            } else if (node.isAnon()) {
                csvrow[varIdx] = "_:" + node.toString();
            }
            varIdx += 1;
        }
        writer.writeNext(csvrow);
    }

    public static void writeCsvFull(final QuerySolution qs, final List<String> vars, final CSVWriter writer) {
        final int nbVars = vars.size();
        String[] csvrow = new String[3 * nbVars];
        int varIdx = 0;
        for (String key : vars) {
            RDFNode node = qs.get(key);
            if (node == null) {
                csvrow[3 * varIdx] = "";
                csvrow[3 * varIdx + 1] = "";
                csvrow[3 * varIdx + 2] = "";
            } else if (node.isURIResource()) {
                csvrow[3 * varIdx] = node.asResource().getURI();
                csvrow[3 * varIdx + 1] = "@id";
                csvrow[3 * varIdx + 2] = "";
            } else if (node.isLiteral()) {
                final Literal l = node.asLiteral();
                csvrow[3 * varIdx] = l.getValue().toString();
                final String type = l.getDatatypeURI();
                csvrow[3 * varIdx + 1] = (type == null) ? "" : type;
                final String lang = l.getLanguage();
                csvrow[3 * varIdx + 2] = (lang == null) ? "" : lang;
            } else if (node.isAnon()) {
                csvrow[3 * varIdx] = "_:" + node.toString();
                csvrow[3 * varIdx + 1] = "@id";
                csvrow[3 * varIdx + 2] = "";
            }
            varIdx += 1;
        }
        writer.writeNext(csvrow);
    }
}
