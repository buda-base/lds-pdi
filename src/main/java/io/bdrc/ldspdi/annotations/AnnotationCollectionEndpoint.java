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
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.formatters.JSONLDFormatter.DocType;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.MediaTypeUtils;
import io.bdrc.ldspdi.utils.ResponseOutputStream;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

@Path("/anncollection/")
public class AnnotationCollectionEndpoint {
    public final static Logger log = LoggerFactory.getLogger(AnnotationCollectionEndpoint.class.getName());
    public String fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);

    // we need to transform collections into sc:AnnotationLists, we do so when we receive the profile:

    public static enum Prefer {
        MINIMAL,
        IRI,
        DESCRIPTION
        ;
    }

    static final Map<Prefer,String> preferToQueryFile = new HashMap<>();
    static {
        preferToQueryFile.put(Prefer.MINIMAL,     "AnnCollection-minimal.arq");
        preferToQueryFile.put(Prefer.IRI,         "AnnCollection-iri.arq");
        preferToQueryFile.put(Prefer.DESCRIPTION, "AnnCollection-description.arq");
    }

    static Prefer getPrefer(final String preferHeader) {
        if (preferHeader == null)
            return Prefer.DESCRIPTION;
        switch (preferHeader) {
        case AnnotationEndpoint.LDP_PMC:
            return Prefer.MINIMAL;
        case AnnotationEndpoint.LDP_PCI:
            return Prefer.IRI;
        case AnnotationEndpoint.LDP_PCD:
            return Prefer.DESCRIPTION;
        default:
            return Prefer.DESCRIPTION; // default in the WAP spec
        }
    }

    @GET
    @Path("/{res}")
    public Response getWholeCollection(@PathParam("res") final String res,
            @HeaderParam("Accept") final String format,
            @HeaderParam("Prefer") final String preferHeader,
            @Context final UriInfo info,
            @Context final Request request,
            @Context final HttpHeaders headers
            ) throws RestException {
        log.error("Call to getWholeCollection() with URL: " + info.getPath() + " Accept >> " + format);
        final MediaType mediaType;
        // spec says that when the Accept: header is absent, JSON-LD should be answered
        if (format == null) {
            mediaType = MediaTypeUtils.MT_JSONLD;
        } else {
            mediaType = MediaTypeUtils.getMediaType(request, format, MediaTypeUtils.resVariants);
            if (mediaType == null)
                return AnnotationEndpoint.mediaTypeChoiceResponse(info);
        }
        String prefixedRes = AnnotationEndpoint.ANC_PREFIX_SHORT+':'+res;
        if (mediaType.equals(MediaType.TEXT_HTML_TYPE))
            return AnnotationEndpoint.htmlResponse(info, prefixedRes);
        String contentType = mediaType.toString();
        final String ext = MediaTypeUtils.getExtFormatFromMime(mediaType.toString());
        System.out.println(mediaType);
        System.out.println(ext);
        final Prefer prefer = getPrefer(preferHeader);
        final String queryFileName = preferToQueryFile.get(prefer);
        Model model = QueryProcessor.getSimpleResourceGraph(prefixedRes, queryFileName, fusekiUrl, null);
        if (model.size() < 2) // there is a count added in the construct so there should always be one triple
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes));
        final String fullUri = AnnotationEndpoint.ANC_PREFIX+res;
        model = CollectionUtils.toW3CCollection(model, fullUri, prefer);
        ResponseBuilder builder = Response.ok(ResponseOutputStream.getModelStream(model, ext, fullUri, DocType.ANC));
        return AnnotationEndpoint.setHeaders(builder,
                AnnotationEndpoint.getAnnotationHeaders(info.getPath(), ext, "Choice", null, contentType))
                .build();
    }

}
