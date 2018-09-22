package io.bdrc.ldspdi.sparql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapStd;
import org.apache.jena.shared.PrefixMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;


public class Prefixes {

    public final static Logger log = LoggerFactory.getLogger(Prefixes.class.getName());
    private final static PrefixMap pMap = new PrefixMapStd();
    private static String prefixesString;
    private final static PrefixMapping PREFIXES_MAP = PrefixMapping.Factory.create();

    static {
        try {
            loadPrefixes();
        }catch(RestException ex) {
            log.error("Prefixes initialization error", ex);
        }
    }

    public static String getPrefixesString() throws RestException {
        return prefixesString;
    }

    public static void loadPrefixes() throws RestException {
        loadPrefixes(ServiceConfig.getProperty(QueryConstants.QUERY_PATH)+"public/prefixes.txt");
    }

    public static void loadPrefixes(final String filePath) throws RestException {
        try {
            log.info("reading prefixes from {}", filePath);
            final File file = new File(filePath);
            final BufferedReader br = new BufferedReader(new FileReader(file));
            final StringBuilder sb = new StringBuilder();
            PREFIXES_MAP.clearNsPrefixMap();
            pMap.clear();
            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
                if (line.length() < 10 || line.startsWith("#"))
                    continue;
                final String uri = line.substring(line.indexOf('<')+1, line.indexOf('>'));
                final String prefix = line.substring(7, line.indexOf(':')).trim();
                pMap.add(prefix, uri);
                PREFIXES_MAP.setNsPrefix(prefix, uri);
            }
            prefixesString = sb.toString();
            br.close();
        } catch (IOException e) {
            throw new RestException(500,new LdsError(LdsError.MISSING_RES_ERR).
                    setContext("Couldn't read prefixes from "
                            +filePath
                            +"public/prefixes.txt",e));
        }
    }

    public static PrefixMap getPrefixMap() {
        return pMap;
    }

    public static PrefixMapping getPrefixMapping() {
        return PREFIXES_MAP;
    }

    public static String getFullIRI(String prefix) {
        if(prefix!=null) {
            return PREFIXES_MAP.getNsPrefixURI(prefix);
        }
        return null;
    }

    public static String getPrefix(String IRI) {
        if(IRI!=null) {
            return PREFIXES_MAP.getNsURIPrefix(IRI);
        }
        return "";
    }

    public static String getPrefixedIRI(String IRI) {
        if(IRI!=null) {
            return PREFIXES_MAP.shortForm(IRI);
        }
        return "";
    }

}
