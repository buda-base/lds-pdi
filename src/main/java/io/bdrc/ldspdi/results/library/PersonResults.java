package io.bdrc.ldspdi.results.library;

import java.util.HashMap;

import org.apache.jena.graph.Node;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import io.bdrc.ldspdi.results.Field;
import io.bdrc.ldspdi.results.LiteralStringField;

public class PersonResults {
    
    static final String CREATOR_TYPE="http://purl.bdrc.io/ontology/tmp/creatorType";
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
                lf=new LiteralStringField(prop,node.getLiteralLanguage(),node.getLiteral().toString());
            }
            PersonMatch pm=map.get(uri);            
            if(pm == null) {
                pm=new PersonMatch();
            }
            if(prop.equals(CREATOR_TYPE)) {
                pm.addOptions(new Field(prop,val));
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
            }else {
                pm.addMatch(lf);
            }
            map.put(uri, pm);                
                       
        }
        res.put("data",map);
        res.put("metadata",count);
        return res;
    }

}
