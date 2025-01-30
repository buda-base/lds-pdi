package io.bdrc.ldspdi.rest.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.ewtsconverter.EwtsConverter;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.export.MarcExport;
import io.bdrc.ldspdi.results.ResultsCache;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.script.ScriptType;
import org.opensearch.script.Script;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.ScriptSortBuilder;
import org.opensearch.search.sort.SortOrder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/")
public class ReconciliationController {

    /*
     * Implementation of the Reconciliation API
     * https://reconciliation-api.github.io/specs/0.1/
     */
    
    @Autowired
    private RestHighLevelClient client;
    
    static final String index_name = ServiceConfig.getProperty("opensearchIndex");
    
    public static final ObjectMapper objectMapper = new ObjectMapper();
    public static final EwtsConverter converter = new EwtsConverter();
    
    public final static Logger log = LoggerFactory
            .getLogger(ReconciliationController.class);
    
    public final static class PropertyValue {
        @JsonProperty(value="pid", required=true)
        public String pid = null;
        
        @JsonProperty(value="v", required=true)
        public Object v = null;
    }
    
    public final static class Query {
        @JsonProperty(value="query", required=true)
        public String query = null;
        
        @JsonProperty(value="type", required=false)
        public String type = null;
        
        @JsonProperty(value="limit", required=false)
        public Integer limit = null;
        
        // "should", "all" or "any"
        @JsonProperty(value="type_strict", required=false)
        public String type_strict = null;
        
        @JsonProperty(value="properties", required=false)
        public List<PropertyValue> properties = null;
    }
    
    public final static class Results {
        @JsonProperty(value="result", required=true)
        public List<Result> result = null;
        
        public Results(final List<Result> result) {
            this.result = result;
        }
    }
    
    public final static class Result implements Comparable<Result> {
        @JsonProperty(value="id", required=true)
        public String id = null;
        
        @JsonProperty(value="name", required=true)
        public String name = null;
        
        @JsonProperty(value="description", required=false)
        public String description = null;
        
        @JsonProperty(value="type", required=true)
        public List<String> type = null;
        
        @JsonProperty(value="score", required=true)
        public Double score = null;
        
        @JsonProperty(value="match", required=true)
        public Boolean match = null;

        @Override
        public int compareTo(final Result o) {
            if (score == null)
                return 0;
            return score.compareTo(o.score);
        }
    }
    
    public final static class DefaultType {
        @JsonProperty(value="name", required=true)
        public String name = null;
        
        @JsonProperty(value="id", required=true)
        public String id = null;
        
        public DefaultType(final String name, final String id) {
            this.name = name;
            this.id = id;
        }
    }
    
    public final static class Preview {
        @JsonProperty(value="width", required=true)
        public final Integer width = 400;
        
        @JsonProperty(value="height", required=true)
        public final Integer height = 400;
        
        @JsonProperty(value="url", required=true)
        public final String url = "https://library.bdrc.io/preview/bdr:{{id}}";
    }
    
    public final static class View {
        @JsonProperty(value="url", required=true)
        public final String url = "https://library.bdrc.io/show/bdr:{{id}}";
    }
    
    public static final class PropertySuggestService {
        @JsonProperty(value="service_url")
        public final String service_url = "https://ldspdi.bdrc.io";
        
        @JsonProperty(value="service_path")
        public final String service_path = "/reconciliation/suggest/properties/";
    }
    
    public static final class Suggests {
        @JsonProperty(value="property")
        public final PropertySuggestService property = new PropertySuggestService();
    }
    
    public final static class Service {
        @JsonProperty(value="name", required=true)
        public String name = null;
        
        @JsonProperty(value="versions")
        public final List<String> versions = Arrays.asList(new String[]{"0.1", "0.2"});
        
        @JsonProperty(value="documentation")
        public final String documentation = "https://github.com/buda-base/lds-pdi/tree/master/reconciliation";
        
        @JsonProperty(value="logo")
        public final String logo = "https://iiif.bdrc.io/static::logo.png/full/max/0/default.png";
        
        @JsonProperty(value="serviceVersion")
        public final String serviceVersion = "0.1";
        
        @JsonProperty(value="batchSize")
        public final Integer batchSize = 20;
        
        @JsonProperty(value="identifierSpace")
        public final String identifierSpace = "http://purl.bdrc.io/";
        
        @JsonProperty(value="schemaSpace", required=true)
        public final String schemaSpace = "http://purl.bdrc.io/ontology/core/";
        
        @JsonProperty(value="defaultTypes", required=true)
        public List<DefaultType> defaultTypes = null;
        
        @JsonProperty(value="suggest", required=true)
        public Suggests suggest = new Suggests();
        
        @JsonProperty(value="view", required=true)
        public final View view = new View();
        
        @JsonProperty(value="preview", required=true)
        public final Preview preview = new Preview();

    }
    
    public static final class SuggestReponse {
        @JsonProperty(value="name", required=true)
        public String name;
        
        @JsonProperty(value="description", required=true)
        public String description;
        
        @JsonProperty(value="id", required=true)
        public String id;
        
        public SuggestReponse(final String name, final String description, final String id) {
            this.name = name;
            this.description = description;
            this.id = id;
        }
    }
    
    public static final Service service_en = new Service();
    static {
        service_en.name = "Buddhist Digital Resource Center";
        service_en.defaultTypes = new ArrayList<>();
        service_en.defaultTypes.add(new DefaultType("person", "Person"));
        service_en.defaultTypes.add(new DefaultType("work", "Work"));
    }
    
    public final static TypeReference<Map<String,Query>> QueryBatchTR = new TypeReference<Map<String,Query>>(){};
    public final static TypeReference<Map<String,List<Result>>> ResultBatchTR = new TypeReference<Map<String,List<Result>>>(){};
    
    final static List<String> prefixes = new ArrayList<>();
    final static List<String> suffixes = new ArrayList<>();
    final static Pattern prefixPattern;
    final static Pattern suffixPattern;
    static {
        // discussed in https://github.com/buda-base/library-issues/issues/466
        prefixes.add("mkhan [pm]o ");
        prefixes.add("rgya gar kyi ");
        prefixes.add("mkhan chen ");
        prefixes.add("a lag ");
        prefixes.add("a khu ");
        prefixes.add("rgan ");
        prefixes.add("rgan lags ");
        prefixes.add("zhabs drung "); // can appear in the middle of words
        prefixes.add("mkhas grub ");
        prefixes.add("mkhas dbang ");
        prefixes.add("mkhas pa ");
        prefixes.add("bla ma ");
        prefixes.add("sman pa "); // ?
        prefixes.add("em chi "); // ?
        prefixes.add("yongs 'dzin "); // ?
        prefixes.add("ma hA ");
        prefixes.add("sngags pa ");
        prefixes.add("sngags mo ");
        prefixes.add("sngags pa'i rgyal po ");
        prefixes.add("sems dpa' chen po ");
        prefixes.add("rnal 'byor [pm]a ");
        prefixes.add("rje ");
        prefixes.add("rje btsun ");
        prefixes.add("rje btsun [pm]a ");
        prefixes.add("kun mkhyen ");
        prefixes.add("lo tsA ba ");
        prefixes.add("lo tswa ba ");
        prefixes.add("lo cA ba ");
        prefixes.add("lo chen ");
        prefixes.add("slob dpon ");
        prefixes.add("paN\\+Di ta ");
        prefixes.add("paN chen ");
        prefixes.add("srI ");
        prefixes.add("dpal ");
        prefixes.add("dge slong ");
        prefixes.add("dge slong ma ");
        prefixes.add("dge bshes ");
        prefixes.add("dge ba'i bshes gnyen ");
        prefixes.add("shAkya'i dge slong ");
        prefixes.add("'phags pa ");
        prefixes.add("A rya ");
        prefixes.add("gu ru ");
        prefixes.add("sprul sku ");
        prefixes.add("a ni ");
        prefixes.add("a ni lags ");
        prefixes.add("rig 'dzin ");
        prefixes.add("chen [pm]o ");
        prefixes.add("A tsar\\+yA ");
        prefixes.add("gter ston ");
        prefixes.add("gter chen ");
        prefixes.add("thams cad mkhyen pa ");
        prefixes.add("rgyal dbang ");
        prefixes.add("rgyal ba ");
        prefixes.add("btsun [pm]a ");
        prefixes.add("dge rgan ");
        prefixes.add("theg pa chen po'i ");
        // prefixes found in Mongolian names
        prefixes.add("hor ");
        prefixes.add("sog [pm]o ");
        prefixes.add("sog ");
        prefixes.add("a lags sha ");
        prefixes.add("khal kha ");
        prefixes.add("cha har ");
        prefixes.add("jung gar ");
        prefixes.add("o rad ");
        prefixes.add("hor chin ");
        prefixes.add("thu med ");
        prefixes.add("hor pa ");
        prefixes.add("na'i man ");
        prefixes.add("ne nam ");
        prefixes.add("su nyid ");
        prefixes.add("har chen ");
        
        suffixes.add(" dpal bzang po");
        suffixes.add(" lags");
        suffixes.add(" rin po che");
        suffixes.add(" sprul sku");
        suffixes.add(" le'u");
        suffixes.add(" rgyud kyi rgyal po");
        suffixes.add(" bzhugs so");
        suffixes.add(" sku gzhogs");
        suffixes.add(" (c|[sz])es bya ba");
        
        String patStr = String.join("|", prefixes);
        prefixPattern = Pattern.compile("^(?:"+patStr+")+");
        patStr = String.join("|", suffixes);
        suffixPattern = Pattern.compile("(?:"+patStr+")+$");
    }
    
    public static boolean isAllTibetanUnicode(String input) {
        final int len = Math.min(10, input.length());
        if (len == 0) return false;
        int nbNonTibUni = 0;
        for (int i = 0; i < len; i++) {
            int c = input.charAt(i);
            if ((c < 0x0F00 || c > 0x0FFF) && c != ' ') {
                nbNonTibUni += 1;
            }
        }
        return nbNonTibUni < 3;
    }
    
    public static String normalize(final String orig, final String type) {
        String repl = orig;
        if (isAllTibetanUnicode(orig))
            repl = converter.toWylie(orig);
        repl = repl.replaceAll("[\\s#_/\\-\\*\\.@\\d\\(\\)]+$", "");
        repl = repl.replaceAll("^[\\s#_/@\\*]+", "");
        repl = prefixPattern.matcher(repl).replaceAll("");
        // we add a space at the beginning so that suffixes can match from the start
        repl = suffixPattern.matcher(" "+repl).replaceAll("");
        if (repl.length() == 0 && orig.length() > 0)
            return orig;
        // offset by 1 because of space we added earlier
        return repl.substring(1);
    }
    
    @GetMapping(value = "/reconciliation/{lang}/")
    public ResponseEntity<Service> getResourceGraph(@PathVariable("lang") String lang)
            throws IOException {
        return ResponseEntity.status(200).header("Content-Type", "application/json")
                .body(service_en);
    }
    
    public static String normalize_res(final Object os_value, final String langSuffix, final boolean toUni) {
        if (os_value == null)
            return "";
        if (os_value instanceof String) {
            if (toUni && "bo_x_ewts".equals(langSuffix))
                return converter.toUnicode((String) os_value);
            return (String) os_value;
        }
        if (os_value instanceof List<?>) {
            List<?> list = (List<?>) os_value;
            if (!list.isEmpty() && list.get(0) instanceof String) {
                if (toUni && "bo_x_ewts".equals(langSuffix)) {
                    List<String> converted_list = new ArrayList<>();
                    for (final Object v : list) {
                        converted_list.add(converter.toUnicode((String) v));
                    }
                    return String.join(", ", converted_list);
                }
                return String.join(", ", (List<String>) list);
            }
        }
        return "";
    }
    
    final static List<String> langSuffixes = Arrays.asList("bo_x_ewts", "en", "hani");
    public static void addSourceToResult(final Map<String,Object> sourcemap, final Result res, final String type, final boolean toUni) {
        // first, prefLabel
        final List<String> otherLabels = new ArrayList<>();
        res.description = "";
        boolean mainLabelFound = false;
        for (final String langSuffix : langSuffixes) {
            final String field = "prefLabel_"+langSuffix;
            if (sourcemap.containsKey(field)) {
                String value = normalize_res(sourcemap.get(field), langSuffix, toUni);
                if (!mainLabelFound) {
                    res.name = value;
                    mainLabelFound = true;
                } else {
                    otherLabels.add(value);
                }
            }
        }
        for (final String langSuffix : langSuffixes) {
            final String field = "altLabel_"+langSuffix;
            if (sourcemap.containsKey(field))
                otherLabels.add(normalize_res(sourcemap.get(field), langSuffix, toUni));
        }
        if ("Person".equals(type)) {
            // look at birthDate, deathDate, flourishedDate
            boolean dateAdded = false;
            if (sourcemap.containsKey("birthDate")) {
                res.description += "b. "+sourcemap.get("birthDate");
                dateAdded = true;
            }
            if (sourcemap.containsKey("deathDate")) {
                if (dateAdded)
                    res.description += ", ";
                res.description += "d. "+sourcemap.get("deathDate");
                dateAdded = true;
            }
            if (!dateAdded && sourcemap.containsKey("flourishedDate")) {
                res.description += "fl. "+sourcemap.get("flourishedDate");
                dateAdded = true;
            }
            if (dateAdded)
                res.description += "\n";
        }
        if ("Version".equals(type)) {
            // look at volumeNumber?
            // seriesName_*, issueName,
            boolean lineAdded = false;
            if (sourcemap.containsKey("issueName")) {
                lineAdded = true;
                res.description += "Issue: "+sourcemap.get("issueName")+" ";
            }
            for (final String langSuffix : langSuffixes) {
                final String field = "seriesName_"+langSuffix;
                if (sourcemap.containsKey(field)) {
                    lineAdded = true;
                    res.description += "In: "+normalize_res(sourcemap.get(field), langSuffix, toUni);
                }
            }
            if (lineAdded)
                res.description += "\n";
            // publisherName_*, creation_date, publisherLocation_*
            lineAdded = false;
            for (final String langSuffix : langSuffixes) {
                final String field = "publisherName_"+langSuffix;
                if (sourcemap.containsKey(field)) {
                    lineAdded = true;
                    res.description += normalize_res(sourcemap.get(field), langSuffix, toUni)+", ";
                }
            }
            if (sourcemap.containsKey("creation_date")) {
                lineAdded = true;
                res.description += sourcemap.get("creation_date")+", ";
            }
            for (final String langSuffix : langSuffixes) {
                final String field = "publisherName_"+langSuffix;
                if (sourcemap.containsKey(field)) {
                    lineAdded = true;
                    res.description += normalize_res(sourcemap.get(field), langSuffix, toUni)+", ";
                }
            }
            if (lineAdded)
                res.description += "\n";
            // authorName_*, authorshipStatement_*, 
            lineAdded = false;
            for (final String langSuffix : langSuffixes) {
                final String field = "authorName_"+langSuffix;
                if (sourcemap.containsKey(field)) {
                    lineAdded = true;
                    res.description += normalize_res(sourcemap.get(field), langSuffix, toUni)+", ";
                }
            }
            for (final String langSuffix : langSuffixes) {
                final String field = "authorshipStatement_"+langSuffix;
                if (sourcemap.containsKey(field)) {
                    lineAdded = true;
                    res.description += normalize_res(sourcemap.get(field), langSuffix, toUni)+", ";
                }
            }
            if (lineAdded)
                res.description += "\n";
            // extent
            if (sourcemap.containsKey("extent")) {
                res.description += sourcemap.get("extent")+"\n";
            }
        }
        res.description += "Other names: "+String.join(", ", otherLabels)+"\n";
        // finally, add comment_* ?
        
    }
    
    public List<Result> getOsPersonResultsBo(String searchTerm) throws IOException {
        SearchRequest searchRequest = new SearchRequest(index_name);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        
        boolean isUni = isAllTibetanUnicode(searchTerm);
        
        searchTerm = normalize(searchTerm, "Person");
        
        // Build the main query
        var boolQuery = QueryBuilders.boolQuery()
            .must(QueryBuilders.boolQuery()
                .should(QueryBuilders.matchPhraseQuery("prefLabel_bo_x_ewts", searchTerm).boost(10.0f))
                .should(QueryBuilders.matchPhraseQuery("prefLabel_bo_x_ewts.ewts-phonetic", searchTerm).boost(5.0f))
                .should(QueryBuilders.matchPhraseQuery("altLabel_bo_x_ewts", searchTerm).boost(2.0f))
                .should(QueryBuilders.matchPhraseQuery("altLabel_bo_x_ewts.ewts-phonetic", searchTerm).boost(1.0f)))
            .filter(QueryBuilders.termQuery("type", "Person"));

        // Add script score
        Script script = new Script(ScriptType.STORED, null, "bdrc-score", Collections.emptyMap());
        var functionScoreQuery = QueryBuilders.scriptScoreQuery(boolQuery, script);
        
        searchSourceBuilder.query(functionScoreQuery);
        
        // Configure source inclusion
        searchSourceBuilder.fetchSource(true);
        
        // Set size (adjust as needed)
        searchSourceBuilder.size(20);
        
        searchRequest.source(searchSourceBuilder);
        
        // Execute search
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        
        final List<Result> res = new ArrayList<>();
        
        for (var hit : searchResponse.getHits().getHits()) {
            final Result r = new Result();
            r.score = Double.valueOf(hit.getScore());
            r.id = hit.getId();
            r.type = person_types;
            addSourceToResult(hit.getSourceAsMap(), r, "Person", isUni);
            res.add(r);
        }
        return res;
    }
    

    public static String getWorkQuery(final String name, final String lang, final String personName, final String personName_lang, final String personId) {
        String res = "prefix :      <http://purl.bdrc.io/ontology/core/>\n"
                + "prefix tmp:   <http://purl.bdrc.io/ontology/tmp/>\n"
                + "prefix bdo:   <http://purl.bdrc.io/ontology/core/>\n"
                + "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "prefix skos:  <http://www.w3.org/2004/02/skos/core#>\n"
                + "prefix bda:   <http://purl.bdrc.io/admindata/>\n"
                + "prefix text:  <http://jena.apache.org/text#>\n"
                + "prefix adm:   <http://purl.bdrc.io/ontology/admin/>\n"
                + "prefix owl:   <http://www.w3.org/2002/07/owl#>\n"
                + "prefix bdr:   <http://purl.bdrc.io/resource/>\n"
                + "construct {\n"
                + "    ?res tmp:luceneScore ?sc ;\n"
                + "       tmp:isMain true ;"
                + "       tmp:superMatch ?superMatch .\n"
                + "    ?res ?resp ?reso .\n"
                + "    ?res :creator ?aac .\n"
                + "    ?aac ?aacp ?aaco .\n"
                + "    ?p skos:prefLabel ?pl .\n"
                + "} where {\n"
                + "  {\n"
                + "    (?res ?sc) text:query ( bdo:skosLabels \"\\\""+name+"\\\"\"@"+lang+" ) .\n"
                + "    ?res a :Work .\n"
                + "\n"
                + "    ?resAdm adm:adminAbout ?res .\n"
                + "    ?resAdm adm:metadataLegal  bda:LD_BDRC_CC0 ;\n"
                + "            adm:status    bda:StatusReleased .\n"
                + "    VALUES ?resp { skos:prefLabel tmp:entityScore }\n"
                + "    ?res ?resp ?reso .\n"
                + "  } union {\n"
                + "    (?res) text:query ( bdo:skosLabels \"\\\""+name+"\\\"\"@"+lang+" ) .\n"
                + "    ?res a :Work .\n"
                + "\n"
                + "    ?resAdm adm:adminAbout ?res .\n"
                + "    ?resAdm adm:metadataLegal  bda:LD_BDRC_CC0 ;\n"
                + "            adm:status    bda:StatusReleased .\n"
                + "    ?res :creator ?aac .\n"
                + "    ?aac ?aacp ?aaco .\n"
                + "    ?aac :agent ?p .\n"
                + "    ?p skos:prefLabel ?pl .\n";
                if (personName != null) {
                    res += "  } union {\n"
                    + "    (?res) text:query ( bdo:skosLabels \"\\\""+name+"\\\"\"@"+lang+" ) .\n"
                    + "    ?res a :Work .\n"
                    + "\n"
                    + "    ?resAdm adm:adminAbout ?res .\n"
                    + "    ?resAdm adm:metadataLegal  bda:LD_BDRC_CC0 ;\n"
                    + "            adm:status    bda:StatusReleased .\n"
                    + "    {\n"
                    + "        (?name) text:query ( rdfs:label \"\\\""+personName+"\\\"\"@"+personName_lang+" ) .\n"
                    + "        ?p bdo:personName ?name .\n"
                    + "    } union {\n"
                    + "        (?p) text:query ( bdo:skosLabels \"\\\""+personName+"\\\"\"@"+personName_lang+" ) .\n"
                    + "        ?p a :Person .\n"
                    + "    }\n"
                    + "    ?res :creator ?aac .\n"
                    + "    ?aac ?aacp ?aaco .\n"
                    + "    ?aac :agent ?p .\n"
                    + "    ?p skos:prefLabel ?pl .\n"
                    + "    BIND(true as ?superMatch)\n";
                } else if (personId != null) {
                    res += "  } union {\n"
                    + "    BIND(bdr:"+personId+" as ?p)\n"
                    + "    (?res ?sc ?labelMatch) text:query ( bdo:skosLabels \"\\\""+name+"\\\"\"@"+lang+" ) .\n"
                    + "    ?res a :Work .\n"
                    + "\n"
                    + "    ?resAdm adm:adminAbout ?res .\n"
                    + "    ?resAdm adm:metadataLegal  bda:LD_BDRC_CC0 ;\n"
                    + "            adm:status    bda:StatusReleased .\n"
                    + "    ?res :creator ?aac .\n"
                    + "    ?aac ?aacp ?aaco .\n"
                    + "    ?aac :agent ?p .\n"
                    + "    ?p skos:prefLabel ?pl .\n"
                    + "    BIND(true as ?superMatch)\n";
                }
        res += "  }\n"
        + "}";
        return res;
    }
   
    
    public static final Property isMain = ResourceFactory.createProperty(MarcExport.TMP + "isMain");
    public static final Property entityScore = ResourceFactory.createProperty(MarcExport.TMP + "entityScore");
    public static final Property luceneScore = ResourceFactory.createProperty(MarcExport.TMP + "luceneScore");
    public static final Property associatedCentury = ResourceFactory.createProperty(MarcExport.TMP + "associatedCentury");
    public static final Property superMatch = ResourceFactory.createProperty(MarcExport.TMP + "superMatch");
    public static final Property idMatch = ResourceFactory.createProperty(MarcExport.TMP + "idMatch");
    
    public static final List<String> person_types = Arrays.asList("Person");
    public static final List<String> work_types = Arrays.asList("Work");
    
    
    public static final String getPrefLabel(final Model m, final Resource main, final String lang) {
        Statement nameS;
        if (lang.equals("en")) {
            nameS = main.getProperty(SKOS.prefLabel, "bo-x-ewts");
            if (nameS == null)
                nameS = main.getProperty(SKOS.prefLabel, lang);
        } else {
            nameS = main.getProperty(SKOS.prefLabel, lang);
        }
        if (nameS == null)
            nameS = main.getProperty(SKOS.prefLabel);
        if (nameS == null)
            return null;
        return nameS.getString();
    }
    
    public static List<Result> workModelToResult(final Model m, final String lang) {
        final List<Result> resList = new ArrayList<>();
        final List<Result> superMatchList = new ArrayList<>();
        final List<Result> otherMatchList = new ArrayList<>();
        final ResIterator mainIt = m.listSubjectsWithProperty(isMain);
        while (mainIt.hasNext()) {
            final Result res = new Result();
            res.type = work_types;
            final Resource main = mainIt.next();
            res.id = main.getLocalName();
            if (main.hasProperty(superMatch)) {
                res.match = true;
                superMatchList.add(res);
            } else {
                res.match = false;
                otherMatchList.add(res);
            }
            Statement scoreS = main.getProperty(entityScore);
            Double baseScore = 1.0;
            if (scoreS != null) {
                baseScore = Double.valueOf(scoreS.getInt());
            }
            // work have a score of 1 in many cases, we differentiate them through their lucene score
            scoreS = main.getProperty(luceneScore);
            if (scoreS != null) {
                res.score = baseScore*scoreS.getFloat();
            } else {
                res.score = baseScore;
            }
            String name = getPrefLabel(m, main, lang);
            final String creatorsStr = getCreatorsStr(m, main, lang);
            if (creatorsStr != null) {
                res.name = name+creatorsStr;
                continue;
            }
            res.name = name;
        }
        Collections.sort(superMatchList, Collections.reverseOrder());
        Collections.sort(otherMatchList, Collections.reverseOrder());
        resList.addAll(superMatchList);
        resList.addAll(otherMatchList);
        return resList;
    }
    
    static final Map<String,String> roleToStr = new HashMap<>();
    static {
        roleToStr.put("R0ER0011", "A"); // attributed author
        roleToStr.put("R0ER0014", "C"); // commentator
        roleToStr.put("R0ER0016", "A"); // contributing author
        roleToStr.put("R0ER0017", "T"); // head translator
        roleToStr.put("R0ER0018", "T"); // pandita
        roleToStr.put("R0ER0019", "A"); // main author
        roleToStr.put("R0ER0020", "T"); // oral translator
        roleToStr.put("R0ER0015", "E"); // compiler (editor)
        roleToStr.put("R0ER0022", "T"); // reciter (in Chinese translations)
        roleToStr.put("R0ER0025", "TT"); // Terton
        roleToStr.put("R0ER0026", "T"); // translator
    }
    
    private static String getCreatorsStr(final Model m, final Resource main, final String lang) {
        final StmtIterator mainIt = main.listProperties(MarcExport.creator);
        String res = null;
        int nbFound = 0;
        while (mainIt.hasNext()) {
            final Resource aac = mainIt.next().getResource(); 
            final Resource role = aac.getPropertyResourceValue(MarcExport.role);
            if (role == null)
                continue;
            final String roleLname = role.getLocalName();
            final String roleShort = roleToStr.getOrDefault(roleLname, null);
            if (roleShort == null)
                continue;
            final Resource agent = aac.getPropertyResourceValue(MarcExport.agent);
            if (agent == null)
                continue;
            final String agentLabel = getPrefLabel(m, agent, lang);
            if (agentLabel == null)
                continue;
            if (nbFound == 0) {
                res = "  ["+agentLabel+" ("+roleShort+")";
            } else {
                res += ", "+agentLabel+" ("+roleShort+")";
            }
            nbFound += 1;
        }
        if (nbFound > 0) {
            return res+"]";
        }
        return null;
    }
    
    public static String guessLang(final String str) {
        if (str == null)
            return null;
        if (isAllTibetanUnicode(str))
            return "bo";
        return "bo-x-ewts";
        
    }

    public static Object getFirstPropertyValue(final Query q, final String pid) {
        if (q.properties == null)
            return null;
        for (PropertyValue pv : q.properties) {
            if (pv.pid.equals(pid)) {
                return pv.v;
            }
        }
        return null;
    }
    
    public void fillResultsForPersons(final Map<String,Results> res, final String qid, final Query q, final String lang) throws IOException {
        final List<Result> resList =  getOsPersonResultsBo(q.query);
        final Integer limit = q.limit;
        if (limit != null && limit < resList.size()) {
            res.put(qid, new Results(resList.subList(0, limit)));
        } else {
            res.put(qid, new Results(resList));
        }
    }
    
    public static void fillResultsForWorks(final Map<String,Results> res, final String qid, final Query q, final String lang) {
        final String normalized = normalize(q.query, "Work");
        String personName = null;
        String personName_lang = null;
        String personId = null;
        final Object value = getFirstPropertyValue(q, "hasAuthor");
        if (value instanceof String) {
            personName = (String) value;
            personName_lang = guessLang(personName);
        } else if (value instanceof Map) {
            personId = ((Map<String,String>)value).getOrDefault("id", null);
        }
        final String qstr = getWorkQuery(normalized, "bo-x-ewts", personName, personName_lang, personId);
        //System.out.println(qstr);
        final Model model;
        RDFConnection rvf = RDFConnectionFuseki.create().destination(ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL)).build();
        model = rvf.queryConstruct(qstr);
        rvf.close();
        //model.write(System.out, "TTL");
        final List<Result> resList = workModelToResult(model, lang);
        final Integer limit = q.limit;
        if (limit != null && limit < resList.size()) {
            res.put(qid, new Results(resList.subList(0, limit)));
        } else {
            res.put(qid, new Results(resList));
        }
    }
    
    public Map<String,Results> runQueries(final Map<String,Query> queryBatch, final String lang) throws IOException {
        final Map<String,Results> res = new HashMap<>();
        for (final Entry<String,Query> e : queryBatch.entrySet()) {
            final Query q = e.getValue();
            if (q.type == null)
                q.type = "Person";
            switch (q.type) {
            case "Person":
                fillResultsForPersons(res, e.getKey(), q, lang);
                break;
            case "Work":
                fillResultsForWorks(res, e.getKey(), q, lang);
                break;
            }
        }
        return res;
    }

    @PostMapping(path = "/reconciliation/{lang}/",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<Map<String,Results>> query(@RequestParam Map<String,String> paramMap, @PathVariable("lang") String lang) throws IOException {
        final String jsonStr = paramMap.get("queries");
        //System.out.println(jsonStr);
        final Map<String,Query> queryBatch = objectMapper.readValue(jsonStr, QueryBatchTR);
        final Map<String,Results> res = runQueries(queryBatch, lang);
        return ResponseEntity.status(200).header("Content-Type", "application/json")
                .body(res);
    }
    
    final static Map<String,SuggestReponse> propertiesSuggest = new HashMap<>();
    static {
        propertiesSuggest.put("hasauthor", new SuggestReponse("has author", "use on work or version data to connect it with authors data", "hasAuthor"));
        propertiesSuggest.put("authorof", new SuggestReponse("author of", "use on person data to connect it with title data", "authorOf"));
    }
    
    @GetMapping(path = "/reconciliation/suggest/properties/")
    public ResponseEntity<Map<String,List<SuggestReponse>>> query(@RequestParam(value="prefix") String prefix, @RequestParam(value="cursor", required=false) Integer cursor) throws IOException {
        final List<SuggestReponse> suggests = new ArrayList<>();
        final Map<String,List<SuggestReponse>> res = new HashMap<>();
        res.put("result", suggests);
        prefix = prefix.toLowerCase().strip();
        for (final Map.Entry<String, SuggestReponse> e : propertiesSuggest.entrySet()) {
            if (e.getKey().contains(prefix)) {
                suggests.add(e.getValue());
            }
        }
        return ResponseEntity.status(200).header("Content-Type", "application/json")
                .body(res);
    }
    
}
