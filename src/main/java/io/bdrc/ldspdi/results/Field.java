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
            //String value = st.getObject().asLiteral().getValue().toString();
            String value = st.getObject().asLiteral().getLexicalForm();
            return new LiteralStringField(st.getPredicate().getURI(),st.getObject().asLiteral().getLanguage(), value);  
        }else {
            if(st.getObject().isAnon()) {
                return new Field(st.getPredicate().getURI(),"_:"+st.getObject().asNode().getBlankNodeLabel());
            }
            else {
                return new Field(st.getPredicate().getURI(),st.getObject().toString());
            }
        }
    }

}
