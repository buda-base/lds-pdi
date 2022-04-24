package io.bdrc.ldspdi.service;

import java.io.IOException;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.rdf.IPCache;

public class IPCacheImpl implements IPCache {

    public static Cache<String, String> CACHE;
    public final static Logger log = LoggerFactory.getLogger(IPCacheImpl.class.getName());

    public IPCacheImpl() {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
        cacheManager.init();
        CACHE = cacheManager.createCache("ipcache", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, String.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(100, EntryUnit.ENTRIES)));
        log.debug("Cache was initialized {}", CACHE);
    }
    
    @Override
    public String getSubscriber(String ip, Loader loader) throws IOException {
        try {
            String value = CACHE.get(ip);
            if (value == null) {
                value = loader.loadSubscriber(ip);
                if (value == null)
                    value = "";
                CACHE.put(ip, value);
            }
            if (value.isEmpty())
                return null;
            return value;
        } catch (IOException e) {
            log.error("An issue occured while getting Subscriber from cache for key " + ip + " message: " + e.getMessage());
            throw e;
        }
    }

}

