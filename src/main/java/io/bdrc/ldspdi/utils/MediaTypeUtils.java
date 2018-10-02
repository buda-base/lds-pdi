package io.bdrc.ldspdi.utils;

import java.util.HashMap;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Variant;

import org.apache.jena.riot.RDFLanguages;

import io.bdrc.formatters.TTLRDFWriter;

public class MediaTypeUtils {

    public static final HashMap<String,String> ExtToJena;
    public static final HashMap<MediaType,String> MimeToExt;
    public static final HashMap<String,MediaType> ExtToMime;
    // these are the extensions advertised in the http headers
    public static final HashMap<String,MediaType> ResExtToMime;

    public static final MediaType MT_MRCX_CU = MediaType.valueOf("application/marcxml+xml; profile=\"http://purl.bdrc.io/resource/MarcCUProfile\"");
    public static final MediaType MT_MRCX = new MediaType("application","marcxml+xml");
    public static final MediaType MT_CSV = new MediaType("text","csv");
    public static final MediaType MT_JSONLD = new MediaType("application","ld+json");
    public static final MediaType MT_RT = new MediaType("application","rdf+thrift");
    public static final MediaType MT_TTL = new MediaType("text","turtle");
    public static final MediaType MT_NT = new MediaType("application","n-triples");
    public static final MediaType MT_NQ = new MediaType("application","n-quads");
    public static final MediaType MT_TRIG = new MediaType("text","trig");
    public static final MediaType MT_RDF = new MediaType("application","rdf+xml");
    public static final MediaType MT_OWL = new MediaType("application","owl+xml");
    public static final MediaType MT_TRIX = new MediaType("application","trix+xml");
    public static final MediaType MT_JSONLD_WA = MediaType.valueOf("application/ld+json; profile=\"http://www.w3.org/ns/anno.jsonld\"");
    public static final MediaType MT_JSONLD_OA = MediaType.valueOf("application/ld+json; profile=\"http://www.w3.org/ns/oa.jsonld\"");

    public static final List<Variant> resVariants;
    public static final List<Variant> resVariantsWithMarc;
    public static final List<Variant> resVariantsNoHtml;
    public static final List<Variant> graphVariants;
    public static final List<Variant> annVariants;

    static {
        resVariants = Variant.mediaTypes(MediaType.TEXT_HTML_TYPE,
                MT_JSONLD, MT_RT, MT_TTL, MediaType.APPLICATION_JSON_TYPE,
                MT_NT, MT_NQ, MT_TRIG, MT_RDF, MT_OWL,
                MT_TRIX).build();

        resVariantsWithMarc = Variant.mediaTypes(MediaType.TEXT_HTML_TYPE,
                MT_JSONLD, MT_RT, MT_TTL, MediaType.APPLICATION_JSON_TYPE,
                MT_NT, MT_NQ, MT_TRIG, MT_RDF, MT_OWL,
                MT_TRIX, MT_MRCX, MT_MRCX_CU).build();

        resVariantsNoHtml = Variant.mediaTypes(
                MT_JSONLD, MT_RT, MT_TTL, MediaType.APPLICATION_JSON_TYPE,
                MT_NT, MT_NQ, MT_TRIG, MT_RDF, MT_OWL,
                MT_TRIX).build();

        annVariants = Variant.mediaTypes(MediaType.TEXT_HTML_TYPE,
                MT_JSONLD_WA, MT_JSONLD_OA, MT_RT, MT_TTL, MediaType.APPLICATION_JSON_TYPE,
                MT_NT, MT_NQ, MT_TRIG, MT_RDF, MT_OWL,
                MT_TRIX, MT_JSONLD).add().build();

        graphVariants = Variant.mediaTypes(
                MT_JSONLD, MT_RT, MT_TTL, MediaType.APPLICATION_JSON_TYPE,
                MT_NT, MT_NQ, MT_TRIG, MT_RDF, MT_OWL,
                MT_TRIX).build();

        ExtToJena = new HashMap<>();
        ExtToJena.put("ttl",  TTLRDFWriter.strLangSttl);
        ExtToJena.put("nt",   RDFLanguages.strLangNTriples);
        ExtToJena.put("nq",   RDFLanguages.strLangNQuads);
        ExtToJena.put("trig", RDFLanguages.strLangTriG);
        ExtToJena.put("rdf",  RDFLanguages.strLangRDFXML);
        ExtToJena.put("owl",  RDFLanguages.strLangRDFXML);
        ExtToJena.put("jsonld", RDFLanguages.strLangJSONLD);
        ExtToJena.put("rt",   RDFLanguages.strLangRDFTHRIFT);
        ExtToJena.put("rj",   RDFLanguages.strLangRDFJSON);
        ExtToJena.put("json", RDFLanguages.strLangRDFJSON);
        ExtToJena.put("trix", RDFLanguages.strLangTriX);

        MimeToExt = new HashMap<>();
        MimeToExt.put(MT_JSONLD_WA,   "jsonld");
        MimeToExt.put(MT_JSONLD_OA,   "jsonld");
        MimeToExt.put(MT_MRCX_CU,     "mrcx");
        MimeToExt.put(MediaType.TEXT_HTML_TYPE,   "html");
        MimeToExt.put(MT_MRCX,   "mrcx");
        MimeToExt.put(MT_TTL,    "ttl");
        MimeToExt.put(MT_NT,     "nt");
        MimeToExt.put(MT_NQ,     "nq");
        MimeToExt.put(MT_TRIG,   "trig");
        MimeToExt.put(MT_RDF,    "rdf");
        MimeToExt.put(MT_OWL,    "owl");
        MimeToExt.put(MT_JSONLD, "jsonld");
        MimeToExt.put(MT_RT,     "rt");
        MimeToExt.put(MediaType.APPLICATION_JSON_TYPE, "rj");
        MimeToExt.put(MT_TRIX,   "trix");

        ExtToMime = new HashMap<>();
        ExtToMime.put("mrcx", MT_MRCX);
        ExtToMime.put("ttl",  MT_TTL);
        ExtToMime.put("nt",   MT_NT);
        ExtToMime.put("nq",   MT_NQ);
        ExtToMime.put("trig", MT_TRIG);
        ExtToMime.put("rdf",  MT_RDF);
        ExtToMime.put("owl",  MT_OWL);
        ExtToMime.put("jsonld", MT_JSONLD);
        ExtToMime.put("rt",   MT_RT);
        ExtToMime.put("rj",   MediaType.APPLICATION_JSON_TYPE);
        ExtToMime.put("json", MediaType.APPLICATION_JSON_TYPE);
        ExtToMime.put("trix", MT_TRIX);
        ExtToMime.put("html", MediaType.TEXT_HTML_TYPE);

        ResExtToMime = new HashMap<>();
        ResExtToMime.put("ttl",  MT_TTL);
        ResExtToMime.put("nt",   MT_NT);
        ResExtToMime.put("nq",   MT_NQ);
        ResExtToMime.put("trig", MT_TRIG);
        ResExtToMime.put("rdf",  MT_RDF);
        ResExtToMime.put("jsonld", MT_JSONLD);
        ResExtToMime.put("rj",   MediaType.APPLICATION_JSON_TYPE);
        ResExtToMime.put("trix", MT_TRIX);
        ResExtToMime.put("html", MediaType.TEXT_HTML_TYPE);
    }

    public static HashMap<String,MediaType> getResExtensionMimeMap(){
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

    public static MediaType getMediaType(final Request request, final String format, final List<Variant> variants) {
        if (format == null)
            return null;
        final Variant variant = request.selectVariant(variants);
        if (variant == null) {
            return null;
        }
        return variant.getMediaType();
    }

}