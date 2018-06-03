package io.bdrc.ldspdi.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;

public class MediaTypeUtils {
    
    public static ArrayList<String> MIMES;
    public static HashMap<String,String> JENAFORMAT;
    public static HashMap<String,String> MIMEFORMAT;
    public static HashMap<String,String> EXTENSION;
    public static HashMap<String,MediaType> MEDIAS;
    
    static {
        MIMES = new ArrayList<>();
        MIMES.add("text/*");
        MIMES.add("application/*");
        MIMES.add("text/turtle");
        MIMES.add("application/n-triples");
        MIMES.add("application/n-quads");
        MIMES.add("text/trig");
        MIMES.add("application/rdf+xml");
        MIMES.add("application/owl+xml");
        MIMES.add("application/ld+json");
        MIMES.add("application/rdf+thrift");
        MIMES.add("application/json");
        MIMES.add("application/trix+xml");
        
        JENAFORMAT=new HashMap<>();
        JENAFORMAT.put("ttl","TURTLE");
        JENAFORMAT.put("nt","N-Triples");
        JENAFORMAT.put("nq","N-Quads");
        JENAFORMAT.put("trig","TriG");
        JENAFORMAT.put("rdf","RDF/XML");
        JENAFORMAT.put("owl","RDF/XML");
        JENAFORMAT.put("jsonld","JSON-LD");
        JENAFORMAT.put("rt","RDFThrift");
        JENAFORMAT.put("trdf", "RDFThrift");
        JENAFORMAT.put("rj","RDF/JSON");
        JENAFORMAT.put("json","RDF/JSON");
        JENAFORMAT.put("trix","Trix");
        
        MIMEFORMAT=new HashMap<>();
        MIMEFORMAT.put("text/*","ttl");
        MIMEFORMAT.put("text/turtle","ttl");
        MIMEFORMAT.put("application/n-triples","nt");
        MIMEFORMAT.put("application/n-quads","nq");
        MIMEFORMAT.put("text/trig","trig");
        MIMEFORMAT.put("application/rdf+xml","rdf");
        MIMEFORMAT.put("application/owl+xml","owl");
        MIMEFORMAT.put("application/ld+json","jsonld");
        MIMEFORMAT.put("application/rdf+thrift","rt");
        MIMEFORMAT.put("application/*","jsonld");
        MIMEFORMAT.put("application/json","rj");
        MIMEFORMAT.put("application/trix+xml","trix");
        
        EXTENSION=new HashMap<>();
        EXTENSION.put("ttl","text/turtle");
        EXTENSION.put("nt","application/n-triples");
        EXTENSION.put("nq","application/n-quads");
        EXTENSION.put("trig","text/trig");
        EXTENSION.put("rdf","application/rdf+xml");
        EXTENSION.put("owl","application/owl+xml");
        EXTENSION.put("jsonld","application/ld+json");
        EXTENSION.put("rt","application/rdf+thrift");
        EXTENSION.put("rj","application/json");
        EXTENSION.put("json","application/json");
        EXTENSION.put("trix","application/trix+xml");
        
        MEDIAS=new HashMap<>();
        MEDIAS.put("jsonld",new MediaType("application","ld+json"));
        MEDIAS.put("rt",new MediaType("application","rdf+thrift"));
        MEDIAS.put("ttl",new MediaType("text","turtle"));
        MEDIAS.put("nt",new MediaType("application","n-triples"));
        MEDIAS.put("nq",new MediaType("application","n-quads"));
        MEDIAS.put("trig",new MediaType("text","trig"));
        MEDIAS.put("rdf",new MediaType("application","rdf+xml"));
        MEDIAS.put("owl",new MediaType("application","owl+xml"));
        MEDIAS.put("jsonld",new MediaType("application","ld+json"));
        MEDIAS.put("rt",new MediaType("application","rdf+thrift"));
        MEDIAS.put("trig",new MediaType("text","trig"));
        MEDIAS.put("rdf",new MediaType("application","rdf+xml"));
        MEDIAS.put("rj",MediaType.APPLICATION_JSON_TYPE);
        MEDIAS.put("json",MediaType.APPLICATION_JSON_TYPE);
        MEDIAS.put("trix",new MediaType("application","trix+xml"));
    }
    
    public static HashMap<String,String> getExtensionMimeMap(){
        return EXTENSION;
    }
    
    public static HashMap<String,String> getMimeMap(){
        return MIMEFORMAT;
    }
    
    public static String getExtFormatFromMime(String mime) {
        return MIMEFORMAT.get(mime);
    }
    
    public static String getMimeFromExtension(String ext) {
        return EXTENSION.get(ext);
    }
    
    public static String getJenaFromExtension(String ext) {
        return JENAFORMAT.get(ext);
    }
    
    public static boolean isMime(String media) {
        return MIMES.contains(media) ;  
    }
    
    public static ArrayList<String> getValidMime(List<MediaType> list) {
        ArrayList<String> valid=new ArrayList<>();
        for(MediaType m:list) {
            if(isMime(m.toString())){
                valid.add(m.toString());
            }
        }
        return valid ;  
    }
    
    public static MediaType getMediaTypeFromExt(String format) {
        MediaType media=new MediaType("text","turtle","utf-8");        
        String tmp=getMimeFromExtension(format);
        if(tmp!=null){
            if(isMime(tmp)){
                String[] parts=tmp.split(Pattern.quote("/"));
                media = new MediaType(parts[0],parts[1]); 
            }
        }
        return media;
    }
    
    public static MediaType getMediaTypeFromMime(String mime) {
        MediaType media=new MediaType("text","turtle","utf-8");        
            if(mime.equals("text/*")) {return new MediaType("text","turtle");} 
            if(mime.equals("application/*")) {return new MediaType("application","ld+json");} 
            if(isMime(mime)){
                String[] parts=mime.split(Pattern.quote("/"));
                media = new MediaType(parts[0],parts[1]); 
            }        
        return media;
    }
    
    public static ArrayList<Variant> getJenaVariantList(){
        ArrayList<Variant> variants=new ArrayList<>();
        for(String key:MEDIAS.keySet()) {
            variants.add(new Variant(MEDIAS.get(key),(Locale)null,null));
        }
        return variants;
    }
    
    public static List<Variant> getBasicVariantList(){
        //ArrayList<Variant> variants=new ArrayList<>(); 
        List<Variant> variants=Variant.mediaTypes(MediaType.TEXT_HTML_TYPE).build();
        return variants;
    }

}
