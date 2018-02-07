package io.bdrc.ldspdi.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.vocabulary.SKOS;

import io.bdrc.jena.sttl.CompareComplex;
import io.bdrc.jena.sttl.ComparePredicates;
import io.bdrc.jena.sttl.STTLWriter;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.sparql.results.Results;
import io.bdrc.ldspdi.sparql.results.ResultsCache;

public class RestUtils {
    
    public static Logger log=Logger.getLogger(RestUtils.class.getName());
    
    public static boolean getJsonOutput(String param) {
        boolean json;
        try {
            json=Boolean.parseBoolean(param);            
        }catch(Exception ex){
            json=false;            
        }
        return json;
    }
    
    public static int getHash(String param) {
        int hash;
        try {
            hash=Integer.parseInt(param);            
        }catch(Exception ex){
            hash= -1;            
        }
        return hash;
    }
    
    public static int getPageSize(String param) {
        int pageSize;
        try {
            pageSize=Integer.parseInt(param);
            
        }catch(Exception ex){
            pageSize= Integer.parseInt(ServiceConfig.getProperty(QueryConstants.PAGE_SIZE));            
        }
        return pageSize;
    }
    
    public static int getPageNumber(String param) {
        int pageNumber;
        try {
            pageNumber=Integer.parseInt(param);
            
        }catch(Exception ex){
            pageNumber= 1;            
        }
        return pageNumber;
    }
    
    public static MediaType getMediaType(String format){
        String[] parts=format.split(Pattern.quote("/"));
        return new MediaType(parts[0],parts[1]);        
    }
    
    public static boolean isValidExtension(String ext){
        return (ServiceConfig.getProperty(ext)!=null);
    }
    
    public static RDFWriter getSTTLRDFWriter(Model m) throws IOException{
        Lang sttl = STTLWriter.registerWriter();
        SortedMap<String, Integer> nsPrio = ComparePredicates.getDefaultNSPriorities();
        nsPrio.put(SKOS.getURI(), 1);
        nsPrio.put("http://purl.bdrc.io/ontology/admin/", 5);
        nsPrio.put("http://purl.bdrc.io/ontology/toberemoved/", 6);
        List<String> predicatesPrio = CompareComplex.getDefaultPropUris();
        predicatesPrio.add("http://purl.bdrc.io/ontology/admin/logWhen");
        predicatesPrio.add("http://purl.bdrc.io/ontology/onOrAbout");
        predicatesPrio.add("http://purl.bdrc.io/ontology/noteText");
        org.apache.jena.sparql.util.Context ctx = new org.apache.jena.sparql.util.Context();
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsPriorities"), nsPrio);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsDefaultPriority"), 2);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "complexPredicatesPriorities"), predicatesPrio);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "indentBase"), 3);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "predicateBaseWidth"), 12);
        RDFWriter w = RDFWriter.create().source(m.getGraph()).context(ctx).lang(sttl).build();
        return w;
    }    
       
    public static Results getResults(String query, String fuseki, int hash, int pageSize) {
        Results res;
        if(hash ==-1) {
            long start=System.currentTimeMillis();
            QueryProcessor processor=new QueryProcessor();
            ResultSet jrs=processor.getResultSet(query, fuseki);
            long end=System.currentTimeMillis();
            long elapsed=end-start;
            res=new Results(jrs,elapsed,pageSize);                    
            int new_hash=Objects.hashCode(res);                    
            res.setHash(new_hash);                    
            ResultsCache.addToCache(res, Objects.hashCode(res));
            log.info("New Results object loaded into cache with hash:"+new_hash);
        }
        else {
            res=ResultsCache.getResultsFromCache(hash);
            log.info("Got Results object from cache with hash:"+hash);
        }
        return res;
    }
    
    public static HashMap<String,String> convertMulti(MultivaluedMap<String,String> map){
        HashMap<String,String> copy=new HashMap<>();
        Set<String> set=map.keySet();
        for(String key:set) {
            copy.put(key, map.getFirst(key));
        }
        return copy;
    }
    
    public static String relativizeURL(String uri) {
        uri=uri.substring(8);
        return uri.substring(uri.indexOf("/"));
    }

}
