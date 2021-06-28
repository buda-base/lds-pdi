package io.bdrc.ldspdi.rest.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.export.CSLJsonExport;
import io.bdrc.ldspdi.export.CSLJsonExport.CSLResObj;

@RestController
@RequestMapping("/")
public class CitationFormatsController {
    
    @GetMapping(value = "CSLObj/{qname}")
    public ResponseEntity<CSLResObj> CSLExport(@RequestParam(value="qname") String qname) throws JsonProcessingException, RestException {
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXxxx");
        String uri = qname;
        if (uri.startsWith("bdr:")) {
            uri = "http://purl.bdrc.io/resource/"+qname.substring(4);
        }
        return CSLJsonExport.getResponse(uri);
    }
    
    @GetMapping(value = "UNAPI", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> unAPI(@RequestParam(value="id", required=false) String id, @RequestParam(value="format", required=false) String format) {
        if (id == null || id.isEmpty())
            return ResponseEntity.ok().body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><formats></formats>");
        String shortId = id;
        if (id.startsWith("http://purl.bdrc.io/resource/")) {
            shortId = "bdr:"+id.substring(29);
        }
        if (!shortId.startsWith("bdr:MW") && (!shortId.startsWith("bdr:W") || shortId.startsWith("bdr:WA"))) {
            return ResponseEntity.status(404).body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><formats id=\""+id+"\"></formats>");
        }
        if (format == null || format.isEmpty()) {
            // specs says HTTP status 300 but:
            // https://github.com/zotero/translators/issues/2459
            return ResponseEntity.status(200).body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><formats id=\""+id+"\"><format name=\"marcxml\" type=\"application/marcxml+xml\" /></formats>");
        }
        if (!format.equals("marcxml"))
            return ResponseEntity.status(406).body("format not available");
        String redirect;
        if (id.startsWith("bdr:")) {
            redirect = "https://purl.bdrc.io/resource/"+id.substring(4)+".mrcx"; 
        } else {
            redirect = "https"+id.substring(4)+".mrcx";
        }
        return ResponseEntity.status(302).header("Location", redirect).build();
    }

    // this is temporary and should be removed once
    // https://github.com/zotero/translators/issues/2459 is solved
    @GetMapping(value = "UNAPI300", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> unAPI300(@RequestParam(value="id", required=false) String id, @RequestParam(value="format", required=false) String format) {
        if (id == null || id.isEmpty())
            return ResponseEntity.ok().body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><formats></formats>");
        String shortId = id;
        if (id.startsWith("http://purl.bdrc.io/resource/")) {
            shortId = "bdr:"+id.substring(29);
        }
        if (!shortId.startsWith("bdr:MW") && (!shortId.startsWith("bdr:W") || shortId.startsWith("bdr:WA"))) {
            return ResponseEntity.status(404).body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><formats id=\""+id+"\"></formats>");
        }
        if (format == null || format.isEmpty()) {
            // specs says HTTP status 300 but:
            // https://github.com/zotero/translators/issues/2459
            return ResponseEntity.status(300).body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><formats id=\""+id+"\"><format name=\"marcxml\" type=\"application/marcxml+xml\" /></formats>");
        }
        if (!format.equals("marcxml"))
            return ResponseEntity.status(406).body("format not available");
        String redirect;
        if (id.startsWith("bdr:")) {
            redirect = "https://purl.bdrc.io/resource/"+id.substring(4)+".mrcx"; 
        } else {
            redirect = "https"+id.substring(4)+".mrcx";
        }
        return ResponseEntity.status(302).header("Location", redirect).build();
    }
    
}
