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
import java.util.Map.Entry;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.jena.rdf.model.Model;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.ldspdi.rest.features.JerseyCacheControl;
import io.bdrc.ldspdi.results.FusekiResultSet;
import io.bdrc.ldspdi.results.ResultPage;
import io.bdrc.ldspdi.results.ResultSetWrapper;
import io.bdrc.ldspdi.results.Results;
import io.bdrc.ldspdi.service.GitService;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.InjectionTracker;
import io.bdrc.ldspdi.sparql.Prefixes;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ldspdi.sparql.QueryFileParser;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.ldspdi.utils.MediaTypeUtils;
import io.bdrc.ldspdi.utils.ResponseOutputStream;
import io.bdrc.restapi.exceptions.RestException;


@Path("/")
public class PublicTemplatesResource {
    
    public final static Logger log=LoggerFactory.getLogger(PublicDataResource.class.getName());    
    public String fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL); 
    
    @GET
    @Path("/query/{file}")
    @JerseyCacheControl()
    @Produces("text/html")
    public Response getQueryTemplateResults(@Context UriInfo info, 
            @HeaderParam("fusekiUrl") final String fuseki,
            @PathParam("file") String file) throws RestException{     
        
        log.info("Call to getQueryTemplateResults()");
        if(fuseki !=null){fusekiUrl=fuseki;}        
        
        //Settings       
        HashMap<String,String> hm=Helpers.convertMulti(info.getQueryParameters());        
        hm.put(QueryConstants.REQ_URI, info.getRequestUri().toString().replace(info.getBaseUri().toString(), "/"));        
                
        //process
        QueryFileParser qfp=new QueryFileParser(file+".arq");        
        String check=qfp.checkQueryArgsSyntax();
        if(!check.trim().equals("")) {
            throw new RestException(500,
                    RestException.GENERIC_APP_ERROR_CODE,
                    "Exception : File->"+ hm.get(QueryConstants.SEARCH_TYPE)+".arq"+"; ERROR: "+check);
        }
        String query=InjectionTracker.getValidQuery(qfp.getQuery(), hm,qfp.getLitLangParams(),true);        
        if(query.startsWith(QueryConstants.QUERY_ERROR)) {
            return Response.ok(new Viewable("/error.jsp",query)).build();
        }
        ResultSetWrapper res = QueryProcessor.getResults(query,fuseki,hm.get(QueryConstants.RESULT_HASH),hm.get(QueryConstants.PAGE_SIZE));
        ResultPage model=null;        
        try {
            if(hm.get(QueryConstants.JSON_OUT)!=null) {
                Results mod=new Results(res,hm);
                hm.remove("query");            
                String it=new ObjectMapper().writeValueAsString(mod);            
                return Response.ok(new Viewable("/json.jsp",it)).build();
            }
            hm.put(QueryConstants.REQ_METHOD, "GET");             
            hm.put("query", qfp.getQueryHtml());
            model=new ResultPage(res,hm.get(QueryConstants.PAGE_NUMBER),hm,qfp.getTemplate());
        }
        catch (JsonProcessingException jx) {
            throw new RestException(500,RestException.GENERIC_APP_ERROR_CODE,"JsonProcessingException"+jx.getMessage());
        }
        return Response.ok(new Viewable("/resPage.jsp",model)).build();        
    }
    
    @GET
    @Path("/test/{file}")
    @JerseyCacheControl()
    @Produces(MediaType.APPLICATION_JSON)
    public Response testTemplateResults(@Context UriInfo info, 
            @HeaderParam("fusekiUrl") final String fuseki,
            @PathParam("file") String file) throws RestException{     
        
        log.info("Call to testTemplateResults()");
        if(fuseki !=null){fusekiUrl=fuseki;}        
        
        //Settings       
        HashMap<String,String> hm=Helpers.convertMulti(info.getQueryParameters());        
        hm.put(QueryConstants.REQ_URI, info.getRequestUri().toString().replace(info.getBaseUri().toString(), "/"));        
                
        //process
        QueryFileParser qfp=new QueryFileParser(file+".arq");        
        String check=qfp.checkQueryArgsSyntax();
        if(!check.trim().equals("")) {
            throw new RestException(500,
                    RestException.GENERIC_APP_ERROR_CODE,
                    "Exception : File->"+ hm.get(QueryConstants.SEARCH_TYPE)+".arq"+"; ERROR: "+check);
        }
        String query=InjectionTracker.getValidQuery(qfp.getQuery(), hm,qfp.getLitLangParams(),true);        
        if(query.startsWith(QueryConstants.QUERY_ERROR)) {
            throw new RestException(500,
                    RestException.GENERIC_APP_ERROR_CODE,
                    "template ->"+ hm.get(QueryConstants.SEARCH_TYPE)+".arq"+"; ERROR: "+query);
        }
        //The output we build using jackson
        ResultSetWrapper res = QueryProcessor.getResults(query,fuseki,hm.get(QueryConstants.RESULT_HASH),hm.get(QueryConstants.PAGE_SIZE));
        FusekiResultSet model=new FusekiResultSet(res);  
        return Response.ok(ResponseOutputStream.getJsonResponseStream(model)).build();
            
    }
       
    @POST 
    @Path("/query/{file}")
    @JerseyCacheControl()
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getQueryTemplateResultsPost(
            @HeaderParam("fusekiUrl") final String fuseki,
            @PathParam("file") String file,
            MultivaluedMap<String,String> mp) throws RestException{ 
        log.info("Call to getQueryTemplateResultsPost()");
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
        String query=InjectionTracker.getValidQuery(qfp.getQuery(),hm,qfp.getLitLangParams(),true);
        
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
            } catch(JsonProcessingException jx) {
                throw new RestException(500,
                        RestException.GENERIC_APP_ERROR_CODE,
                        "JsonProcessingException :"+ jx.getMessage());                
            }
            return Response.ok(ResponseOutputStream.getJsonResponseStream(rp)).build();
        }
    }
    
    @POST 
    @Path("/query/{file}")
    @JerseyCacheControl()
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
        if (fuseki !=null) {fusekiUrl=fuseki;}         
        QueryFileParser qfp=new QueryFileParser(file+".arq");             
        String check=qfp.checkQueryArgsSyntax();
        if (!check.trim().equals("")) {
            throw new RestException(500,
                    RestException.GENERIC_APP_ERROR_CODE,
                    "Exception : File->"+ map.get(QueryConstants.SEARCH_TYPE)+".arq"+"; ERROR: "+check);
        }
        String query=InjectionTracker.getValidQuery(qfp.getQuery(),map,qfp.getLitLangParams(),true);        
        if (query.startsWith(QueryConstants.QUERY_ERROR)) {
            return Response.ok(ResponseOutputStream.getJsonResponseStream(query)).build();
        } else {
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
    
    @GET
    @Path("/graph/{file}")  
    @JerseyCacheControl()
    public Response getGraphTemplateResults(@Context UriInfo info, 
            @HeaderParam("fusekiUrl") final String fuseki,
            @DefaultValue("jsonld") @QueryParam("format") final String format,            
            @PathParam("file") String file,            
            @Context Request request,
            HashMap<String,String> map) throws RestException{     
        String path=info.getPath()+info.relativize(info.getRequestUri());
        Variant variant = request.selectVariant(MediaTypeUtils.graphVariants);
        log.info("Call to getGraphTemplateResults() with URL: "+path+" Accept: "+format+ " Variant >> "+variant);
        if(fuseki !=null){fusekiUrl=fuseki;}        
        
        
        if(format==null || variant == null) {
            final String html=Helpers.getMultiChoicesHtml(path,false);
            final ResponseBuilder rb=Response.status(300).entity(html).header("Content-Type", "text/html").
                    header("Content-Location",info.getBaseUri()+"choice?path="+path);
            return setHeaders(rb,getGraphResourceHeaders(path,null,"List")).build();
        }
        //Settings       
        HashMap<String,String> hm=Helpers.convertMulti(info.getQueryParameters());        
                 
        //process
        QueryFileParser qfp=new QueryFileParser(file+".arq");        
        String check=qfp.checkQueryArgsSyntax();
        if(!check.trim().equals("")) {
            throw new RestException(500,
                    RestException.GENERIC_APP_ERROR_CODE,
                    "Exception : File->"+ file+".arq"+"; ERROR: "+check);
        }
        String query=InjectionTracker.getValidQuery(qfp.getQuery(), hm,qfp.getLitLangParams(),false);        
        if(query.startsWith(QueryConstants.QUERY_ERROR)) {
            throw new RestException(500,RestException.GENERIC_APP_ERROR_CODE,"The injection Tracker failed to build the query : "+qfp.getQuery());
        }
        final MediaType mediaType = variant.getMediaType();
        Model model=QueryProcessor.getGraph(query,fusekiUrl,null);
        if(model.size()==0) {
            throw new RestException(404,RestException.GENERIC_APP_ERROR_CODE,"No graph was found for the given resource Id");
        }
        final String ext = MediaTypeUtils.getExtFormatFromMime(mediaType.toString());
        ResponseBuilder builder=Response.ok(ResponseOutputStream.getModelStream(model,ext), mediaType);
        return setHeaders(builder,getGraphResourceHeaders(path,ext,"Choice")).build();
    }
    
    @POST
    @Path("/graph/{file}") 
    @JerseyCacheControl()
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getGraphTemplateResultsPost(@HeaderParam("fusekiUrl") final String fuseki,
            @DefaultValue("jsonld") @HeaderParam("Accept") final String format,
            @PathParam("file") String file,
            @Context UriInfo info,
            @Context Request request,
            HashMap<String,String> map) throws RestException{
        String path=info.getPath()+info.relativize(info.getRequestUri());
        Variant variant = request.selectVariant(MediaTypeUtils.graphVariants);
        log.info("Call to getGraphTemplateResultsPost() with URL: "+path+" Accept: "+format+ " Selected Variant >> "+variant);
        if(fuseki !=null){fusekiUrl=fuseki;} 
        String params="";
        for(String key:map.keySet()) {
            params="&"+key+"="+map.get(key)+"&";
        }
        params="?"+params.substring(1,params.length()-1);
        if(format==null || variant == null) {            
            final String html=Helpers.getMultiChoicesHtml(path+params,false);
            final ResponseBuilder rb=Response.status(300).entity(html).header("Content-Type", "text/html").
                    header("Content-Location",info.getBaseUri()+"choice?path="+path+params);
            return setHeaders(rb,getGraphResourceHeaders(path+params,null,"List")).build();
        }    
        //process
        QueryFileParser qfp=new QueryFileParser(file+".arq");
        String check=qfp.checkQueryArgsSyntax();
        if(!check.trim().equals("")) {
            throw new RestException(500,
                    RestException.GENERIC_APP_ERROR_CODE,
                    "Exception : File->"+ file+".arq"+"; ERROR: "+check);
        }
        String query=InjectionTracker.getValidQuery(qfp.getQuery(), map,qfp.getLitLangParams(),false);         
        if(query.startsWith(QueryConstants.QUERY_ERROR)) {
            throw new RestException(500,RestException.GENERIC_APP_ERROR_CODE,"The injection Tracker failed to build the query : "+qfp.getQuery());
        }
        final MediaType mediaType = variant.getMediaType();
        Model model=QueryProcessor.getGraph(query,fusekiUrl,null);
        if(model.size()==0) {
            throw new RestException(404,RestException.GENERIC_APP_ERROR_CODE,"No graph was found for the given resource Id");
        }
        final String ext = MediaTypeUtils.getExtFormatFromMime(mediaType.toString());
        ResponseBuilder builder=Response.ok(ResponseOutputStream.getModelStream(model,ext), mediaType);
        return setHeaders(builder,getGraphResourceHeaders(path+params,ext,"Choice")).build();       
    }
    
    @POST
    @Path("/callbacks/github/lds-queries") 
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateQueries() throws RestException{        
        log.info("updating query templates >>");
        Thread t=new Thread(new GitService());
        t.start();
        Prefixes.loadPrefixes();
        return Response.ok().build();       
    }
    
    private static HashMap<String,String> getGraphResourceHeaders(String url,String ext, String tcn) {
        HashMap<String,String> map =MediaTypeUtils.getExtensionMimeMap();
        HashMap<String,String> headers=new HashMap<>();
        if(ext!=null) {
            if(url.indexOf(".")<0) {
                headers.put("Content-Location", url+"&format="+ext);
            }else {
                url=url.substring(0, url.lastIndexOf("."));
            }
        }
        StringBuilder sb=new StringBuilder("");
        for(Entry<String,String> e :map.entrySet()) {
            sb.append("{\""+url+"&format="+e.getKey()+"\" 1.000 {type "+e.getValue()+"}},");               
        }
        headers.put("Alternates", sb.toString().substring(0, sb.toString().length()-1));
        headers.put("TCN", tcn);
        headers.put("Vary", "Negotiate, Accept");
        return headers;
    }
    
    private static ResponseBuilder setHeaders(ResponseBuilder builder, HashMap<String,String> headers) {
        for(String key:headers.keySet()) {
            builder.header(key, headers.get(key));
        }
        return builder;
    }
}
