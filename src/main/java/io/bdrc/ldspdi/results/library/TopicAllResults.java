package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import io.bdrc.ldspdi.results.Field;
import io.bdrc.ldspdi.results.LiteralStringField;
import io.bdrc.restapi.exceptions.RestException;

public class TopicAllResults {
    
    public final static String TYPE="http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    public final static String WORK="http://purl.bdrc.io/ontology/core/Work";    
    public final static String LINEAGE="http://purl.bdrc.io/ontology/core/Lineage";
    public final static String ACCESS="http://purl.bdrc.io/ontology/admin/access";
    public final static String LICENSE="http://purl.bdrc.io/ontology/admin/license";
    public final static String STATUS="http://purl.bdrc.io/ontology/admin/status";
    public final static String LANG_SCRIPT="http://purl.bdrc.io/ontology/core/workLangScript";
    
    public static HashMap<String,Object> getResultsMap(Model mod) throws RestException{
        HashMap<String,ArrayList<Field>> works=new HashMap<>();         
        HashMap<String,ArrayList<Field>> lineages=new HashMap<>();
        HashMap<String,Object> res=new HashMap<>();
        HashMap<String,Integer> total=new HashMap<>();
        HashMap<String,Integer> access=new HashMap<>();
        HashMap<String,Integer> license=new HashMap<>();
        HashMap<String,Integer> status=new HashMap<>();
        HashMap<String,Integer> langScript=new HashMap<>();
        HashMap<String,HashMap<String,Integer>> count=new HashMap<>();
        ArrayList<String> processed=new ArrayList<>();
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
                    if(st.getObject().isLiteral()) {
                        wl.add(new LiteralStringField(st.getPredicate().getURI(),st.getObject().asLiteral().getLanguage(),st.getObject().asLiteral().getValue().toString()));  
                    }else {
                        wl.add(new Field(st.getPredicate().getURI(),st.getObject().toString()));
                    }
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
                    break;                
                case LINEAGE:
                    ArrayList<Field> pli=lineages.get(st.getSubject().getURI());
                    if(pli==null) {
                        pli=new ArrayList<Field>();
                    }
                    if(st.getObject().isLiteral()) {
                        pli.add(new LiteralStringField(st.getPredicate().getURI(),st.getObject().asLiteral().getLanguage(),st.getObject().asLiteral().getValue().toString()));  
                    }else {
                        pli.add(new Field(st.getPredicate().getURI(),st.getObject().toString()));
                    }
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
        return res;
        
    }

}
