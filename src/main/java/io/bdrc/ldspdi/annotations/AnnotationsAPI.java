package io.bdrc.ldspdi.annotations;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.bdrc.formatters.JSONLDFormatter.DocType;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.LdsQuery;
import io.bdrc.ldspdi.sparql.LdsQueryService;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.BudaMediaTypes;
import io.bdrc.ldspdi.utils.Helpers;

@RestController
@RequestMapping("/annotations/")
public class AnnotationsAPI {

    final static Model COLL_SERV = ModelFactory.createDefaultModel().read(AnnotationsAPI.class.getClassLoader().getResourceAsStream("collectionService.ttl"), "http://api.bdrc.io/annotations/", "TURTLE");
    final static String annotSearchQueryFile = "annotLayerSearch.arq";

    @GetMapping(value = "/collectionService")
    public ResponseEntity<StreamingResponseBody> collectionService(@RequestHeader(value = "Accept", required = false) String accept, HttpServletRequest request) throws RestException {
        MediaType mediaType;
        if (accept == null || accept.contains("text/html")) {
            mediaType = BudaMediaTypes.MT_JSONLD;
        } else {
            mediaType = BudaMediaTypes.selectVariant(accept, BudaMediaTypes.graphVariants);
            if (mediaType == null)
                return AnnotationUtils.mediaTypeChoiceResponse(request);
        }
        String ext = BudaMediaTypes.getExtFromMime(mediaType);
        COLL_SERV.write(System.out, "JSON-LD");
        BodyBuilder bb = ResponseEntity.ok();
        bb = AnnotationUtils.setRespHeaders(bb, request.getServletPath(), ext, "Choice", null, mediaType, false);
        return bb.body(Helpers.getModelStream(COLL_SERV, ext, "http://api.bdrc.io/annotations/collectionService", null));

    }

    @GetMapping(value = "/search/{res}/")
    public ResponseEntity<StreamingResponseBody> search(@RequestHeader(value = "Accept", required = false) String accept, @PathVariable("res") String res, HttpServletRequest request) throws RestException {
        MediaType mediaType;
        if (accept == null || accept.contains("text/html")) {
            mediaType = BudaMediaTypes.MT_JSONLD;
        } else {
            mediaType = BudaMediaTypes.selectVariant(accept, BudaMediaTypes.graphVariants);
            if (mediaType == null)
                return AnnotationUtils.mediaTypeChoiceResponse(request);
        }
        DocType dct = null;
        if (mediaType.getSubtype().equals("ld+json")) {
            dct = DocType.ANN;
        }
        String ext = BudaMediaTypes.getExtFromMime(mediaType);
        Map<String, String> map = Helpers.convertMulti(request.getParameterMap());
        String range = map.get("range");
        Integer[] rg = CollectionUtils.getRangeFromUrlElt(range);
        final LdsQuery qfp = LdsQueryService.get(annotSearchQueryFile, "library");
        final Map<String, String> args = new HashMap<>();
        args.put("R_RES", res);
        args.put("I_SUBRANGEFIRST", rg[0].toString());
        args.put("I_SUBRANGELAST", rg[1].toString());
        final String query = qfp.getParametizedQuery(args);
        Model model = QueryProcessor.getGraph(query, ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL), null);
        BodyBuilder bb = ResponseEntity.ok();
        bb = AnnotationUtils.setRespHeaders(bb, request.getServletPath(), ext, "Choice", null, mediaType, false);
        return bb.body(Helpers.getModelStream(model, ext, "http://api.bdrc.io/annotations/collectionService", dct));
    }

}
