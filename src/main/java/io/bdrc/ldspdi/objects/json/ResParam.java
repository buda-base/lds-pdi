package io.bdrc.ldspdi.objects.json;

import io.bdrc.ldspdi.sparql.QueryConstants;

public class ResParam extends Param {
    
    public String description;
    public String subType;
    
    public ResParam(String name,String subType) {
        super(QueryConstants.RES_PARAM,name);
        this.subType=subType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    @Override
    public String toString() {
        return " <b>description : </b>" + description + ", <b>subType : </b>" + subType;
    }
    
    

}
