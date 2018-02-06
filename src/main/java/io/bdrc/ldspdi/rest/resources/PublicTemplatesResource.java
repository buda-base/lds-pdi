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
import java.util.logging.Logger;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.ldspdi.Utils.RestUtils;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.InjectionTracker;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ldspdi.sparql.QueryFileParser;
import io.bdrc.ldspdi.sparql.results.ResultPage;
import io.bdrc.ldspdi.sparql.results.Results;


@Path("/")
public class PublicTemplatesResource {
    
    public static Logger log=Logger.getLogger(PublicDataResource.class.getName());
    
    public String fusekiUrl="";
    
    public PublicTemplatesResource() {
        super();        
        ResourceConfig config=new ResourceConfig(PublicTemplatesResource.class);
        config.register(CorsFilter.class);        
    } 
    
    @GET
    @Path("/resource/templates")
    @Produces("text/html")
    public Viewable getQueryTemplateResults(@Context UriInfo info, @HeaderParam("fusekiUrl") final String fuseki) throws Exception{     
        
        log.info("Call to getQueryTemplateResults()"); 
        log.info("URL :"+info.getRequestUri());
        
        if(fuseki !=null)
            {fusekiUrl=fuseki;}
        else 
            {fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);}
        
        //Settings        
        String relativeUri=info.getRequestUri().toString().replace(info.getBaseUri().toString(), "/");
        MultivaluedMap<String,String> mp=info.getQueryParameters();
        HashMap<String,String> hm=RestUtils.convertMulti(mp);
        hm.put(QueryConstants.REQ_METHOD, "GET");
        hm.put(QueryConstants.REQ_URI, relativeUri);        
        ObjectMapper mapper = new ObjectMapper();
        
        //params
        String filename= hm.get(QueryConstants.SEARCH_TYPE)+".arq";        
        int pageSize =RestUtils.getPageSize(hm.get(QueryConstants.PAGE_SIZE));
        int pageNumber=RestUtils.getPageNumber(hm.get(QueryConstants.PAGE_NUMBER));
        int hash=RestUtils.getHash(hm.get(QueryConstants.RESULT_HASH));
        boolean jsonOutput=RestUtils.getJsonOutput(hm.get(QueryConstants.JSON_OUT));
        
        //process
        QueryFileParser qfp;
        final String query;
        
        qfp=new QueryFileParser(filename);
        String q=qfp.getQuery();
        hm.put("query", qfp.getQueryHtml());
        String check=qfp.checkQueryArgsSyntax();
        if(check.length()>0) {
            throw new Exception("Exception : File->"+ filename+"; ERROR: "+check);
        }
        query=InjectionTracker.getValidQuery(q, hm,qfp.getLitLangParams());
        boolean error=query.startsWith(QueryConstants.QUERY_ERROR);
        String msg =query;
        if(error) {
            return new Viewable("/error.jsp",msg);
        }
        Results res = RestUtils.getResults(query, fuseki, hash, pageSize); 
        ResultPage model=new ResultPage(res,pageNumber,hm,qfp.getTemplate());
        if(jsonOutput) {
            model.setQuery(q);
            String it=mapper.writeValueAsString(model);
            return new Viewable("/json.jsp",it);
        }
        return new Viewable("/resPage.jsp",model);
    }
       
    @POST 
    @Path("/resource/templates")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getQueryTemplateResultsPost(@Context UriInfo info, 
            @HeaderParam("fusekiUrl") final String fuseki,
            MultivaluedMap<String,String> mp) throws Exception{ 
        log.info("Call to getQueryTemplateResultsPost()");              
        if(fuseki !=null){
            fusekiUrl=fuseki;
        }else {
            fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);  
        } 
        HashMap<String,String> hm=RestUtils.convertMulti(mp);
        ObjectMapper mapper = new ObjectMapper();
        String filename= hm.get(QueryConstants.SEARCH_TYPE)+".arq";
        
        int pageSize =RestUtils.getPageSize(hm.get(QueryConstants.PAGE_SIZE));
        int pageNumber=RestUtils.getPageNumber(hm.get(QueryConstants.PAGE_NUMBER));
        int hash=RestUtils.getHash(hm.get(QueryConstants.RESULT_HASH));
        QueryFileParser qfp; 
        final String query;
        
        qfp=new QueryFileParser(filename);
        String q=qfp.getQuery();
        String check=qfp.checkQueryArgsSyntax();
        if(check.length()>0) {
            throw new Exception("Exception : File->"+ filename+".arq; ERROR: "+check);
        }
        query=InjectionTracker.getValidQuery(q, hm,qfp.getLitLangParams());
        //MultivaluedMap<String,String> copy=map;
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException { 
                if(query.startsWith(QueryConstants.QUERY_ERROR)) {
                    os.write(query.getBytes());
                }else {
                    Results res = RestUtils.getResults(query, fuseki, hash, pageSize);                
                    hm.put(QueryConstants.RESULT_HASH, Integer.toString(res.getHash()));
                    hm.put(QueryConstants.PAGE_SIZE, Integer.toString(res.getPageSize()));                
                    ResultPage rp=new ResultPage(res,pageNumber,hm,qfp.getTemplate());
                    mapper.writerWithDefaultPrettyPrinter().writeValue(os , rp); 
                }
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
        
        int pageSize =RestUtils.getPageSize(map.get(QueryConstants.PAGE_SIZE));
        int pageNumber=RestUtils.getPageNumber(map.get(QueryConstants.PAGE_NUMBER));
        int hash=RestUtils.getHash(map.get(QueryConstants.RESULT_HASH));
        
        QueryFileParser qfp=new QueryFileParser(filename);;
        final String query;
        String q=qfp.getQuery();
        String check=qfp.checkQueryArgsSyntax();
        if(check.length()>0) {
            throw new Exception("Exception : File->"+ filename+".arq; ERROR: "+check);
        }
        query=InjectionTracker.getValidQuery(q, map,qfp.getLitLangParams());        
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                if(query.startsWith(QueryConstants.QUERY_ERROR)) {
                    os.write(query.getBytes());
                }else {
                    Results res = RestUtils.getResults(query, fuseki, hash, pageSize);                
                    map.put(QueryConstants.RESULT_HASH, Integer.toString(res.getHash()));
                    map.put(QueryConstants.PAGE_SIZE, Integer.toString(res.getPageSize()));
                    ResultPage rp=new ResultPage(res,pageNumber,map,qfp.getTemplate());
                    mapper.writerWithDefaultPrettyPrinter().writeValue(os , rp); 
                }
            }
        };
        return Response.ok(stream).build();
    }
    
     
    
}
