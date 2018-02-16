package io.bdrc.ldspdi.objects.json;

/*******************************************************************************
 * Copyright (c) 2018 Buddhist Digital Resource Center (BDRC)
 * 
 * If this file is a derivation of another work the license header will appear below; 
 * otherwise, this work is licensed under the Apache License, Version 2.0 
 * (the "License"); you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

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
