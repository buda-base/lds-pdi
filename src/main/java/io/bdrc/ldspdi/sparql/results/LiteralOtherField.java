package io.bdrc.ldspdi.sparql.results;

@SuppressWarnings("serial")
public class LiteralOtherField extends Field{
            
    
    public LiteralOtherField(String type,String datatype, String value) {
        super(type, value);
        this.put("datatype",datatype);               
    }
    
}
