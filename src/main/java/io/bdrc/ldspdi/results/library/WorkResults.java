package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.results.Field;
import io.bdrc.restapi.exceptions.RestException;
import io.bdrc.taxonomy.Taxonomy;



public class WorkResults {
    
    static final String TYPE="http://purl.bdrc.io/ontology/core/Work";
    static final String ACCESS="http://purl.bdrc.io/ontology/admin/access";
    static final String LICENSE="http://purl.bdrc.io/ontology/admin/license";
    static final String STATUS="http://purl.bdrc.io/ontology/admin/status";
    static final String LANG_SCRIPT="http://purl.bdrc.io/ontology/core/workLangScript";
    static final String WORK_GENRE="http://purl.bdrc.io/ontology/core/workGenre";
    static final String WORK_IS_ABOUT="http://purl.bdrc.io/ontology/core/workIsAbout";
    static final String PREFLABEL="http://www.w3.org/2004/02/skos/core#prefLabel";
    static final String MATCH="http://purl.bdrc.io/ontology/core/labelMatch";
        
    public final static Logger log=LoggerFactory.getLogger(WorkResults.class.getName());  
     
      
    public static HashMap<String,Object> getResultsMap(Model mod) throws RestException{
        HashMap<String,Object> res=new HashMap<>();
        HashMap<String,HashMap<String,Integer>> count=new HashMap<>();
        HashMap<String,Integer> access=new HashMap<>();
        HashMap<String,Integer> license=new HashMap<>();
        HashMap<String,Integer> status=new HashMap<>();
        HashMap<String,Integer> langScript=new HashMap<>();        
        HashMap<String,Integer> topics=new HashMap<>();        
        HashMap<String,ArrayList<Field>> works=new HashMap<>();
        HashMap<String,HashSet<String>> Wtopics=new HashMap<>();
        HashMap<String,HashSet<String>> WorkBranch=new HashMap<>();
        HashSet<String> tops=new HashSet<>();       
        StmtIterator iter=mod.listStatements();
        while(iter.hasNext()) {
            Statement st=iter.next();  
            String uri=st.getSubject().getURI();            
            ArrayList<Field> w=works.get(uri);
                     
            if(w == null) {
                w=new ArrayList<Field>();
            }
            w.add(Field.getField(st)); 
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
            works.put(uri, w);
        }
        res.put("works",works);
        res.put("tree",Taxonomy.buildFacetTree(tops, topics));
        count.put("access", access);
        count.put("license",license);
        count.put("status",status);
        count.put("langScript",langScript);
        res.put("metadata",count);
        return res;
    }
}
