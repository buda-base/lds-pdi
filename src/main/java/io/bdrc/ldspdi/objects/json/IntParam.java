package io.bdrc.ldspdi.objects.json;

import io.bdrc.ldspdi.sparql.QueryConstants;

public class IntParam extends Param {
    
    public String description;
    
    public IntParam(String name) {
        super(QueryConstants.INT_PARAM,name);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "IntParam [description=" + description + "]";
    } 
    
    
}
