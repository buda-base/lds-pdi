package io.bdrc.ldspdi.sparql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.jena.riot.system.PrefixMapStd;
import org.apache.jena.shared.PrefixMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;


public class Prefixes {

    public final static Logger log=LoggerFactory.getLogger(Prefixes.class.getName());
    private final static PrefixMapStd pMap=new PrefixMapStd();
    private static String prefixes;
    private static PrefixMapping PREFIXES_MAP;

    static {
        try {
            File file=new File(ServiceConfig.getProperty(QueryConstants.QUERY_PATH)+"public/prefixes.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String readLine = "";
            loadPrefixes();
            PREFIXES_MAP=PrefixMapping.Factory.create();
            while ((readLine = br.readLine()) != null) {
                String tmp=readLine.trim().substring(6).trim();
                String uri= tmp.trim().substring(tmp.indexOf(':')+1).replace(">","").replace("<", "");
                pMap.add(tmp.substring(0, tmp.indexOf(':')+1), uri.trim());
                PREFIXES_MAP.setNsPrefix(tmp.substring(0, tmp.indexOf(':')),uri.trim());
            }
            br.close();
        }catch(IOException | RestException ex) {
            ex.printStackTrace();
            log.debug("Prefixes initialization error", ex);
        }
    }

    public static String getPrefixes() throws RestException {
        return prefixes;
    }

    public static void loadPrefixes() throws RestException {
        try {
            prefixes = new String(Files.readAllBytes(Paths.get(ServiceConfig.getProperty(QueryConstants.QUERY_PATH)+"public/prefixes.txt")));

        } catch (IOException e) {
            throw new RestException(500,new LdsError(LdsError.MISSING_RES_ERR).
                    setContext("Couldn't read prefixes from >> "
            +ServiceConfig.getProperty(QueryConstants.QUERY_PATH)
            +"public/prefixes.txt",e));
        }
    }

    public static PrefixMapStd getPrefixMap() {
        return pMap;
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
