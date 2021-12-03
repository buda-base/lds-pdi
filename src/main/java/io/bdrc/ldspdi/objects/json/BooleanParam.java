package io.bdrc.ldspdi.objects.json;


import io.bdrc.ldspdi.sparql.QueryConstants;

public class BooleanParam extends Param {
    
    public String description;
    
    public BooleanParam(String name) {
        super(QueryConstants.BOOLEAN_PARAM,name);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return " <b>description : </b>" + description;
    } 
    
    
}
