package io.bdrc.ldspdi.objects.json;

import java.util.ArrayList;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ldspdi.utils.Helpers;

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
    ArrayList<Param> params; 
    ArrayList<Output> outputs; 
    String template;
    String demoLink;    
    
    
    public QueryTemplate(String id, String domain, 
            String demoLink, String queryScope, String queryResults, 
            String queryReturn, String queryParams, ArrayList<Param> params,
            ArrayList<Output> outputs, String template) {
        super();
        this.id = id;
        this.domain=domain;        
        this.queryScope = queryScope;
        this.queryResults = queryResults;
        this.queryReturn = queryReturn;
        this.queryParams = queryParams;
        this.params = params;
        this.outputs = outputs;
        this.template = template;        
        if(queryReturn.equals(QueryConstants.GRAPH)) {
            this.demoLink = Helpers.bdrcEncode(ServiceConfig.getProperty("urlGraphPath")+demoLink);
        }else {
            this.demoLink = Helpers.bdrcEncode(ServiceConfig.getProperty("urlTemplatePath")+demoLink);
        }
    }
    
    public String getId() {
        return id;
    }
    public String getDomain() {
        return domain;
    }
    public String getQueryScope() {
        return queryScope;
    }
    public String getQueryResults() {
        return queryResults;
    }
    public ArrayList<Param> getParams() {
        return params;
    }
    public String getTemplate() {
        return template;
    }
    public String getQueryReturn() {
        return queryReturn;
    }
    public String getDemoLink() {
        return demoLink;
    }
    public ArrayList<Output> getOutputs() {
        return outputs;
    }    
    public String getQueryParams() {
        return queryParams;
    }
    
    
    
}
