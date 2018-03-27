package io.bdrc.ldspdi.sparql.results;

import java.util.HashMap;

@SuppressWarnings("serial")
public class Field extends HashMap<String,String>{    
    

    public Field(String type, String value) {
        super();
        this.put("type", type);        
        this.put("value", value);
    }

}
