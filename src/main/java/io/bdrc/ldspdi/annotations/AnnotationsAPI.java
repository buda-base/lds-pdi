package io.bdrc.ldspdi.annotations;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import io.bdrc.formatters.JSONLDFormatter.DocType;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.LdsQuery;
import io.bdrc.ldspdi.sparql.LdsQueryService;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.MediaTypeUtils;
import io.bdrc.ldspdi.utils.ResponseOutputStream;
import io.bdrc.restapi.exceptions.RestException;

@Path("/annotations/")
public class AnnotationsAPI {

    final static Model COLL_SERV=ModelFactory.createDefaultModel().read(AnnotationsAPI.class.getClassLoader().getResourceAsStream("collectionService.ttl"),"http://api.bdrc.io/annotations/","TURTLE");
    final static String annotSearchQueryFile="annotLayerSearch.arq";

    @GET
    @Path("/collectionService")
    public Response collectionService(@HeaderParam("Accept") String accept,
            @Context UriInfo info,
            @Context Request request,
            @Context HttpHeaders headers) throws RestException {
       MediaType mediaType;
       if (accept == null || accept.contains("text/html")) {
           mediaType = MediaTypeUtils.MT_JSONLD;
       } else {
           mediaType = MediaTypeUtils.getMediaType(request, accept, MediaTypeUtils.graphVariants);
           if (mediaType == null)
               return AnnotationEndpoint.mediaTypeChoiceResponse(info);
       }
       String ext = MediaTypeUtils.getExtFromMime(mediaType);
       COLL_SERV.write(System.out,"JSON-LD");
       ResponseBuilder builder = Response.ok(ResponseOutputStream.getModelStream(COLL_SERV, ext, "http://api.bdrc.io/annotations/collectionService", null));
       return AnnotationEndpoint.setHeaders(builder, info.getPath(), ext, "Choice", null, mediaType, false).build();

    }

    @GET
    @Path("/search/{res}/")
    public Response search(@HeaderParam("Accept") String accept,
            @Context UriInfo info,
            @PathParam("res") String res,
            @Context Request request,
            @Context HttpHeaders headers) throws RestException {
        MediaType mediaType;
        if (accept == null || accept.contains("text/html")) {
            mediaType = MediaTypeUtils.MT_JSONLD;
        } else {
            mediaType = MediaTypeUtils.getMediaType(request, accept, MediaTypeUtils.graphVariants);
            if (mediaType == null)
                return AnnotationEndpoint.mediaTypeChoiceResponse(info);
        }
       String ext = MediaTypeUtils.getExtFromMime(mediaType);
       MultivaluedMap<String,String> map= info.getQueryParameters();
       String range=map.getFirst("range");
       Integer[] rg= CollectionUtils.getRangeFromUrlElt(range);
       final LdsQuery qfp = LdsQueryService.get(annotSearchQueryFile,"library");
       final Map<String,String> args = new HashMap<>();
       args.put("R_RES", res);
       args.put("I_SUBRANGEFIRST", rg[0].toString());
       args.put("I_SUBRANGELAST", rg[1].toString());
       final String query = qfp.getParametizedQuery(args, false);
       Model model= QueryProcessor.getGraph(query, ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL), null);
       ResponseBuilder builder = Response.ok(ResponseOutputStream.getModelStream(model, ext, "http://api.bdrc.io/annotations/collectionService", DocType.ANN));
       return AnnotationEndpoint.setHeaders(builder, info.getPath(), ext, "Choice", null, mediaType, false).build();

    }

}
