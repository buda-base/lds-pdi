package io.bdrc.ldspdi.annotations;

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
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.libraries.BudaMediaTypes;
import io.bdrc.libraries.StreamingHelpers;
import io.bdrc.libraries.formatters.JSONLDFormatter.DocType;

@RestController
@RequestMapping("/annotations/")
public class AnnotationEndpoint {

    public final static Logger log = LoggerFactory.getLogger(AnnotationEndpoint.class);
    public String fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);

    public final static String ANN_PROFILE = "http://www.w3.org/ns/anno.jsonld";
    public final static String ANN_SERVICE = "http://www.w3.org/ns/oa#annotationService";
    public final static String ANN_PROTOCOL = "http://www.w3.org/TR/annotation-protocol/";
    public final static String LDP_RES = "http://www.w3.org/ns/ldp#Resource";
    public final static String ANN_TYPE = "http://www.w3.org/ns/oa#Annotation";
    public final static String LDP_BC = "http://www.w3.org/ns/ldp#BasicContainer";
    public final static String LDP_CB = "http://www.w3.org/ns/ldp#constrainedBy";
    public final static String LDP_PMC = "http://www.w3.org/ns/ldp#PreferMinimalContainer";
    public final static String OA_PCI = "http://www.w3.org/ns/oa#PreferContainedIRIs";
    public final static String OA_PCD = "http://www.w3.org/ns/oa#PreferContainedDescriptions";
    public final static String OA_CONTEXT = "https://www.w3.org/ns/oa.jsonld";

    public final static String OA_CT = "application/ld+json; profile=\"http://www.w3.org/ns/oa.jsonld\"";
    public final static String W3C_CT = "application/ld+json; profile=\"http://www.w3.org/ns/anno.jsonld\"";

    public final static int W3C_ANN_MODE = 0;
    public final static int OA_ANN_MODE = 1;
    public final static int DEFAULT_ANN_MODE = W3C_ANN_MODE;

    public static final String ANN_PREFIX_SHORT = "bdan";
    public static final String ANC_PREFIX_SHORT = "bdac";
    public static final String ANN_PREFIX = "http://purl.bdrc.io/annotation/";
    public static final String ANC_PREFIX = "http://purl.bdrc.io/anncollection/";

    @GetMapping(value = "/{res}")
    public ResponseEntity<StreamingResponseBody> getResourceGraph(@PathVariable("res") String res,
            @RequestHeader(value = "Accept", required = false) String accept, HttpServletRequest request, HttpServletResponse response)
            throws RestException {
        log.info("Call to getResourceGraph() with URL: {}, accept: {}", request.getServletPath(), accept);
        MediaType mediaType;
        // spec says that when the Accept: header is absent, JSON-LD should be answered
        if (accept == null || accept.equals("*/*")) {
            mediaType = BudaMediaTypes.MT_JSONLD;
        } else {
            mediaType = BudaMediaTypes.selectVariant(accept, BudaMediaTypes.annVariants);
            if (mediaType == null) {
                // return AnnotationUtils.mediaTypeChoiceResponse(request);
            }
        }
        log.info("Call to getResourceGraph() MEDIA TYPE: {}", mediaType);
        String prefixedRes = ANN_PREFIX_SHORT + ':' + res;
        if (Helpers.equals(mediaType, MediaType.TEXT_HTML))
            AnnotationUtils.htmlResponse(request, response, prefixedRes);
        int mode = DEFAULT_ANN_MODE;
        DocType docType = DocType.ANN;
        String ext = null;
        if (mediaType.getSubtype().equals("ld+json")) {
            ext = "jsonld";
            mode = getJsonLdMode(mediaType);
            if (mode == OA_ANN_MODE) {
                mediaType = BudaMediaTypes.MT_JSONLD_OA;
                docType = DocType.OA;

            } else {
                mediaType = BudaMediaTypes.MT_JSONLD_WA;
            }
        } else {
            ext = BudaMediaTypes.getExtFromMime(mediaType);
        }
        log.info("Call to getResourceGraph() MEDIA TYPE FINAL: {} and ext= {}", mediaType, ext);
        Model model = QueryProcessor.getSimpleResourceGraph(prefixedRes, "AnnGraph.arq", fusekiUrl, null);
        if (model.size() == 0)
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes));
        // BodyBuilder bb = ResponseEntity.ok();
        // return ResponseEntity.ok().body(Helpers.getModelStream(model, ext, ANN_PREFIX
        // + res, docType));
        response = AnnotationUtils.setRespHeaders(response, request.getServletPath(), ext, "Choice", null, mediaType, false);
        return (ResponseEntity<StreamingResponseBody>) ResponseEntity.ok().contentType(mediaType)
                .body(StreamingHelpers.getModelStream(model, ext, ANN_PREFIX + res, docType, ServiceConfig.PREFIX.getPrefixMap()));
    }

    @GetMapping(value = "/{res}.{ext}")
    // these are always W3C web annotations, maybe there could be another endpoint
    // for OA
    public Object getResourceGraphSuffix(@PathVariable("res") String res, @PathVariable("ext") final String ext, HttpServletRequest request)
            throws RestException {
        log.info("Call to getResourceGraphSuffix() with URL: {}", request.getServletPath());
        MediaType mediaType = BudaMediaTypes.getMimeFromExtension(ext);
        if (mediaType == null) {
            return AnnotationUtils.mediaTypeChoiceResponse(request);
        }
        if ("jsonld".equals(ext)) {
            mediaType = BudaMediaTypes.MT_JSONLD_WA;
        }
        String prefixedRes = ANN_PREFIX_SHORT + ':' + res;
        Model model = QueryProcessor.getSimpleResourceGraph(prefixedRes, "AnnGraph.arq", fusekiUrl, null);
        if (model.size() == 0)
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes));
        BodyBuilder bb = ResponseEntity.ok();
        bb = AnnotationUtils.setRespHeaders(bb, request.getServletPath(), ext, "Choice", null, mediaType, false);
        return (ResponseEntity<StreamingResponseBody>) bb
                .body(StreamingHelpers.getModelStream(model, ext, ANN_PREFIX + res, DocType.ANN, ServiceConfig.PREFIX.getPrefixMap()));
    }

    static int getJsonLdMode(final MediaType mediaType) {
        String profile = mediaType.getParameters().get("profile");
        log.info("PROFILE >> " + profile);
        if (profile != null && profile.equals(OA_CONTEXT))
            return OA_ANN_MODE;
        return W3C_ANN_MODE;
    }

}
