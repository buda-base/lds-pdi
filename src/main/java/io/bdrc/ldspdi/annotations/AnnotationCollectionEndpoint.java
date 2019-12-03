package io.bdrc.ldspdi.annotations;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.rest.features.SpringCacheControl;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.libraries.BudaMediaTypes;
import io.bdrc.libraries.StreamingHelpers;
import io.bdrc.libraries.formatters.JSONLDFormatter.DocType;

@RestController
@RequestMapping("/anncollection/*")
public class AnnotationCollectionEndpoint {

    public final static Logger log = LoggerFactory.getLogger(AnnotationCollectionEndpoint.class);
    public String fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);

    // we need to transform collections into sc:AnnotationLists, we do so when we
    // receive the profile:

    static final Integer[] defaultRange = new Integer[] { 0, 0 };

    public static enum Prefer {
        MINIMAL, IRI, DESCRIPTION;
    }

    public final static String PREFER_LDP_PMC = "return=representation;include=\"http://www.w3.org/ns/ldp#PreferMinimalContainer\"";
    public final static String PREFER_OA_PCI = "return=representation;include=\"http://www.w3.org/ns/oa#PreferContainedIRIs\"";
    public final static String PREFER_OA_PCD = "return=representation;include=\"http://www.w3.org/ns/oa#PreferContainedDescriptions\"";

    static final Map<Prefer, String> preferToQueryFile = new HashMap<>();
    static {
        preferToQueryFile.put(Prefer.MINIMAL, "AnnCollection-minimal.arq");
        preferToQueryFile.put(Prefer.IRI, "AnnCollection-iri.arq");
        preferToQueryFile.put(Prefer.DESCRIPTION, "AnnCollection-description.arq");
    }

    static Prefer getPrefer(final String preferHeader) {
        if (preferHeader == null)
            return Prefer.DESCRIPTION;
        switch (preferHeader) {
        case PREFER_LDP_PMC:
            return Prefer.MINIMAL;
        case PREFER_OA_PCI:
            return Prefer.IRI;
        case PREFER_OA_PCD:
            return Prefer.DESCRIPTION;
        default:
            return Prefer.DESCRIPTION; // default in the WAP spec
        }
    }

    @GetMapping("/{res}")
    @SpringCacheControl()
    // whole collections (no subset)
    public ResponseEntity<StreamingResponseBody> getWholeCollection(@PathVariable("res") final String res, @RequestHeader(value = "Accept", required = false) final String accept,
            @RequestHeader(value = "Prefer", required = false) final String preferHeader, HttpServletRequest request, HttpServletResponse response) throws RestException {
        log.info("Call to getWholeCollection() with URL: {}, accept: {}", request.getServletPath(), accept);
        final String prefixedCollectionRes = AnnotationEndpoint.ANC_PREFIX_SHORT + ':' + res;
        final MediaType mediaType;
        // spec says that when the Accept: header is absent, JSON-LD should be answered
        if (accept == null) {
            mediaType = BudaMediaTypes.MT_JSONLD;
        } else {
            mediaType = BudaMediaTypes.selectVariant(accept, BudaMediaTypes.resVariants);
            if (mediaType == null)
                return AnnotationUtils.mediaTypeChoiceResponse(request);
        }
        if (Helpers.equals(mediaType, MediaType.TEXT_HTML))
            AnnotationUtils.htmlResponse(request, response, prefixedCollectionRes);
        final Prefer prefer = getPrefer(preferHeader);
        final Model model = CollectionUtils.getSubsetGraph(prefixedCollectionRes, prefer, fusekiUrl, CollectionUtils.SubsetType.NONE, defaultRange, prefixedCollectionRes);
        return getResponse(model, DocType.ANP, AnnotationEndpoint.ANC_PREFIX + res, mediaType, prefer, request.getServletPath(), "Choice");
    }

    @GetMapping("/{res}.{ext}")
    @SpringCacheControl()
    public ResponseEntity<StreamingResponseBody> getWholeCollectionSuffix(@PathVariable("res") final String res, @PathVariable("ext") final String ext, @RequestHeader(value = "Prefer", required = false) final String preferHeader,
            HttpServletRequest request) throws RestException {
        log.info("Call to getWholeCollectionSuffix() with URL: {}", request.getServletPath());
        final MediaType mediaType = BudaMediaTypes.getMimeFromExtension(ext);
        if (mediaType == null)
            return AnnotationUtils.mediaTypeChoiceResponse(request);
        final String prefixedCollectionRes = AnnotationEndpoint.ANC_PREFIX_SHORT + ':' + res;
        final Prefer prefer = getPrefer(preferHeader);
        final Model model = CollectionUtils.getSubsetGraph(prefixedCollectionRes, prefer, fusekiUrl, CollectionUtils.SubsetType.NONE, defaultRange, prefixedCollectionRes);
        return getResponse(model, DocType.ANP, AnnotationEndpoint.ANC_PREFIX + res, mediaType, prefer, request.getServletPath(), null);
    }

    @GetMapping("/{res}/p{ptype}/{pnum}")
    @SpringCacheControl()
    // Pages of whole collections
    // this is almost identical as the getWholeCollection because collections
    // currently
    // have only one page. The only difference is the json serialization.
    public ResponseEntity<StreamingResponseBody> getWholeCollectionPage(@PathVariable("res") final String res, @RequestHeader(value = "Accept", required = false) final String accept, @PathVariable("ptype") final char ptype,
            @PathVariable("pnum") final int pnum, HttpServletRequest request) throws RestException {
        log.info("Call to getWholeCollectionPage() with URL: {}, accept: {}", request.getServletPath(), accept);
        final String prefixedCollectionRes = AnnotationEndpoint.ANC_PREFIX_SHORT + ':' + res;
        final Prefer prefer = CollectionUtils.pageUrlCharToPrefer.get(ptype);
        if (prefer == null || pnum != 1) { // all collections currently have 1 page
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedCollectionRes));
        }
        final MediaType mediaType;
        // spec says that when the Accept: header is absent, JSON-LD should be answered
        if (accept == null) {
            mediaType = BudaMediaTypes.MT_JSONLD;
        } else {
            mediaType = BudaMediaTypes.selectVariant(accept, BudaMediaTypes.resVariantsNoHtml);
            if (mediaType == null)
                return AnnotationUtils.mediaTypeChoiceResponse(request);
        }
        final Model model = CollectionUtils.getSubsetGraph(prefixedCollectionRes, prefer, fusekiUrl, CollectionUtils.SubsetType.NONE, defaultRange, prefixedCollectionRes);
        return getResponse(model, DocType.ANP, AnnotationEndpoint.ANC_PREFIX + res, mediaType, prefer, request.getServletPath(), "Choice");
    }

    @GetMapping("/{res}/p{ptype}/{pnum}.{ext}")
    @SpringCacheControl()
    public ResponseEntity<StreamingResponseBody> getWholeCollectionPageSuffix(@PathVariable("res") final String res, @PathVariable("ext") final String ext, @PathVariable("ptype") final char ptype, @PathVariable("pnum") final int pnum,
            HttpServletRequest request) throws RestException {
        log.info("Call to getWholeCollectionPageSuffix() with URL: {}", request.getServletPath());
        final String prefixedCollectionRes = AnnotationEndpoint.ANC_PREFIX_SHORT + ':' + res;
        final Prefer prefer = CollectionUtils.pageUrlCharToPrefer.get(ptype);
        if (prefer == null || pnum != 1) { // all collections currently have 1 page
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedCollectionRes));
        }
        final MediaType mediaType = BudaMediaTypes.getMimeFromExtension(ext);
        if (mediaType == null)
            return AnnotationUtils.mediaTypeChoiceResponse(request);
        final Model model = CollectionUtils.getSubsetGraph(prefixedCollectionRes, prefer, fusekiUrl, CollectionUtils.SubsetType.NONE, defaultRange, prefixedCollectionRes);
        return getResponse(model, DocType.ANP, AnnotationEndpoint.ANC_PREFIX + res, mediaType, prefer, request.getServletPath(), null);
    }

    @GetMapping("/{res}/sub/{subtype}/{subcoordinates}")
    @SpringCacheControl()
    // Collection subset
    // a subcollection corresponding to a page or character range
    public ResponseEntity<StreamingResponseBody> getCollectionSubset(@PathVariable("res") final String res, @RequestHeader(value = "Accept", required = false) final String accept, @PathVariable("subtype") final String subtype,
            @PathVariable("subcoordinates") final String subcoordinates, @RequestHeader(value = "Prefer", required = false) final String preferHeader, HttpServletRequest request) throws RestException {
        log.info("Call to getWholeCollectionPage() with URL: {}, accept: {}", request.getServletPath(), accept);
        final String prefixedCollectionRes = AnnotationEndpoint.ANC_PREFIX_SHORT + ':' + res;
        final String collectionAliasUri = AnnotationEndpoint.ANC_PREFIX + res + "/sub/" + subtype + "/" + subcoordinates;
        final MediaType mediaType;
        // spec says that when the Accept: header is absent, JSON-LD should be answered
        if (accept == null) {
            mediaType = BudaMediaTypes.MT_JSONLD;
        } else {
            mediaType = BudaMediaTypes.selectVariant(accept, BudaMediaTypes.resVariantsNoHtml);
            if (mediaType == null)
                return AnnotationUtils.mediaTypeChoiceResponse(request);
        }
        final Prefer prefer = getPrefer(preferHeader);
        final Model model = CollectionUtils.getSubsetGraph(prefixedCollectionRes, prefer, fusekiUrl, subtype, subcoordinates, collectionAliasUri);
        return getResponse(model, DocType.ANC, collectionAliasUri, mediaType, prefer, request.getServletPath(), "Choice");
    }

    @GetMapping("/{res}/sub/{subtype}/{subcoordinates}.{ext}")
    @SpringCacheControl()
    public ResponseEntity<StreamingResponseBody> getCollectionSubsetSuffix(@PathVariable("res") final String res, @PathVariable("ext") final String ext, @PathVariable("subtype") final String subtype,
            @PathVariable("subcoordinates") final String subcoordinates, @RequestHeader("Prefer") final String preferHeader, HttpServletRequest request) throws RestException {
        log.info("Call to getCollectionSubsetSuffix() with URL: {}", request.getServletPath());
        final String prefixedCollectionRes = AnnotationEndpoint.ANC_PREFIX_SHORT + ':' + res;
        final String collectionAliasUri = AnnotationEndpoint.ANC_PREFIX + res + "/sub/" + subtype + "/" + subcoordinates;
        final MediaType mediaType = BudaMediaTypes.getMimeFromExtension(ext);
        if (mediaType == null)
            return AnnotationUtils.mediaTypeChoiceResponse(request);
        final Prefer prefer = getPrefer(preferHeader);
        final Model model = CollectionUtils.getSubsetGraph(prefixedCollectionRes, prefer, fusekiUrl, subtype, subcoordinates, collectionAliasUri);
        return getResponse(model, DocType.ANC, collectionAliasUri, mediaType, prefer, request.getServletPath(), null);
    }

    @GetMapping("/{res}/sub/{subtype}/{subcoordinates}/p{ptype}/{pnum}")
    @SpringCacheControl()
    // Collection subset page
    public ResponseEntity<StreamingResponseBody> getCollectionSubsetPage(@PathVariable("res") final String res, @RequestHeader(value = "Accept", required = false) final String accept, @PathVariable("subtype") final String subtype,
            @PathVariable("subcoordinates") final String subcoordinates, @PathVariable("ptype") final char ptype, @PathVariable("pnum") final String pnum, HttpServletRequest request) throws RestException {
        if (pnum.contains(".")) {
            String[] parts = pnum.split("\\.");
            return getCollectionSubsetPageSuffix(res, parts[1], subtype, subcoordinates, ptype, parts[0], request);

        }
        log.info("Call to getCollectionSubsetPage() with URL: {}, accept: {}", request.getServletPath(), accept);
        final String prefixedCollectionRes = AnnotationEndpoint.ANC_PREFIX_SHORT + ':' + res;
        final Prefer prefer = CollectionUtils.pageUrlCharToPrefer.get(ptype);
        if (prefer == null || Integer.parseInt(pnum) != 1) { // all collections currently have 1 page
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedCollectionRes));
        }
        final String collectionAliasUri = AnnotationEndpoint.ANC_PREFIX + res + "/sub/" + subtype + "/" + subcoordinates;
        final MediaType mediaType;
        // spec says that when the Accept: header is absent, JSON-LD should be answered
        if (accept == null) {
            mediaType = BudaMediaTypes.MT_JSONLD;
        } else {
            mediaType = BudaMediaTypes.selectVariant(accept, BudaMediaTypes.resVariantsNoHtml);
            if (mediaType == null)
                return AnnotationUtils.mediaTypeChoiceResponse(request);
        }
        final Model model = CollectionUtils.getSubsetGraph(prefixedCollectionRes, prefer, fusekiUrl, subtype, subcoordinates, collectionAliasUri);
        return getResponse(model, DocType.ANP, collectionAliasUri, mediaType, prefer, request.getServletPath(), "Choice");
    }

    // @GetMapping("/{res}/sub/{subtype}/{subcoordinates}/p{ptype}/{pnum}.{ext}")
    @SpringCacheControl()
    public ResponseEntity<StreamingResponseBody> getCollectionSubsetPageSuffix(@PathVariable("res") final String res, @PathVariable("ext") final String ext, @PathVariable("subtype") final String subtype,
            @PathVariable("subcoordinates") final String subcoordinates, @PathVariable("ptype") final char ptype, @PathVariable("pnum") final String pnum, HttpServletRequest request) throws RestException {

        log.info("Call to getCollectionSubsetPage() with URL: {}", request.getServletPath());
        final String prefixedCollectionRes = AnnotationEndpoint.ANC_PREFIX_SHORT + ':' + res;
        final Prefer prefer = CollectionUtils.pageUrlCharToPrefer.get(ptype);
        if (prefer == null || Integer.parseInt(pnum) != 1) { // all collections currently have 1 page
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedCollectionRes));
        }
        final String collectionAliasUri = AnnotationEndpoint.ANC_PREFIX + res + "/sub/" + subtype + "/" + subcoordinates;
        final MediaType mediaType = BudaMediaTypes.getMimeFromExtension(ext);
        if (mediaType == null)
            return AnnotationUtils.mediaTypeChoiceResponse(request);
        final Model model = CollectionUtils.getSubsetGraph(prefixedCollectionRes, prefer, fusekiUrl, subtype, subcoordinates, collectionAliasUri);
        return getResponse(model, DocType.ANP, collectionAliasUri, mediaType, prefer, request.getServletPath(), null);
    }

    private ResponseEntity<StreamingResponseBody> getResponse(final Model model, DocType docType, final String collectionAliasUri, final MediaType mediaType, final Prefer prefer, final String path, final String tcn) throws RestException {
        if (model.size() < 2) // there is a count added in the construct so there should always be one triple
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(collectionAliasUri));
        final String ext = BudaMediaTypes.getExtFromMime(mediaType);
        CollectionUtils.toW3CCollection(model, collectionAliasUri, prefer);
        BodyBuilder bb = ResponseEntity.ok();
        bb = AnnotationUtils.setRespHeaders(bb, path, ext, tcn, null, mediaType, true);
        return bb.body(StreamingHelpers.getModelStream(model, ext, collectionAliasUri, docType));

    }

}
