package io.bdrc.ldspdi.sparql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.apache.jena.riot.system.PrefixMapStd;

import io.bdrc.ldspdi.service.ServiceConfig;


public class Prefixes {
    
    private final static HashMap<String,String> IRIByprefixes=new HashMap<>();
    private final static HashMap<String,String> prefixesByIRI=new HashMap<>();
    private final static PrefixMapStd pMap=new PrefixMapStd();
    
    static {
        try {
            File file=new File(ServiceConfig.getProperty(QueryConstants.QUERY_PATH)+"public/prefixes.txt");            
            BufferedReader br = new BufferedReader(new FileReader(file));
            String readLine = ""; 
            while ((readLine = br.readLine()) != null) {
                String tmp=readLine.trim().substring(6).trim();                
                String uri= tmp.trim().substring(tmp.indexOf(':')+1).replace(">","").replace("<", "");
                pMap.add(tmp.substring(0, tmp.indexOf(':')+1), uri.trim());
                IRIByprefixes.put(tmp.substring(0, tmp.indexOf(':')+1),uri.trim());
                prefixesByIRI.put(uri.trim(),tmp.substring(0, tmp.indexOf(':')+1));
            }
            br.close();
        }catch(IOException ex) {
            
        }
    }
    
    public static PrefixMapStd getPrefixMap() {        
        return pMap;
    }
    
    public static String getFullIRI(String prefix) {
        return IRIByprefixes.get(prefix);
    }
    
    public static String getPrefix(String IRI) {
        return prefixesByIRI.get(IRI);
    }
    
}
