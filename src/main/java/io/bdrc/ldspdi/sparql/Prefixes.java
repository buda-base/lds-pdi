package io.bdrc.ldspdi.sparql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.omg.CORBA.NameValuePair;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.results.Field;


public class Prefixes {
    
    public static HashMap<String,String> IRIByprefixes;
    public static HashMap<String,String> prefixesByIRI;
    
    static {
        try {
            File file=new File(ServiceConfig.getProperty(QueryConstants.QUERY_PATH)+"public/prefixes.txt");                       
            BufferedReader br = new BufferedReader(new FileReader(file));
            String readLine = "";
            IRIByprefixes=new HashMap<>();
            prefixesByIRI=new HashMap<>();
            while ((readLine = br.readLine()) != null) {
                String tmp=readLine.trim().substring(6).trim();                
                String uri= tmp.trim().substring(tmp.indexOf(':')+1).replace(">","").replace("<", "");
                IRIByprefixes.put(tmp.substring(0, tmp.indexOf(':')+1),uri.trim());
                prefixesByIRI.put(uri.trim(),tmp.substring(0, tmp.indexOf(':')+1));
            }
            br.close();
        }catch(IOException ex) {
            
        }
    }
    
    public static String getFullIRI(String prefix) {
        return IRIByprefixes.get(prefix);
    }
    
    public static String getPrefix(String IRI) {
        return prefixesByIRI.get(IRI);
    }
    
}
