package io.bdrc.ldspdi.rest.resources;

import java.util.ArrayList;
import java.util.HashMap;

public class MediaTypeUtils {
    
    public static ArrayList<String> RDFMEDIA;
    public static HashMap<String,String> JENAFORMAT;
    
    static {
        RDFMEDIA = new ArrayList<>();
        RDFMEDIA.add("text/turtle");
        RDFMEDIA.add("application/n-triples");
        RDFMEDIA.add("application/n-quads");
        RDFMEDIA.add("text/trig");
        RDFMEDIA.add("application/rdf+xml");
        RDFMEDIA.add("application/owl+xml");
        RDFMEDIA.add("application/ld+json");
        RDFMEDIA.add("application/rdf+thrift");
        RDFMEDIA.add("application/json");
        RDFMEDIA.add("application/trix+xml");
        
        JENAFORMAT=new HashMap<>();
        JENAFORMAT.put("text/turtle","ttl");
        JENAFORMAT.put("application/n-triples","nt");
        JENAFORMAT.put("application/n-quads","nq");
        JENAFORMAT.put("text/trig","trig");
        JENAFORMAT.put("application/rdf+xml","rdf");
        JENAFORMAT.put("application/owl+xml","owl");
        JENAFORMAT.put("application/ld+json","jsonld");
        JENAFORMAT.put("application/rdf+thrift","rt");
        JENAFORMAT.put("application/json","rj");
        JENAFORMAT.put("application/trix+xml","trix");
        
    }
    
    public static String getJenaFormat(String media) {
        return JENAFORMAT.get(media);
    }
    
    public static boolean isRdfMedia(String media) {
        return RDFMEDIA.contains(media) ;  
    }

}
