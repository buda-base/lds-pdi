package io.bdrc.ldspdi.utils;

import java.io.IOException;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;

import com.fasterxml.jackson.databind.JsonNode;
import com.maxmind.db.NodeCache;

public class JCSNodeCache implements NodeCache {

    private CacheAccess<String, Object> jcscache;
    
    public JCSNodeCache() {
        jcscache = JCS.getInstance("geoloc");
    }

    @Override
    public JsonNode get(int key, Loader loader) throws IOException {
        String k = Integer.toString(key);
        JsonNode value = (JsonNode) jcscache.get(k);
        if (value == null) {
            value = loader.load(key);
            jcscache.put(k, value);
        }
        return value;
    }
    
}