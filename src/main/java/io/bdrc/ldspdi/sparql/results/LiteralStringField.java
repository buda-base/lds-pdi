package io.bdrc.ldspdi.sparql.results;

@SuppressWarnings("serial")
public class LiteralStringField extends Field {
    
    public LiteralStringField(String type, String lang, String value) {
        super(type, value);        
        this.put("xml:lang", lang);        
    }
}
