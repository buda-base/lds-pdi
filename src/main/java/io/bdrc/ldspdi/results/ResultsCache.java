package io.bdrc.ldspdi.results;

import javax.cache.CacheException;

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

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultsCache {

    public static Cache<Integer, Object> CACHE;
    public final static Logger log = LoggerFactory.getLogger(ResultsCache.class);

    public static void init() {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
        cacheManager.init();
        CACHE = cacheManager.createCache("geoloc", CacheConfigurationBuilder.newCacheConfigurationBuilder(Integer.class, Object.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));
        log.debug("Cache was initialized {}", CACHE);
        //cacheManager.close();
    }

    public static void addToCache(final Object res, final int hash) {
        try {
            CACHE.put(Integer.valueOf(hash), res);
        } catch (CacheException e) {
            log.error("Problem putting Results -->" + res + " in the cache, for key -->" + hash + " Exception:" + e.getMessage());
        }
        
    }

    public static Object getObjectFromCache(final int hash) {
        return CACHE.get(Integer.valueOf(hash));
    }

    public static boolean clearCache() {
        try {
            CACHE.clear();
            log.info("The ldspdi cache has been cleared");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}