package io.bdrc.ldspdi.rest.controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.bdrc.ewtsconverter.EwtsConverter;
import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.libraries.Models;

@RestController
@RequestMapping("/")
public class ISBNAPIController {

    public static final EwtsConverter ewtsConverter = new EwtsConverter();
    public final static Logger log = LoggerFactory.getLogger(ISBNAPIController.class);
    final static ObjectMapper mapper = new ObjectMapper();
    public static final String TMP = "http://purl.bdrc.io/ontology/tmp/";
    
    public String normalizeID(String id) {
        final int slashidx = id.indexOf('/');
        if (slashidx != -1)
            id = id.substring(0, slashidx);
        return id.replace("-", "").replace(">", "").replace(" ", "");
    }
    
    public static final class Volinfo {
        public Resource ig = null;
        public Resource v = null;
        public Integer volnum = null;
        public Boolean matching = false;
        
        public Volinfo() {}
    }
    
    public static List<Volinfo> getVolumes(final Model m, final Resource mw, final List<Resource> matchingParts) {
        final Map<Integer,Volinfo> volInfoMap = new HashMap<>();
        final boolean requiresMatches = !(matchingParts == null || matchingParts.size() == 0 || (matchingParts.size() == 1 && matchingParts.get(0).equals(mw)));
        StmtIterator vsti = m.listStatements(mw, m.createProperty(TMP, "imageGroup"), (RDFNode) null);
        while (vsti.hasNext()) {
            final Resource ig = vsti.next().getResource();
            final Statement vns = ig.getProperty(m.createProperty(Models.BDO, "volumeNumber"));
            if (vns == null) {
                log.error("image group {} has no volume number in model", ig.getLocalName());
                continue;
            }
            final Volinfo vi = new Volinfo();
            vi.ig = ig;
            vi.volnum = vns.getInt();
            volInfoMap.put(vns.getInt(), vi);
        }
        vsti = m.listStatements(mw, m.createProperty(TMP, "volume"), (RDFNode) null);
        while (vsti.hasNext()) {            
            final Resource v = vsti.next().getResource();
            // start looking for the volume number in the content location
            Statement vns = v.getProperty(m.createProperty(Models.BDO, "volumeNumber"));
            if (vns == null) {
                // fallback on the part index, error prone
                log.error("volume {} has no content location volume in model", v.getLocalName());
                vns = v.getProperty(m.createProperty(Models.BDO, "partIndex"));
            }
            final Volinfo vi = volInfoMap.computeIfAbsent(vns.getInt(), x -> new Volinfo());
            vi.v = v;
            vi.matching = matchingParts.contains(v);
            vi.volnum = vns.getInt();
        }
        final List<Integer> sortedVnums = new ArrayList<>(volInfoMap.keySet());
        Collections.sort(sortedVnums);
        final List<Volinfo> sortedR = new ArrayList<>();
        for (final Integer vn : sortedVnums) {
            final Volinfo vi = volInfoMap.get(vn);
            if (requiresMatches && !vi.matching)
                continue;
            sortedR.add(vi);
        }
        return sortedR;
    }
    
    public static String proplitToStr(final Resource r, final Property p, final String languageh) {
        final StmtIterator si = r.listProperties(p);
        String res = null;
        while (si.hasNext()) {
            final Literal l = si.next().getObject().asLiteral();
            if (l.getLanguage().equals(languageh))
                return l.getString();
            if (l.getLanguage().equals("bo-x-ewts") && languageh.equals("bo")) {
                return ewtsConverter.toUnicode(l.getString());
            }
            res = l.getString();
        }
        return res;
    }
    
    public static ArrayNode proplitToArray(final Resource r, final Property p, final String languageh) {
        final StmtIterator si = r.listProperties(p);
        ArrayNode res = null;
        while (si.hasNext()) {
            if (res == null)
                res = mapper.createArrayNode();
            final Literal l = si.next().getObject().asLiteral();
            if (l.getLanguage().equals("bo-x-ewts") && languageh.equals("bo")) {
                res.add(ewtsConverter.toUnicode(l.getString()));
            } else {
                res.add(l.getString());
            }
        }
        return res;
    }
    
    public static void addIds(final Resource r, final ObjectNode node) {
        final ArrayNode an = node.arrayNode();
        node.set("ids", an);
        if (r == null)
            return;
        final StmtIterator idsi = r.listProperties(r.getModel().createProperty(TMP, "idvalue"));
        while (idsi.hasNext()) {
            final Statement s = idsi.next();
            an.add(s.getString());
        }
        
    }
    
    public static ArrayNode objectFromModel(final Model m, final String languageh) {
        final ArrayNode root = mapper.createArrayNode();
        final NodeIterator ni = m.listObjectsOfProperty(m.createProperty(Models.BDO, "inRootInstance"));
        while (ni.hasNext()) {
            final ObjectNode matchNode = root.addObject();
            final Resource rootR = ni.next().asResource();
            final List<Resource> matchingParts = m.listSubjectsWithProperty(m.createProperty(Models.BDO, "inRootInstance"), rootR).toList();
            matchNode.put("id", rootR.getURI());
            final List<Volinfo> volumes = getVolumes(m, rootR, matchingParts);
            final ArrayNode an = matchNode.arrayNode();
            for (final Volinfo vi : volumes) {
                final ObjectNode von = an.addObject();
                if (vi.v != null) {
                    von.put("id", vi.v.getURI());
                    von.put("title", proplitToStr(vi.v, SKOS.prefLabel, languageh));
                    von.set("title_matched", proplitToArray(rootR, m.createProperty(TMP, "labelMatch"), languageh));
                }
                addIds(vi.v, von);
                if (vi.ig != null) {
                    final ArrayNode imgan = an.arrayNode();
                    imgan.add(vi.ig.getURI());
                    von.set("image_groups", imgan);
                }
                if (vi.volnum != null) {
                    von.put("volume_number", vi.volnum);
                }
            }
            matchNode.put("nb_volumes", volumes.size());
            matchNode.set("volumes", an);
            addIds(rootR, matchNode);
            final Resource authorR = rootR.getPropertyResourceValue(m.createProperty(TMP, "mainAuthor"));
            if (authorR != null) {
                matchNode.put("author_id", authorR.getURI());
                matchNode.put("author_name", proplitToStr(authorR, SKOS.prefLabel, languageh));
            }
            matchNode.put("title", proplitToStr(rootR, SKOS.prefLabel, languageh));
            matchNode.set("title_matched", proplitToArray(rootR, m.createProperty(TMP, "labelMatch"), languageh));
            matchNode.put("publisherName", proplitToStr(rootR, m.createProperty(Models.BDO, "publisherName"), languageh));
            matchNode.put("editionStatement", proplitToStr(rootR, m.createProperty(Models.BDO, "editionStatement"), languageh));
            matchNode.put("extentStatement", proplitToStr(rootR, m.createProperty(Models.BDO, "extentStatement"), languageh));
            matchNode.put("publisherLocation", proplitToStr(rootR, m.createProperty(Models.BDO, "publisherLocation"), languageh));
            matchNode.put("publicationDate", proplitToStr(rootR, m.createProperty(TMP, "publicationDate"), languageh));
            final Resource iiifThumbnail = rootR.getPropertyResourceValue(m.createProperty(TMP, "thumbnailIIIFService"));
            if (iiifThumbnail != null)
                matchNode.put("thumbnail_iiif_service", iiifThumbnail.getURI());
            if (volumes.size() == 1)
                matchNode.put("match_type", "monovolume");
            else if (matchingParts.get(0).equals(rootR))
                matchNode.put("match_type", "full_multivolume");
            else
                matchNode.put("match_type", "subset");
        }
        return root;
    }
    
    @GetMapping(value = "ID/searchByID", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ArrayNode> ISBNController(@RequestParam(value="id") String id, @RequestHeader("Accept-Language") String languageh) throws JsonProcessingException, RestException {
        if (languageh == null)
            languageh = "bo";
        id = normalizeID(id);
        final Map<String, String> map = new HashMap<>();
        map.put("L_ID", id);
        final Model model = QueryProcessor.getSimpleGraph(map, id, "isbn.arq", null, null);
        if (model.size() < 1) {
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(id));
        }
        final ArrayNode rootNode = objectFromModel(model, languageh);
        return ResponseEntity.ok().header("Allow", "GET, OPTIONS, HEAD").contentType(MediaType.APPLICATION_JSON).body(rootNode);
    }
    
    @GetMapping(value = "TLMS/searchByID", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ArrayNode> TLMSControllerISBN(@RequestParam(value="id") String id, @RequestHeader("Accept-Language") final String languageh) throws JsonProcessingException, RestException {
        return ISBNController(id, languageh);
    }
    
    @GetMapping(value = "TLMS/searchByTitle", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ArrayNode> TLMSControllerTitle(@RequestParam(value="title") String title, @RequestHeader("Accept-Language") String languageh) throws JsonProcessingException, RestException {
        if (languageh == null)
            languageh = "bo";
        final Map<String, String> map = new HashMap<>();
        map.put("L_NAME", "\""+title.replace("\"", "")+"\"");
        map.put("LG_NAME", "bo");
        final Model model = QueryProcessor.getSimpleGraph(map, title, "tlms_bytitle.arq", null, null);
        if (model.size() < 1) {
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(title));
        }
        final ArrayNode rootNode = objectFromModel(model, languageh);
        return ResponseEntity.ok().header("Allow", "GET, OPTIONS, HEAD").contentType(MediaType.APPLICATION_JSON).body(rootNode);
    }

}
