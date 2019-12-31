package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.results.Field;
import io.bdrc.taxonomy.Taxonomy;

public class WorkResults {

    public final static Logger log = LoggerFactory.getLogger(WorkResults.class);
    public final static List<String> sortingProps = Arrays.asList(Taxonomy.INSTANCEACCESS, Taxonomy.LANGUAGE, Taxonomy.INSTANCETYPE, Taxonomy.AUTHOR);

    public static Map<String, Object> getResultsMap(Model mod) throws RestException {
        Map<String, Object> res = new HashMap<>();
        Map<String, Integer> topics = new HashMap<>();
        Map<Resource, ArrayList<Field>> main = new HashMap<>();
        Map<Resource, ArrayList<Field>> aux = new HashMap<>();
        Map<String, HashSet<String>> Wtopics = new HashMap<>();
        Map<String, HashSet<String>> WorkBranch = new HashMap<>();
        Map<String, Object> facets = new HashMap<>();
        HashSet<String> tops = new HashSet<>();
        Map<Resource, Boolean> isWork = new HashMap<>();
        ResIterator workit = mod.listResourcesWithProperty(RDF.type, mod.getResource("http://purl.bdrc.io/ontology/core/Work"));
        while (workit.hasNext()) {
            Resource work = workit.next();
            isWork.put(work, true);
        }

        StmtIterator allIterator = mod.listStatements();
        while (allIterator.hasNext()) {
            Statement st = allIterator.next();
            
            final Resource subject = st.getSubject();
            
            final Boolean subjectIsWork = isWork.getOrDefault(subject, false);
            
            List<Field> stlist;
            if (subjectIsWork) {
                stlist = main.computeIfAbsent(subject, x -> new ArrayList<Field>());
            } else {
                stlist = aux.computeIfAbsent(subject, x -> new ArrayList<Field>());
            }
            stlist.add(Field.getField(st));

            String prop = st.getPredicate().getURI();
            if (st.getObject().isURIResource() && sortingProps.contains(prop)) {
                // we assume that sortingProps is handling resources
                Resource object = st.getObject().asResource();
                @SuppressWarnings("unchecked")
                Map<Resource, List<Resource>> valueList = (Map<Resource, List<Resource>>) facets.computeIfAbsent(prop, x -> new HashMap<Resource, List<Resource>>());
                List<Resource> list = valueList.computeIfAbsent(object, x -> new ArrayList<Resource>());
                list.add(subject);
            }
            if (prop.equals(Taxonomy.WORK_GENRE) || prop.equals(Taxonomy.WORK_IS_ABOUT)) {
                Taxonomy.processTopicStatement(st, tops, Wtopics, WorkBranch, topics);
            }
        }
        res.put("main", main);
        res.put("aux", aux);
        JsonNode topicstree = Taxonomy.buildFacetTree(tops, topics);
        facets.put("topics", topicstree);
        res.put("facets", facets);
        return res;
    }
}