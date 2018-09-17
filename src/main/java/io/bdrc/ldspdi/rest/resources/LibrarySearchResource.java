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

import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.rest.features.JerseyCacheControl;
import io.bdrc.ldspdi.results.library.ChunksResults;
import io.bdrc.ldspdi.results.library.EtextResults;
import io.bdrc.ldspdi.results.library.PersonAllResults;
import io.bdrc.ldspdi.results.library.PersonResults;
import io.bdrc.ldspdi.results.library.PlaceAllResults;
import io.bdrc.ldspdi.results.library.ResourceResults;
import io.bdrc.ldspdi.results.library.RootResults;
import io.bdrc.ldspdi.results.library.TopicAllResults;
import io.bdrc.ldspdi.results.library.WorkAllResults;
import io.bdrc.ldspdi.results.library.WorkResults;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.AsyncSparql;
import io.bdrc.ldspdi.sparql.QueryFileParser;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.ldspdi.utils.ResponseOutputStream;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;


@Path("/")
public class LibrarySearchResource {

    public final static Logger log=LoggerFactory.getLogger(LibrarySearchResource.class.getName());
    public String fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);

    @POST
    @Path("/lib/{file}")
    @JerseyCacheControl()
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLibGraphPost(
            @HeaderParam("fusekiUrl") final String fuseki,
            @PathParam("file") final String file,
            HashMap<String,String> map) throws RestException {

        log.info("Call to getLibGraphPost() with template name >> "+file);
        Thread t=null;
        AsyncSparql async=null;
        if(file.equals("rootSearchGraph")) {
            async=new AsyncSparql(fusekiUrl,"Etexts_count.arq",map);
            t=new Thread(async);
            t.run();
        }
        QueryFileParser qfp=new QueryFileParser(file+".arq","library");
        String query=qfp.getParametizedQuery(map,false);
        Model model=QueryProcessor.getGraph(query,fusekiUrl,null);
        HashMap<String,Object> res=null;
        switch (file) {
            case "rootSearchGraph":
                log.info("MAP >>> "+map);
                int etext_count=0;
                if(t!=null) {
                    try {
                        t.join();
                        ResultSet rs=async.getRes();
                        etext_count=rs.next().getLiteral("?c").getInt();
                    } catch (InterruptedException e) {
                        throw new RestException(500,new LdsError(LdsError.ASYNC_ERR).
                                setContext("getLibGraphPost()",e));
                    }
                }
                res=RootResults.getResultsMap(model,etext_count);
                break;
            case "personFacetGraph":
                res=PersonResults.getResultsMap(model);
                break;
            case "workFacetGraph":
            case "workAllAssociations":
                res=WorkAllResults.getResultsMap(model);
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
            case "etextFacetGraph":
                res=EtextResults.getResultsMap(model);
                break;
            case "chunksByEtextGraph":
                res=EtextResults.getResultsMap(model);
                break;
            case "chunksFacetGraph":
                res=ChunksResults.getResultsMap(model);
                break;
            case "roleAllAssociations":
                res=ResourceResults.getResultsMap(model);
                break;
            default:
                throw new RestException(404,new LdsError(LdsError.NO_GRAPH_ERR).setContext(file));
        }
        return Response.ok(ResponseOutputStream.getJsonResponseStream(res),MediaType.APPLICATION_JSON_TYPE).build();
    }

    @GET
    @Path("/lib/{file}")
    @JerseyCacheControl()
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLibGraphGet( @Context UriInfo info,
            @HeaderParam("fusekiUrl") final String fuseki,
            @PathParam("file") String file
            ) throws RestException {

        log.info("Call to getLibGraphGet() with template name >> "+file);
        HashMap<String,String> map=Helpers.convertMulti(info.getQueryParameters());
        Thread t=null;
        AsyncSparql async=null;
        if(file.equals("rootSearchGraph")) {
            async=new AsyncSparql(fusekiUrl,"Etexts_count.arq",map);
            t=new Thread(async);
            t.run();
        }
        QueryFileParser qfp=new QueryFileParser(file+".arq","library");
        String query=qfp.getParametizedQuery(map,false);
        Model model=QueryProcessor.getGraph(query,fusekiUrl,null);
        HashMap<String,Object> res=null;
        switch (file) {
            case "rootSearchGraph":
                int etext_count=0;
                if(t!=null) {
                    try {
                        t.join();
                        ResultSet rs=async.getRes();
                        etext_count=rs.next().getLiteral("?c").getInt();
                    } catch (InterruptedException e) {
                        throw new RestException(500,new LdsError(LdsError.ASYNC_ERR).
                                setContext("getLibGraphGet()",e));
                    }
                }

                res=RootResults.getResultsMap(model,etext_count);
                break;
            case "personFacetGraph":
                res=PersonResults.getResultsMap(model);
                break;
            case "workAllAssociations":
                res=WorkAllResults.getResultsMap(model);
                break;
            case "workFacetGraph":
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
            case "etextFacetGraph":
                res=EtextResults.getResultsMap(model);
                break;
            case "chunksByEtextGraph":
                res=EtextResults.getResultsMap(model);
                break;
            case "chunksFacetGraph":
                res=ChunksResults.getResultsMap(model);
                break;
            case "roleAllAssociations":
                res=ResourceResults.getResultsMap(model);
                break;
            default:
                throw new RestException(404,new LdsError(LdsError.NO_GRAPH_ERR).setContext(file));
        }
        return Response.ok(ResponseOutputStream.getJsonLDResponseStream(res),MediaType.APPLICATION_JSON_TYPE).build();
    }
}
