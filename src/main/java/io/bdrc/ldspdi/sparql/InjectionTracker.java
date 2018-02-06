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
import java.util.logging.Logger;

import org.apache.jena.query.ParameterizedSparqlString;

import io.bdrc.ldspdi.rest.resources.PublicDataResource;
import io.bdrc.ldspdi.service.ServiceConfig;

public class InjectionTracker {    
    
    public static Logger log=Logger.getLogger(PublicDataResource.class.getName());
    
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
                queryStr.setIri(st, "http://purl.bdrc.io/resource/"+converted.get(st));                
            }
            if(st.startsWith(QueryConstants.LITERAL_ARGS_PARAMPREFIX)) {
                if(lit.contains(st)) {
                    String lang=converted.get(litParams.get(st));
                    try {
                        new Locale.Builder().setLanguageTag(lang).build();
                    }catch(IllformedLocaleException ex) {
                        return "ERROR --> language param :"+lang+" is not a valid BCP 47 language tag";
                    }
                    queryStr.setLiteral(st, converted.get(st),converted.get(litParams.get(st)));
                    log.info("Setting literal st:"+st+ " with value:"+converted.get(st)+" lang:"+converted.get(litParams.get(st)));
                }else {
                    //Some literals do not have a lang associated with them
                    queryStr.setLiteral(st, converted.get(st));
                    log.info("Setting literal without specified lang: st="+st+ " with value:"+converted.get(st));
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
            queryStr.setLiteral("URL", param);
            queryStr.setIri("TYPE", "http://purl.bdrc.io/ontology/core/"+first+type.substring(1));
        }catch(Exception ex) {
            return "ERROR --> path param :"+param+" is invalid in Injection Tracker";
        }
        
        return queryStr.toString();
    }
}
