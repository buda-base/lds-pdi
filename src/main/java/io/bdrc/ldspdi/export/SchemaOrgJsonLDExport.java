package io.bdrc.ldspdi.export;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.bdrc.ewtsconverter.EwtsConverter;
import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.libraries.BudaMediaTypes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Map;
import java.util.HashMap;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.OWL;

public class SchemaOrgJsonLDExport {
    public final static Logger log = LoggerFactory.getLogger(SchemaOrgJsonLDExport.class);
    
    public static final EwtsConverter ewtsConverter = new EwtsConverter();
    public static final Property inRootInstance = ResourceFactory.createProperty(MarcExport.BDO + "inRootInstance");
    public static final Property numberOfVolumes = ResourceFactory.createProperty(MarcExport.BDO + "numberOfVolumes");
    public static final Property instanceHasVolume = ResourceFactory.createProperty(MarcExport.BDO + "instanceHasVolume");
    public static final Property instanceReproductionOf = ResourceFactory.createProperty(MarcExport.BDO + "instanceReproductionOf");
    public static final Property partType = ResourceFactory.createProperty(MarcExport.BDO + "partType");
    public static final Property identifiedBy = ResourceFactory.createProperty(MarcExport.BF + "identifiedBy");
    public static final Property isbn = ResourceFactory.createProperty(MarcExport.BF + "Isbn");
    public static final Property issn = ResourceFactory.createProperty(MarcExport.BF + "Issn");
    public static final Property instanceOf = ResourceFactory.createProperty(MarcExport.BDO + "instanceOf");
    public static final Property copyrightStatus = ResourceFactory.createProperty(MarcExport.BDO + "copyrightStatus");
    public static final String TMP = "http://purl.bdrc.io/ontology/tmp/";
    public static final Property language = ResourceFactory.createProperty(MarcExport.BDO + "language");
    public static final Property script = ResourceFactory.createProperty(MarcExport.BDO + "script");
    public static final Property publisherName = ResourceFactory.createProperty(MarcExport.BDO + "publisherName");
    public static final Property publisherLocation = ResourceFactory.createProperty(MarcExport.BDO + "publisherLocation");
    public static final Property creator = ResourceFactory.createProperty(MarcExport.BDO + "creator");
    public static final Property agent   = ResourceFactory.createProperty(MarcExport.BDO + "agent");
    public static final Property thumbnailIIIFService   = ResourceFactory.createProperty(TMP + "thumbnailIIIFService");
    public static final Property eventWhen          = ResourceFactory.createProperty(MarcExport.BDO + "eventWhen");
    public static final Property onYear             = ResourceFactory.createProperty(MarcExport.BDO + "onYear");
    public static final Property eventWho           = ResourceFactory.createProperty(MarcExport.BDO + "eventWho");

    public static final Property placeLat           = ResourceFactory.createProperty(MarcExport.BDO + "placeLat");
    public static final Property placeLong          = ResourceFactory.createProperty(MarcExport.BDO + "placeLong");
    public static final Property placeLocatedIn     = ResourceFactory.createProperty(MarcExport.BDO + "placeLocatedIn");
    public static final Property placeContains      = ResourceFactory.createProperty(MarcExport.BDO + "placeContains");
    public static final Property placeIsNear        = ResourceFactory.createProperty(MarcExport.BDO + "placeIsNear");
    public static final Property placeEvent         = ResourceFactory.createProperty(MarcExport.BDO + "placeEvent");
    public static final Property instanceEvent         = ResourceFactory.createProperty(MarcExport.BDO + "instanceEvent");
    public static final Property personEvent         = ResourceFactory.createProperty(MarcExport.BDO + "personEvent");
    public static final Property PublishedEvent         = ResourceFactory.createProperty(MarcExport.BDO + "PublishedEvent");
    public static final Property PersonDeath         = ResourceFactory.createProperty(MarcExport.BDO + "PersonDeath");
    public static final Property PersonBirth         = ResourceFactory.createProperty(MarcExport.BDO + "PersonBirth");
    
    public static final Property placeType          = ResourceFactory.createProperty(MarcExport.BDO + "placeType");
    public static final Property associatedTradition= ResourceFactory.createProperty(MarcExport.BDO + "associatedTradition");

    // bdo classes we'll test against
    public static final Resource BDO_Person         = ResourceFactory.createResource(MarcExport.BDO + "Person");
    public static final Resource BDO_Place          = ResourceFactory.createResource(MarcExport.BDO + "Place");
    public static final Resource BDO_Instance          = ResourceFactory.createResource(MarcExport.BDO + "Instance");
    public static final Resource BDO_Work          = ResourceFactory.createResource(MarcExport.BDO + "Work");
    public static final Resource BDO_PlaceFounded   = ResourceFactory.createResource(MarcExport.BDO + "PlaceFounded");
    
    public static final String ewtsToBo(String ewts) {
        boolean addBrackets = false;
        if (ewts.startsWith("*")){
            ewts = ewts.substring(1);
            addBrackets = true;
        }
        String unicode = ewtsConverter.toUnicode(ewts);
        if (addBrackets)
            unicode = '['+unicode+']';
        return unicode;
    }
    
    public static Model getModelForSchemaOrg(final String resUri) throws RestException {
        Model model = QueryProcessor.getSimpleResourceGraph(resUri, "resForSchema.arq");
        if (model.size() < 1) {
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(resUri));
        }
        return model;
    }
    
    public static ResponseEntity<JsonNode> getResponse(final String resUri) throws RestException, JsonProcessingException {
        final Model m;
        final Resource main;

        m = getModelForSchemaOrg(resUri);
        main = m.getResource(resUri);
        
        JsonNode res = getObject(m, main);
        return ResponseEntity.ok().header("Allow", "GET, OPTIONS, HEAD").contentType(BudaMediaTypes.MT_JSONLD).body(res);
    }
    
    private static final ObjectMapper MAPPER = new ObjectMapper(); 
    
    public static JsonNode getObject(final Model m, final Resource main) {
        // Decide what kind of thing we’re exporting
        if (m.contains(main, RDF.type, BDO_Person)) {
            return buildPersonEntity(m, main);
        } else if (m.contains(main, RDF.type, BDO_Place)) {
            return buildPlaceEntity(m, main);
        } else if (m.contains(main, RDF.type, BDO_Instance) || m.contains(main, RDF.type, BDO_Work)) {
            // default: bibliographic/instance-like
            return buildBiblioEntity(m, main);
        }
        return null;
    }
    
    private static JsonNode buildPlaceEntity(final Model m, final Resource place) {
        final String resUri  = place.getURI();
        final String lname   = place.getLocalName();

        ObjectNode obj = MAPPER.createObjectNode();
        obj.put("@context", "https://schema.org");
        obj.put("@id", resUri);
        obj.put("@type", "Place");

        ArrayNode ids = MAPPER.createArrayNode();
        ids.add("bdr:" + lname);
        ids.add(resUri);
        obj.set("identifier", ids);

        applyLabels(m, place, obj);

        // sameAs
        ArrayNode sameAs = collectSameAs(m, place);
        if (sameAs.size() > 0) obj.set("sameAs", sameAs);

        // geo: latitude / longitude
        Double lat = getDouble(place, placeLat);
        Double lon = getDouble(place, placeLong);
        if (lat != null && lon != null) {
            ObjectNode geo = MAPPER.createObjectNode();
            geo.put("@type", "GeoCoordinates");
            geo.put("latitude", lat);
            geo.put("longitude", lon);
            obj.set("geo", geo);
        }

        // Founding date (from placeEvent of type PlaceFounded); founders from eventWho
        String foundingDate = null;
        ArrayNode founders = MAPPER.createArrayNode();
        StmtIterator evIt = m.listStatements(place, placeEvent, (RDFNode) null);
        try {
            while (evIt.hasNext()) {
                RDFNode evN = evIt.next().getObject();
                if (!evN.isResource()) continue;
                Resource ev = evN.asResource();

                // If event is a PlaceFounded, extract date and founder(s)
                if (m.contains(ev, RDF.type, BDO_PlaceFounded)) {
                    String d = extractEventDate(m, ev);
                    if (d != null && (foundingDate == null || d.compareTo(foundingDate) < 0)) {
                        foundingDate = d;
                    }
                    // founders
                    StmtIterator whoIt = m.listStatements(ev, eventWho, (RDFNode) null);
                    while (whoIt.hasNext()) {
                        RDFNode w = whoIt.next().getObject();
                        if (!w.isResource()) continue;
                        Resource pe = w.asResource();
                        // Build as lightweight Person (with labels) if possible
                        ObjectNode founder = buildPersonNode(m, pe);
                        founders.add(founder);
                    }
                    whoIt.close();
                }
            }
        } finally { evIt.close(); }
        if (foundingDate != null) obj.put("foundingDate", foundingDate);

        return obj;
    }
    
    private static Double getDouble(Resource r, Property p) {
        Statement s = r.getProperty(p);
        if (s != null && s.getObject().isLiteral()) {
            try { return s.getLiteral().getDouble(); } catch (Exception ignore) {}
        }
        return null;
    }
    
    private static JsonNode buildPersonEntity(final Model m, final Resource person) {
        final String resUri  = person.getURI();
        final String lname   = person.getLocalName();

        ObjectNode p = MAPPER.createObjectNode();
        p.put("@context", "https://schema.org");
        p.put("@id", resUri);
        p.put("@type", "Person");

        ArrayNode ids = MAPPER.createArrayNode();
        ids.add("bdr:" + lname);
        ids.add(resUri);
        p.set("identifier", ids);

        applyLabels(m, person, p);

        // sameAs (if present)
        ArrayNode sameAs = collectSameAs(m, person);
        if (sameAs.size() > 0) p.set("sameAs", sameAs);

        // Birth / death from event nodes
        String birth = extractEventDateFromLink(m, person, personEvent, PersonBirth);
        if (birth != null) p.put("birthDate", birth);

        String death = extractEventDateFromLink(m, person, personEvent, PersonDeath);
        if (death != null) p.put("deathDate", death);

        return p;
    }

    public static JsonNode buildBiblioEntity(final Model m, final Resource main) {
        final String resUri  = main.getURI();
        final String lname   = main.getLocalName();
        //final String pageUrl = "https://library.bdrc.io/show/bdr:" + lname;

        // Build the entity node first
        ObjectNode entity = MAPPER.createObjectNode();
        entity.put("@context", "https://schema.org");
        entity.put("@id", resUri);
        entity.put("@type", detectSchemaType(m, main));
        //entity.put("url", pageUrl);

        // identifiers (compact + full URI)
        ArrayNode identifiers = MAPPER.createArrayNode();
        identifiers.add("bdr:" + lname);
        identifiers.add(resUri);
        entity.set("identifier", identifiers);

        // name + alternateName from pref/alt labels with Tibetan EWTS handling
        applyLabels(m, main, entity);

        // languages (derived)
        entity.set("inLanguage", deriveLanguages(m, main));

        // record-level license (may be absent)
        String license = licenseFromCopyright(main);
        if (license != null) entity.put("license", license);

        // identifiers: ISBN/ISSN via bf:identifiedBy
        addIdentifiers(m, main, entity);

        // publisher
        addPublisher(m, main, entity);

        // authors (persons with same label rules)
        addAuthors(m, main, entity);

        // Build the WebPage node (CC0) and wire mainEntity relations
        //ObjectNode page = MAPPER.createObjectNode();
        //page.put("@context", "https://schema.org");
        //page.put("@type", "WebPage");
        //page.put("@id", pageUrl);
        //page.put("url", pageUrl);
        //page.put("license", "https://creativecommons.org/publicdomain/zero/1.0/");

        // mainEntity links
        //page.set("mainEntity", MAPPER.createObjectNode().put("@id", resUri));
        //entity.set("mainEntityOfPage", MAPPER.createObjectNode().put("@id", pageUrl));
        
        String pubDate = extractEventDateFromLink(m, main, instanceEvent, PublishedEvent);
        if (pubDate != null) entity.put("datePublished", pubDate);

        return entity;
    }
    
    private static ArrayNode collectSameAs(Model m, Resource r) {
        ArrayNode sameAs = MAPPER.createArrayNode();
        StmtIterator eq = m.listStatements(r, OWL.sameAs, (RDFNode) null);
        try {
            while (eq.hasNext()) {
                RDFNode o = eq.next().getObject();
                if (o.isURIResource()) sameAs.add(o.asResource().getURI());
            }
        } finally { eq.close(); }
        return sameAs;
    }

    private static String extractEventDateFromLink(Model m, Resource subject, Property linkProp, Property type) {
        final StmtIterator st = subject.listProperties(linkProp);
        while (st.hasNext()) {
        	final Resource evt = st.next().getResource();
        	if (m.contains(evt, RDF.type, type)) {
        		return extractEventDate(m, evt);
        	}
        }
        return null;
    }

    private static String extractEventDate(Model m, Resource event) {
        // Prefer bdo:eventWhen (EDTF), else bdo:onYear
        Statement w = event.getProperty(eventWhen);
        if (w != null && w.getObject().isLiteral()) {
            return w.getString(); // keep EDTF lexical form
        }
        return null;
    }

    private static final Map<String,String> copyrightToLicense = new HashMap<>();
    static {
    	copyrightToLicense.put("CopyrightInCopyright", "https://rightsstatements.org/vocab/InC/1.0/");
    	copyrightToLicense.put("CopyrightClaimed", "https://rightsstatements.org/vocab/InC/1.0/");
    	copyrightToLicense.put("CopyrightWaived", "http://rightsstatements.org/vocab/InC-NC/1.0/");
    	copyrightToLicense.put("CopyrightUndetermined", "https://rightsstatements.org/vocab/CNE/1.0/");
    	copyrightToLicense.put("CopyrightPublicDomain", "https://creativecommons.org/publicdomain/mark/1.0/");
    }
    
    private static String licenseFromCopyright(final Resource r) {
    	final Resource cs = r.getPropertyResourceValue(copyrightStatus);
    	if (cs == null) return null;
    	return copyrightToLicense.getOrDefault(cs.getLocalName(), null);
    }
    
    /* ---------------- label handling per your rules ---------------- */

    private static void applyLabels(Model m, Resource r, ObjectNode target) {
        // Gather prefLabels
        java.util.List<Literal> pref = new java.util.ArrayList<>();
        StmtIterator itPref = m.listStatements(r, SKOS.prefLabel, (RDFNode) null);
        while (itPref.hasNext()) {
            Statement s = itPref.next();
            if (s.getObject().isLiteral()) pref.add(s.getLiteral());
        }
        itPref.close();

        // Choose name:
        // 1) if any prefLabel @bo-x-ewts -> convert to Tibetan for name,
        //    and add the original EWTS to alternateName with language bo-Latn.
        Literal ewtsPref = firstWithLang(pref, "bo-x-ewts");
        if (ewtsPref != null) {
            String ewtsVal = ewtsPref.getString();
            String bo = safeEwtsToBo(ewtsVal);
            target.put("name", bo);
            ensureAltArray(target).add(langValue(ewtsVal, "bo-Latn"));
            // any other prefLabels become alternates (converted if they’re EWTS)
            for (Literal l : pref) {
                if (l == ewtsPref) continue;
                addAltLabelLiteral(target, l, true);
            }
        } else if (!pref.isEmpty()) {
            // 2) else: any prefLabel (first)
            target.put("name", pref.get(0).getString());
            for (int i = 1; i < pref.size(); i++) {
                addAltLabelLiteral(target, pref.get(i), false);
            }
        }

        // Process all altLabels: every altLabel becomes alternateName.
        StmtIterator itAlt = m.listStatements(r, SKOS.altLabel, (RDFNode) null);
        while (itAlt.hasNext()) {
            Statement s = itAlt.next();
            if (!s.getObject().isLiteral()) continue;
            addAltLabelLiteral(target, s.getLiteral(), true);
        }
        itAlt.close();
    }

    private static void addAltLabelLiteral(ObjectNode target, Literal lit, boolean convertTibetanEWTS) {
        String lang = lit.getLanguage() == null ? "" : lit.getLanguage().toLowerCase();
        String val  = lit.getString();

        ArrayNode alts = ensureAltArray(target);

        if (convertTibetanEWTS && "bo-x-ewts".equals(lang)) {
            // Add converted Tibetan as an altName STRING
            alts.add(safeEwtsToBo(val));
            // And add original EWTS with language "bo-Latn"
            alts.add(langValue(val, "bo-Latn"));
        } else {
            // Keep it simple for non-Tibetan or when conversion not requested
            alts.add(val);
        }
    }

    private static ArrayNode ensureAltArray(ObjectNode target) {
        if (target.has("alternateName") && target.get("alternateName").isArray()) {
            return (ArrayNode) target.get("alternateName");
        }
        ArrayNode arr = MAPPER.createArrayNode();
        target.set("alternateName", arr);
        return arr;
    }

    private static ObjectNode langValue(String value, String lang) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("@value", value);
        n.put("@language", lang);
        return n;
    }

    private static String safeEwtsToBo(String ewts) {
        try { return ewtsToBo(ewts); }
        catch (Exception e) { return ewts; }
    }

    private static String detectSchemaType(Model m, Resource main) {
        boolean isInstance = m.contains(main, RDF.type, BDO_Instance);
        int vols = getInt(m, main, numberOfVolumes);
        if (isInstance) return (vols > 1) ? "BookSeries" : "Book";
        return "CreativeWork";
    }

    private static int getInt(Model m, Resource main, Property p) {
        StmtIterator it = m.listStatements(main, p, (RDFNode) null);
        try {
            if (it.hasNext()) {
                Statement s = it.next();
                if (s.getObject().isLiteral()) {
                    try { return s.getLiteral().getInt(); } catch (Exception ignore) {}
                }
            }
        } finally { it.close(); }
        return -1;
    }

    private static final Map<String,String> langToLang = new HashMap<>();
    static {
    	langToLang.put("LangBo", "bo");
    	langToLang.put("LangEn", "en");
    	langToLang.put("LangKm", "km");
    	langToLang.put("LangMy", "my");
    	langToLang.put("LangPi", "pi");
    	langToLang.put("LangZh", "zh");
    }
    
    private static ArrayNode deriveLanguages(Model m, Resource main) {
    	final Resource wa = main.getPropertyResourceValue(instanceOf);
    	if (wa == null) return null;
    	final Resource l = wa.getPropertyResourceValue(language);
    	if (l == null) return null;
    	final String mainL = langToLang.getOrDefault(l.getLocalName(), null);
    	final ArrayNode res = MAPPER.createArrayNode();
    	return res.add(mainL);
    }

    private static void addIdentifiers(Model m, Resource main, ObjectNode root) {
        ArrayNode identifiers = (ArrayNode) root.get("identifier");
        if (identifiers == null) {
            identifiers = MAPPER.createArrayNode();
            root.set("identifier", identifiers);
        }
        StmtIterator it = m.listStatements(main, identifiedBy, (RDFNode) null);
        try {
            while (it.hasNext()) {
                RDFNode idNode = it.next().getObject();
                if (!idNode.isResource()) continue;
                Resource idRes = idNode.asResource();
                if (m.contains(idRes, RDF.type, m.createResource(MarcExport.BF + "Isbn"))) {
                    String val = valueOf(m, idRes);
                    if (val != null && !val.isEmpty()) { root.put("isbn", val); }
                }
                if (m.contains(idRes, RDF.type, m.createResource(MarcExport.BF + "Issn"))) {
                    String val = valueOf(m, idRes);
                    if (val != null && !val.isEmpty()) { root.put("issn", val); }
                }
            }
        } finally { it.close(); }
    }
    private static String valueOf(Model m, Resource r) {
        Statement s = r.getProperty(RDF.value);
        if (s != null && s.getObject().isLiteral()) return s.getString();
        return null;
    }

    private static void addPublisher(Model m, Resource main, ObjectNode root) {
        Statement pn = main.getProperty(publisherName);
        Statement pl = main.getProperty(publisherLocation);
        if (pn == null && pl == null) return;
        ObjectNode publisher = MAPPER.createObjectNode();
        publisher.put("@type", "Organization");
        if (pn != null && pn.getObject().isLiteral()) publisher.put("name", pn.getString());
        if (pl != null && pl.getObject().isLiteral()) publisher.put("address", pl.getString());
        root.set("publisher", publisher);
    }

    private static void addAuthors(Model m, Resource main, ObjectNode root) {
        Statement instStmt = main.getProperty(instanceOf);
        if (instStmt == null || !instStmt.getObject().isResource()) return;
        Resource work = instStmt.getResource();

        StmtIterator creators = m.listStatements(work, creator, (RDFNode) null);
        ArrayNode authors = MAPPER.createArrayNode();
        java.util.HashSet<String> seen = new java.util.HashSet<>();

        try {
            while (creators.hasNext()) {
                RDFNode crwNode = creators.next().getObject();
                if (!crwNode.isResource()) continue;
                Resource crw = crwNode.asResource();
                Statement ag = crw.getProperty(agent);
                if (ag == null || !ag.getObject().isResource()) continue;

                Resource person = ag.getResource();
                ObjectNode personJson = buildPersonNode(m, person);
                if (personJson == null) continue;
                String pid = personJson.has("@id") ? personJson.get("@id").asText() : null;
                if (pid != null && !seen.contains(pid)) {
                    seen.add(pid);
                    authors.add(personJson);
                }
            }
        } finally { creators.close(); }

        if (authors.size() > 0) root.set("author", authors);

    }

    private static ObjectNode buildPersonNode(Model m, Resource person) {
        ObjectNode p = MAPPER.createObjectNode();
        p.put("@type", "Person");
        if (person.getURI() != null) p.put("@id", person.getURI());

        ArrayNode ids = MAPPER.createArrayNode();
        if (person.getLocalName() != null) ids.add("bdr:" + person.getLocalName());
        if (person.getURI() != null) ids.add(person.getURI());
        if (ids.size() > 0) p.set("identifier", ids);

        applyLabels(m, person, p);

        ArrayNode sameAs = collectSameAs(m, person);
        if (sameAs.size() > 0) p.set("sameAs", sameAs);

        String birth = extractEventDateFromLink(m, person, personEvent, PersonBirth);
        if (birth != null) p.put("birthDate", birth);

        String death = extractEventDateFromLink(m, person, personEvent, PersonDeath);
        if (death != null) p.put("deathDate", death);

        return p;
    }

    private static Literal firstWithLang(java.util.List<Literal> lits, String langLower) {
        for (Literal l : lits) {
            String ll = l.getLanguage() == null ? "" : l.getLanguage().toLowerCase();
            if (ll.equals(langLower)) return l;
        }
        return null;
    }
    
}
