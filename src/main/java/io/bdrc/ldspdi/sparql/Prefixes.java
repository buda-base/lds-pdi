package io.bdrc.ldspdi.sparql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import io.bdrc.ldspdi.service.ServiceConfig;


public class Prefixes {
    
    public static HashMap<String,String> prefixes;
    
    static {
        try {
            File file=new File(ServiceConfig.getProperty(QueryConstants.QUERY_PATH)+"public/prefixes.txt");                       
            BufferedReader br = new BufferedReader(new FileReader(file));
            String readLine = "";
            prefixes=new HashMap<>();
            while ((readLine = br.readLine()) != null) {
                String tmp=readLine.trim().substring(6).trim();                
                String uri= tmp.trim().substring(tmp.indexOf(':')+1).replace(">","").replace("<", "");
                prefixes.put(tmp.substring(0, tmp.indexOf(':')+1),uri.trim());                
            }
            br.close();
        }catch(IOException ex) {
            
        }
    }
    
    public static String getFullIRI(String prefix) {
        return prefixes.get(prefix);
    }
}
