package io.bdrc.ldspdi.rest.controllers;

import java.io.IOException;
import java.util.ArrayList;
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
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.export.MarcExport;
import io.bdrc.ldspdi.results.ResultsCache;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;

@RestController
@RequestMapping("/")
public class ReconciliationController {

    /*
     * Implementation of the Reconciliation API
     * https://reconciliation-api.github.io/specs/0.1/
     */
    
    public static final ObjectMapper objectMapper = new ObjectMapper();
    
    public final static Logger log = LoggerFactory
            .getLogger(ReconciliationController.class);
    
    public final static class PropertyValue {
        @JsonProperty(value="pid", required=true)
        public String pid = null;
        
        @JsonProperty(value="v", required=true)
        public String v = null;
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
        public Integer type_strict = null;
        
        @JsonProperty(value="properties", required=false)
        public List<PropertyValue> properties = null;
    }
    
    public final static class Result implements Comparable<Result> {
        @JsonProperty(value="id", required=true)
        public String id = null;
        
        @JsonProperty(value="name", required=true)
        public String name = null;
        
        @JsonProperty(value="type", required=true)
        public String type = null;
        
        @JsonProperty(value="score", required=true)
        public Integer score = null;
        
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
        public final String url = "https://library.bdrc.io/preview/{{id}}";
    }
    
    public final static class View {
        @JsonProperty(value="url", required=true)
        public final String url = "https://library.bdrc.io/show/{{id}}";
    }
    
    public final static class Service {
        @JsonProperty(value="name", required=true)
        public String name = null;
        
        @JsonProperty(value="identifierSpace", required=false)
        public final String identifierSpace = "http://purl.bdrc.io/";
        
        @JsonProperty(value="schemaSpace", required=true)
        public final String schemaSpace = "http://purl.bdrc.io/ontology/core/";
        
        @JsonProperty(value="defaultTypes", required=true)
        public List<DefaultType> defaultTypes = null;
        
        @JsonProperty(value="view", required=true)
        public final View view = new View();
        
        @JsonProperty(value="preview", required=true)
        public final Preview preview = new Preview();

    }
    
    public static final Service service_en = new Service();
    static {
        service_en.name = "Buddhist Digital Resource Center";
        service_en.defaultTypes = new ArrayList<>();
        service_en.defaultTypes.add(new DefaultType("person", "Person"));
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
    
    public static String normalize(String orig, final String type) {
        // TODO: if Tibetan Unicode, convert to Wylie
        String repl = orig;
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
            throws RestException, IOException {
        return ResponseEntity.status(200).header("Content-Type", "application/json")
                .body(service_en);
    }
    
    public static Map<String,Map<String,List<String>>> analyzeQueryBatch(final Map<String,Query> queryBatch) {
        final Map<String,Map<String,List<String>>> typeToNormalizedToQueryIds = new HashMap<>();
        for (final Entry<String,Query> e : queryBatch.entrySet()) {
            final Query q = e.getValue();
            final String query_str = q.query;
            String type = q.type;
            if (type == null)
                type = "Person";
            final Map<String,List<String>> normalizedToQueryIds = typeToNormalizedToQueryIds.computeIfAbsent(type, k -> new HashMap<>());
            final List<String> ids = normalizedToQueryIds.computeIfAbsent(normalize(query_str, type), k -> new ArrayList<>());
            ids.add(e.getKey());
        }
        return typeToNormalizedToQueryIds;
    }
    
    public static String getPersonQuery(final String name, final String lang) {
        return "prefix :      <http://purl.bdrc.io/ontology/core/>\n"
                + "prefix tmp:   <http://purl.bdrc.io/ontology/tmp/>\n"
                + "prefix bdo:   <http://purl.bdrc.io/ontology/core/>\n"
                + "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "prefix skos:  <http://www.w3.org/2004/02/skos/core#>\n"
                + "prefix bda:   <http://purl.bdrc.io/admindata/>\n"
                + "prefix text:  <http://jena.apache.org/text#>\n"
                + "prefix adm:   <http://purl.bdrc.io/ontology/admin/>\n"
                + "construct {\n"
                + "    ?res tmp:luceneScore ?sc ;\n"
                + "       tmp:isMain true .\n"
                + "    ?res ?resp ?reso .\n"
                + "    ?evt :eventWhen ?evtw ;\n"
                + "         a ?evtType .\n"
                + "} where {\n"
                + "  {\n"
                + "    {\n"
                + "        (?name ?sc ?nameMatch) text:query ( rdfs:label \"\\\""+name+"\\\"\"@"+lang+" ) .\n"
                + "        ?res bdo:personName ?name .\n"
                + "    } union {\n"
                + "        (?res ?sc ?labelMatch) text:query ( bdo:skosLabels \"\\\""+name+"\\\"\"@"+lang+" ) .\n"
                + "        ?res a :Person .\n"
                + "    }\n"
                + "\n"
                + "    ?resAdm adm:adminAbout ?res .\n"
                + "    ?resAdm adm:metadataLegal  bda:LD_BDRC_CC0 ;\n"
                + "            adm:status    bda:StatusReleased .\n"
                + "    VALUES ?resp { skos:prefLabel tmp:entityScore tmp:associatedCentury tmp:hasRole }\n"
                + "    ?res ?resp ?reso .\n"
                + "  } union {\n"
                + "\n"
                + "    # get events\n"
                + "    {\n"
                + "        (?name ?sc ?nameMatch) text:query ( rdfs:label \"\\\""+name+"\\\"\"@"+lang+" \"highlight:\" ) .\n"
                + "        ?res bdo:personName ?name .\n"
                + "    } union {\n"
                + "        (?res ?sc ?labelMatch) text:query ( bdo:skosLabels \"\\\""+name+"\\\"\"@"+lang+" \"highlight:\" ) .\n"
                + "        ?res a :Person .\n"
                + "    }\n"
                + "\n"
                + "    ?resAdm adm:adminAbout ?res .\n"
                + "    ?resAdm adm:metadataLegal  bda:LD_BDRC_CC0 ;\n"
                + "            adm:status    bda:StatusReleased .\n"
                + "    ?res bdo:personEvent ?evt .\n"
                + "    ?evt a ?evtType .\n"
                + "    FILTER (?evtType IN(bdo:PersonBirth , bdo:PersonDeath))\n"
                + "    ?evt :eventWhen ?evtw .\n"
                + "\n"
                + "  }\n"
                + "}";
    }
    
    public static final Property isMain = ResourceFactory.createProperty(MarcExport.TMP + "isMain");
    public static final Property entityScore = ResourceFactory.createProperty(MarcExport.TMP + "entityScore");
    public static final Property luceneScore = ResourceFactory.createProperty(MarcExport.TMP + "luceneScore");
    public static final Property associatedCentury = ResourceFactory.createProperty(MarcExport.TMP + "associatedCentury");
    public static List<Result> personModelToResult(final Model m, final String lang) {
        final List<Result> resList = new ArrayList<>();
        final ResIterator mainIt = m.listSubjectsWithProperty(isMain);
        while (mainIt.hasNext()) {
            final Result res = new Result();
            resList.add(res);
            res.type = "Person";
            final Resource main = mainIt.next();
            res.id = main.getLocalName();
            // not quite sure what a good value is...
            res.match = false;
            Statement scoreS = main.getProperty(entityScore);
            if (scoreS == null)
                scoreS = main.getProperty(luceneScore);
            if (scoreS != null)
                res.score = scoreS.getInt();
            //System.out.println(lang);
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
                continue;
            String name = nameS.getString();
            final String dateStr = MarcExport.getDateStr(m, main);
            if (dateStr != null) {
                name += " ("+dateStr+")";
                res.name = name;
                continue;
            }
            final Statement centuryS = main.getProperty(associatedCentury);
            if (centuryS != null)
                name += " (~"+String.valueOf(centuryS.getInt())+"c.)";
            res.name = name;
        }
        Collections.sort(resList, Collections.reverseOrder());
        return resList;
    }
    
    public static void fillResultsForPersons(Map<String,List<Result>> res, final Map<String,List<String>> queries, final Map<String,Query> queryBatch, final String lang) {
        for (final Entry<String,List<String>> e : queries.entrySet()) {
            final String qstr = getPersonQuery(e.getKey(), "bo-x-ewts");
            //System.out.println(qstr);
            final Model model;
            RDFConnection rvf = RDFConnectionFuseki.create().destination(ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL)).build();
            model = rvf.queryConstruct(qstr);
            rvf.close();
            model.write(System.out, "TTL");
            final List<Result> resList = personModelToResult(model, lang);
            for (final String qid : e.getValue()) {
                final Query q = queryBatch.get(qid);
                final Integer limit = q.limit;
                if (limit != null && limit > resList.size()) {
                    res.put(qid, resList.subList(0, limit));
                } else {
                    res.put(qid, resList);
                }
            }
        }
    }
    
    public static Map<String,List<Result>> runSPARQLs(Map<String,Map<String,List<String>>> analyzedQuery, final Map<String,Query> queryBatch, final String lang) {
        final Map<String,List<Result>> res = new HashMap<>();
        for (final Entry<String,Map<String,List<String>>> e : analyzedQuery.entrySet()) {
            final String type = e.getKey();
            switch(type) {
            case "Person":
                fillResultsForPersons(res, e.getValue(), queryBatch, lang);
            }
        }
        return res;
    }

    @PostMapping(path = "/reconciliation/{lang}/",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<Map<String,List<Result>>> query(@RequestParam Map<String,String> paramMap, @PathVariable("lang") String lang) throws IOException {
        final String jsonStr = paramMap.get("queries");
        final Map<String,Query> queryBatch = objectMapper.readValue(jsonStr, QueryBatchTR);
        final Map<String,Map<String,List<String>>> analyzedQuery = analyzeQueryBatch(queryBatch);
        objectMapper.writerWithDefaultPrettyPrinter().writeValues(System.out).write(analyzedQuery);
        final Map<String,List<Result>> res = runSPARQLs(analyzedQuery, queryBatch,lang);
        return ResponseEntity.status(200).header("Content-Type", "application/json")
                .body(res);
    }
    
}
