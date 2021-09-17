package io.bdrc.ldspdi.rest.controllers;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.ontology.service.shapes.OntShapesData;
import io.bdrc.ldspdi.service.OntPolicies;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.libraries.BudaMediaTypes;
import io.bdrc.libraries.StreamingHelpers;

@RestController
@RequestMapping("/")
public class ShaclDataController {

    public final static Logger log = LoggerFactory.getLogger("default");

    @GetMapping(value = "/shapes/**")
    public ResponseEntity<StreamingResponseBody> getShaclOntGraph(@RequestHeader(value = "fusekiUrl", required = false) final String fusekiUrl,
            @RequestHeader(value = "Accept", required = false) String format, HttpServletResponse response, HttpServletRequest request)
            throws RestException, IOException {
        log.info("getShaclOntGraph() for path {} ", request.getRequestURL());
        Helpers.setCacheControl(response, "public");
        return processShapeUrl(request.getRequestURL().toString(), false, response);
    }

    @GetMapping(value = "/ontology/shapes/**")
    public ResponseEntity<StreamingResponseBody> getShaclOntResGraph(@RequestHeader(value = "fusekiUrl", required = false) final String fusekiUrl,
            @RequestHeader(value = "Accept", required = false) String format, HttpServletResponse response, HttpServletRequest request)
            throws RestException, IOException {
        log.info("getShaclOntResGraph() for path {} ", request.getRequestURL());
        Helpers.setCacheControl(response, "public");
        return processShapeUrl(request.getRequestURL().toString(), true, response);
    }

    @GetMapping(value = "/ontology/shapes/core")
    public ResponseEntity<StreamingResponseBody> getShaclAllOntGraph(@RequestHeader(value = "fusekiUrl", required = false) final String fusekiUrl,
            @RequestHeader(value = "Accept", required = false) String format, HttpServletResponse response, HttpServletRequest request)
            throws RestException, IOException {
        Helpers.setCacheControl(response, "public");
        response.setHeader("ETag", OntShapesData.getCommitId());
        log.info("getShaclAllOntGraph {} ", request.getRequestURL());
        String url = request.getRequestURL().toString();
        Model m = OntShapesData.getFullModel();
        boolean ext = hasValidExtension(url);
        String extension = "ttl";
        MediaType mt = null;
        if (ext) {
            extension = url.substring(url.lastIndexOf(".") + 1);
            url = url.substring(0, url.lastIndexOf("."));
            mt = BudaMediaTypes.getMimeFromExtension(extension);
            log.info("getShaclOntGraph() Url is {} and is baseuri {}", url, OntPolicies.isBaseUri(url));
        }
        if (mt == null || extension == null) {
            mt = BudaMediaTypes.MT_TTL;
            extension = "ttl";
        }
        return ResponseEntity.ok().eTag(OntShapesData.getCommitId()).contentType(mt)
                .body(StreamingHelpers.getModelStream(m, extension, url, null, ServiceConfig.PREFIX.getPrefixMap()));
    }

    private ResponseEntity<StreamingResponseBody> processShapeUrl(String url, boolean resource, HttpServletResponse response) throws RestException {
        Model m = ModelFactory.createDefaultModel();
        MediaType mt = null;
        String extension = null;
        boolean ext = hasValidExtension(url);
        if (ext) {
            extension = url.substring(url.lastIndexOf(".") + 1);
            url = url.substring(0, url.lastIndexOf("."));
            mt = BudaMediaTypes.getMimeFromExtension(extension);
            log.info("getShaclOntGraph() Url is {} and is baseuri {}", url, OntPolicies.isBaseUri(url));
        }
        if (mt == null || extension == null) {
            mt = BudaMediaTypes.MT_TTL;
            extension = "ttl";
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.startsWith("https")) {
            url = "http"+url.substring(5, url.length());
        }
        if (OntPolicies.isBaseUri(url)) {
            //log.info("url {} is base uri", url);
            m = OntShapesData.getOntModelByBase(url);
        } else {
            if (resource) {
                url = url.replace(ServiceConfig.getProperty("serverRoot"), "purl.bdrc.io");
            }
            String query = "describe <" + url + ">";
            m = QueryProcessor.getGraphFromModel(query, OntShapesData.getFullModel());
        }
        Helpers.setCacheControl(response, "public");
        if (m != null && m.size() > 0) {
            return ResponseEntity.ok().eTag(OntShapesData.getCommitId()).contentType(mt)
                    .body(StreamingHelpers.getModelStream(m, extension, url, null, ServiceConfig.PREFIX.getPrefixMap()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_PLAIN)
                .body(StreamingHelpers.getStream("The requested shape could not be found"));
    }

    private static boolean hasValidExtension(String url) {
        String ext = url.substring(url.lastIndexOf(".") + 1);
        if (BudaMediaTypes.getMimeFromExtension(ext) == null) {
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        System.out.println(hasValidExtension("whatever.ttl"));
        System.out.println(hasValidExtension("http://localhost:8080/shapes1/tt/fgt/.json"));
    }

}
