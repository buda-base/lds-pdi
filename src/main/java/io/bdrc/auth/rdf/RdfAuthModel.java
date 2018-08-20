package io.bdrc.auth.rdf;


import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.restapi.exceptions.RestException;

public class RdfAuthModel implements Runnable{
    
    static Model authMod;       
    
    public final static Logger log=LoggerFactory.getLogger(RdfAuthModel.class.getName());
    public final static String query="\n SELECT ?s ?p ?o \n" + 
            "WHERE {\n" + 
            "  GRAPH <http://purl.bdrc.io/ontology/ext/authData> { \n" + 
            "    ?s ?p ?o .\n" + 
            "    }\n" + 
            "}";
        
    public static void init() throws RestException {        
        authMod=QueryProcessor.getAuthDataGraph(query);
    }
    
    public static Model getFullModel() {
        return authMod;
    }
        
    @Override
    public void run() {
        QueryProcessor.updateAuthData(null);
        log.info("Done loading rdfAuth Model");
    }
    

}
