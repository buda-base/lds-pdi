package io.bdrc.ldspdi.utils;

import java.io.IOException;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.maxmind.db.CacheKey;
import com.maxmind.db.DecodedValue;
import com.maxmind.db.NodeCache;

public class JCSNodeCache implements NodeCache {

    // The raison d'être of this class is to be used by Geoloc's cache mechanism

    public static Cache<CacheKey, DecodedValue> CACHE;
    public final static Logger log = LoggerFactory.getLogger(JCSNodeCache.class.getName());

    public JCSNodeCache() {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
        cacheManager.init();
        CACHE = cacheManager.createCache("geoloc", CacheConfigurationBuilder.newCacheConfigurationBuilder(CacheKey.class, DecodedValue.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));
        log.debug("Cache was initialized {}", CACHE);
        //cacheManager.close();
    }

    @Override
    public DecodedValue get(CacheKey key, Loader loader) throws IOException {
        try {
            DecodedValue value = CACHE.get(key);
            if (value == null) {
                value = loader.load(key);
                CACHE.put(key, value);
            }
            return value;
        } catch (IOException e) {
            log.error("An issue occured while getting Json Node from cache for key " + key + " message: " + e.getMessage());
            throw e;
        }
    }

}
