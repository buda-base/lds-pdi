package io.bdrc.ldspdi.sparql.results;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;


public class QuerySolutionItem implements Serializable{
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public static Logger log=Logger.getLogger(QuerySolutionItem.class.getName());    
    public HashMap<String,String> dataRow;
    
    public QuerySolutionItem(QuerySolution qs,List<String> headers) {
        
        dataRow=new HashMap<>();
        for(String key:headers) {
            RDFNode node=qs.get(key);
            
            if(node !=null) { 
                
                if(node.isResource()) {
                    Resource res=node.asResource();
                    dataRow.put(key, res.getURI());                    
                } 
                if(node.isLiteral()) {
                    dataRow.put(key, node.asLiteral().toString());                    
                }
                if(node.isAnon()) {
                    dataRow.put(key, node.toString());                    
                }
            }            
        }        
    }
        
    public HashMap<String, String> getDataRow() {
        return dataRow;
    }

}
