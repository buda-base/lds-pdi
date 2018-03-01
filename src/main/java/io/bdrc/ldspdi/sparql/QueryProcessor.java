package io.bdrc.ldspdi.sparql;

import java.util.Objects;

/*******************************************************************************
 * Copyright (c) 2017 Buddhist Digital Resource Center (BDRC)
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

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.results.ResultSetWrapper;
import io.bdrc.ldspdi.sparql.results.ResultsCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryProcessor {	
	
    public static Logger log=LoggerFactory.getLogger(QueryProcessor.class.getName());
    
	public static Model getResourceGraph(String resID,String fusekiUrl){			
		
	    String prefixes=ServiceConfig.getPrefixes();
		Query q=QueryFactory.create(prefixes+" DESCRIBE <http://purl.bdrc.io/resource/"+resID.trim()+">");
		log.info("Processor query describe:" +q);
		QueryExecution qe = QueryExecutionFactory.sparqlService(fusekiUrl,q);
		Model model = qe.execDescribe();		
		return model;		
	}
		
	public static ResultSet getResultSet(String query,String fusekiUrl){
        log.info("Processor Json query select:" +query);        
        if(fusekiUrl == null) {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }        
        Query q=QueryFactory.create(query);        
        QueryExecution qe = QueryExecutionFactory.sparqlService(fusekiUrl,q);
        ResultSet rs = qe.execSelect();
        return rs;           
    }
	
	public static ResultSetWrapper getResults(String query, String fuseki, String hash, String pageSize) {
        ResultSetWrapper res;
        if(hash==null) {
            long start=System.currentTimeMillis();            
            ResultSet jrs=getResultSet(query, fuseki);
            long end=System.currentTimeMillis();
            long elapsed=end-start;
            int psz=Integer.parseInt(ServiceConfig.getProperty(QueryConstants.PAGE_SIZE));  
            if(pageSize!=null) {
                psz=Integer.parseInt(pageSize);
            }
            res=new ResultSetWrapper(jrs,elapsed,psz);                    
            int new_hash=Objects.hashCode(res);                    
            res.setHash(new_hash);                    
            ResultsCache.addToCache(res, Objects.hashCode(res));            
        }
        else {
            res=ResultsCache.getResultsFromCache(Integer.parseInt(hash));            
        }
        return res;
    }


}
