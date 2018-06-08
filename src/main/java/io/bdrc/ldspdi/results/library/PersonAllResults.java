package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import io.bdrc.ldspdi.results.Field;
import io.bdrc.restapi.exceptions.Error;
import io.bdrc.restapi.exceptions.RestException;
import io.bdrc.taxonomy.Taxonomy;

public class PersonAllResults {
           
    public static HashMap<String,Object> getResultsMap(Model mod) throws RestException{
        HashMap<String,ArrayList<Field>> works=new HashMap<>(); 
        HashMap<String,ArrayList<Field>> people=new HashMap<>();
        HashMap<String,ArrayList<Field>> places=new HashMap<>();
        HashMap<String,ArrayList<Field>> lineages=new HashMap<>();
        HashMap<String,Integer> topics=new HashMap<>();
        HashMap<String,HashSet<String>> Wtopics=new HashMap<>();
        HashMap<String,HashSet<String>> WorkBranch=new HashMap<>();
        HashSet<String> tops=new HashSet<>(); 
        HashMap<String,Object> res=new HashMap<>();
        StmtIterator it=mod.listStatements();
        while(it.hasNext()) {
            Statement st=it.next();
            String type=mod.getProperty(st.getSubject(), mod.getProperty(Taxonomy.TYPE)).getObject().asResource().getURI().toString();
            switch (type) {
                case Taxonomy.WORK:
                    ArrayList<Field> wl=works.get(st.getSubject().getURI());
                    if(wl==null) {
                        wl=new ArrayList<Field>();
                    }
                    wl.add(Field.getField(st)); 
                    works.put(st.getSubject().getURI(),wl);
                    if(st.getPredicate().getURI().equals(Taxonomy.WORK_GENRE) || st.getPredicate().getURI().equals(Taxonomy.WORK_IS_ABOUT)) {                        
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
                case Taxonomy.LINEAGE:
                    ArrayList<Field> pli=lineages.get(st.getSubject().getURI());
                    if(pli==null) {
                        pli=new ArrayList<Field>();
                    }
                    pli.add(Field.getField(st));                    
                    lineages.put(st.getSubject().getURI(),pli);
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
                    throw new RestException(500,new Error(Error.UNKNOWN_ERR).setContext(" type in PersonAllResults.getResultsMap(Model mod) >> "+type));
            }
        }        
        res.put("associatedWorks",works);
        res.put("associatedPeople",people);
        res.put("associatedPlaces",places);
        res.put("associatedLineages",lineages);
        res.put("tree",Taxonomy.buildFacetTree(tops, topics));
        return res;
        
    }

}