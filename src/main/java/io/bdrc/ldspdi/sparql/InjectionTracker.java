package io.bdrc.ldspdi.sparql;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.jena.query.ParameterizedSparqlString;

import io.bdrc.ldspdi.service.ServiceConfig;

public class InjectionTracker {
    
    public static String getValidQuery(String query,MultivaluedMap<String,String> mvm) {
        
        HashMap<String,String> converted=new HashMap<>();
        Set<String> set=mvm.keySet();
        for(String st:set) {
            List<String> str=mvm.get(st);
            for(String ss:str) {
                converted.put(st, ss);                
            }
        }
        ParameterizedSparqlString queryStr = new ParameterizedSparqlString(ServiceConfig.getPrefixes()+" " +query);
        Set<String> s = converted.keySet();        
        for(String st:s) {
            
            if(st.startsWith(QueryConstants.INT_ARGS_PARAMPREFIX)) {
                queryStr.setLiteral(st, Integer.parseInt(converted.get(st)));                
            }
            if(st.startsWith(QueryConstants.RES_ARGS_PARAMPREFIX)) {
                queryStr.setIri(st, "http://purl.bdrc.io/resource/"+converted.get(st));                
            }
            if(st.startsWith(QueryConstants.LITERAL_ARGS_PARAMPREFIX)) {
                queryStr.setLiteral(st, converted.get(st));
            }
        }
        return queryStr.toString();
    }    
}
