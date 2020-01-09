package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.HashMap;
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

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.results.Field;

public class WorkInstanceResults {

    public final static Logger log = LoggerFactory.getLogger(WorkInstanceResults.class);

    public static Map<String, Object> getResultsMap(Model mod) throws RestException {
        long start = 0;
        if (log.isDebugEnabled())
            start = System.nanoTime();
        Map<String, Object> res = new HashMap<>();
        Map<String, ArrayList<Field>> main = new HashMap<>();
        Map<String, ArrayList<Field>> aux = new HashMap<>();
        Map<Resource, Boolean> isInstance = new HashMap<>();
        //ResIterator workit = mod.listResourcesWithProperty(RDF.type, mod.getResource("http://purl.bdrc.io/ontology/core/Work"));
        ResIterator instanceit = mod.listResourcesWithProperty(RDF.type, mod.getResource("http://purl.bdrc.io/ontology/core/Instance"));
        while (instanceit.hasNext()) {
            Resource instance = instanceit.next();
            isInstance.put(instance, true);
        }
        if (log.isDebugEnabled())
            log.debug("InstanceResults.getResultMap(), checkpoint1: {}", (System.nanoTime()-start)/1000);
        StmtIterator allIterator = mod.listStatements();
        while (allIterator.hasNext()) {
            Statement st = allIterator.next();
            
            final Resource subject = st.getSubject();
            
            final Boolean subjectIsWork = isInstance.getOrDefault(subject, false);
            
            List<Field> stlist;
            if (subjectIsWork) {
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