package io.bdrc.ldspdi.rest.resources;

import java.util.ArrayList;

public class MediaTypeUtils {
    
    public static ArrayList<String> RDFMEDIA;
    
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
    }
    
    public static boolean isRdfMedia(String media) {
        return RDFMEDIA.contains(media) ;  
    }

}
