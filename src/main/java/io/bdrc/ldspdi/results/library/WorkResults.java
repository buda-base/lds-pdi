package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.results.Field;
import io.bdrc.restapi.exceptions.RestException;
import io.bdrc.taxonomy.Taxonomy;



public class WorkResults {


    public final static Logger log=LoggerFactory.getLogger(WorkResults.class.getName());
    public final static String[] types= {Taxonomy.ACCESS,Taxonomy.LICENSE,Taxonomy.LANG_SCRIPT,Taxonomy.STATUS};


    public static HashMap<String,Object> getResultsMap(Model mod) throws RestException{
        HashMap<String,Object> res=new HashMap<>();
        HashMap<String,HashMap<String,Integer>> count=new HashMap<>();
        HashMap<String,Integer> topics=new HashMap<>();
        HashMap<String,ArrayList<Field>> works=new HashMap<>();
        HashMap<String,HashSet<String>> Wtopics=new HashMap<>();
        HashMap<String,HashSet<String>> WorkBranch=new HashMap<>();
        HashSet<String> tops=new HashSet<>();
        StmtIterator iter=mod.listStatements();
        List<String> all_types=Arrays.asList(types);
        while(iter.hasNext()) {
            Statement st=iter.next();
            String uri=st.getSubject().getURI();
            ArrayList<Field> w=works.get(uri);

            if(w == null) {
                w=new ArrayList<Field>();
            }
            w.add(Field.getField(st));
            works.put(uri, w);
            String type=st.getPredicate().getURI();
            if(st.getObject().isURIResource() && all_types.contains(type) ) {
                HashMap<String,Integer> map=count.get(type);
                if(map==null){
                    map=new HashMap<String,Integer>();
                }
                Integer ct=map.get(st.getObject().asNode().getURI());
                if(ct!=null) {
                    map.put(st.getObject().asNode().getURI(), ct.intValue()+1);
                }
                else {
                    map.put(st.getObject().asNode().getURI(), 1);
                }
                count.put(type, map);
            }
            if(type.equals(Taxonomy.WORK_GENRE) || type.equals(Taxonomy.WORK_IS_ABOUT)) {
                Taxonomy.processTopicStatement(st, tops, Wtopics, WorkBranch, topics);
            }
        }
        res.put(Taxonomy.WORK,works);
        res.put("tree",Taxonomy.buildFacetTree(tops, topics));
        res.put("metadata",count);
        return res;
    }
}