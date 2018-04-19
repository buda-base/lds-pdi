package io.bdrc.ldspdi.results.library;

import java.util.HashMap;

import org.apache.jena.graph.Node;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import io.bdrc.ldspdi.results.LiteralStringField;

public class ResourceResults {
    
    static final String RELATION_TYPE="http://purl.bdrc.io/ontology/tmp/relationType";
    
    public static HashMap<String,Object> getResultsMap(ResultSet rs){
        
        HashMap<String,Object> res=new HashMap<>();
        HashMap<String,ResourceDesc> map=new HashMap<>();
        while(rs.hasNext()) {             
            QuerySolution qs=rs.next();            
            String uri=qs.get("?s").asNode().getURI();
            String prop=qs.get("?p").asNode().getURI();
            Node node=qs.get("?o").asNode();
            LiteralStringField lf=null;
            
            if(node.isLiteral()) {
                lf=new LiteralStringField(prop,node.getLiteralLanguage(),node.getLiteral().getValue().toString());
            }
            ResourceDesc desc=map.get(uri);            
            if(desc == null) {
                desc=new ResourceDesc();
            }
            desc.addMatch(lf);
            map.put(uri,desc);
        }
        res.put("data",map);
        return res;
    }

}
