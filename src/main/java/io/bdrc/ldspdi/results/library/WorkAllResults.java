package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import io.bdrc.ldspdi.results.Field;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;
import io.bdrc.taxonomy.Taxonomy;

public class WorkAllResults {

    public static HashMap<String, Object> getResultsMap(Model mod) throws RestException {
        HashMap<String, Object> res = new HashMap<>();
        HashMap<String, ArrayList<Field>> works = new HashMap<>();
        HashMap<String, ArrayList<Field>> published_works = new HashMap<>();
        HashMap<String, ArrayList<Field>> unicode_works = new HashMap<>();
        HashMap<String, ArrayList<Field>> abstract_works = new HashMap<>();
        HashMap<String, ArrayList<Field>> unspec_works = new HashMap<>();
        HashMap<String, ArrayList<Field>> virtual_works = new HashMap<>();
        HashMap<String, ArrayList<Field>> lineages = new HashMap<>();
        HashMap<String, Integer> topics = new HashMap<>();
        HashMap<String, HashSet<String>> Wtopics = new HashMap<>();
        HashMap<String, HashSet<String>> WorkBranch = new HashMap<>();
        HashSet<String> tops = new HashSet<>();
        StmtIterator iter = mod.listStatements();
        while (iter.hasNext()) {
            Statement st = iter.next();
            String type = mod.getProperty(st.getSubject(), RDF.type).getObject().asResource().getURI();
            switch (type) {
            case Taxonomy.PUBLISHED_WORK:
                ArrayList<Field> pwl = published_works.get(st.getSubject().getURI());
                if (pwl == null) {
                    pwl = new ArrayList<Field>();
                }
                if (st.getPredicate().getURI().equals(Taxonomy.WORK_GENRE) || st.getPredicate().getURI().equals(Taxonomy.WORK_IS_ABOUT)) {
                    Taxonomy.processTopicStatement(st, tops, Wtopics, WorkBranch, topics);
                }
                pwl.add(Field.getField(st));
                published_works.put(st.getSubject().getURI(), pwl);
                break;
            case Taxonomy.UNICODE_WORK:
                ArrayList<Field> uwl = unicode_works.get(st.getSubject().getURI());
                if (uwl == null) {
                    uwl = new ArrayList<Field>();
                }
                if (st.getPredicate().getURI().equals(Taxonomy.WORK_GENRE) || st.getPredicate().getURI().equals(Taxonomy.WORK_IS_ABOUT)) {
                    Taxonomy.processTopicStatement(st, tops, Wtopics, WorkBranch, topics);
                }
                uwl.add(Field.getField(st));
                unicode_works.put(st.getSubject().getURI(), uwl);
                break;
            case Taxonomy.ABSTRACT_WORK:
                ArrayList<Field> awl = abstract_works.get(st.getSubject().getURI());
                if (awl == null) {
                    awl = new ArrayList<Field>();
                }
                if (st.getPredicate().getURI().equals(Taxonomy.WORK_GENRE) || st.getPredicate().getURI().equals(Taxonomy.WORK_IS_ABOUT)) {
                    Taxonomy.processTopicStatement(st, tops, Wtopics, WorkBranch, topics);
                }
                awl.add(Field.getField(st));
                abstract_works.put(st.getSubject().getURI(), awl);
                break;
            case Taxonomy.UNSPEC_WORK:
                ArrayList<Field> swl = unspec_works.get(st.getSubject().getURI());
                if (swl == null) {
                    swl = new ArrayList<Field>();
                }
                if (st.getPredicate().getURI().equals(Taxonomy.WORK_GENRE) || st.getPredicate().getURI().equals(Taxonomy.WORK_IS_ABOUT)) {
                    Taxonomy.processTopicStatement(st, tops, Wtopics, WorkBranch, topics);
                }
                swl.add(Field.getField(st));
                unspec_works.put(st.getSubject().getURI(), swl);
                break;
            case Taxonomy.VIRTUAL_WORK:
                ArrayList<Field> vwl = virtual_works.get(st.getSubject().getURI());
                if (vwl == null) {
                    vwl = new ArrayList<Field>();
                }
                if (st.getPredicate().getURI().equals(Taxonomy.WORK_GENRE) || st.getPredicate().getURI().equals(Taxonomy.WORK_IS_ABOUT)) {
                    Taxonomy.processTopicStatement(st, tops, Wtopics, WorkBranch, topics);
                }
                vwl.add(Field.getField(st));
                virtual_works.put(st.getSubject().getURI(), vwl);
                break;
            case Taxonomy.WORK:
                ArrayList<Field> wl = works.get(st.getSubject().getURI());
                if (wl == null) {
                    wl = new ArrayList<Field>();
                }
                if (st.getPredicate().getURI().equals(Taxonomy.WORK_GENRE) || st.getPredicate().getURI().equals(Taxonomy.WORK_IS_ABOUT)) {
                    Taxonomy.processTopicStatement(st, tops, Wtopics, WorkBranch, topics);
                }
                wl.add(Field.getField(st));
                works.put(st.getSubject().getURI(), wl);
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
                throw new RestException(500, new LdsError(LdsError.UNKNOWN_ERR).setContext(" type in WorkAllResults.getResultsMap(Model mod) >> " + type));
            }
        }
        res.put(Taxonomy.WORK, works);
        res.put(Taxonomy.LINEAGE, lineages);
        res.put(Taxonomy.PUBLISHED_WORK, published_works);
        res.put(Taxonomy.UNICODE_WORK, unicode_works);
        res.put(Taxonomy.ABSTRACT_WORK, abstract_works);
        res.put(Taxonomy.UNSPEC_WORK, unspec_works);
        res.put(Taxonomy.VIRTUAL_WORK, virtual_works);
        res.put("tree", Taxonomy.buildFacetTree(tops, topics));
        return res;
    }

}