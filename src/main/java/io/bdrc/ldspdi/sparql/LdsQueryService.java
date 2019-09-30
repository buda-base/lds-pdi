package io.bdrc.ldspdi.sparql;

import java.util.HashMap;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.restapi.exceptions.RestException;

public class LdsQueryService {
    private static final HashMap<String, LdsQuery> instances = new HashMap<>();

    public static void clearCache() {
        instances.clear();
    }

    private static LdsQuery getFromFilePath(final String filePath) throws RestException {
        LdsQuery res = instances.get(filePath);
        if (res != null)
            return res;
        res = new LdsQuery(filePath);
        instances.put(filePath, res);
        return res;
    }

    public static LdsQuery get(final String fileName, String type) throws RestException {
        if (type == null)
            type = "public";
        final String filePath = ServiceConfig.LOCAL_QUERIES_DIR + type + "/" + fileName;
        return getFromFilePath(filePath);
    }

    public static LdsQuery get(final String fileName) throws RestException {
        final String filePath = ServiceConfig.LOCAL_QUERIES_DIR + "public/" + fileName;
        return getFromFilePath(filePath);
    }
}
