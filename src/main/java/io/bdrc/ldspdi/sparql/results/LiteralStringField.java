package io.bdrc.ldspdi.sparql.results;

public class LiteralStringField extends Field {
    
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
        
    
    public LiteralStringField(String type, String lang, String value) {
        super(type, value);        
        this.put("\"xml:lang\"", lang);        
    }
}
