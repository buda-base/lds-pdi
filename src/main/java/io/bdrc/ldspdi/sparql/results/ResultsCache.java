package io.bdrc.ldspdi.sparql.results;

import java.util.logging.Logger;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.access.exception.CacheException;

import io.bdrc.ldspdi.rest.resources.PublicDataResource;

public class ResultsCache {
    
    private static CacheAccess<Integer,Results> CACHE = JCS.getInstance( "default" );
    public static Logger log=Logger.getLogger(PublicDataResource.class.getName());
    
    public static void addToCache(Results res, int hash) {
        log.info("Hash is:"+hash);
        try{
            CACHE.put( new Integer(hash), res );
        }
        catch ( CacheException e ){
            log.finest("Problem putting Results -->"+res+" in the cache, for key -->"+hash+ " Exception:"+e.getMessage());
        }
    }
    
    public static Results getResultsFromCache(int hash) {
        return CACHE.get( new Integer(hash));
    }
    
    

}
