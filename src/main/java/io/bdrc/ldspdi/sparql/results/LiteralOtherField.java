package io.bdrc.ldspdi.sparql.results;

public class LiteralOtherField extends Field{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
        
    
    public LiteralOtherField(String type,String datatype, String value) {
        super(type, value);
        this.put("datatype",datatype);               
    }
    
}
