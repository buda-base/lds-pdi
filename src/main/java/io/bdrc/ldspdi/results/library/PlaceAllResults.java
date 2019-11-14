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

public class PlaceAllResults {

    public final static String[] types= {Taxonomy.ACCESS,Taxonomy.LICENSE,Taxonomy.LANG_SCRIPT,Taxonomy.STATUS};

    public static HashMap<String,Object> getResultsMap(Model mod) throws RestException{
        HashMap<String,Object> res=new HashMap<>();
        HashMap<String,ArrayList<Field>> works=new HashMap<>();
        HashMap<String,ArrayList<Field>> people=new HashMap<>();
        HashMap<String,ArrayList<Field>> places=new HashMap<>();
        HashMap<String,Integer> total=new HashMap<>();
        HashMap<String,HashMap<String,Integer>> count=new HashMap<>();
        ArrayList<String> processed=new ArrayList<>();
        HashMap<String,Integer> topics=new HashMap<>();
        HashMap<String,HashSet<String>> Wtopics=new HashMap<>();
        HashMap<String,HashSet<String>> WorkBranch=new HashMap<>();
        HashSet<String> tops=new HashSet<>();

        StmtIterator it=mod.listStatements();
        List<String> all_types=Arrays.asList(types);
        while(it.hasNext()) {
            Statement st=it.next();
            String type=mod.getProperty(st.getSubject(), RDF.type).getObject().asResource().getURI();
            Integer ctt=total.get(type);
            if(!processed.contains(st.getSubject().getURI())) {
                if(ctt!=null) {
                    total.put(type, ctt.intValue()+1);
                }
                else {
                    total.put(type, 1);
                }
            }
            switch (type) {
            case Taxonomy.WORK:
                ArrayList<Field> wl=works.get(st.getSubject().getURI());
                String predicate=st.getPredicate().getURI();
                if(wl==null) {
                    wl=new ArrayList<Field>();
                }
                wl.add(Field.getField(st));
                works.put(st.getSubject().getURI(),wl);
                if(st.getObject().isURIResource() && all_types.contains(predicate) ) {
                    HashMap<String,Integer> map=count.get(predicate);
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
                    count.put(predicate, map);
                }
                if(predicate.equals(Taxonomy.WORK_GENRE) || predicate.equals(Taxonomy.WORK_IS_ABOUT)) {
                    Taxonomy.processTopicStatement(st, tops, Wtopics, WorkBranch, topics);
                }
                break;
            case Taxonomy.PLACE:
                ArrayList<Field> pla=places.get(st.getSubject().getURI());
                if(pla==null) {
                    pla=new ArrayList<Field>();
                }
                pla.add(Field.getField(st));
                places.put(st.getSubject().getURI(),pla);
                break;
            case Taxonomy.PERSON:
                if(!st.getPredicate().getURI().equals(st.getObject().toString())) {
                    ArrayList<Field> pl=people.get(st.getSubject().getURI());
                    if(pl==null) {
                        pl=new ArrayList<Field>();
                    }
                    pl.add(Field.getField(st));
                    people.put(st.getSubject().getURI(),pl);
                }
                break;
            default:
                throw new RestException(500,new LdsError(LdsError.UNKNOWN_ERR).
                        setContext(" type in PlaceAllResults.getResultsMap(Model mod) >> "+type));
            }
            processed.add(st.getSubject().getURI());
        }
        res.put("metadata",count);
        res.put(Taxonomy.WORK,works);
        res.put(Taxonomy.PERSON,people);
        res.put(Taxonomy.PLACE,places);
        res.put("tree",Taxonomy.buildFacetTree(tops, topics));
        return res;
    }


}