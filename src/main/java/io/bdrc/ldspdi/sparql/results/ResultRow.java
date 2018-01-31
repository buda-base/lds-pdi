package io.bdrc.ldspdi.sparql.results;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.jena.query.QuerySolution;


public class ResultRow implements Serializable{
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public static Logger log=Logger.getLogger(ResultRow.class.getName());
    ArrayList<QuerySolutionItem> items;
    
    public ResultRow(QuerySolution qs, List<String> headers) {
        items=new ArrayList<>();
        //for(String key:headers) {
            //RDFNode node=qs.get(key);
            //log.info("Key ="+key+" Node="+node);
            //if(node !=null) {                
                items.add(new QuerySolutionItem(qs,headers));
            //}
        //}        
    }

    public ArrayList<QuerySolutionItem> getItems() {
        return items;
    }
    
    

}
