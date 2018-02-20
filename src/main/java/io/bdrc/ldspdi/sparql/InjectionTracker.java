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
import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.jena.query.ParameterizedSparqlString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.service.ServiceConfig;

public class InjectionTracker {    
    
    public static Logger log=LoggerFactory.getLogger(InjectionTracker.class.getName());
    
    public static String getValidQuery(String query,HashMap<String,String> converted,HashMap<String,String> litParams) {
        log.info("Query before injection tracking :"+query);
        ParameterizedSparqlString queryStr = new ParameterizedSparqlString(ServiceConfig.getPrefixes()+" " +query);
        Set<String> s = converted.keySet(); 
        Set<String> lit = litParams.keySet();        
        for(String st:s) {
            
            if(st.startsWith(QueryConstants.INT_ARGS_PARAMPREFIX)) {
                queryStr.setLiteral(st, Integer.parseInt(converted.get(st)));                
            }
            if(st.startsWith(QueryConstants.RES_ARGS_PARAMPREFIX)) {
                String param=converted.get(st);
                if(param.contains(":") && !param.contains("http://")) {
                    String[] parts=param.split(Pattern.compile(":").toString());
                    if(parts[0]==null) {parts[0]="";}
                    queryStr.setIri(st, Prefixes.getFullIRI(parts[0]+":")+parts[1]);
                }else {
                    queryStr.setIri(st, param);
                }
            }
            if(st.startsWith(QueryConstants.LITERAL_ARGS_PARAMPREFIX)) {
                if(lit.contains(st)) {
                    String lang=converted.get(litParams.get(st));
                    try {
                        new Locale.Builder().setLanguageTag(lang).build();
                    }catch(IllformedLocaleException ex) {
                        return "ERROR --> language param :"+lang+" is not a valid BCP 47 language tag"+ex.getMessage();
                    }
                    queryStr.setLiteral(st, converted.get(st),converted.get(litParams.get(st)));                    
                }else {
                    //Some literals do not have a lang associated with them
                    queryStr.setLiteral(st, converted.get(st));                    
                }
            }
        }
        return queryStr.toString();
    }
    
    public static String getValidURLQuery(String query,String param,String type) {
        
        ParameterizedSparqlString queryStr = new ParameterizedSparqlString(ServiceConfig.getPrefixes()+" " +query);
        String first=Character.toString(type.charAt(0)).toUpperCase();
        try {
            param=param.replace("+", " ");
            param=param.replace("/", "\"/");
            queryStr.setLiteral("NAME", param);
            queryStr.setIri("TYPE", "http://purl.bdrc.io/ontology/core/"+first+type.substring(1));
        }catch(Exception ex) {
            return "ERROR --> path param :"+param+" is invalid in Injection Tracker "+ex.getMessage();
        }
        
        return queryStr.toString();
    }
}
