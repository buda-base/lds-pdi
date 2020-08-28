package io.bdrc.ldspdi.objects.json;

import io.bdrc.ldspdi.sparql.QueryConstants;

public class DateTimeParam extends Param {
    
    public String description;
    
    public DateTimeParam(String name) {
        super(QueryConstants.DATETIME_PARAM,name);
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
