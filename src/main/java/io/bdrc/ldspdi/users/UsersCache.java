package io.bdrc.ldspdi.users;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.access.exception.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsersCache {

    public static CacheAccess<Integer, Object> CACHE;
    public final static Logger log = LoggerFactory.getLogger(UsersCache.class);

    public static void init() {
        CACHE = JCS.getInstance("usr");
    }

    public static void addToCache(Object res, int hash) {
        try {
            CACHE.put(Integer.valueOf(hash), res);
            res = null;
        } catch (CacheException e) {
            log.error("Problem putting Users Results -->" + res + " in the cache, for key -->" + hash + " Exception:" + e.getMessage());
        }
    }

    public static Object getObjectFromCache(int hash) {
        return CACHE.get(Integer.valueOf(hash));
    }

    public static boolean clearCache() {
        try {
            CACHE.clear();
            log.info("The users cache has been cleared");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
