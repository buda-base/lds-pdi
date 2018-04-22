package io.bdrc.ldspdi.results;

import java.util.HashMap;

import org.apache.jena.rdf.model.Statement;

@SuppressWarnings("serial")
public class Field extends HashMap<String,String>{    
    

    public Field(String type, String value) {
        super();
        this.put("type", type);        
        this.put("value", value);
    }
    
    public static Field getField(Statement st) {
        if(st.getObject().isLiteral()) {
            return new LiteralStringField(st.getPredicate().getURI(),st.getObject().asLiteral().getLanguage(),st.getObject().asLiteral().getValue().toString());  
        }else {
            return new Field(st.getPredicate().getURI(),st.getObject().toString());
        }
    }

}
