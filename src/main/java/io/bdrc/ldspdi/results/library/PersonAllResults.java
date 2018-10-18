package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import io.bdrc.ldspdi.results.Field;
import io.bdrc.restapi.exceptions.RestException;
import io.bdrc.taxonomy.Taxonomy;

public class PersonAllResults {

    public static HashMap<String,Object> getResultsMap(Model mod) throws RestException{
        HashMap<String,Integer> topics=new HashMap<>();
        HashMap<String,HashSet<String>> Wtopics=new HashMap<>();
        HashMap<String,HashSet<String>> WorkBranch=new HashMap<>();
        HashSet<String> tops=new HashSet<>();
        HashMap<String,Object> res=new HashMap<>();
        StmtIterator it=mod.listStatements();
        while(it.hasNext()) {
            Statement st=it.next();
            String type=mod.getProperty(st.getSubject(), RDF.type).getObject().asResource().getURI();
            @SuppressWarnings("unchecked") HashMap<String,ArrayList<Field>> map=(HashMap<String,ArrayList<Field>>)res.get(type);
            if(map==null){
                map=new HashMap<String,ArrayList<Field>>();
            }
            ArrayList<Field> wl=map.get(st.getSubject().getURI());
            if(wl==null) {
                wl=new ArrayList<Field>();
            }
            wl.add(Field.getField(st));
            map.put(st.getSubject().getURI(),wl);
            res.put(type, map);
            if(st.getPredicate().getURI().equals(Taxonomy.WORK_GENRE) || st.getPredicate().getURI().equals(Taxonomy.WORK_IS_ABOUT)) {
                Taxonomy.processTopicStatement(st, tops, Wtopics, WorkBranch, topics);
            }
        }
        res.put("tree",Taxonomy.buildFacetTree(tops, topics));
        return res;

    }

}