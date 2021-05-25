package io.bdrc.ldspdi.results;

import java.util.HashMap;

import org.apache.jena.rdf.model.Literal;
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
            final Literal l = st.getObject().asLiteral();
            final String value = l.getLexicalForm();
            if (!l.getLanguage().isEmpty())
                return new LiteralStringField(st.getPredicate().getURI(), l.getLanguage(), value);  
            return new LiteralOtherField(st.getPredicate().getURI(), l.getDatatypeURI(), value);
        } else {
            if(st.getObject().isAnon()) {
                return new Field(st.getPredicate().getURI(),"_:"+st.getObject().asNode().getBlankNodeLabel());
            }
            else {
                return new Field(st.getPredicate().getURI(),st.getObject().toString());
            }
        }
    }

}
