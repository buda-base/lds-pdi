package io.bdrc.ldspdi.sparql;

import java.util.HashMap;
import java.util.Objects;

import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;

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

import io.bdrc.ldspdi.results.ResultSetWrapper;
import io.bdrc.ldspdi.results.ResultsCache;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryProcessor {	
	
    public final static Logger log=LoggerFactory.getLogger(QueryProcessor.class.getName());
    
	public static Model getCoreResourceGraph(String resID,String fusekiUrl,String prefixes) throws RestException{			
	    if(prefixes==null) {
	        prefixes=loadPrefixes();
	    }
	    int hash=Objects.hashCode(resID);
	    Model model=(Model)ResultsCache.getObjectFromCache(hash);	    
	    if(model==null) {
	        QueryFileParser qfp=new QueryFileParser("Resgraph.arq","library"); 
	        HashMap<String,String> map=new HashMap<>();
	        map.put("R_RES", resID);
	        String query=qfp.getParametizedQuery(map,false);
    		Query q=QueryFactory.create(prefixes+" "+query);
    		QueryExecution qe = QueryExecutionFactory.sparqlService(fusekiUrl,q);
    		qe.setTimeout(Long.parseLong(ServiceConfig.getProperty(QueryConstants.QUERY_TIMEOUT)));
    		model = qe.execDescribe();
    		qe.close();
    		ResultsCache.addToCache(model, hash);
	    }
		return model;		
	}
	
	public static Model getGraph(String query,String fusekiUrl, String prefixes) throws RestException{           
	    if(prefixes==null) {
            prefixes=loadPrefixes();
        }
	    if(fusekiUrl == null) {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }
        int hash=Objects.hashCode(query);
        Model model=(Model)ResultsCache.getObjectFromCache(hash);
        if(model==null) {
            Query q=QueryFactory.create(prefixes+" "+query);            
            QueryExecution qe = QueryExecutionFactory.sparqlService(fusekiUrl,q);
            qe.setTimeout(Long.parseLong(ServiceConfig.getProperty(QueryConstants.QUERY_TIMEOUT)));
            model = qe.execConstruct();
            qe.close();
            ResultsCache.addToCache(model, hash);
        }
        return model;       
    }
	
	public static Model getAuthDataGraph(String fusekiUrl) throws RestException{  
	    if(fusekiUrl == null) {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        } 
        fusekiUrl = fusekiUrl.substring(0, fusekiUrl.lastIndexOf("/"));
        DatasetAccessor access=DatasetAccessorFactory.createHTTP(fusekiUrl);
        Model m=access.getModel(ServiceConfig.getProperty("authDataGraph"));
        return m;        
    }
		
	public static QueryExecution getResultSet(String query,String fusekiUrl){               
        if(fusekiUrl == null) {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }  
        QueryExecution qe = QueryExecutionFactory.sparqlService(fusekiUrl,QueryFactory.create(query)); 
        qe.setTimeout(Long.parseLong(ServiceConfig.getProperty(QueryConstants.QUERY_TIMEOUT)));
        return qe;           
    }
	
	public static ResultSet getData(String query,String fusekiUrl){
               
        if(fusekiUrl == null) {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }  
        QueryExecution qe = QueryExecutionFactory.sparqlService(fusekiUrl,QueryFactory.create(query)); 
        qe.setTimeout(Long.parseLong(ServiceConfig.getProperty(QueryConstants.QUERY_TIMEOUT)));
        return qe.execSelect();           
    }
	
	public static void updateOntology(Model mod, String fusekiUrl) {
	    if(fusekiUrl == null) {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }
	    log.info("Service fuseki >> "+fusekiUrl);
	    log.info("OntologyGraph >> "+ServiceConfig.getProperty("ontGraph"));
	    log.info("InfModel Size >> "+mod.size());	    
	    DatasetAccessor access=DatasetAccessorFactory.createHTTP(fusekiUrl);
	    access.putModel(ServiceConfig.getProperty("ontGraph"), mod);	    
	}
	
	public static void updateAuthOntology(Model mod, String fusekiUrl) {
        if(fusekiUrl == null) {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }
        log.info("Service fuseki >> "+fusekiUrl);
        log.info("AuthGraph >> "+ServiceConfig.getProperty("authGraph"));
        log.info("InfModel Size >> "+mod.size());       
        DatasetAccessor access=DatasetAccessorFactory.createHTTP(fusekiUrl);        
        access.putModel(ServiceConfig.getProperty("authGraph"), mod);         
    }
	
	public static ResultSetWrapper getResults(String query, String fuseki, String hash, String pageSize) {
        ResultSetWrapper res;
        
        if(hash==null) {            
            long start=System.currentTimeMillis(); 
            QueryExecution qe=getResultSet(query, fuseki);
            ResultSet jrs=qe.execSelect();           
            long elapsed=System.currentTimeMillis()-start;
            int psz=Integer.parseInt(ServiceConfig.getProperty(QueryConstants.PAGE_SIZE));  
            if(pageSize!=null) {
                psz=Integer.parseInt(pageSize);
            }
            res=new ResultSetWrapper(jrs,elapsed,psz); 
            qe.close();
            int new_hash=Objects.hashCode(res);                    
            res.setHash(new_hash);                    
            ResultsCache.addToCache(res, Objects.hashCode(res));
            return res;
        }
        else {            
            return (ResultSetWrapper)ResultsCache.getObjectFromCache(Integer.parseInt(hash));            
        }
    }
	
	public static ResultSet getResultsFromModel(String query, Model model) throws RestException {
	    
	    try {
    	    QueryExecution qexec = QueryExecutionFactory.create(query, model);
            ResultSet res = qexec.execSelect() ;
            return res;
	    }
	    catch(Exception ex) {
	        throw new RestException(500, new LdsError(LdsError.SPARQL_ERR).
	                setContext(" in QueryProcessor.getResultsFromModel(query, model)) \""+query+"\"",ex));
	    }
	}
	
	private static String loadPrefixes() throws RestException {
	    String pref=Prefixes.getPrefixes();
        if(pref!=null) {
            return pref;
        }
        else {
            return "PREFIX : <http://purl.bdrc.io/ontology/core/>\n" + 
                    " PREFIX bdo: <http://purl.bdrc.io/ontology/core/>\n" + 
                    " PREFIX adm: <http://purl.bdrc.io/ontology/admin/>\n" + 
                    " PREFIX bdr: <http://purl.bdrc.io/resource/>\n" + 
                    " PREFIX bdan: <http://purl.bdrc.io/annotation/>\n" +
                    " PREFIX bdac: <http://purl.bdrc.io/anncollection/>\n" +
                    " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" + 
                    " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + 
                    " PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" + 
                    " PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" + 
                    " PREFIX bf: <http://id.loc.gov/ontologies/bibframe/>\n" + 
                    " PREFIX tbr: <http://purl.bdrc.io/ontology/toberemoved/>\n" + 
                    " PREFIX vcard: <http://www.w3.org/2006/vcard/ns#>\n" + 
                    " PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" + 
                    " PREFIX text: <http://jena.apache.org/text#>\n" + 
                    " PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" + 
                    " PREFIX dcterms: <http://purl.org/dc/terms/>\n" + 
                    " PREFIX f: <java:io.bdrc.ldspdi.sparql.functions.>"+
                    " PREFIX aut:   <http://purl.bdrc.io/ontology/ext/auth/>"+
                    " PREFIX adr:   <http://purl.bdrc.io/resource-auth/>";
        }
	}
}
