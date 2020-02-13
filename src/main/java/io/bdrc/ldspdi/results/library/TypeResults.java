package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.results.Field;

public class TypeResults {

    public final static Logger log = LoggerFactory.getLogger(TypeResults.class);

    public final static Property tmpIsMain = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/tmp/isMain");

    public static Map<String, Object> getResultsMap(final Model mod) throws RestException {
        long start = 0;
        if (log.isDebugEnabled())
            start = System.nanoTime();
        Map<String, Object> res = new HashMap<>();
        Map<String, ArrayList<Field>> main = new HashMap<>();
        Map<String, ArrayList<Field>> aux = new HashMap<>();
        Map<Resource, Boolean> isMain = new HashMap<>();
        final StmtIterator mainIt = mod.listLiteralStatements(null, tmpIsMain, true);
        while (mainIt.hasNext()) {
            final Resource mainRes = mainIt.next().getSubject();
            isMain.put(mainRes, true);
        }
        if (log.isDebugEnabled())
            log.debug("InstanceResults.getResultMap(), checkpoint1: {}", (System.nanoTime() - start) / 1000);
        StmtIterator allIterator = mod.listStatements();
        while (allIterator.hasNext()) {
            final Statement st = allIterator.next();

            if (st.getPredicate().equals(tmpIsMain))
                continue;

            final Resource subject = st.getSubject();
            List<Field> stlist;
            if (isMain.getOrDefault(subject, false)) {
                stlist = main.computeIfAbsent(subject.getURI(), x -> new ArrayList<Field>());
            } else {
                stlist = aux.computeIfAbsent(subject.getURI(), x -> new ArrayList<Field>());
            }
            stlist.add(Field.getField(st));

        }
        res.put("main", main);
        res.put("aux", aux);
        return res;
    }
}
