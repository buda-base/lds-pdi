package io.bdrc.ldspdi.service;

import java.util.HashMap;

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

import javax.servlet.ServletContextEvent;

import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ontology.service.core.OntAccess;



public class BootClass implements javax.servlet.ServletContextListener{
	
	public void contextDestroyed(ServletContextEvent arg0) {
        //Do nothing;
    }
 
    public void contextInitialized(ServletContextEvent arg0) {
        try {
            
            String fuseki=arg0.getServletContext().getInitParameter("fuseki");            
            String queryPath=arg0.getServletContext().getInitParameter("queryPath");
            HashMap<String,String> params=new HashMap<>();            
            params.put(QueryConstants.QUERY_PATH,queryPath);
            params.put("fusekiUrl",fuseki);            
            GitService.update(queryPath);
            ServiceConfig.init(params); 
            OntAccess.init();
            
             
        } 
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        
    }
    
    private String initParamsString(String qPath,String f_server) {
        String params="<context-param>\n" + 
                "    <param-name>fuseki</param-name>\n" + 
                "    <param-value>"+f_server+"</param-value>\n" + 
                "</context-param>\n" + 
                "<context-param>\n" + 
                "    <param-name>queryPath</param-name>\n" + 
                "    <param-value>"+qPath+"</param-value>\n" + 
                "</context-param>";
        return params;
    }
    

}
