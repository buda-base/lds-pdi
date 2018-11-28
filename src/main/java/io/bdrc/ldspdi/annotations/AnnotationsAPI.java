package io.bdrc.ldspdi.annotations;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import io.bdrc.ldspdi.utils.MediaTypeUtils;
import io.bdrc.ldspdi.utils.ResponseOutputStream;
import io.bdrc.restapi.exceptions.RestException;

@Path("/annotations/")
public class AnnotationsAPI {


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
       Model m=ModelFactory.createDefaultModel();
       m.read(AnnotationsAPI.class.getClassLoader().getResourceAsStream("collectionService.ttl"),"http://api.bdrc.io/annotations/","TURTLE");
       m.write(System.out,"JSON-LD");
       ResponseBuilder builder = Response.ok(ResponseOutputStream.getModelStream(m, ext, "http://api.bdrc.io/annotations/collectionService", null));
       return AnnotationEndpoint.setHeaders(builder, info.getPath(), ext, "Choice", null, mediaType, false).build();

    }

}
