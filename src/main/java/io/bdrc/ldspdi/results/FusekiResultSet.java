package io.bdrc.ldspdi.results;

import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings("serial")
public class FusekiResultSet extends HashMap<String,Object>{
    
    
    public FusekiResultSet(ResultSetWrapper res) {
        Head head=new Head(res.getHead());
        HashMap<String,ArrayList<Row>> results=new HashMap<>();
        results.put("bindings", res.getRows());
        this.put("head", head);
        this.put("results",results);
    }

}
