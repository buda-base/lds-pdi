package io.bdrc.ldspdi.sparql;

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
        return getValidQuery(query,converted);
    }
    
    public static String getValidQuery(String query,HashMap<String,String> converted) {
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
