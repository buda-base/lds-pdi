package io.bdrc.ldspdi.sparql.results;

import java.util.ArrayList;
import java.util.HashMap;

public class FusekiResultSet extends HashMap<String,Object>{
    
    /*public Head head;
    public HashMap<String,ArrayList<Row>> results;*/
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public FusekiResultSet(ResultSetWrapper res) {
        Head head=new Head(res.getHead());
        HashMap<String,ArrayList<Row>> results=new HashMap<>();
        results.put("\"bindings\"", res.getRows());
        this.put("\"head\"", head);
        this.put("\"results\"",results);
    }

}
