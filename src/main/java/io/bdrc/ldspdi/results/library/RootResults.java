package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.results.Field;
import io.bdrc.restapi.exceptions.RestException;

public class RootResults {

    public final static Logger log = LoggerFactory.getLogger(RootResults.class);

    public static HashMap<String, Object> getResultsMap(Model mod, int etext_count) throws RestException {
        final HashMap<String, Object> res = new HashMap<>();
        final HashMap<String, Integer> count = new HashMap<>();
        final ArrayList<String> processed = new ArrayList<>();
        count.put("http://purl.bdrc.io/ontology/core/Etext", etext_count);
        final StmtIterator it = mod.listStatements();
        while (it.hasNext()) {
            final Statement st = it.next();
            final String type = mod.getProperty(st.getSubject(), RDF.type).getObject().asResource().getURI();
            Integer ct = count.get(type);
            if (!processed.contains(st.getSubject().getURI())) {
                if (ct != null) {
                    count.put(type, ct.intValue() + 1);
                } else {
                    count.put(type, 1);
                }
            }
            if (!st.getPredicate().getURI().equals(RDF.type.getURI())) {
                @SuppressWarnings("unchecked")
                HashMap<String, ArrayList<Field>> map = (HashMap<String, ArrayList<Field>>) res.get(type);
                if (map == null) {
                    map = new HashMap<String, ArrayList<Field>>();
                }
                ArrayList<Field> wl = map.get(st.getSubject().getURI());
                if (wl == null) {
                    wl = new ArrayList<Field>();
                }
                wl.add(Field.getField(st));
                map.put(st.getSubject().getURI(), wl);
                res.put(type, map);

                // Keeping the code below in case I am missing something about Person type
                // as I don't understand why I initially set a condition regarding predicate and
                // object !

                /*
                 * case Taxonomy.PERSON:
                 * if(!st.getPredicate().getURI().equals(st.getObject().toString())) {
                 * ArrayList<Field> pl=people.get(st.getSubject().getURI()); if(pl==null) {
                 * pl=new ArrayList<Field>(); } pl.add(Field.getField(st));
                 * people.put(st.getSubject().getURI(),pl); } break;
                 */
                processed.add(st.getSubject().getURI());
            }
        }
        res.put("metadata", count);
        return res;
    }

}