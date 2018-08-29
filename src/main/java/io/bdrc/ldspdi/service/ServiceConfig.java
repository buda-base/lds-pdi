package io.bdrc.ldspdi.service;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServiceConfig {
    
    static Properties prop = new Properties();  
    public static HashMap<String,String> params;
    public final static String FUSEKI_URL="fusekiUrl";
    public final static Logger log=LoggerFactory.getLogger(ServiceConfig.class.getName());
    
    public static void init(HashMap<String,String> params) {
        ServiceConfig.params=params;
        
        try {
            InputStream input = ServiceConfig.class.getClassLoader().getResourceAsStream("ldspdi.properties");
            // load a properties file
            prop.load(input);
            input.close();
            /** 
             * sets the PROD values of fuseki and queryPath properties 
             * Overrides test queryPath value            * 
            **/
            Set<String> set=params.keySet();
            for(String st:set) {
                prop.setProperty(st, params.get(st));
            } 
            InputStream in = ServiceConfig.class.getClassLoader().getResourceAsStream("taxTreeContext.jsonld");
            in.close();
            
        } catch (IOException ex) {
            log.error("ServiceConfig init error", ex);
            ex.printStackTrace();
        }
    }
    
    public static void initForTests(String fusekiUrl) {
        try {
            InputStream input = ServiceConfig.class.getClassLoader().getResourceAsStream("ldspdi.properties");
            // load a properties file
            prop.load(input);
            input.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        prop.setProperty(FUSEKI_URL, fusekiUrl);
    }
    
    public static String getProperty(String key){
        return prop.getProperty(key);
    }
    
    public static String getRobots() {
        return "User-agent: *"+System.lineSeparator()+"Disallow: /";
    }
    
    
}