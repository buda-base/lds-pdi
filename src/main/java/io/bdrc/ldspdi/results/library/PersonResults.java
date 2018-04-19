package io.bdrc.ldspdi.results.library;

import java.util.HashMap;

import org.apache.jena.graph.Node;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.rest.resources.LibrarySearchResource;
import io.bdrc.ldspdi.results.Field;
import io.bdrc.ldspdi.results.LiteralStringField;

public class PersonResults {
    
    public final static Logger log=LoggerFactory.getLogger(PersonResults.class.getName());
    
    static final String LABEL_MATCH="http://purl.bdrc.io/ontology/tmp/prefLabelMatch";
    static final String PREF_LABEL="http://www.w3.org/2004/02/skos/core#prefLabel";
    static final String RELATION_TYPE="http://purl.bdrc.io/ontology/tmp/relationType";
    static final String WORK_ABOUT="http://purl.bdrc.io/ontology/core/workIsAbout";
    static final String PERSONGENDER="http://purl.bdrc.io/ontology/core/personGender";    
    
    public static HashMap<String,Object> getResultsMap(ResultSet rs){
        HashMap<String,Object> res=new HashMap<>();
        HashMap<String,Integer> count=new HashMap<>();
        HashMap<String,PersonMatch> map=new HashMap<>();        
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
            PersonMatch pm=map.get(uri);            
            if(pm == null) {
                pm=new PersonMatch();
            }
            if(prop.equals(RELATION_TYPE)) {                
                pm.addOptions(new Field(prop,val));
            }
            if(prop.equals(WORK_ABOUT)) {
                PersonMatch pm1=map.get(val);
                if(pm1==null) {
                    pm1=new PersonMatch();
                }
                pm1.addOptions(new Field(prop,val));
            }
            if(prop.equals(PERSONGENDER)) {
                pm.setGender(val);
                Integer ct=count.get(val);
                if(ct!=null) {
                    count.put(val, ct.intValue()+1);
                }
                else {
                    count.put(val, 1);
                }
            }
            if(prop.equals(PREF_LABEL)) {
                log.info("URI >>> "+uri+"  Pref label >> "+node.getLiteral().getValue().toString());
                pm.addPrefLabel(lf);               
            }
            else {
                if(lf!=null) {
                    pm.addMatch(lf);
                }
            }
            map.put(uri, pm);                
                       
        }
        res.put("data",map);
        res.put("metadata",count);
        return res;
    }

}
