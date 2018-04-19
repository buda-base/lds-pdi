package io.bdrc.ldspdi.results.library;

import java.util.HashMap;

import org.apache.jena.graph.Node;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.results.LiteralStringField;
import io.bdrc.taxonomy.TaxModel;


public class WorkResults {
    
    static final String TYPE="http://purl.bdrc.io/ontology/core/Work";
    static final String ACCESS="http://purl.bdrc.io/ontology/admin/access";
    static final String LICENSE="http://purl.bdrc.io/ontology/admin/license";
    static final String STATUS="http://purl.bdrc.io/ontology/admin/status";
    static final String LANG_SCRIPT="http://purl.bdrc.io/ontology/core/workLangScript";
    static final String WORK_GENRE="http://purl.bdrc.io/ontology/core/workGenre";
    static final String PREFLABEL="http://www.w3.org/2004/02/skos/core#prefLabel";
    static final String MATCH="http://purl.bdrc.io/ontology/core/labelMatch";
        
    public final static Logger log=LoggerFactory.getLogger(WorkResults.class.getName());
    
    public static HashMap<String,Object> getResultsMap(ResultSet rs){
        HashMap<String,Object> res=new HashMap<>();
        HashMap<String,HashMap<String,Integer>> count=new HashMap<>();
        HashMap<String,Integer> access=new HashMap<>();
        HashMap<String,Integer> license=new HashMap<>();
        HashMap<String,Integer> status=new HashMap<>();
        HashMap<String,Integer> langScript=new HashMap<>();
        HashMap<String,Integer> tax=new HashMap<>();
        HashMap<String,Integer> topics=new HashMap<>();
        HashMap<String,WorkMatch> map=new HashMap<>();
        
        while(rs.hasNext()) {            
            QuerySolution qs=rs.next();
            String uri=qs.get("?s").asNode().getURI();
            String prop=qs.get("?p").asNode().getURI();            
            Node node=qs.get("?o").asNode();
            String val="";
            LiteralStringField lf=null;
            if(node.isURI()) {
                val=node.getURI();
            }
            if(node.isLiteral()) {
                lf=new LiteralStringField(prop,node.getLiteralLanguage(),node.getLiteral().getValue().toString());
            }
            WorkMatch wm=map.get(uri);            
            if(wm == null) {
                wm=new WorkMatch();
            }
            boolean done=false;
            if(prop.equals(MATCH)) {
                done=true;  
                lf=new LiteralStringField(PREFLABEL,node.getLiteralLanguage(),node.getLiteral().getValue().toString());
                wm.addMatch(lf);
            }
            if(prop.equals(WORK_GENRE)) {
                done=true;                
                wm.addTopic(val);
                String it=TaxModel.getTaxonomyItem(val);
                if(it !=null) {                    
                    Integer ct=tax.get(it);
                    if(ct!=null) {
                        tax.put(it, ct.intValue()+1);
                    }
                    else {
                        tax.put(it, 1);
                    }
                    wm.addTaxonomy(it);
                }
                Integer t=topics.get(val);
                if(t!=null) {
                    topics.put(val, t.intValue()+1);
                }
                else {
                    topics.put(val, 1);
                }
            }
            
            if(prop.equals(PREFLABEL)) {                
                done=true;                
                wm.setPrefLabel(node.getLiteral().toString());
            }   
            if(prop.equals(ACCESS)) {
                done=true;
                wm.setAccess(val);
                Integer ct=access.get(val);
                if(ct!=null) {
                    access.put(val, ct.intValue()+1);
                }
                else {
                    access.put(val, 1);
                }
            }
            if(prop.equals(LICENSE)) {
                done=true;
                wm.setLicense(val);
                Integer ct=license.get(val);
                if(ct!=null) {
                    license.put(val, ct.intValue()+1);
                }
                else {
                    license.put(val, 1);
                }
            }
            if(prop.equals(STATUS)) {
                done=true;
                wm.setStatus(val);
                Integer ct=status.get(val);
                if(ct!=null) {
                    status.put(val, ct.intValue()+1);
                }
                else {
                    status.put(val, 1);
                }
            }
            if(prop.equals(LANG_SCRIPT)) {
                done=true;
                String tmp=node.getURI();
                wm.setLangScript(tmp);
                Integer ct=langScript.get(tmp);
                if(ct!=null) {
                    langScript.put(tmp, ct.intValue()+1);
                }
                else {
                    langScript.put(tmp, 1);
                }
            }
            if(!done && lf!=null) {
                wm.addMatch(lf);
            }
            
            map.put(uri, wm);
        }
        res.put("data",map);
        count.put("access", access);
        count.put("license",license);
        count.put("status",status);
        count.put("langScript",langScript);
        count.put("taxonomies",tax);
        count.put("topics",topics);
        res.put("metadata",count);
        return res;
    }
}
