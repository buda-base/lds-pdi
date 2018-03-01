package io.bdrc.ldspdi.sparql.results;

public class LiteralField extends Field {
    
    
    public String lang;
    
    
    public LiteralField(String type,String lang, String value) {
        super(type, value);        
        this.lang = lang;        
    }

    

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    
    
    

}
