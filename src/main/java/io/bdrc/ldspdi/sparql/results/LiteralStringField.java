package io.bdrc.ldspdi.sparql.results;

public class LiteralStringField extends Field {
    
    
    public String lang;
    public String datatype;
    
    
    public LiteralStringField(String type,String datatype, String lang, String value) {
        super(type, value);
        this.datatype=datatype;
        this.lang = lang;        
    }
    
    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
}
