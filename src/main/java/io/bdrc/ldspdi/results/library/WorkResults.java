package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.results.Field;
import io.bdrc.taxonomy.Taxonomy;

public class WorkResults {

    public final static Logger log = LoggerFactory.getLogger(WorkResults.class);
    
    public final static Property tmpIsMain = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/tmp/isMain");

    public static Map<String, Object> getResultsMap(Model mod) throws RestException {
        long start = 0;
        if (log.isDebugEnabled())
            start = System.nanoTime();
        Map<String, Object> res = new HashMap<>();
        Map<String, Integer> topics = new HashMap<>();
        Map<String, ArrayList<Field>> main = new HashMap<>();
        Map<String, ArrayList<Field>> aux = new HashMap<>();
        Map<String, HashSet<String>> Wtopics = new HashMap<>();
        Map<String, HashSet<String>> WorkBranch = new HashMap<>();
        Map<String, Object> facets = new HashMap<>();
        HashSet<String> tops = new HashSet<>();
        Map<Resource, Boolean> isMain = new HashMap<>();
        ResIterator mainit = mod.listResourcesWithProperty(tmpIsMain);
        while (mainit.hasNext()) {
            Resource mainR = mainit.next();
            isMain.put(mainR, true);
        }
        if (log.isDebugEnabled())
            log.debug("WorkResults.getResultMap(), checkpoint1: {}", (System.nanoTime() - start) / 1000);
        StmtIterator allIterator = mod.listStatements();
        while (allIterator.hasNext()) {
            Statement st = allIterator.next();
            
            if (st.getPredicate().equals(tmpIsMain))
                continue;

            final Resource subject = st.getSubject();

            final Boolean subjectIsMain = isMain.getOrDefault(subject, false);

            List<Field> stlist;
            if (subjectIsMain) {
                stlist = main.computeIfAbsent(subject.getURI(), x -> new ArrayList<Field>());
            } else {
                stlist = aux.computeIfAbsent(subject.getURI(), x -> new ArrayList<Field>());
            }
            stlist.add(Field.getField(st));
            
            if (subjectIsMain) {
                String prop = st.getPredicate().getURI();
                if (prop.equals(Taxonomy.WORK_GENRE)
                        || prop.equals(Taxonomy.WORK_IS_ABOUT) && st.getObject().asResource().getLocalName().startsWith("T")) {
                    Taxonomy.processTopicStatement(st, tops, Wtopics, WorkBranch, topics);
                }
            }
        }
        res.put("main", main);
        res.put("aux", aux);
        facets.put("topics", Taxonomy.buildFacetTree(tops, topics));
        if (log.isDebugEnabled())
            log.debug("WorkResults.getResultMap(), checkpoint3: {}", (System.nanoTime() - start) / 1000);
        res.put("facets", facets);
        return res;
    }
}