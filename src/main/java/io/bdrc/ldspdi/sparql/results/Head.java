package io.bdrc.ldspdi.sparql.results;

import java.util.List;

public class Head {
    
    public List<String> vars;
    
    
    public Head(List<String> vars) {
        super();
        this.vars = vars;        
    }

    public List<String> getVars() {
        return vars;
    }

}
