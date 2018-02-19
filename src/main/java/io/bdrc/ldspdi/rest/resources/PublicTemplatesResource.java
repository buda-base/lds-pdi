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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.ldspdi.Utils.Helpers;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.InjectionTracker;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ldspdi.sparql.QueryFileParser;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.sparql.results.JsonResult;
import io.bdrc.ldspdi.sparql.results.ResultPage;
import io.bdrc.ldspdi.sparql.results.Results;
import io.bdrc.restapi.exceptions.RestException;


@Path("/")
public class PublicTemplatesResource {
    
    public static Logger log=LoggerFactory.getLogger(PublicDataResource.class.getName());
    
    public String fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
    
    public PublicTemplatesResource() {
        super();        
        ResourceConfig config=new ResourceConfig(PublicTemplatesResource.class);
        config.register(CorsFilter.class);        
    } 
    
    @GET
    @Path("/resource/templates")
    @Produces("text/html")
    public Viewable getQueryTemplateResults(@Context UriInfo info, 
            @HeaderParam("fusekiUrl") final String fuseki) throws RestException{     
        
        log.info("Call to getQueryTemplateResults()");
        if(fuseki !=null){fusekiUrl=fuseki;}        
        
        //Settings       
        HashMap<String,String> hm=Helpers.convertMulti(info.getQueryParameters());        
        hm.put(QueryConstants.REQ_URI, info.getRequestUri().toString().replace(info.getBaseUri().toString(), "/"));        
                
        //params
        String filename= hm.get(QueryConstants.SEARCH_TYPE)+".arq";
        
        //process
        QueryFileParser qfp=new QueryFileParser(filename);;
        final String query;
        String check=qfp.checkQueryArgsSyntax();
        if(!check.trim().equals("")) {
            throw new RestException(500,RestException.GENERIC_APP_ERROR_CODE,"Exception : File->"+ filename+"; ERROR: "+check);
        }
        query=InjectionTracker.getValidQuery(qfp.getQuery(), hm,qfp.getLitLangParams());
        boolean error=query.startsWith(QueryConstants.QUERY_ERROR);
        String msg =query;
        if(error) {
            return new Viewable("/error.jsp",msg);
        }
        Results res = QueryProcessor.getResults(
                query, 
                fuseki, 
                hm.get(QueryConstants.RESULT_HASH), 
                hm.get(QueryConstants.PAGE_SIZE));
        ResultPage model=null;
        try {
            if(hm.get(QueryConstants.JSON_OUT)!=null) {
                JsonResult mod=new JsonResult(res,hm);
                hm.remove("query");            
                String it=new ObjectMapper().writeValueAsString(mod);            
                return new Viewable("/json.jsp",it);
            }
            hm.put(QueryConstants.REQ_METHOD, "GET");             
            hm.put("query", qfp.getQueryHtml());
            model=new ResultPage(res,hm.get(QueryConstants.PAGE_NUMBER),hm,qfp.getTemplate());
        }
        catch (JsonProcessingException jx) {
            throw new RestException(500,RestException.GENERIC_APP_ERROR_CODE,"JsonProcessingException"+jx.getMessage());
        }
        return new Viewable("/resPage.jsp",model);
    }
       
    @POST 
    @Path("/resource/templates")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getQueryTemplateResultsPost(
            @HeaderParam("fusekiUrl") final String fuseki,
            MultivaluedMap<String,String> mp) throws RestException{ 
        log.info("Call to getQueryTemplateResultsPost()");              
        
        if(fuseki !=null){fusekiUrl=fuseki;}
        HashMap<String,String> hm=Helpers.convertMulti(mp);        
        String filename= hm.get(QueryConstants.SEARCH_TYPE)+".arq";
        
        
        QueryFileParser qfp=new QueryFileParser(filename); 
        final String query;        
        String check=qfp.checkQueryArgsSyntax();
        if(!check.trim().equals("")) {
            throw new RestException(500,RestException.GENERIC_APP_ERROR_CODE,"Exception : File->"+ filename+"; ERROR: "+check);
        }
        query=InjectionTracker.getValidQuery(
                qfp.getQuery(), 
                hm,
                qfp.getLitLangParams());
        
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException { 
                if(query.startsWith(QueryConstants.QUERY_ERROR)) {
                    os.write(query.getBytes());
                }else {
                    Results res = QueryProcessor.getResults(
                            query, 
                            fuseki, 
                            hm.get(QueryConstants.RESULT_HASH), 
                            hm.get(QueryConstants.PAGE_SIZE));                
                    hm.put(QueryConstants.RESULT_HASH, Integer.toString(res.getHash()));
                    hm.put(QueryConstants.PAGE_SIZE, Integer.toString(res.getPageSize()));                
                    JsonResult rp=new JsonResult(res,hm);
                    new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(os , rp); 
                }
            }
        };
        return Response.ok(stream).build();
    }
    
    @POST 
    @Path("/resource/templates")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getQueryTemplateResultsJsonPost( 
            @HeaderParam("fusekiUrl") final String fuseki,
            HashMap<String,String> map) throws RestException{     
        log.info("Call to getQueryTemplateResultsJsonPost()");
        ObjectMapper mapper = new ObjectMapper();
        
        if(fuseki !=null){fusekiUrl=fuseki;}        
        String filename= map.get(QueryConstants.SEARCH_TYPE)+".arq"; 
        
        QueryFileParser qfp=new QueryFileParser(filename);;
        final String query;        
        String check=qfp.checkQueryArgsSyntax();
        if(!check.trim().equals("")) {
            throw new RestException(500,RestException.GENERIC_APP_ERROR_CODE,"Exception : File->"+ filename+"; ERROR: "+check);
        }
        query=InjectionTracker.getValidQuery(
                qfp.getQuery(), 
                map,
                qfp.getLitLangParams());        
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                if(query.startsWith(QueryConstants.QUERY_ERROR)) {
                    os.write(query.getBytes());
                }else {
                    Results res = QueryProcessor.getResults(
                            query, 
                            fuseki, 
                            map.get(QueryConstants.RESULT_HASH), 
                            map.get(QueryConstants.PAGE_SIZE));                
                    map.put(QueryConstants.RESULT_HASH, Integer.toString(res.getHash()));
                    map.put(QueryConstants.PAGE_SIZE, Integer.toString(res.getPageSize()));
                    JsonResult rp=new JsonResult(res,map);
                    mapper.writerWithDefaultPrettyPrinter().writeValue(os , rp); 
                }
            }
        };
        return Response.ok(stream).build();
    }
}
