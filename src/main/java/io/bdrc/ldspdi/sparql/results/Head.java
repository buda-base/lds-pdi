package io.bdrc.ldspdi.sparql.results;

import java.util.HashMap;
import java.util.List;

public class Head extends HashMap<String,List<String>>{
    
    
    public List<String> vars;
    
    public Head(List<String> vars) {
        super();
        this.put("vars",vars);        
    }
    

}
