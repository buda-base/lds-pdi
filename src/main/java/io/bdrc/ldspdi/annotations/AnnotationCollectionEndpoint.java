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
import io.bdrc.ldspdi.rest.features.JerseyCacheControl;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.utils.MediaTypeUtils;
import io.bdrc.ldspdi.utils.ResponseOutputStream;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

@Path("/anncollection/")
public class AnnotationCollectionEndpoint {
    public final static Logger log = LoggerFactory.getLogger(AnnotationCollectionEndpoint.class.getName());
    public String fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);

    // we need to transform collections into sc:AnnotationLists, we do so when we receive the profile:

    static final Integer[] defaultRange = new Integer[]{0,0};

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
    @JerseyCacheControl()
    // whole collections (no subset)
    public Response getWholeCollection(@PathParam("res") final String res,
            @HeaderParam("Accept") final String accept,
            @HeaderParam("Prefer") final String preferHeader,
            @Context final UriInfo info,
            @Context final Request request,
            @Context final HttpHeaders headers
            ) throws RestException {
        log.info("Call to getWholeCollection() with URL: {}, accept: {}", info.getPath(), accept);
        final String prefixedCollectionRes = AnnotationEndpoint.ANC_PREFIX_SHORT+':'+res;
        final MediaType mediaType;
        // spec says that when the Accept: header is absent, JSON-LD should be answered
        if (accept == null) {
            mediaType = MediaTypeUtils.MT_JSONLD;
        } else {
            mediaType = MediaTypeUtils.getMediaType(request, accept, MediaTypeUtils.resVariants);
            if (mediaType == null)
                return AnnotationEndpoint.mediaTypeChoiceResponse(info);
        }
        if (mediaType.equals(MediaType.TEXT_HTML_TYPE))
            return AnnotationEndpoint.htmlResponse(info, prefixedCollectionRes);
        final Prefer prefer = getPrefer(preferHeader);
        final Model model = CollectionUtils.getSubsetGraph(prefixedCollectionRes, prefer, fusekiUrl, CollectionUtils.SubsetType.NONE, defaultRange, prefixedCollectionRes);
        return getResponse(model, DocType.ANP, AnnotationEndpoint.ANC_PREFIX+res, mediaType, prefer, info.getPath());
    }

    @GET
    @Path("/{res}.{ext}")
    @JerseyCacheControl()
    public Response getWholeCollectionSuffix(@PathParam("res") final String res,
            @PathParam("ext") final String ext,
            @HeaderParam("Prefer") final String preferHeader,
            @Context final UriInfo info,
            @Context final Request request,
            @Context final HttpHeaders headers
            ) throws RestException {
        log.info("Call to getWholeCollectionSuffix() with URL: {}", info.getPath());
        final MediaType mediaType = MediaTypeUtils.getMimeFromExtension(ext);
        if (mediaType == null)
            return AnnotationEndpoint.mediaTypeChoiceResponse(info);
        final String prefixedCollectionRes = AnnotationEndpoint.ANC_PREFIX_SHORT+':'+res;
        final Prefer prefer = getPrefer(preferHeader);
        final Model model = CollectionUtils.getSubsetGraph(prefixedCollectionRes, prefer, fusekiUrl, CollectionUtils.SubsetType.NONE, defaultRange, prefixedCollectionRes);
        return getResponse(model, DocType.ANP, AnnotationEndpoint.ANC_PREFIX+res, mediaType, prefer, info.getPath());
    }

    @GET
    @Path("/{res}/p{ptype}/{pnum}")
    @JerseyCacheControl()
    // Pages of whole collections
    // this is almost identical as the getWholeCollection because collections currently
    // have only one page. The only difference is the json serialization.
    public Response getWholeCollectionPage(@PathParam("res") final String res,
            @HeaderParam("Accept") final String accept,
            @PathParam("ptype") final char ptype,
            @PathParam("pnum") final int pnum,
            @Context final UriInfo info,
            @Context final Request request,
            @Context final HttpHeaders headers
            ) throws RestException {
        log.info("Call to getWholeCollectionPage() with URL: {}, accept: {}", info.getPath(), accept);
        final String prefixedCollectionRes = AnnotationEndpoint.ANC_PREFIX_SHORT+':'+res;
        final Prefer prefer = CollectionUtils.pageUrlCharToPrefer.get(ptype);
        if (prefer == null || pnum != 1) { // all collections currently have 1 page
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedCollectionRes));
        }
        final MediaType mediaType;
        // spec says that when the Accept: header is absent, JSON-LD should be answered
        if (accept == null) {
            mediaType = MediaTypeUtils.MT_JSONLD;
        } else {
            mediaType = MediaTypeUtils.getMediaType(request, accept, MediaTypeUtils.resVariantsNoHtml);
            if (mediaType == null)
                return AnnotationEndpoint.mediaTypeChoiceResponse(info);
        }
        final Model model = CollectionUtils.getSubsetGraph(prefixedCollectionRes, prefer, fusekiUrl, CollectionUtils.SubsetType.NONE, defaultRange, prefixedCollectionRes);
        return getResponse(model, DocType.ANP, AnnotationEndpoint.ANC_PREFIX+res, mediaType, prefer, info.getPath());
    }

    @GET
    @Path("/{res}/p{ptype}/{pnum}.{ext}")
    @JerseyCacheControl()
    public Response getWholeCollectionPageSuffix(@PathParam("res") final String res,
            @PathParam("ext") final String ext,
            @PathParam("ptype") final char ptype,
            @PathParam("pnum") final int pnum,
            @Context final UriInfo info,
            @Context final Request request,
            @Context final HttpHeaders headers
            ) throws RestException {
        log.info("Call to getWholeCollectionPageSuffix() with URL: {}", info.getPath());
        final String prefixedCollectionRes = AnnotationEndpoint.ANC_PREFIX_SHORT+':'+res;
        final Prefer prefer = CollectionUtils.pageUrlCharToPrefer.get(ptype);
        if (prefer == null || pnum != 1) { // all collections currently have 1 page
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedCollectionRes));
        }
        final MediaType mediaType = MediaTypeUtils.getMimeFromExtension(ext);
        if (mediaType == null)
            return AnnotationEndpoint.mediaTypeChoiceResponse(info);
        final Model model = CollectionUtils.getSubsetGraph(prefixedCollectionRes, prefer, fusekiUrl, CollectionUtils.SubsetType.NONE, defaultRange, prefixedCollectionRes);
        return getResponse(model, DocType.ANP, AnnotationEndpoint.ANC_PREFIX+res, mediaType, prefer, info.getPath());
    }

    @GET
    @Path("/{res}/sub/{subtype}/{subcoordinates}")
    @JerseyCacheControl()
    // Collection subset
    // a subcollection corresponding to a page or character range
    public Response getCollectionSubset(@PathParam("res") final String res,
            @HeaderParam("Accept") final String accept,
            @PathParam("subtype") final String subtype,
            @PathParam("subcoordinates") final String subcoordinates,
            @HeaderParam("Prefer") final String preferHeader,
            @Context final UriInfo info,
            @Context final Request request,
            @Context final HttpHeaders headers
            ) throws RestException {
        log.info("Call to getWholeCollectionPage() with URL: {}, accept: {}", info.getPath(), accept);
        final String prefixedCollectionRes = AnnotationEndpoint.ANC_PREFIX_SHORT+':'+res;
        final String collectionAliasUri = AnnotationEndpoint.ANC_PREFIX+res+"/sub/"+subtype+"/"+subcoordinates;
        final MediaType mediaType;
        // spec says that when the Accept: header is absent, JSON-LD should be answered
        if (accept == null) {
            mediaType = MediaTypeUtils.MT_JSONLD;
        } else {
            mediaType = MediaTypeUtils.getMediaType(request, accept, MediaTypeUtils.resVariantsNoHtml);
            if (mediaType == null)
                return AnnotationEndpoint.mediaTypeChoiceResponse(info);
        }
        final Prefer prefer = getPrefer(preferHeader);
        final Model model = CollectionUtils.getSubsetGraph(prefixedCollectionRes, prefer, fusekiUrl, subtype, subcoordinates, collectionAliasUri);
        return getResponse(model, DocType.ANC, collectionAliasUri, mediaType, prefer, info.getPath());
    }

    @GET
    @Path("/{res}/sub/{subtype}/{subcoordinates}.{ext}")
    @JerseyCacheControl()
    public Response getCollectionSubsetSuffix(@PathParam("res") final String res,
            @PathParam("ext") final String ext,
            @PathParam("subtype") final String subtype,
            @PathParam("subcoordinates") final String subcoordinates,
            @HeaderParam("Prefer") final String preferHeader,
            @Context final UriInfo info,
            @Context final Request request,
            @Context final HttpHeaders headers
            ) throws RestException {
        log.info("Call to getCollectionSubsetSuffix() with URL: {}", info.getPath());
        final String prefixedCollectionRes = AnnotationEndpoint.ANC_PREFIX_SHORT+':'+res;
        final String collectionAliasUri = AnnotationEndpoint.ANC_PREFIX+res+"/sub/"+subtype+"/"+subcoordinates;
        final MediaType mediaType = MediaTypeUtils.getMimeFromExtension(ext);
        if (mediaType == null)
            return AnnotationEndpoint.mediaTypeChoiceResponse(info);
        final Prefer prefer = getPrefer(preferHeader);
        final Model model = CollectionUtils.getSubsetGraph(prefixedCollectionRes, prefer, fusekiUrl, subtype, subcoordinates, collectionAliasUri);
        return getResponse(model, DocType.ANC, collectionAliasUri, mediaType, prefer, info.getPath());
    }

    @GET
    @Path("/{res}/sub/{subtype}/{subcoordinates}/p{ptype}/{pnum}")
    @JerseyCacheControl()
    // Collection subset page
    public Response getCollectionSubsetPage(@PathParam("res") final String res,
            @HeaderParam("Accept") final String accept,
            @PathParam("subtype") final String subtype,
            @PathParam("subcoordinates") final String subcoordinates,
            @PathParam("ptype") final char ptype,
            @PathParam("pnum") final int pnum,
            @Context final UriInfo info,
            @Context final Request request,
            @Context final HttpHeaders headers
            ) throws RestException {
        log.info("Call to getCollectionSubsetPage() with URL: {}, accept: {}", info.getPath(), accept);
        final String prefixedCollectionRes = AnnotationEndpoint.ANC_PREFIX_SHORT+':'+res;
        final Prefer prefer = CollectionUtils.pageUrlCharToPrefer.get(ptype);
        if (prefer == null || pnum != 1) { // all collections currently have 1 page
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedCollectionRes));
        }
        final String collectionAliasUri = AnnotationEndpoint.ANC_PREFIX+res+"/sub/"+subtype+"/"+subcoordinates;
        final MediaType mediaType;
        // spec says that when the Accept: header is absent, JSON-LD should be answered
        if (accept == null) {
            mediaType = MediaTypeUtils.MT_JSONLD;
        } else {
            mediaType = MediaTypeUtils.getMediaType(request, accept, MediaTypeUtils.resVariantsNoHtml);
            if (mediaType == null)
                return AnnotationEndpoint.mediaTypeChoiceResponse(info);
        }
        final Model model = CollectionUtils.getSubsetGraph(prefixedCollectionRes, prefer, fusekiUrl, subtype, subcoordinates, collectionAliasUri);
        return getResponse(model, DocType.ANP, collectionAliasUri, mediaType, prefer, info.getPath());
    }

    @GET
    @Path("/{res}/sub/{subtype}/{subcoordinates}/p{ptype}/{pnum}.{ext}")
    @JerseyCacheControl()
    public Response getCollectionSubsetPageSuffix(@PathParam("res") final String res,
            @PathParam("ext") final String ext,
            @PathParam("subtype") final String subtype,
            @PathParam("subcoordinates") final String subcoordinates,
            @PathParam("ptype") final char ptype,
            @PathParam("pnum") final int pnum,
            @Context final UriInfo info,
            @Context final Request request,
            @Context final HttpHeaders headers
            ) throws RestException {
        log.info("Call to getCollectionSubsetPage() with URL: {}", info.getPath());
        final String prefixedCollectionRes = AnnotationEndpoint.ANC_PREFIX_SHORT+':'+res;
        final Prefer prefer = CollectionUtils.pageUrlCharToPrefer.get(ptype);
        if (prefer == null || pnum != 1) { // all collections currently have 1 page
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedCollectionRes));
        }
        final String collectionAliasUri = AnnotationEndpoint.ANC_PREFIX+res+"/sub/"+subtype+"/"+subcoordinates;
        final MediaType mediaType = MediaTypeUtils.getMimeFromExtension(ext);
        if (mediaType == null)
            return AnnotationEndpoint.mediaTypeChoiceResponse(info);
        final Model model = CollectionUtils.getSubsetGraph(prefixedCollectionRes, prefer, fusekiUrl, subtype, subcoordinates, collectionAliasUri);
        return getResponse(model, DocType.ANP, collectionAliasUri, mediaType, prefer, info.getPath());
    }

    private Response getResponse(final Model model, final DocType docType,
            final String collectionAliasUri, final MediaType mediaType, final Prefer prefer,
            final String path) throws RestException {
        if (model.size() < 2) // there is a count added in the construct so there should always be one triple
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(collectionAliasUri));
        final String contentType = mediaType.toString();
        final String ext = MediaTypeUtils.getExtFormatFromMime(contentType);
        CollectionUtils.toW3CCollection(model, collectionAliasUri, prefer);
        final ResponseBuilder builder = Response.ok(ResponseOutputStream.getModelStream(model, ext, collectionAliasUri, docType));
        return AnnotationEndpoint.setHeaders(builder,
                AnnotationEndpoint.getAnnotationHeaders(path, ext, "Choice", null, contentType))
                .build();

    }

}
