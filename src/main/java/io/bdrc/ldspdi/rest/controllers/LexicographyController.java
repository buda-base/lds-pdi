package io.bdrc.ldspdi.rest.controllers;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jena.rdf.model.Literal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.lexicography.EntriesUtils;

@RestController
@RequestMapping("/lexicography/")
public class LexicographyController {
    
    public final static Logger log = LoggerFactory.getLogger(LexicographyController.class);
    
    public static final ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        final SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Literal.class, new EntriesUtils.SimpleLiteralSerializer());
        objectMapper.registerModule(simpleModule);
    }
    
    @GetMapping(value = "entriesForChunk")
    public ResponseEntity<String> getEntriesForChunk(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = "chunk") String chunk,
            @RequestParam(value = "lang") String lang,
            @RequestParam(value = "cursor_start") int cursor_start,
            @RequestParam(value = "cursor_end") int cursor_end
            ) throws RestException, IOException {
        if (!lang.equals("bo") && !lang.equals("bo-x-ewts"))
            throw new RestException(404, 5000, "invalid lang parameter");
        if (chunk.length() == 0)
            throw new RestException(404, 5000, "invalid chunk parameter");
        if (cursor_start < 0 || cursor_end < 0 || cursor_start > cursor_end || cursor_end > chunk.length())
            throw new RestException(404, 5000, "invalid cursor_start or cursor_end parameter");
        List<EntriesUtils.Entry> entries = EntriesUtils.getEntries(chunk, lang, cursor_start, cursor_end);
        return ResponseEntity.status(200).header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(entries));
    }
}
