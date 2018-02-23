package io.bdrc.ldspdi.objects.json;

import io.bdrc.ldspdi.sparql.QueryConstants;

public class StringParam extends Param {
    
    public String langTag;
    public String isLuceneParam;
    public String example;
    
    public StringParam(String name) {
        super(QueryConstants.STRING_PARAM,name);
    }
    
    public StringParam(String name,String langTag,String isLuceneParam,String example) {
        super(QueryConstants.STRING_PARAM,name);
        this.langTag=langTag;
        this.isLuceneParam=isLuceneParam;
        this.example=example;
    }
    
    public String getLangTag() {
        return langTag;
    }

    public void setLangTag(String langTag) {
        this.langTag = langTag;
    }

    public String getIsLuceneParam() {
        return isLuceneParam;
    }

    public void setIsLuceneParam(String isLuceneParam) {
        this.isLuceneParam = isLuceneParam;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    @Override
    public String toString() {
        return " <b>langTag</b>=" + langTag + ", <b>isLuceneParam</b>=" + isLuceneParam + ", <b>example</b>=" + example;
    }
    
    

}
