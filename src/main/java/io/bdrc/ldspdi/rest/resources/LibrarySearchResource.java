package io.bdrc.ldspdi.rest.resources;

import java.util.HashMap;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
import io.bdrc.ldspdi.results.library.PersonAllResults;
import io.bdrc.ldspdi.results.library.PersonResults;
import io.bdrc.ldspdi.results.library.PlaceAllResults;
import io.bdrc.ldspdi.results.library.ResourceResults;
import io.bdrc.ldspdi.results.library.RootResults;
import io.bdrc.ldspdi.results.library.TopicAllResults;
import io.bdrc.ldspdi.results.library.WorkResults;
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
    @Path("/lib/{file}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLibGraphPost(
            @HeaderParam("fusekiUrl") final String fuseki,
            @PathParam("file") final String file,
            HashMap<String,String> map) throws RestException {

        log.info("Call to getLibGraphPost() with template name >> "+file);
        QueryFileParser qfp=new QueryFileParser(file+".arq","library");
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
        Model model=QueryProcessor.getGraph(query,fusekiUrl,null);
        HashMap<String,Object> res=null;
        switch (file) {
            case "rootSearchGraph":
                res=RootResults.getResultsMap(model);
                break;
            case "personFacetGraph":
            case "workAssocPersons":
            case "placeAssocPersons":
                res=PersonResults.getResultsMap(model);
                break;
            case "workFacetGraph":
            case "workAssocWorks":
            case "workAllAssociations":
                res=WorkResults.getResultsMap(model);
                break;
            case "allAssocResource":
                res=ResourceResults.getResultsMap(model);
                break;
            case "personAllAssociations":
                res=PersonAllResults.getResultsMap(model);
                break;
            case "topicAllAssociations":
                res=TopicAllResults.getResultsMap(model);
                break;
            case "placeAllAssociations":
                res=PlaceAllResults.getResultsMap(model);
                break;
            default:
                throw new RestException(404,RestException.GENERIC_APP_ERROR_CODE,"No graph template was found for the given path >>"+file);
        }
        return Response.ok(ResponseOutputStream.getJsonResponseStream(res),MediaType.APPLICATION_JSON_TYPE).build();
    }

    @GET
    @Path("/lib/{file}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLibGraphGet( @Context UriInfo info,
            @HeaderParam("fusekiUrl") final String fuseki,
            @PathParam("file") String file
            ) throws RestException {

        log.info("Call to getLibGraphGet() with template name >> "+file);
        HashMap<String,String> map=Helpers.convertMulti(info.getQueryParameters());
        QueryFileParser qfp=new QueryFileParser(file+".arq","library");
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
        Model model=QueryProcessor.getGraph(query,fusekiUrl,null);
        log.info("Model Size >>>>>> "+model.size());
        HashMap<String,Object> res=null;
        switch (file) {
            case "rootSearchGraph":
                res=RootResults.getResultsMap(model);
                break;
            case "personFacetGraph":
            case "workAssocPersons":
            case "placeAssocPersons":
                res=PersonResults.getResultsMap(model);
                break;
            case "workFacetGraph":
            case "workAssocWorks":
            case "workAllAssociations":
                res=WorkResults.getResultsMap(model);
                break;
            case "allAssocResource":
                res=ResourceResults.getResultsMap(model);
                break;
            case "personAllAssociations":
                res=PersonAllResults.getResultsMap(model);
                break;
            case "topicAllAssociations":
                res=TopicAllResults.getResultsMap(model);
                break;
            case "placeAllAssociations":
                res=PlaceAllResults.getResultsMap(model);
                break;
            default:
                throw new RestException(404,RestException.GENERIC_APP_ERROR_CODE,"No graph template was found for the given path >>"+file);
        }
        return Response.ok(ResponseOutputStream.getJsonLDResponseStream(res),MediaType.APPLICATION_JSON_TYPE).build();

    }
}
