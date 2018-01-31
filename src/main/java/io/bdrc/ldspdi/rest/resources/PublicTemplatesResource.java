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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
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
import io.bdrc.ldspdi.sparql.QueryFileParser;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.sparql.results.ResultSetCopy;

@Path("/")
public class PublicTemplatesResource {
    
    public static Logger log=Logger.getLogger(PublicDataResource.class.getName());
    
    QueryProcessor processor=new QueryProcessor();
    public String fusekiUrl="";
    
    @GET
    @Path("/resource/templates")
    public Response getQueryTemplateResults(@Context UriInfo info, @HeaderParam("fusekiUrl") final String fuseki) throws Exception{     
        
        log.info("Call to getQueryTemplateResults()");           
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
        String filename= mp.getFirst("searchType")+".arq";      
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
                String res=processor.getResource(query, fusekiUrl, true);
                os.write(res.getBytes());
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
        String filename= map.getFirst("searchType")+".arq";      
        QueryFileParser qfp;
        final String query;
        
        qfp=new QueryFileParser(filename);
        String q=qfp.getQuery();
        String check=qfp.checkQueryArgsSyntax();
        if(check.length()>0) {
            throw new Exception("Exception : File->"+ filename+".arq; ERROR: "+check);
        }
        query=InjectionTracker.getValidQuery(q, map); 
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {               
                long start=System.currentTimeMillis();
                ResultSet jrs=processor.getResultSet(query, fuseki);
                long end=System.currentTimeMillis();
                long elapsed=end-start;
                ObjectMapper mapper = new ObjectMapper();
                ResultSetCopy copy=new ResultSetCopy(jrs,elapsed);
                mapper.writerWithDefaultPrettyPrinter().writeValue(os , copy);
                //JSONLDFormatter.jsonObjectToOutputStream(copy, os);  
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
        if(fuseki !=null){
            fusekiUrl=fuseki;
        }else {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);  
        }        
        String filename= map.get("searchType")+".arq";      
        QueryFileParser qfp;
        final String query;
        
        qfp=new QueryFileParser(filename);
        String q=qfp.getQuery();
        String check=qfp.checkQueryArgsSyntax();
        if(check.length()>0) {
            throw new Exception("Exception : File->"+ filename+".arq; ERROR: "+check);
        }
        query=InjectionTracker.getValidQuery(q, map); 
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                        long start=System.currentTimeMillis();
                        ResultSet jrs=processor.getResultSet(query, fuseki);
                        long end=System.currentTimeMillis();
                        long elapsed=end-start;ObjectMapper mapper = new ObjectMapper();
                        ResultSetCopy copy=new ResultSetCopy(jrs,elapsed);
                        mapper.writerWithDefaultPrettyPrinter().writeValue(os , copy);  
            }
        };
        return Response.ok(stream).build();
    }
}
