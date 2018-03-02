package io.bdrc.ldspdi.sparql.results;

import java.util.HashMap;
import java.util.List;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryMvcSolutionItem {    
        
    public final static Logger log=LoggerFactory.getLogger(QuerySolutionItem.class.getName());    
    public HashMap<String,String> dataRow;
    
    public QueryMvcSolutionItem(QuerySolution qs,List<String> headers) {
        
        dataRow=new HashMap<>();
        for(String key:headers) {
            RDFNode node=qs.get(key);
            
            if(node !=null) { 
                
                if(node.isResource()) {
                    Resource res=node.asResource();
                    String Uri=res.getURI();
                    String tmp="";
                    if(node.asNode().isBlank()) {
                        tmp=res.getLocalName();
                    }
                    else {
                        if(Uri.startsWith("http://purl.bdrc.io/resource")) {
                            tmp="<a href=/resource/"+res.getLocalName()+"> "+res.getLocalName()+"</a>";
                        }else if(Uri.startsWith("http://purl.bdrc.io/ontology/core/")){
                            tmp="<a href=/demo/ontology?classUri="+Uri+"> "+res.getLocalName()+"</a>"; 
                        }
                    }
                    dataRow.put(key, tmp);                    
                } 
                if(node.isLiteral()) {
                    dataRow.put(key, node.asLiteral().toString());                    
                }
                if(node.isAnon()) {
                    dataRow.put(key, node.toString());                    
                }                
            }else {
                dataRow.put(key, "");
            }
        }        
    }
        
    public HashMap<String, String> getDataRow() {
        return dataRow;
    }
    
    public String getValue(String key) {
        return dataRow.get(key);
    }


}
