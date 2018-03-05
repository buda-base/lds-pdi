package io.bdrc.ldspdi.sparql.results;

public class LiteralOtherField extends Field{

    public String datatype;
    
    
    public LiteralOtherField(String type,String datatype, String value) {
        super(type, value);
        this.datatype=datatype;               
    }
    
    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }
    
}
