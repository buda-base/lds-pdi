package io.bdrc.ldspdi.rest.resources;

import java.util.HashMap;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.jena.rdf.model.Model;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.jsp.JspMvcFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.rest.features.CorsFilter;
import io.bdrc.ldspdi.rest.features.GZIPWriterInterceptor;
import io.bdrc.ldspdi.results.library.RootSearchResults;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.InjectionTracker;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ldspdi.sparql.QueryFileParser;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.ldspdi.utils.ResponseOutputStream;
import io.bdrc.restapi.exceptions.RestException;

@Path("/")
public class LibrarySearchResource {
    
    public final static Logger log=LoggerFactory.getLogger(LibrarySearchResource.class.getName());   
    public String fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
    
    public LibrarySearchResource() {
        super();
        ResourceConfig config=new ResourceConfig(PublicDataResource.class);
        config.register(LoggingFeature.class);
        config.register(CorsFilter.class); 
        config.register(GZIPWriterInterceptor.class);
        config.property(JspMvcFeature.TEMPLATE_BASE_PATH, "").register(JspMvcFeature.class);
    }
    
    @POST
    @Path("/lib/rootSearch")  
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGraphTemplateResultsPost(
            @HeaderParam("fusekiUrl") final String fuseki,
            HashMap<String,String> map) throws RestException {
        
        QueryFileParser qfp=new QueryFileParser("RootSearchGraphComplet.arq","library");
        log.info("QueryResult Type >> "+qfp.getTemplate().getQueryReturn());
        String check=qfp.checkQueryArgsSyntax();
        if(!check.trim().equals("")) {
            throw new RestException(500,
                    RestException.GENERIC_APP_ERROR_CODE,
                    "Exception : File->"+ "RootSearchGraph.arq"+"; ERROR: "+check);
        }
        String query=InjectionTracker.getValidQuery(qfp.getQuery(), map,qfp.getLitLangParams(),false); 
        log.info("Call to getQueryTemplateResultsPost() processed query is >>"+query);
        if(query.startsWith(QueryConstants.QUERY_ERROR)) {
            throw new RestException(500,RestException.GENERIC_APP_ERROR_CODE,"The injection Tracker failed to build the query : "+qfp.getQuery());
        }
        Model model=QueryProcessor.getGraph(query,fusekiUrl);
        String q="select * where {?s ?p ?o}";        
        HashMap<String,Object> res=RootSearchResults.getResultsMap(QueryProcessor.getResultsFromModel(q, model));
        return Response.ok(ResponseOutputStream.getJsonResponseStream(res),MediaType.APPLICATION_JSON_TYPE).build();
    }
    
    @GET
    @Path("/lib/rootSearch") 
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGraphTemplateResultsGet( @Context UriInfo info,
            @HeaderParam("fusekiUrl") final String fuseki
            ) throws RestException {
        HashMap<String,String> map=Helpers.convertMulti(info.getQueryParameters());  
        QueryFileParser qfp=new QueryFileParser("RootSearchGraphComplet.arq","library");
        log.info("QueryResult Type >> "+qfp.getTemplate().getQueryReturn());
        String check=qfp.checkQueryArgsSyntax();
        if(!check.trim().equals("")) {
            throw new RestException(500,
                    RestException.GENERIC_APP_ERROR_CODE,
                    "Exception : File->"+ "RootSearchGraphComplet1.arq"+"; ERROR: "+check);
        }
        String query=InjectionTracker.getValidQuery(qfp.getQuery(), map,qfp.getLitLangParams(),false); 
        log.info("Call to getQueryTemplateResultsPost() processed query is >>"+query);
        if(query.startsWith(QueryConstants.QUERY_ERROR)) {
            throw new RestException(500,RestException.GENERIC_APP_ERROR_CODE,"The injection Tracker failed to build the query : "+qfp.getQuery());
        }
        Model model=QueryProcessor.getGraph(query,fusekiUrl);
        String q="select * where {?s ?p ?o}";        
        HashMap<String,Object> res=RootSearchResults.getResultsMap(QueryProcessor.getResultsFromModel(q, model));
        return Response.ok(ResponseOutputStream.getJsonResponseStream(res),MediaType.APPLICATION_JSON_TYPE).build();
        
    }

}
