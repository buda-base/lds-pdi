package io.bdrc.ldspdi.sparql.results;

/*******************************************************************************
 * Copyright (c) 2018 Buddhist Digital Resource Center (BDRC)
 * 
 * If this file is a derivation of another work the license header will appear below; 
 * otherwise, this work is licensed under the Apache License, Version 2.0 
 * (the "License"); you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.access.exception.CacheException;
import org.apache.commons.jcs.admin.JCSAdminBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultsCache {
    
    protected static CacheAccess<Integer,Object> CACHE = JCS.getInstance( "default" );
    public final static Logger log=LoggerFactory.getLogger(ResultsCache.class.getName());
        
    public static void addToCache(Object res, int hash) {        
        try{
            CACHE.put( Integer.valueOf(hash), res );            
            res=null;
        }
        catch ( CacheException e ){
            log.error("Problem putting Results -->"+res+" in the cache, for key -->"+hash+ " Exception:"+e.getMessage());
        }
    }
    
    public static Object getObjectFromCache(int hash) {
        
        return CACHE.get( Integer.valueOf(hash));
    }
}
