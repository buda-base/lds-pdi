package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import io.bdrc.ldspdi.results.Field;
import io.bdrc.restapi.exceptions.RestException;
import io.bdrc.taxonomy.Taxonomy;

public class PlaceAllResults {
    
    public final static String TYPE="http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    public final static String PERSON="http://purl.bdrc.io/ontology/core/Person";
    public final static String WORK="http://purl.bdrc.io/ontology/core/Work";
    public final static String PLACE="http://purl.bdrc.io/ontology/core/Place";    
    public final static String ACCESS="http://purl.bdrc.io/ontology/admin/access";
    public final static String LICENSE="http://purl.bdrc.io/ontology/admin/license";
    public final static String STATUS="http://purl.bdrc.io/ontology/admin/status";
    public final static String LANG_SCRIPT="http://purl.bdrc.io/ontology/core/workLangScript";
    public final static String WORK_GENRE="http://purl.bdrc.io/ontology/core/workGenre";
    public final static String WORK_IS_ABOUT="http://purl.bdrc.io/ontology/core/workIsAbout";
    
    public static HashMap<String,Object> getResultsMap(Model mod) throws RestException{
        HashMap<String,Object> res=new HashMap<>();
        HashMap<String,ArrayList<Field>> works=new HashMap<>(); 
        HashMap<String,ArrayList<Field>> people=new HashMap<>();
        HashMap<String,ArrayList<Field>> places=new HashMap<>();        
        HashMap<String,Integer> access=new HashMap<>();
        HashMap<String,Integer> license=new HashMap<>();
        HashMap<String,Integer> status=new HashMap<>();
        HashMap<String,Integer> langScript=new HashMap<>();
        HashMap<String,Integer> total=new HashMap<>();
        HashMap<String,HashMap<String,Integer>> count=new HashMap<>();
        ArrayList<String> processed=new ArrayList<>();
        HashMap<String,Integer> topics=new HashMap<>();
        HashMap<String,HashSet<String>> Wtopics=new HashMap<>();
        HashMap<String,HashSet<String>> WorkBranch=new HashMap<>();
        HashSet<String> tops=new HashSet<>();
        
        StmtIterator it=mod.listStatements();
        while(it.hasNext()) {
            Statement st=it.next();
            String type=mod.getProperty(st.getSubject(), mod.getProperty(TYPE)).getObject().asResource().getURI().toString();
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
                case WORK:
                    ArrayList<Field> wl=works.get(st.getSubject().getURI());
                    if(wl==null) {
                        wl=new ArrayList<Field>();
                    }
                    wl.add(Field.getField(st));
                    works.put(st.getSubject().getURI(),wl);
                    if(st.getPredicate().getURI().equals(ACCESS)) {
                        Integer ct=access.get(st.getObject().asNode().getURI());
                        if(ct!=null) {
                            access.put(st.getObject().asNode().getURI(), ct.intValue()+1);
                        }
                        else {
                            access.put(st.getObject().asNode().getURI(), 1);
                        }
                    }
                    if(st.getPredicate().getURI().equals(LICENSE)) {
                        Integer ct=license.get(st.getObject().asNode().getURI());
                        if(ct!=null) {
                            license.put(st.getObject().asNode().getURI(), ct.intValue()+1);
                        }
                        else {
                            license.put(st.getObject().asNode().getURI(), 1);
                        }
                    }
                    if(st.getPredicate().getURI().equals(LANG_SCRIPT)) {
                        Integer ct=langScript.get(st.getObject().asNode().getURI());
                        if(ct!=null) {
                            langScript.put(st.getObject().asNode().getURI(), ct.intValue()+1);
                        }
                        else {
                            langScript.put(st.getObject().asNode().getURI(), 1);
                        }
                    }
                    if(st.getPredicate().getURI().equals(STATUS)) {
                        Integer ct=status.get(st.getObject().asNode().getURI());
                        if(ct!=null) {
                            status.put(st.getObject().asNode().getURI(), ct.intValue()+1);
                        }
                        else {
                            status.put(st.getObject().asNode().getURI(), 1);
                        }
                    }
                    if(st.getPredicate().getURI().equals(WORK_GENRE) || st.getPredicate().getURI().equals(WORK_IS_ABOUT)) {                        
                        tops.add(st.getObject().asNode().getURI());
                        HashSet<String> tmp=Wtopics.get(st.getObject().asNode().getURI());
                        if (tmp==null) {
                            tmp=new HashSet<>();
                        }
                        tmp.add(st.getSubject().asNode().getURI());
                        Wtopics.put(st.getObject().asNode().getURI(), tmp); 
                        LinkedList<String> nodes=Taxonomy.getRootToLeafPath(st.getObject().asNode().getURI());
                        if(!nodes.isEmpty()) {
                            nodes.removeFirst();
                            nodes.removeLast();
                        }
                        for(String s:nodes) {
                            HashSet<String> bt=WorkBranch.get(s);
                            if (bt==null) {
                                bt=new HashSet<>();
                            }
                            bt.add(st.getSubject().asNode().getURI());
                            WorkBranch.put(s, bt);
                            topics.put(s, bt.size());
                        }
                        topics.put(st.getObject().asNode().getURI(), Wtopics.get(st.getObject().asNode().getURI()).size());
                    }
                    break;                
                case PLACE:
                    ArrayList<Field> pla=places.get(st.getSubject().getURI());
                    if(pla==null) {
                        pla=new ArrayList<Field>();
                    }
                    pla.add(Field.getField(st));
                    places.put(st.getSubject().getURI(),pla);
                    break;
                case PERSON:
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
                    throw new RestException(500,RestException.GENERIC_APP_ERROR_CODE,"Unknown type in placeAllAssociations >> "+type);
            }
            processed.add(st.getSubject().getURI());
        }
        count.put("total", total);
        count.put("access", access);
        count.put("license",license);
        count.put("status",status);
        count.put("langScript",langScript);
        res.put("metadata",count);
        res.put("works",works);        
        res.put("persons",people);       
        res.put("places",places); 
        res.put("tree",Taxonomy.buildFacetTree(tops, topics));
        return res;
    }
        

}
