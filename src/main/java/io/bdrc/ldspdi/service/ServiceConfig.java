package io.bdrc.ldspdi.service;

import java.io.BufferedReader;

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
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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
	public static String JSONLD_CONTEXT;
		
	public static void init(HashMap<String,String> params) {
	    ServiceConfig.params=params;
	    
	    try {
			InputStream input = ServiceConfig.class.getClassLoader().getResourceAsStream("ldspdi.properties");
			// load a properties file
			prop.load(input);
			Set<String> set=params.keySet();
			for(String st:set) {
			    prop.setProperty(st, params.get(st));
			}	
					
			JSONLD_CONTEXT=readGithubJsonLDContext();
			input.close();
			
			InputStream in = ServiceConfig.class.getClassLoader().getResourceAsStream("taxTreeContext.jsonld");
			in.close();
			
	    } catch (IOException ex) {
		    log.error("ServiceConfig init error", ex);
			ex.printStackTrace();
		}
	}
	
	public static void initForTests() {
        
        try {
            InputStream input = ServiceConfig.class.getClassLoader().getResourceAsStream("ldspdi.properties");
            // load a properties file
            prop.load(input);            
            input.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
	
	private static String readGithubJsonLDContext() throws MalformedURLException, IOException {
        
	    URL url = new URL("https://raw.githubusercontent.com/BuddhistDigitalResourceCenter/owl-schema/master/context.jsonld");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();        
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder st = new StringBuilder();        
        String line;
        while ((line = in.readLine()) != null) {
            st.append(line+"\n");
        }
        in.close();        
        return st.toString();
    }
	
	
	public static String getProperty(String key){
		return prop.getProperty(key);
	}
	
    public static String getRobots() {
	    return "User-agent: *"+System.lineSeparator()+"Disallow: /";
	}
	
	
}
