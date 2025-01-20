package io.bdrc.ldspdi.annotations;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.libraries.BudaMediaTypes;
import io.bdrc.libraries.StreamingHelpers;

public class AnnotationUtils {

    public static ResponseEntity<StreamingResponseBody> mediaTypeChoiceResponse(HttpServletRequest request) throws RestException {
        final String html = Helpers.getMultiChoicesHtml(request.getServletPath(), true);
        BodyBuilder bb = ResponseEntity.status(300).header("Content-Type", "text/html");
        bb = setRespHeaders(bb, request.getServletPath(), null, "List", null, MediaType.TEXT_HTML, false);
        return bb.header("Content-Location", request.getRequestURI() + "choice?path=" + request.getServletPath()).body(StreamingHelpers.getStream(html));
    }

    public static void htmlResponse(HttpServletRequest request, HttpServletResponse response, final String res) throws RestException {
        try {
            response = setRespHeaders(response, request.getServletPath(), null, "Choice", null, null, false);
            response.sendRedirect(ServiceConfig.getProperty("showUrl") + res);

        } catch (IOException e) {
            throw new RestException(500, new LdsError(LdsError.URI_SYNTAX_ERR).setContext("getResourceGraphGet()", e));
        }
    }

    static BodyBuilder setRespHeaders(BodyBuilder builder, String url, final String ext, final String tcn, final String profile, final org.springframework.http.MediaType mediaType, final boolean collection) {
        final Map<String, MediaType> map = BudaMediaTypes.getResExtensionMimeMap();
        if (collection) {
            builder.header("Link", "<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\"");
            builder.header("Link", "<http://www.w3.org/TR/annotation-protocol/>; rel=\"http://www.w3.org/ns/ldp#constrainedBy\"");
        } else {
            builder.header("Link", "<http://www.w3.org/ns/ldp#Resource>; rel=\"type\"");
            // not mandatory in the spec:
            builder.header("Link", "<http://www.w3.org/ns/oa#Annotation>; rel=\"type\"");
        }
        // TODO: spec mandates the ETag header...
        builder.header("Allow", "GET, OPTIONS, HEAD");
        if (ext != null) {
            final int dotidx = url.lastIndexOf('.');
            if (dotidx < 0) {
                builder.header("Content-Location", url + "." + ext);
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
        if (mediaType != null)
            builder.contentType(mediaType);
        builder.header("Alternates", sb.toString());
        if (tcn != null)
            builder.header("TCN", tcn);
        builder.header("Vary", "Negotiate, Accept");
        return builder;
    }

    static HttpServletResponse setRespHeaders(HttpServletResponse resp, String url, final String ext, final String tcn, final String profile, final org.springframework.http.MediaType mediaType, final boolean collection) {
        final Map<String, MediaType> map = BudaMediaTypes.getResExtensionMimeMap();
        if (collection) {
            resp.addHeader("Link", "<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\"");
            resp.addHeader("Link", "<http://www.w3.org/TR/annotation-protocol/>; rel=\"http://www.w3.org/ns/ldp#constrainedBy\"");
        } else {
            resp.addHeader("Link", "<http://www.w3.org/ns/ldp#Resource>; rel=\"type\"");
            // not mandatory in the spec:
            resp.addHeader("Link", "<http://www.w3.org/ns/oa#Annotation>; rel=\"type\"");
        }
        // TODO: spec mandates the ETag header...
        resp.addHeader("Allow", "GET, OPTIONS, HEAD");
        if (ext != null) {
            final int dotidx = url.lastIndexOf('.');
            if (dotidx < 0) {
                resp.addHeader("Content-Location", url + "." + ext);
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
        if (mediaType != null)
            resp.setContentType(mediaType.getType() + "/" + mediaType.getSubtype());
        resp.addHeader("Alternates", sb.toString());
        if (tcn != null)
            resp.addHeader("TCN", tcn);
        resp.addHeader("Vary", "Negotiate, Accept");
        return resp;
    }

}
