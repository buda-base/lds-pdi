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
import java.util.regex.Pattern;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.restapi.exceptions.RestException;

public class InjectionTracker {    
    
    public final static Logger log=LoggerFactory.getLogger(InjectionTracker.class.getName());
    
    public static String getValidQuery(String query,HashMap<String,String> converted,HashMap<String,String> litParams) 
                throws RestException{        
        ParameterizedSparqlString queryStr = new ParameterizedSparqlString(ServiceConfig.getPrefixes()+" " +query); 
        log.info("HashMap >> "+converted);
        for(String st:converted.keySet()) {            
            if(st.startsWith(QueryConstants.INT_ARGS_PARAMPREFIX)) {
                queryStr.setLiteral(st, Integer.parseInt(converted.get(st)));                
            }
            if(st.startsWith(QueryConstants.RES_ARGS_PARAMPREFIX)) {
                String param=converted.get(st);
                if(param.contains(":")) {
                    if(Helpers.isValidURI(param)) {
                        queryStr.setIri(st,param);
                    }
                    else {
                        String[] parts=param.split(Pattern.compile(":").toString());
                        if(parts[0]==null) {
                            parts[0]="";
                        }                        
                        if(Prefixes.getFullIRI(parts[0]+":")!=null) {
                            queryStr.setIri(st, Prefixes.getFullIRI(parts[0]+":")+parts[1]);
                        }else {
                            throw new RestException(500,RestException.GENERIC_APP_ERROR_CODE,"ParameterException :"+param,
                                    "Unknown prefix","");
                        }
                    }
                }
                else {
                    throw new RestException(500,RestException.GENERIC_APP_ERROR_CODE,"ParameterException :"+param,
                            "This parameter must be of the form prefix:resource or spaceNameUri/resource","");
                }
                    
            }
            if(st.startsWith(QueryConstants.LITERAL_ARGS_PARAMPREFIX)) {
                if(litParams.keySet().contains(st)) {
                    String lang=converted.get(litParams.get(st)).toLowerCase();
                    try {
                        new Locale.Builder().setLanguageTag(lang).build();
                    }catch(IllformedLocaleException ex) {
                        return "ERROR --> language param :"+lang+" is not a valid BCP 47 language tag"+ex.getMessage();
                    }                    
                    queryStr.setLiteral(st, converted.get(st),lang);                    
                }else {                    
                    //Some literals do not have a lang associated with them
                    queryStr.setLiteral(st, converted.get(st));                    
                }
            }
        }
        Query q=queryStr.asQuery();        
        long limit_max=Long.parseLong(ServiceConfig.getProperty(QueryConstants.LIMIT));
        if(q.hasLimit()) {
            if(q.getLimit()>limit_max) {
                q.setLimit(limit_max);
            }
        }else {
            q.setLimit(limit_max);
        }
        return q.toString();
        
    }
    
    public static String getValidURLQuery(String query,String param,String type) {
        
        ParameterizedSparqlString queryStr = new ParameterizedSparqlString(ServiceConfig.getPrefixes()+" " +query);        
        try {
            param=param.replace("+", " ");
            param=param.replace("/", "\"/");
            queryStr.setLiteral("NAME", param);
            queryStr.setIri("TYPE", "http://purl.bdrc.io/ontology/core/"+Character.toString(type.charAt(0)).toUpperCase()+type.substring(1));
        }catch(Exception ex) {
            return "ERROR --> path param :"+param+" is invalid in Injection Tracker "+ex.getMessage();
        }
        
        return queryStr.toString();
    }
}
