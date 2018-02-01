package io.bdrc.ldspdi.rest.resources;

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


import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;

import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.jena.query.ResultSet;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.ldspdi.Utils.StringHelpers;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.InjectionTracker;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ldspdi.sparql.QueryFileParser;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.sparql.results.ResultPage;
import io.bdrc.ldspdi.sparql.results.Results;
import io.bdrc.ldspdi.sparql.results.ResultsCache;

@Path("/")
public class PublicTemplatesResource {
    
    public static Logger log=Logger.getLogger(PublicDataResource.class.getName());
    
    QueryProcessor processor=new QueryProcessor();
    public String fusekiUrl="";
    
    @GET
    @Path("/resource/templates")
    public Response getQueryTemplateResults(@Context UriInfo info, @HeaderParam("fusekiUrl") final String fuseki) throws Exception{     
        
        log.info("Call to getQueryTemplateResults()"); 
        log.info("URL :"+info.getRequestUri());
        if(fuseki !=null){
            fusekiUrl=fuseki;
        }else {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);  
        }       
        MediaType media=new MediaType("text","html","utf-8");
        MultivaluedMap<String,String> mp=info.getQueryParameters();
        HashMap<String,String> converted=new HashMap<>();
        Set<String> set=mp.keySet();
        for(String st:set) {
            List<String> str=mp.get(st);
            for(String ss:str) {
                converted.put(st, StringHelpers.bdrcEncode(ss));                
            }
        }
        ObjectMapper mapper = new ObjectMapper();
        String filename= mp.getFirst(QueryConstants.SEARCH_TYPE)+".arq";
        String jsonParams=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mp);
        int pageSize =getPageSize(mp.getFirst(QueryConstants.PAGE_SIZE));
        int pageNumber=getPageNumber(mp.getFirst(QueryConstants.PAGE_NUMBER));
        int hash=getHash(mp.getFirst(QueryConstants.RESULT_HASH));
        QueryFileParser qfp;
        final String query;
        
        qfp=new QueryFileParser(filename);
        String q=qfp.getQuery();
        String check=qfp.checkQueryArgsSyntax();
        if(check.length()>0) {
            throw new Exception("Exception : File->"+ filename+".arq; ERROR: "+check);
        }
        log.info("Query before Injection Tracking -->"+filename+".arq"+System.lineSeparator()+q);
        query=InjectionTracker.getValidQuery(q, mp);            
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                // when prefix is null, QueryProcessor default prefix is used 
                Results res = getResults(query, fuseki, hash, pageSize); 
                ResultPage rp=new ResultPage(res,pageNumber,jsonParams);
                os.write(StringHelpers.renderHtmlResultPage(rp,info.getRequestUri().toString()).getBytes());
            }
        };
        return Response.ok(stream,media).build();
    } 
    
    @POST 
    @Path("/resource/templates")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getQueryTemplateResultsPost(@Context UriInfo info, 
            @HeaderParam("fusekiUrl") final String fuseki,
            MultivaluedMap<String,String> map) throws Exception{ 
        log.info("Call to getQueryTemplateResultsPost()");              
        if(fuseki !=null){
            fusekiUrl=fuseki;
        }else {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);  
        } 
        ObjectMapper mapper = new ObjectMapper();
        String filename= map.getFirst(QueryConstants.SEARCH_TYPE)+".arq";
        
        int pageSize =getPageSize(map.getFirst(QueryConstants.PAGE_SIZE));
        int pageNumber=getPageNumber(map.getFirst(QueryConstants.PAGE_NUMBER));
        int hash=getHash(map.getFirst(QueryConstants.RESULT_HASH));
        QueryFileParser qfp; 
        final String query;
        
        qfp=new QueryFileParser(filename);
        String q=qfp.getQuery();
        String check=qfp.checkQueryArgsSyntax();
        if(check.length()>0) {
            throw new Exception("Exception : File->"+ filename+".arq; ERROR: "+check);
        }
        query=InjectionTracker.getValidQuery(q, map);
        MultivaluedMap<String,String> copy=map;
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException { 
                
                Results res = getResults(query, fuseki, hash, pageSize); 
                copy.remove(QueryConstants.RESULT_HASH);
                copy.add(QueryConstants.RESULT_HASH, Integer.toString(res.getHash()));
                copy.remove(QueryConstants.PAGE_SIZE);
                copy.add(QueryConstants.PAGE_SIZE, Integer.toString(res.getPageSize()));
                String jsonParams=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(copy);
                ResultPage rp=new ResultPage(res,pageNumber,jsonParams);
                mapper.writerWithDefaultPrettyPrinter().writeValue(os , rp);                 
            }
        };
        return Response.ok(stream).build();
    }
    
    @POST 
    @Path("/resource/templates")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getQueryTemplateResultsJsonPost(@Context UriInfo info, 
            @HeaderParam("fusekiUrl") final String fuseki,
            HashMap<String,String> map) throws Exception{     
        log.info("Call to getQueryTemplateResultsJsonPost()");
        ObjectMapper mapper = new ObjectMapper();
        
        if(fuseki !=null){
            fusekiUrl=fuseki;
        }else {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);  
        }        
        String filename= map.get(QueryConstants.SEARCH_TYPE)+".arq"; 
        
        int pageSize =getPageSize(map.get(QueryConstants.PAGE_SIZE));
        int pageNumber=getPageNumber(map.get(QueryConstants.PAGE_NUMBER));
        int hash=getHash(map.get(QueryConstants.RESULT_HASH));
        
        QueryFileParser qfp;
        final String query;
        
        qfp=new QueryFileParser(filename);
        String q=qfp.getQuery();
        String check=qfp.checkQueryArgsSyntax();
        if(check.length()>0) {
            throw new Exception("Exception : File->"+ filename+".arq; ERROR: "+check);
        }
        query=InjectionTracker.getValidQuery(q, map);
        HashMap<String,String> copy=map;
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                Results res = getResults(query, fuseki, hash, pageSize);                
                copy.put(QueryConstants.RESULT_HASH, Integer.toString(res.getHash()));
                copy.put(QueryConstants.PAGE_SIZE, Integer.toString(res.getPageSize()));
                String jsonParams=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(copy);
                ResultPage rp=new ResultPage(res,pageNumber,jsonParams);
                mapper.writerWithDefaultPrettyPrinter().writeValue(os , rp); 
            }
        };
        return Response.ok(stream).build();
    }
    
    public Results getResults(String query, String fuseki, int hash, int pageSize) {
        Results res;
        if(hash ==-1) {
            long start=System.currentTimeMillis();
            ResultSet jrs=processor.getResultSet(query, fuseki);
            long end=System.currentTimeMillis();
            long elapsed=end-start;
            res=new Results(jrs,elapsed,pageSize);                    
            int new_hash=Objects.hashCode(res);                    
            res.setHash(new_hash);                    
            ResultsCache.addToCache(res, Objects.hashCode(res));
            log.info("New Results object loaded into cache with hash:"+new_hash);
        }
        else {
            res=ResultsCache.getResultsFromCache(hash);
            log.info("Got Results object from cache with hash:"+hash);
        }
        return res;
    }
    
    public int getPageSize(String param) {
        int pageSize;
        try {
            pageSize=Integer.parseInt(param);
            
        }catch(Exception ex){
            pageSize= Integer.parseInt(ServiceConfig.getProperty(QueryConstants.PAGE_SIZE));            
        }
        return pageSize;
    }
    
    public int getPageNumber(String param) {
        int pageNumber;
        try {
            pageNumber=Integer.parseInt(param);
            
        }catch(Exception ex){
            pageNumber= 1;            
        }
        return pageNumber;
    }
    
    public int getHash(String param) {
        int hash;
        try {
            hash=Integer.parseInt(param);            
        }catch(Exception ex){
            hash= -1;            
        }
        return hash;
    }
    
}
