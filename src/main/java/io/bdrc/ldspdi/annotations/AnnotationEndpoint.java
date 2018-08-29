package io.bdrc.ldspdi.annotations;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.ldspdi.utils.MediaTypeUtils;
import io.bdrc.ldspdi.utils.ResponseOutputStream;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

@Path("/annotation/")
public class AnnotationEndpoint {

    public final static Logger log = LoggerFactory.getLogger(AnnotationEndpoint.class.getName());
    public String fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);

    public final static String ANN_PROFILE = "http://www.w3.org/ns/anno.jsonld";
    public final static String ANN_SERVICE = "http://www.w3.org/ns/oa#annotationService";
    public final static String ANN_PROTOCOL = "http://www.w3.org/TR/annotation-protocol/";
    public final static String LDP_RES = "http://www.w3.org/ns/ldp#Resource";
    public final static String ANN_TYPE = "http://www.w3.org/ns/oa#Annotation";
    public final static String LDP_BC = "http://www.w3.org/ns/ldp#BasicContainer";
    public final static String LDP_CB = "http://www.w3.org/ns/ldp#constrainedBy";
    public final static String LDP_PMC = "http://www.w3.org/ns/ldp#PreferMinimalContainer";
    public final static String LDP_PCI = "http://www.w3.org/ns/oa#PreferContainedIRIs";
    public final static String LDP_PCD = "http://www.w3.org/ns/oa#PreferContainedDescriptions";
    public final static String OA_CONTEXT = "https://www.w3.org/ns/oa.jsonld";
    
    public final static String OA_CT = "application/ld+json; profile=\"http://www.w3.org/ns/oa.jsonld\"";
    public final static String W3C_CT = "application/ld+json; profile=\"http://www.w3.org/ns/anno.jsonld\"";
    
    public final static int W3C_ANN_MODE = 0;
    public final static int OA_ANN_MODE = 1;
    public final static int DEFAULT_ANN_MODE = W3C_ANN_MODE;

    @GET
    @Path("/{res}")
    public Response getResourceGraph(@PathParam("res") final String res, @HeaderParam("Accept") String format,
            @Context UriInfo info, @Context Request request, @Context HttpHeaders headers) throws RestException {
        log.info("Call to getResourceGraphGET() with URL: " + info.getPath() + " Accept >> " + format);
        final MediaType mediaType = getMediaType(request, format);
        if (mediaType == null)
            return mediaTypeChoiceResponse(info);
        if (mediaType.equals(MediaType.TEXT_HTML_TYPE))
            return htmlResponse(info, res);
        int mode = DEFAULT_ANN_MODE;
        String contentType = mediaType.toString();
        System.out.println(mediaType.getParameters());
        if (mediaType.getSubtype().equals("ld+json")) {
            mode = getJsonLdMode(mediaType);
            if (mode == OA_ANN_MODE) {
                contentType = OA_CT;
            } else {
                contentType = W3C_CT;
            }
        }
        
        Model model = QueryProcessor.getCoreResourceGraph(res, fusekiUrl, null);
        if (model.size() == 0)
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(res));
        final String ext = MediaTypeUtils.getExtFormatFromMime(mediaType.toString());
        ResponseBuilder builder = Response.ok(ResponseOutputStream.getModelStream(model, ext));
        return setHeaders(builder, getAnnotationHeaders(info.getPath(), ext, "Choice", null, contentType)).build();
    }

    static int getJsonLdMode(final MediaType mediaType) {
        String profile = mediaType.getParameters().get("profile");
        if (profile.equals(OA_CONTEXT))
            return OA_ANN_MODE;
        return W3C_ANN_MODE;
    }
    
//    @GET
//    @Path("/{res}.{ext}")
//    @JerseyCacheControl()
//    public Response getFormattedResourceGraph(@PathParam("res") final String res,
//            @DefaultValue("") @PathParam("ext") final String format, @Context UriInfo info) throws RestException {
//        log.info("Call to getFormattedResourceGraph()");
//        final MediaType mediaType = MediaTypeUtils.getMimeFromExtension(format);
//        if (mediaType == null)
//            return mediaTypeChoiceResponse(info);
//        Model model = QueryProcessor.getCoreResourceGraph(res, fusekiUrl, null);
//        if (model.size() == 0)
//            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(res));
//        ResponseBuilder builder = Response.ok(ResponseOutputStream.getModelStream(model, format, res), mediaType);
//        return setHeaders(builder, getAnnotationHeaders(info.getPath(), format, null, null)).build();
//    }

    public static MediaType getMediaType(Request request, String format) {
        if (format == null)
            return null;
        final Variant variant = request.selectVariant(MediaTypeUtils.annVariants);
        if (variant == null) {
            return null;
        }
        return variant.getMediaType();
    }

    public static Response mediaTypeChoiceResponse(final UriInfo info) throws RestException {
        final String html = Helpers.getMultiChoicesHtml(info.getPath(), true);
        final ResponseBuilder rb = Response.status(300).entity(html)
                .header("Content-Location", info.getBaseUri() + "choice?path=" + info.getPath());
        return setHeaders(rb, getAnnotationHeaders(info.getPath(), null, "List", null, "text/html")).build();
    }

    public static Response htmlResponse(final UriInfo info, final String res) throws RestException {
        try {
            ResponseBuilder builder = Response.seeOther(new URI(ServiceConfig.getProperty("showUrl") + res));
            return setHeaders(builder, getAnnotationHeaders(info.getPath(), null, "Choice", null, null)).build();
        } catch (URISyntaxException e) {
            throw new RestException(500, new LdsError(LdsError.URI_SYNTAX_ERR).setContext("getResourceGraphGet()", e));
        }
    }

    public static HashMap<String, String> getAnnotationHeaders(String url, final String ext, final String tcn,
            final String profile, final String contentType) {
        final HashMap<String, MediaType> map = MediaTypeUtils.getExtensionMimeMap();
        final HashMap<String, String> headers = new HashMap<>();
        if (ext != null) {
            final int dotidx = url.lastIndexOf('.');
            if (dotidx < 0) {
                headers.put("Content-Location", url + "." + ext);
            } else {
                url = url.substring(0, dotidx);
            }
        }
        final StringBuilder sb = new StringBuilder("");
        boolean first = true;
        for (Entry<String, MediaType> e : map.entrySet()) {
            if (!e.getKey().equals(ext)) {
                if (!first)
                    sb.append(",");
                sb.append("{\"" + url + "." + e.getKey() + "\" 1.000 {type " + e.getValue().toString() + "}}");
                first = false;
            }
        }
        if (contentType != null)
            headers.put("Content-Type", contentType);
        headers.put("Alternates", sb.toString());
        if (tcn != null)
            headers.put("TCN", tcn);
        headers.put("Vary", "Negotiate, Accept");
        return headers;
    }

    private static ResponseBuilder setHeaders(ResponseBuilder builder, HashMap<String, String> headers) {
        for (String key : headers.keySet()) {
            builder.header(key, headers.get(key));
        }
        return builder;
    }

}
