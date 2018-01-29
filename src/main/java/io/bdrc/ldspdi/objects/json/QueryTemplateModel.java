package io.bdrc.ldspdi.objects.json;

public class QueryTemplateModel {
    
    String name;
    String queryScope;
    String queryResults;
    String queryParams;
    String queryUrl;
    String template;
    
    
    
    public QueryTemplateModel(String name, String queryScope, String queryResults, String queryParams,
            String queryUrl, String template) {
        super();
        this.name = name;
        this.queryScope = queryScope;
        this.queryResults = queryResults;
        this.queryParams = queryParams;
        this.queryUrl = queryUrl;
        this.template = template;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getQueryScope() {
        return queryScope;
    }
    public void setQueryScope(String queryScope) {
        this.queryScope = queryScope;
    }
    public String getQueryResults() {
        return queryResults;
    }
    public void setQueryResults(String queryResults) {
        this.queryResults = queryResults;
    }
    public String getQueryParams() {
        return queryParams;
    }
    public void setQueryParams(String queryParams) {
        this.queryParams = queryParams;
    }
    public String getQueryUrl() {
        return queryUrl;
    }
    public void setQueryUrl(String queryUrl) {
        this.queryUrl = queryUrl;
    }
    public String getTemplate() {
        return template;
    }
    public void setTemplate(String template) {
        this.template = template;
    }
    
}
