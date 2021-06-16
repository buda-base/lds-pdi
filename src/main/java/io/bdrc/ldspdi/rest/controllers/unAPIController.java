package io.bdrc.ldspdi.rest.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class unAPIController {
    
    @GetMapping(value = "UNAPI", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> commonEndPoint(@RequestParam(value="id", required=false) String id, @RequestParam(value="format", required=false) String format) {
        if (id == null || id.isEmpty())
            return ResponseEntity.ok().body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><formats></formats>");
        String shortId = id;
        if (id.startsWith("http://purl.bdrc.io/resource/")) {
            shortId = "bdr:"+id.substring(29);
        }
        if (!shortId.startsWith("bdr:M") || !shortId.startsWith("bdr:W") || shortId.startsWith("bdr:WA")) {
            return ResponseEntity.status(404).body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><formats id=\""+id+"\"></formats>");
        }
        if (format == null || format.isEmpty()) {
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
