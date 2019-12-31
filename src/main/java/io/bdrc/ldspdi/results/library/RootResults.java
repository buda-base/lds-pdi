package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.results.Field;

public class RootResults {

    public final static Logger log = LoggerFactory.getLogger(RootResults.class);

    public static Map<String, Object> getResultsMap(Model mod, int etext_count) throws RestException {
        final Map<String, Object> res = new HashMap<>();
        final Map<String, Integer> count = new HashMap<>();
        final ArrayList<String> processed = new ArrayList<>();
        //count.put("http://purl.bdrc.io/ontology/core/Etext", etext_count);
        Selector selector = new SimpleSelector(null, RDF.type, (Node)null);
        final StmtIterator it = mod.listStatements(selector);
        while (it.hasNext()) {
            final Statement st = it.next();
            final String sUri = st.getSubject().getURI();
            final String oUri = st.getObject().asResource().getURI();
            Integer ct = count.getOrDefault(oUri, 0);
            if (!processed.contains(sUri)) {
                count.put(oUri, ct.intValue() + 1);
            }
            if (!st.getPredicate().getURI().equals(RDF.type.getURI())) {
                @SuppressWarnings("unchecked")
                Map<String, ArrayList<Field>> map = (Map<String, ArrayList<Field>>) res.computeIfAbsent(oUri, x -> new HashMap<String, ArrayList<Field>>());
                ArrayList<Field> wl = map.get(st.getSubject().getURI());
                if (wl == null) {
                    wl = new ArrayList<Field>();
                }
                wl.add(Field.getField(st));
                map.put(st.getSubject().getURI(), wl);
            }
            processed.add(st.getSubject().getURI());
        }
        return res;
    }

}