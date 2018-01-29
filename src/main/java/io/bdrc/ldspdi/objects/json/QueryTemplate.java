package io.bdrc.ldspdi.objects.json;

public class QueryTemplate {
    
    String name;
    String queryScope;
    String queryResults;
    String queryReturn;
    String queryParams;
    String queryString;
    String template;
    String demoLink;
    
    
    
    public QueryTemplate(String name, String demoLink, String queryScope, String queryResults, String queryReturn, String queryParams,
            String queryString, String template) {
        super();
        this.name = name;
        this.queryScope = queryScope;
        this.queryResults = queryResults;
        this.queryReturn = queryReturn;
        this.queryParams = queryParams;
        this.queryString = queryString;
        this.template = template;
        this.demoLink = demoLink;
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
    
    public String getQueryString() {
        return queryString;
    }
    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }
    public String getTemplate() {
        return template;
    }
    public void setTemplate(String template) {
        this.template = template;
    }
    public String getQueryReturn() {
        return queryReturn;
    }
    public void setQueryReturn(String queryReturn) {
        this.queryReturn = queryReturn;
    }
    public String getDemoLink() {
        return demoLink;
    }
    public void setDemoLink(String demoLink) {
        this.demoLink = demoLink;
    }
    
}
