package io.bdrc.ldspdi.results.library;

import java.util.HashMap;

import org.apache.jena.graph.Node;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import io.bdrc.ldspdi.results.LiteralStringField;

public class RootResults {
    
    static final String TYPE="http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    static final String PREFLABEL="http://www.w3.org/2004/02/skos/core#prefLabel";    
    
    
    public static HashMap<String,Object> getResultsMap(ResultSet rs){
        HashMap<String,Object> res=new HashMap<>();
        HashMap<String,Integer> count=new HashMap<>();
        HashMap<String,RootMatch> map=new HashMap<>();        
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
            RootMatch rm=map.get(uri);
            if(rm == null) {
                rm=new RootMatch();
            }
            if(prop.equals(TYPE)) {
                rm.setType(val);
                Integer ct=count.get(val);
                if(ct!=null) {
                    count.put(val, ct.intValue()+1);
                }
                else {
                    count.put(val, 1);
                }
            }
            if(prop.equals(PREFLABEL)) {
                rm.setLabel(lf);
            }
            if(!prop.equals(PREFLABEL) && !prop.equals(TYPE)) {
                rm.addMatch(lf);
            }
            map.put(uri, rm);                
                       
        }
        res.put("data",map);
        res.put("metadata",count);
        return res;
    }

}
