package io.bdrc.ldspdi.objects.json;

import io.bdrc.ldspdi.sparql.QueryConstants;

public class GYearParam extends Param {
    
    public String description;
    
    public GYearParam(String name) {
        super(QueryConstants.GY_PARAM,name);
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
