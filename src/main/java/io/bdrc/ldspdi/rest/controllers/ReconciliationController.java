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

@RestController
@RequestMapping("/")
public class ReconciliationController {

    /*
     * Implementation of the Reconciliation API
     * https://reconciliation-api.github.io/specs/0.1/
     */
    
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
        
        @JsonProperty(value="type", required=true)
        public List<String> type = null;
        
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
        public final String url = "https://library-dev.bdrc.io/preview/bdr:{{id}}";
    }
    
    public final static class View {
        @JsonProperty(value="url", required=true)
        public final String url = "https://library.bdrc.io/show/bdr:{{id}}";
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
    
    public static String getPersonQuery(final String name, final String lang, final String worktitle, final String worktitle_lang, final String workId) {
        String res = "prefix :      <http://purl.bdrc.io/ontology/core/>\n"
                + "prefix tmp:   <http://purl.bdrc.io/ontology/tmp/>\n"
                + "prefix bdo:   <http://purl.bdrc.io/ontology/core/>\n"
                + "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "prefix skos:  <http://www.w3.org/2004/02/skos/core#>\n"
                + "prefix bda:   <http://purl.bdrc.io/admindata/>\n"
                + "prefix text:  <http://jena.apache.org/text#>\n"
                + "prefix adm:   <http://purl.bdrc.io/ontology/admin/>\n"
                + "prefix owl:   <http://www.w3.org/2002/07/owl#> "
                + "construct {\n"
                + "    ?bdrcres tmp:luceneScore ?sc ;\n"
                + "       tmp:isMain true ;"
                + "       tmp:superMatch ?superMatch .\n"
                + "    ?bdrcres ?resp ?reso .\n"
                + "    ?bdrcres bdo:personEvent ?evt .\n"
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
                + "    ?res owl:sameAs* ?bdrcres ."
                + "    ?resAdm adm:adminAbout ?bdrcres .\n"
                + "    ?resAdm adm:metadataLegal  bda:LD_BDRC_CC0 ;\n"
                + "            adm:status    bda:StatusReleased .\n"
                + "    VALUES ?resp { skos:prefLabel tmp:entityScore tmp:associatedCentury tmp:hasRole }\n"
                + "    ?bdrcres ?resp ?reso .\n"
                + "  } union {\n"
                + "    # get events\n"
                + "    {\n"
                + "        (?name ?sc ?nameMatch) text:query ( rdfs:label \"\\\""+name+"\\\"\"@"+lang+" ) .\n"
                + "        ?res bdo:personName ?name .\n"
                + "    } union {\n"
                + "        (?res ?sc ?labelMatch) text:query ( bdo:skosLabels \"\\\""+name+"\\\"\"@"+lang+" ) .\n"
                + "        ?res a :Person .\n"
                + "    }\n"
                + "\n"
                + "    ?res owl:sameAs* ?bdrcres ."
                + "    ?resAdm adm:adminAbout ?bdrcres .\n"
                + "    ?resAdm adm:metadataLegal  bda:LD_BDRC_CC0 ;\n"
                + "            adm:status    bda:StatusReleased .\n"
                + "    ?bdrcres bdo:personEvent ?evt .\n"
                + "    ?evt a ?evtType .\n"
                + "    FILTER (?evtType IN(bdo:PersonBirth , bdo:PersonDeath , bdo:PersonFlourished))\n"
                + "    ?evt :eventWhen ?evtw .\n";
                if (worktitle != null) {
                    res += "  } union {"
                        + "    # get matches with work\n"
                        + "    {\n"
                        + "        (?name ?sc ?nameMatch) text:query ( rdfs:label \"\\\""+name+"\\\"\"@"+lang+" ) .\n"
                        + "        ?res bdo:personName ?name .\n"
                        + "    } union {\n"
                        + "        (?res ?sc ?labelMatch) text:query ( bdo:skosLabels \"\\\""+name+"\\\"\"@"+lang+" ) .\n"
                        + "        ?res a :Person .\n"
                        + "    }\n"
                        + "    ?res owl:sameAs* ?bdrcres ."
                        + "    ?resAdm adm:adminAbout ?bdrcres .\n"
                        + "    ?resAdm adm:metadataLegal  bda:LD_BDRC_CC0 ;\n"
                        + "            adm:status    bda:StatusReleased ."
                        + "    ?wa text:query ( bdo:skosLabels \"\\\""+worktitle+"\\\"\"@"+worktitle_lang+" ) .\n"
                        + "    ?wa a :Work ;\n"
                        + "        :creator ?aac .\n"
                        + "    ?aac :agent ?bdrcres .\n"
                        + "    ?resAdm adm:adminAbout ?wa .\n"
                        + "    ?resAdm adm:status    bda:StatusReleased .\n"
                        + "    BIND(true as ?superMatch)";
                } else if (workId != null) {
                    res += "  } union {"
                        + "    # get matches with work\n"
                        + "    {\n"
                        + "        (?name ?sc ?nameMatch) text:query ( rdfs:label \"\\\""+name+"\\\"\"@"+lang+" ) .\n"
                        + "        ?res bdo:personName ?name .\n"
                        + "    } union {\n"
                        + "        (?res ?sc ?labelMatch) text:query ( bdo:skosLabels \"\\\""+name+"\\\"\"@"+lang+" ) .\n"
                        + "        ?res a :Person .\n"
                        + "    }\n"
                        + "    ?res owl:sameAs* ?bdrcres ."
                        + "    ?resAdm adm:adminAbout ?bdrcres .\n"
                        + "    ?resAdm adm:metadataLegal  bda:LD_BDRC_CC0 ;\n"
                        + "            adm:status    bda:StatusReleased ."
                        + "    bdr:"+workId+" a :Work ;\n"
                        + "          :creator ?aac .\n"
                        + "    ?aac :agent ?bdrcres .\n"
                        + "    BIND(true as ?superMatch)";
                }
                res += "  }\n"
                + "}";
                return res;
    }
    
    public static String getWorkQuery(final String name, final String lang) {
        return "prefix :      <http://purl.bdrc.io/ontology/core/>\n"
                + "prefix tmp:   <http://purl.bdrc.io/ontology/tmp/>\n"
                + "prefix bdo:   <http://purl.bdrc.io/ontology/core/>\n"
                + "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "prefix skos:  <http://www.w3.org/2004/02/skos/core#>\n"
                + "prefix bda:   <http://purl.bdrc.io/admindata/>\n"
                + "prefix text:  <http://jena.apache.org/text#>\n"
                + "prefix adm:   <http://purl.bdrc.io/ontology/admin/>\n"
                + "prefix owl:   <http://www.w3.org/2002/07/owl#> "
                + "construct {\n"
                + "    ?res tmp:luceneScore ?sc ;\n"
                + "       tmp:isMain true .\n"
                + "    ?res ?resp ?reso .\n"
                + "    ?res :creator ?aac .\n"
                + "    ?aac ?aacp ?aaco .\n"
                + "    ?p skos:prefLabel ?pl .\n"
                + "} where {\n"
                + "  {\n"
                + "    (?res ?sc ?labelMatch) text:query ( bdo:skosLabels \"\\\""+name+"\\\"\"@"+lang+" ) .\n"
                + "    ?res a :Work .\n"
                + "\n"
                + "    ?resAdm adm:adminAbout ?res .\n"
                + "    ?resAdm adm:metadataLegal  bda:LD_BDRC_CC0 ;\n"
                + "            adm:status    bda:StatusReleased .\n"
                + "    VALUES ?resp { skos:prefLabel tmp:entityScore }\n"
                + "    ?res ?resp ?reso .\n"
                + "  } union {\n"
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
                + "  }\n"
                + "}";
    }
    
    public static String getDateStrEdtf(final Model m, final Resource person) {
        // TODO: handle century, see https://www.loc.gov/marc/bibliographic/bdx00.html for format
        String birthStr = null;
        String deathStr = null;
        String floruitStr = null;
        StmtIterator si = person.listProperties(MarcExport.personEvent);
        while (si.hasNext()) {
            final Resource event = si.next().getResource();
            final Resource eventType = event.getPropertyResourceValue(RDF.type);
            if (eventType != null && eventType.getLocalName().equals("PersonBirth")) {
                if (event.hasProperty(MarcExport.eventWhen)) {
                    birthStr = event.getProperty(MarcExport.eventWhen).getLiteral().getLexicalForm();
                }
            }
            if (eventType != null && eventType.getLocalName().equals("PersonDeath")) {
                if (event.hasProperty(MarcExport.eventWhen)) {
                    deathStr = event.getProperty(MarcExport.eventWhen).getString();
                }
            }
            if (eventType != null && eventType.getLocalName().equals("PersonFlourished")) {
                if (event.hasProperty(MarcExport.eventWhen)) {
                    floruitStr = event.getProperty(MarcExport.eventWhen).getString();
                }
            }
        }
        if (birthStr != null || deathStr != null) {
            String dateStr = "";
            // There should be a coma at the end, except when the date ends with a hyphen.
            if (birthStr == null) {
                if (deathStr != null) {
                    dateStr += "d. "+deathStr;
                }
            } else {
                dateStr += birthStr+"-";
                if (deathStr != null) {
                    dateStr += deathStr;
                }
            }
            return dateStr;
        }
        if (floruitStr != null) {
            if (floruitStr.length() > 3)
                return floruitStr;
            return floruitStr+"XX";
        }
        final Statement centuryS = person.getProperty(associatedCentury);
        if (centuryS != null)
            return String.valueOf(centuryS.getInt()-1)+"XX";
        return null;
    }
    
    public static final Property isMain = ResourceFactory.createProperty(MarcExport.TMP + "isMain");
    public static final Property entityScore = ResourceFactory.createProperty(MarcExport.TMP + "entityScore");
    public static final Property luceneScore = ResourceFactory.createProperty(MarcExport.TMP + "luceneScore");
    public static final Property associatedCentury = ResourceFactory.createProperty(MarcExport.TMP + "associatedCentury");
    public static final Property superMatch = ResourceFactory.createProperty(MarcExport.TMP + "superMatch");
    
    public static final List<String> person_types = Arrays.asList("Person");
    public static final List<String> work_types = Arrays.asList("Work");
    
    public static List<Result> personModelToResult(final Model m, final String lang) {
        final List<Result> resList = new ArrayList<>();
        final ResIterator mainIt = m.listSubjectsWithProperty(isMain);
        while (mainIt.hasNext()) {
            final Result res = new Result();
            resList.add(res);
            res.type = person_types;
            final Resource main = mainIt.next();
            res.id = main.getLocalName();
            res.match = main.hasProperty(superMatch);
            Statement scoreS = main.getProperty(entityScore);
            if (scoreS != null)
                res.score = scoreS.getInt();
            else
                res.score = 1;
            String name = getPrefLabel(m, main, lang);
            final String dateStr = getDateStrEdtf(m, main);
            if (dateStr != null) {
                name += " ("+dateStr+")";
                res.name = name;
                continue;
            }
            res.name = name;
        }
        Collections.sort(resList, Collections.reverseOrder());
        return resList;
    }
    
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
        final ResIterator mainIt = m.listSubjectsWithProperty(isMain);
        while (mainIt.hasNext()) {
            final Result res = new Result();
            resList.add(res);
            res.type = work_types;
            final Resource main = mainIt.next();
            res.id = main.getLocalName();
            // not quite sure what a good value is...
            res.match = false;
            Statement scoreS = main.getProperty(entityScore);
            int baseScore = 1;
            if (scoreS != null) {
                baseScore = scoreS.getInt();
            }
            // work have a score of 1 in many cases, we differentiate them through their lucene score
            scoreS = main.getProperty(luceneScore);
            if (scoreS != null) {
                res.score = Math.round(baseScore*scoreS.getFloat());
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
        Collections.sort(resList, Collections.reverseOrder());
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
    
    public static void fillResultsForPersons(final Map<String,Results> res, final String qid, final Query q, final String lang) {
        final String normalized = normalize(q.query, "Person");
        String workTitle = null;
        String workTitle_lang = null;
        String workId = null;
        final Object value = getFirstPropertyValue(q, "authorOf");
        if (value instanceof String) {
            workTitle = (String) value;
            workTitle_lang = guessLang(workTitle);
        } else if (value instanceof Map) {
            workId = ((Map<String,String>)value).getOrDefault("id", null);
        }
        final String qstr = getPersonQuery(normalized, "bo-x-ewts", workTitle, workTitle_lang, workId);
        System.out.println(qstr);
        final Model model;
        RDFConnection rvf = RDFConnectionFuseki.create().destination(ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL)).build();
        model = rvf.queryConstruct(qstr);
        rvf.close();
        model.write(System.out, "TTL");
        final List<Result> resList = personModelToResult(model, lang);
        final Integer limit = q.limit;
        if (limit != null && limit < resList.size()) {
            res.put(qid, new Results(resList.subList(0, limit)));
        } else {
            res.put(qid, new Results(resList));
        }
    }
    
    public static void fillResultsForWorks(final Map<String,Results> res, final String qid, final Query q, final String lang) {
        final String normalized = normalize(q.query, "Work");
        final String qstr = getWorkQuery(normalized, "bo-x-ewts");
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
    
    public static Map<String,Results> runQueries(final Map<String,Query> queryBatch, final String lang) {
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
        System.out.println(jsonStr);
        final Map<String,Query> queryBatch = objectMapper.readValue(jsonStr, QueryBatchTR);
        final Map<String,Results> res = runQueries(queryBatch, lang);
        return ResponseEntity.status(200).header("Content-Type", "application/json")
                .body(res);
    }
    
}
