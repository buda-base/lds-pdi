package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.results.Field;
import io.bdrc.taxonomy.Taxonomy;

public class TopicAllResults {

    public final static String[] types = { Taxonomy.INSTANCEACCESS, Taxonomy.LANGUAGE };

    public static HashMap<String, Object> getResultsMap(Model mod) throws RestException {
        HashMap<String, ArrayList<Field>> works = new HashMap<>();
        HashMap<String, ArrayList<Field>> published_works = new HashMap<>();
        HashMap<String, ArrayList<Field>> unicode_works = new HashMap<>();
        HashMap<String, ArrayList<Field>> abstract_works = new HashMap<>();
        HashMap<String, ArrayList<Field>> unspec_works = new HashMap<>();
        HashMap<String, ArrayList<Field>> virtual_works = new HashMap<>();
        HashMap<String, ArrayList<Field>> lineages = new HashMap<>();
        HashMap<String, Object> res = new HashMap<>();
        HashMap<String, Integer> total = new HashMap<>();
        HashMap<String, Integer> topics = new HashMap<>();
        HashMap<String, HashSet<String>> Wtopics = new HashMap<>();
        HashMap<String, HashSet<String>> WorkBranch = new HashMap<>();
        HashSet<String> tops = new HashSet<>();
        HashMap<String, HashMap<String, Integer>> count = new HashMap<>();
        ArrayList<String> processed = new ArrayList<>();
        StmtIterator it = mod.listStatements();
        List<String> all_types = Arrays.asList(types);
        while (it.hasNext()) {
            Statement st = it.next();
            String type = mod.getProperty(st.getSubject(), RDF.type).getObject().asResource().getURI();
            Integer ctt = total.get(type);
            if (!processed.contains(st.getSubject().getURI())) {
                if (ctt != null) {
                    total.put(type, ctt.intValue() + 1);
                } else {
                    total.put(type, 1);
                }
            }
            switch (type) {
            case Taxonomy.WORK:
                String predicate = st.getPredicate().getURI();
                ArrayList<Field> wl = works.get(st.getSubject().getURI());
                if (wl == null) {
                    wl = new ArrayList<Field>();
                }
                wl.add(Field.getField(st));
                works.put(st.getSubject().getURI(), wl);
                update(all_types, predicate, count, st, tops, Wtopics, WorkBranch, topics);
                break;
            case Taxonomy.PUBLISHED_WORK:
                predicate = st.getPredicate().getURI();
                ArrayList<Field> pwl = published_works.get(st.getSubject().getURI());
                if (pwl == null) {
                    pwl = new ArrayList<Field>();
                }
                pwl.add(Field.getField(st));
                published_works.put(st.getSubject().getURI(), pwl);
                update(all_types, predicate, count, st, tops, Wtopics, WorkBranch, topics);
                break;
            case Taxonomy.UNICODE_WORK:
                predicate = st.getPredicate().getURI();
                ArrayList<Field> uwl = unicode_works.get(st.getSubject().getURI());
                if (uwl == null) {
                    uwl = new ArrayList<Field>();
                }
                uwl.add(Field.getField(st));
                unicode_works.put(st.getSubject().getURI(), uwl);
                update(all_types, predicate, count, st, tops, Wtopics, WorkBranch, topics);
                break;
            case Taxonomy.ABSTRACT_WORK:
                predicate = st.getPredicate().getURI();
                ArrayList<Field> awl = abstract_works.get(st.getSubject().getURI());
                if (awl == null) {
                    awl = new ArrayList<Field>();
                }
                awl.add(Field.getField(st));
                abstract_works.put(st.getSubject().getURI(), awl);
                update(all_types, predicate, count, st, tops, Wtopics, WorkBranch, topics);
                break;
            case Taxonomy.UNSPEC_WORK:
                predicate = st.getPredicate().getURI();
                ArrayList<Field> swl = unspec_works.get(st.getSubject().getURI());
                if (swl == null) {
                    swl = new ArrayList<Field>();
                }
                swl.add(Field.getField(st));
                unspec_works.put(st.getSubject().getURI(), swl);
                update(all_types, predicate, count, st, tops, Wtopics, WorkBranch, topics);
                break;
            case Taxonomy.VIRTUAL_WORK:
                predicate = st.getPredicate().getURI();
                ArrayList<Field> vwl = virtual_works.get(st.getSubject().getURI());
                if (vwl == null) {
                    vwl = new ArrayList<Field>();
                }
                vwl.add(Field.getField(st));
                virtual_works.put(st.getSubject().getURI(), vwl);
                update(all_types, predicate, count, st, tops, Wtopics, WorkBranch, topics);
                break;
            case Taxonomy.LINEAGE:
                ArrayList<Field> pli = lineages.get(st.getSubject().getURI());
                if (pli == null) {
                    pli = new ArrayList<Field>();
                }
                pli.add(Field.getField(st));
                lineages.put(st.getSubject().getURI(), pli);
                break;
            default:
                throw new RestException(500, new LdsError(LdsError.UNKNOWN_ERR).setContext(" type in TopicAllResults.getResultsMap(Model mod) >> " + type));
            }
            processed.add(st.getSubject().getURI());
        }
        count.put("total", total);
        res.put(Taxonomy.WORK, works);
        res.put(Taxonomy.LINEAGE, lineages);
        res.put(Taxonomy.PUBLISHED_WORK, published_works);
        res.put(Taxonomy.UNICODE_WORK, unicode_works);
        res.put(Taxonomy.ABSTRACT_WORK, abstract_works);
        res.put(Taxonomy.UNSPEC_WORK, unspec_works);
        res.put(Taxonomy.VIRTUAL_WORK, virtual_works);
        res.put("metadata", count);
        res.put("tree", Taxonomy.buildFacetTree(tops, topics));
        return res;

    }

    private static void update(List<String> all_types, String predicate, HashMap<String, HashMap<String, Integer>> count, Statement st, HashSet<String> tops, HashMap<String, HashSet<String>> Wtopics, HashMap<String, HashSet<String>> WorkBranch,
            HashMap<String, Integer> topics) {
        if (st.getObject().isURIResource() && all_types.contains(predicate)) {
            HashMap<String, Integer> map = count.get(predicate);
            if (map == null) {
                map = new HashMap<String, Integer>();
            }
            Integer ct = map.get(st.getObject().asNode().getURI());
            if (ct != null) {
                map.put(st.getObject().asNode().getURI(), ct.intValue() + 1);
            } else {
                map.put(st.getObject().asNode().getURI(), 1);
            }
            count.put(predicate, map);
        }
        if (predicate.equals(Taxonomy.WORK_GENRE) || predicate.equals(Taxonomy.WORK_IS_ABOUT)) {
            Taxonomy.processTopicStatement(st, tops, Wtopics, WorkBranch, topics);
        }
    }

}