package io.bdrc.ldspdi.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.springframework.http.MediaType;

import io.bdrc.formatters.TTLRDFWriter;

public class BudaMediaTypes {

    public static final MediaType MT_MRCX_CU = MediaType.valueOf("application/marcxml+xml; profile=\"http://purl.bdrc.io/resource/MarcCUProfile\"");
    public static final MediaType MT_MRCX = new MediaType("application", "marcxml+xml");
    public static final MediaType MT_MRC = new MediaType("application", "marc");
    public static final MediaType MT_CSV = new MediaType("text", "csv");
    public static final MediaType MT_JSONLD = new MediaType("application", "ld+json");
    public static final MediaType MT_RT = new MediaType("application", "rdf+thrift");
    public static final MediaType MT_TTL = new MediaType("text", "turtle");
    public static final MediaType MT_NT = new MediaType("application", "n-triples");
    public static final MediaType MT_NQ = new MediaType("application", "n-quads");
    public static final MediaType MT_TRIG = new MediaType("text", "trig");
    public static final MediaType MT_RDF = new MediaType("application", "rdf+xml");
    public static final MediaType MT_OWL = new MediaType("application", "owl+xml");
    public static final MediaType MT_TRIX = new MediaType("application", "trix+xml");
    public static final MediaType MT_JSONLD_WA = MediaType.valueOf("application/ld+json; profile=\"http://www.w3.org/ns/anno.jsonld\"");
    public static final MediaType MT_JSONLD_OA = MediaType.valueOf("application/ld+json; profile=\"http://www.w3.org/ns/oa.jsonld\"");

    public static final List<MediaType> resVariants;
    public static final List<MediaType> resVariantsWithMarc;
    public static final List<MediaType> resVariantsNoHtml;
    public static final List<MediaType> graphVariants;
    public static final List<MediaType> annVariants;

    public static final HashMap<String, String> ExtToJena;
    public static final HashMap<String, String> ExtToJenaRead;
    public static final HashMap<String, Lang> ExtToJenaLang;
    public static final HashMap<MediaType, String> MimeToExt;
    public static final HashMap<String, MediaType> ExtToMime;
    // these are the extensions advertised in the http headers
    public static final HashMap<String, MediaType> ResExtToMime;

    static {
        resVariants = new ArrayList<>(Arrays.asList(MediaType.TEXT_HTML, MT_JSONLD, MT_RT, MT_TTL, MediaType.APPLICATION_JSON, MT_NT, MT_NQ, MT_TRIG, MT_RDF, MT_OWL, MT_TRIX));
        resVariantsWithMarc = new ArrayList<>(Arrays.asList(MediaType.TEXT_HTML, MT_JSONLD, MT_RT, MT_TTL, MediaType.APPLICATION_JSON, MT_NT, MT_NQ, MT_TRIG, MT_RDF, MT_OWL, MT_TRIX, MT_MRCX, MT_MRC, MT_MRCX_CU));
        resVariantsNoHtml = new ArrayList<>(Arrays.asList(MT_JSONLD, MT_RT, MT_TTL, MediaType.APPLICATION_JSON, MT_NT, MT_NQ, MT_TRIG, MT_RDF, MT_OWL, MT_TRIX));
        graphVariants = new ArrayList<>(Arrays.asList(MT_JSONLD, MT_RT, MT_TTL, MediaType.APPLICATION_JSON, MT_NT, MT_NQ, MT_TRIG, MT_RDF, MT_OWL, MT_TRIX));
        annVariants = new ArrayList<>(Arrays.asList(MediaType.TEXT_HTML, MT_JSONLD_WA, MT_JSONLD_OA, MT_RT, MT_TTL, MediaType.APPLICATION_JSON, MT_NT, MT_NQ, MT_TRIG, MT_RDF, MT_OWL, MT_TRIX, MT_JSONLD));

        ExtToJena = new HashMap<>();
        ExtToJena.put("ttl", TTLRDFWriter.strLangSttl);
        ExtToJena.put("nt", RDFLanguages.strLangNTriples);
        ExtToJena.put("nq", RDFLanguages.strLangNQuads);
        ExtToJena.put("trig", RDFLanguages.strLangTriG);
        ExtToJena.put("rdf", RDFLanguages.strLangRDFXML);
        ExtToJena.put("owl", RDFLanguages.strLangRDFXML);
        ExtToJena.put("jsonld", RDFLanguages.strLangJSONLD);
        ExtToJena.put("rt", RDFLanguages.strLangRDFTHRIFT);
        ExtToJena.put("rj", RDFLanguages.strLangRDFJSON);
        ExtToJena.put("json", RDFLanguages.strLangRDFJSON);
        ExtToJena.put("trix", RDFLanguages.strLangTriX);

        ExtToJenaRead = new HashMap<>(ExtToJena);
        ExtToJenaRead.put("ttl", RDFLanguages.strLangTurtle);

        ExtToJenaLang = new HashMap<>();
        ExtToJenaLang.put("ttl", Lang.TURTLE);
        ExtToJenaLang.put("nt", Lang.NT);
        ExtToJenaLang.put("nq", Lang.NQ);
        ExtToJenaLang.put("trig", Lang.TRIG);
        ExtToJenaLang.put("rdf", Lang.RDFXML);
        ExtToJenaLang.put("owl", Lang.RDFXML);
        ExtToJenaLang.put("jsonld", Lang.JSONLD);
        ExtToJenaLang.put("rt", Lang.RDFTHRIFT);
        ExtToJenaLang.put("rj", Lang.RDFJSON);
        ExtToJenaLang.put("json", Lang.RDFJSON);
        ExtToJenaLang.put("trix", Lang.TRIX);

        MimeToExt = new HashMap<>();
        MimeToExt.put(MT_JSONLD_WA, "jsonld");
        MimeToExt.put(MT_JSONLD_OA, "jsonld");
        MimeToExt.put(MT_MRCX_CU, "mrcx");
        MimeToExt.put(MediaType.TEXT_HTML, "html");
        MimeToExt.put(MT_MRCX, "mrcx");
        MimeToExt.put(MT_MRC, "mrc");
        MimeToExt.put(MediaType.TEXT_PLAIN, "txt");
        MimeToExt.put(MT_TTL, "ttl");
        MimeToExt.put(MT_NT, "nt");
        MimeToExt.put(MT_NQ, "nq");
        MimeToExt.put(MT_TRIG, "trig");
        MimeToExt.put(MT_RDF, "rdf");
        MimeToExt.put(MT_OWL, "owl");
        MimeToExt.put(MT_JSONLD, "jsonld");
        MimeToExt.put(MT_RT, "rt");
        MimeToExt.put(MediaType.APPLICATION_JSON, "rj");
        MimeToExt.put(MT_TRIX, "trix");

        ExtToMime = new HashMap<>();
        ExtToMime.put("mrcx", MT_MRCX);
        ExtToMime.put("mrc", MT_MRC);
        ExtToMime.put("txt", MediaType.TEXT_PLAIN);
        ExtToMime.put("ttl", MT_TTL);
        ExtToMime.put("nt", MT_NT);
        ExtToMime.put("nq", MT_NQ);
        ExtToMime.put("trig", MT_TRIG);
        ExtToMime.put("rdf", MT_RDF);
        ExtToMime.put("owl", MT_OWL);
        ExtToMime.put("jsonld", MT_JSONLD);
        ExtToMime.put("rt", MT_RT);
        ExtToMime.put("rj", MediaType.APPLICATION_JSON);
        ExtToMime.put("json", MediaType.APPLICATION_JSON);
        ExtToMime.put("trix", MT_TRIX);
        ExtToMime.put("html", MediaType.TEXT_HTML);

        ResExtToMime = new HashMap<>();
        ResExtToMime.put("ttl", MT_TTL);
        ResExtToMime.put("nt", MT_NT);
        ResExtToMime.put("nq", MT_NQ);
        ResExtToMime.put("trig", MT_TRIG);
        ResExtToMime.put("rdf", MT_RDF);
        ResExtToMime.put("jsonld", MT_JSONLD);
        ResExtToMime.put("rj", MediaType.APPLICATION_JSON);
        ResExtToMime.put("trix", MT_TRIX);
        ResExtToMime.put("html", MediaType.TEXT_HTML);

    }

    public static HashMap<String, MediaType> getResExtensionMimeMap() {
        return ResExtToMime;
    }

    public static String getExtFromMime(MediaType mime) {
        return MimeToExt.get(mime);
    }

    public static MediaType getMimeFromExtension(String ext) {
        return ExtToMime.get(ext);
    }

    public static String getJenaFromExtension(String ext) {
        return ExtToJena.get(ext);
    }

    public static Lang getJenaLangFromExtension(String ext) {
        return ExtToJenaLang.get(ext);
    }

    public static String getJenaReadFromExtension(String ext) {
        return ExtToJenaRead.get(ext);
    }

    public static MediaType selectVariant(String format, List<MediaType> list) {
        if (format == null) {
            return null;
        }
        List<MediaType> reqMT = MediaType.parseMediaTypes(format);
        MediaType.sortByQualityValue(reqMT);
        for (int x = 0; x < reqMT.size(); x++) {
            MediaType mt = reqMT.get(x);
            if (list.contains(mt)) {
                return mt;
            }
        }
        return null;
    }

}
