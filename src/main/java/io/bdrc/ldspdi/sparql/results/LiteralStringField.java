package io.bdrc.ldspdi.sparql.results;

public class LiteralStringField extends Field {
    
    
           
    
    public LiteralStringField(String type, String lang, String value) {
        super(type, value);        
        this.put("xml:lang", lang);        
    }
}
