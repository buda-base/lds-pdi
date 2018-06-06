package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import io.bdrc.ldspdi.results.Field;
import io.bdrc.restapi.exceptions.RestException;
import io.bdrc.taxonomy.Taxonomy;

public class TopicAllResults {
    
           
    public static HashMap<String,Object> getResultsMap(Model mod) throws RestException{
        HashMap<String,ArrayList<Field>> works=new HashMap<>();         
        HashMap<String,ArrayList<Field>> lineages=new HashMap<>();
        HashMap<String,Object> res=new HashMap<>();
        HashMap<String,Integer> total=new HashMap<>();
        HashMap<String,Integer> access=new HashMap<>();
        HashMap<String,Integer> license=new HashMap<>();
        HashMap<String,Integer> status=new HashMap<>();
        HashMap<String,Integer> langScript=new HashMap<>();
        HashMap<String,Integer> topics=new HashMap<>();
        HashMap<String,HashSet<String>> Wtopics=new HashMap<>();
        HashMap<String,HashSet<String>> WorkBranch=new HashMap<>();
        HashSet<String> tops=new HashSet<>();
        HashMap<String,HashMap<String,Integer>> count=new HashMap<>();
        ArrayList<String> processed=new ArrayList<>();
        StmtIterator it=mod.listStatements();
        while(it.hasNext()) {
            Statement st=it.next();
            String type=mod.getProperty(st.getSubject(), mod.getProperty(Taxonomy.TYPE)).getObject().asResource().getURI().toString();
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
                    if(wl==null) {
                        wl=new ArrayList<Field>();
                    }
                    wl.add(Field.getField(st)); 
                    works.put(st.getSubject().getURI(),wl);
                    if(st.getPredicate().getURI().equals(Taxonomy.ACCESS)) {
                        Integer ct=access.get(st.getObject().asNode().getURI());
                        if(ct!=null) {
                            access.put(st.getObject().asNode().getURI(), ct.intValue()+1);
                        }
                        else {
                            access.put(st.getObject().asNode().getURI(), 1);
                        }
                    }
                    if(st.getPredicate().getURI().equals(Taxonomy.LICENSE)) {
                        Integer ct=license.get(st.getObject().asNode().getURI());
                        if(ct!=null) {
                            license.put(st.getObject().asNode().getURI(), ct.intValue()+1);
                        }
                        else {
                            license.put(st.getObject().asNode().getURI(), 1);
                        }
                    }
                    if(st.getPredicate().getURI().equals(Taxonomy.LANG_SCRIPT)) {
                        Integer ct=langScript.get(st.getObject().asNode().getURI());
                        if(ct!=null) {
                            langScript.put(st.getObject().asNode().getURI(), ct.intValue()+1);
                        }
                        else {
                            langScript.put(st.getObject().asNode().getURI(), 1);
                        }
                    }
                    if(st.getPredicate().getURI().equals(Taxonomy.STATUS)) {
                        Integer ct=status.get(st.getObject().asNode().getURI());
                        if(ct!=null) {
                            status.put(st.getObject().asNode().getURI(), ct.intValue()+1);
                        }
                        else {
                            status.put(st.getObject().asNode().getURI(), 1);
                        }
                    }
                    if(st.getPredicate().getURI().equals(Taxonomy.WORK_GENRE) || st.getPredicate().getURI().equals(Taxonomy.WORK_IS_ABOUT)) {                        
                        Taxonomy.processTopicStatement(st, tops, Wtopics, WorkBranch, topics);
                    }
                    break;                
                case Taxonomy.LINEAGE:
                    ArrayList<Field> pli=lineages.get(st.getSubject().getURI());
                    if(pli==null) {
                        pli=new ArrayList<Field>();
                    }
                    pli.add(Field.getField(st)); 
                    lineages.put(st.getSubject().getURI(),pli);
                    break;                
                default:
                    throw new RestException(500,RestException.GENERIC_APP_ERROR_CODE,"Unknown type in PersonAllResults >> "+type);
            
            }
            processed.add(st.getSubject().getURI());
        }
        count.put("total", total);
        count.put("access", access);
        count.put("license",license);
        count.put("status",status);
        count.put("langScript",langScript);
        res.put("associatedWorks",works);        
        res.put("associatedLineages",lineages);
        res.put("metadata",count);
        res.put("tree",Taxonomy.buildFacetTree(tops, topics));
        return res;
        
    }

}