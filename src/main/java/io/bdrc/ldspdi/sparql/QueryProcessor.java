package io.bdrc.ldspdi.sparql;

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

import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;

import io.bdrc.ldspdi.service.ServiceConfig;

public class QueryProcessor {	
	
	
	public Model getResourceGraph(String resID,String fusekiUrl){			
		
	    String prefixes=ServiceConfig.getPrefixes();
		Query q=QueryFactory.create(prefixes+" DESCRIBE <http://purl.bdrc.io/resource/"+resID.trim()+">");
		System.out.println("Processor query describe:" +q);
		QueryExecution qe = QueryExecutionFactory.sparqlService(fusekiUrl,q);
		Model model = qe.execDescribe();
		
		return model;		
	}
		
	public ResultSet getResultSet(String query,String fusekiUrl){
        System.out.println("Processor Json query select:" +query);        
        if(fusekiUrl == null) {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }        
        Query q=QueryFactory.create(query);        
        QueryExecution qe = QueryExecutionFactory.sparqlService(fusekiUrl,q);
        ResultSet rs = qe.execSelect();
        return rs;           
    }

}
