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


import java.util.HashMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.InjectionTracker;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ldspdi.sparql.QueryFileParser;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.sparql.results.Results;
import io.bdrc.ldspdi.sparql.results.ResultPage;
import io.bdrc.ldspdi.sparql.results.ResultSetWrapper;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.ldspdi.utils.ResponseOutputStream;
import io.bdrc.restapi.exceptions.RestException;


@Path("/")
public class PublicTemplatesResource {
    
    public final static Logger log=LoggerFactory.getLogger(PublicDataResource.class.getName());
    
    public String fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
    
    public PublicTemplatesResource() {
        super();        
        ResourceConfig config=new ResourceConfig(PublicTemplatesResource.class);
        config.register(CorsFilter.class);        
    }
    
    @GET
    @Path("/query/{file}")
    @Produces("text/html")
    public Viewable getQueryTemplateResults(@Context UriInfo info, 
            @HeaderParam("fusekiUrl") final String fuseki,
            @PathParam("file") String file) throws RestException{     
        
        log.info("Call to getQueryTemplateResults()");
        if(fuseki !=null){fusekiUrl=fuseki;}        
        
        //Settings       
        HashMap<String,String> hm=Helpers.convertMulti(info.getQueryParameters());        
        hm.put(QueryConstants.REQ_URI, info.getRequestUri().toString().replace(info.getBaseUri().toString(), "/"));        
                
        //process
        QueryFileParser qfp=new QueryFileParser(file+".arq");
        log.info("QueryResult Type >> "+qfp.getTemplate().getQueryReturn());
        String check=qfp.checkQueryArgsSyntax();
        if(!check.trim().equals("")) {
            throw new RestException(500,
                    RestException.GENERIC_APP_ERROR_CODE,
                    "Exception : File->"+ hm.get(QueryConstants.SEARCH_TYPE)+".arq"+"; ERROR: "+check);
        }
        String query=InjectionTracker.getValidQuery(qfp.getQuery(), hm,qfp.getLitLangParams());        
        if(query.startsWith(QueryConstants.QUERY_ERROR)) {
            return new Viewable("/error.jsp",query);
        }
        ResultSetWrapper res = QueryProcessor.getResults(query,fuseki,hm.get(QueryConstants.RESULT_HASH),hm.get(QueryConstants.PAGE_SIZE));
        ResultPage model=null;
        try {
            if(hm.get(QueryConstants.JSON_OUT)!=null) {
                Results mod=new Results(res,hm);
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
    @Path("/query/{file}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getQueryTemplateResultsPost(
            @HeaderParam("fusekiUrl") final String fuseki,
            @PathParam("file") String file,
            MultivaluedMap<String,String> mp) throws RestException{ 
        log.info("Call to getQueryTemplateResultsPost()"); 
        log.info("Map >> "+mp);
        if(mp.size()==0) {
            throw new RestException(500,
                    RestException.GENERIC_APP_ERROR_CODE,
                    "Exception : request parameters are missing-> in Post urlencoded query template request");
        }
        if(fuseki !=null){fusekiUrl=fuseki;}
        HashMap<String,String> hm=Helpers.convertMulti(mp);
        
        QueryFileParser qfp=new QueryFileParser(file+".arq");                
        String check=qfp.checkQueryArgsSyntax();
        if(!check.trim().equals("")) {
            throw new RestException(500,
                    RestException.GENERIC_APP_ERROR_CODE,
                    "Exception : File->"+ hm.get(QueryConstants.SEARCH_TYPE)+".arq"+"; ERROR: "+check);
        }
        String query=InjectionTracker.getValidQuery(qfp.getQuery(),hm,qfp.getLitLangParams());
        
        if(query.startsWith(QueryConstants.QUERY_ERROR)) {
            return Response.ok(ResponseOutputStream.getJsonResponseStream(query)).build();
        }
        else {
            Results rp=null;
            ResultSetWrapper res = QueryProcessor.getResults(
                    query, 
                    fuseki, 
                    hm.get(QueryConstants.RESULT_HASH), 
                    hm.get(QueryConstants.PAGE_SIZE));                
            hm.put(QueryConstants.RESULT_HASH, Integer.toString(res.getHash()));
            hm.put(QueryConstants.PAGE_SIZE, Integer.toString(res.getPageSize()));
            try {
                rp=new Results(res,hm);
            }catch(JsonProcessingException jx) {
                throw new RestException(500,
                        RestException.GENERIC_APP_ERROR_CODE,
                        "JsonProcessingException :"+ jx.getMessage());                
            }
            return Response.ok(ResponseOutputStream.getJsonResponseStream(rp)).build();
        }
    }
    
    @POST 
    @Path("/query/{file}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getQueryTemplateResultsJsonPost( 
            @HeaderParam("fusekiUrl") final String fuseki,
            @PathParam("file") String file,
            HashMap<String,String> map) throws RestException{     
        log.info("Call to getQueryTemplateResultsJsonPost()");        
        if(map.size()==0) {
            throw new RestException(500,
                    RestException.GENERIC_APP_ERROR_CODE,
                    "Exception : request parameters are missing-> in Post json query template request");
        }
        if(fuseki !=null){fusekiUrl=fuseki;}         
        QueryFileParser qfp=new QueryFileParser(file+".arq");             
        String check=qfp.checkQueryArgsSyntax();
        if(!check.trim().equals("")) {
            throw new RestException(500,
                    RestException.GENERIC_APP_ERROR_CODE,
                    "Exception : File->"+ map.get(QueryConstants.SEARCH_TYPE)+".arq"+"; ERROR: "+check);
        }
        String query=InjectionTracker.getValidQuery(qfp.getQuery(),map,qfp.getLitLangParams());        
        if(query.startsWith(QueryConstants.QUERY_ERROR)) {
            return Response.ok(ResponseOutputStream.getJsonResponseStream(query)).build();
        }else {
            Results rp=null;
            ResultSetWrapper res = QueryProcessor.getResults(query,fuseki,map.get(QueryConstants.RESULT_HASH),map.get(QueryConstants.PAGE_SIZE));                
            map.put(QueryConstants.RESULT_HASH, Integer.toString(res.getHash()));
            map.put(QueryConstants.PAGE_SIZE, Integer.toString(res.getPageSize()));
            try {
                rp=new Results(res,map);
            }catch(JsonProcessingException jx) {
                throw new RestException(500,
                        RestException.GENERIC_APP_ERROR_CODE,
                        "JsonProcessingException :"+ jx.getMessage());                
            }
            return Response.ok(ResponseOutputStream.getJsonResponseStream(rp)).build();            
        }
    }
}
