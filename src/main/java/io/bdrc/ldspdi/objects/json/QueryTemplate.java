package io.bdrc.ldspdi.objects.json;

public class QueryTemplate {
    
    String id;
    String domain;
    String queryScope;
    String queryResults;
    String queryReturn;
    String queryParams;    
    String template;
    String demoLink;
    
    
    public QueryTemplate(String id, String domain, String demoLink, String queryScope, String queryResults, String queryReturn, String queryParams,
            String template) {
        super();
        this.id = id;
        this.domain=domain;
        this.queryScope = queryScope;
        this.queryResults = queryResults;
        this.queryReturn = queryReturn;
        this.queryParams = queryParams;        
        this.template = template;
        this.demoLink = demoLink;
    }
    
    
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getDomain() {
        return domain;
    }
    public void setDomain(String domain) {
        this.domain = domain;
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
