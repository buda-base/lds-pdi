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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.bdrc.ldspdi.sparql.QueryConstants;

public class ServiceConfig {
	
	static Properties prop = new Properties();
	static ArrayList<String> mime=new ArrayList<String>();
	public static Writer logWriter;
	public static String sparqlPrefixes="";
	public static HashMap<String,String> params;
	public final static String FUSEKI_URL="fusekiUrl";
	public static Logger log=Logger.getLogger(ServiceConfig.class.getName());
	public static String JSONLD_CONTEXT_HTML;
	public static String JSONLD_CONTEXT;
		
	public static void init(HashMap<String,String> params) {
	    ServiceConfig.params=params;
	    log.addHandler(new ConsoleHandler());
	    try {
			InputStream input = ServiceConfig.class.getClassLoader().getResourceAsStream("ldspdi.properties");
			// load a properties file
			prop.load(input);
			Set<String> set=params.keySet();
			for(String st:set) {
			    prop.setProperty(st, params.get(st));
			}
			logWriter = new PrintWriter(new OutputStreamWriter(System.err, "UTF-8"));		
			String mimes=prop.getProperty("mime");
			StringTokenizer st=new StringTokenizer(mimes,",");
			while(st.hasMoreTokens()){
				mime.add(st.nextToken());
			}			
			sparqlPrefixes=new String(Files.readAllBytes(Paths.get(params.get(QueryConstants.QUERY_PATH)+"public/prefixes.txt")));
			JSONLD_CONTEXT_HTML=setJsonLDContext(true);	
			JSONLD_CONTEXT=setJsonLDContext(false);
			
	    } catch (IOException ex) {
		    log.log(Level.FINEST, "ServiceConfig init error", ex);
			ex.printStackTrace();
		}
	}
	
	public static void initForTests() {
        
        try {
            InputStream input = ServiceConfig.class.getClassLoader().getResourceAsStream("ldspdi.properties");
            // load a properties file
            prop.load(input);
            
            logWriter = new PrintWriter(new OutputStreamWriter(System.err, "UTF-8"));       
            String mimes=prop.getProperty("mime");
            StringTokenizer st=new StringTokenizer(mimes,",");
            while(st.hasMoreTokens()){
                mime.add(st.nextToken());
            }          
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
	
	private static String setJsonLDContext(boolean html) throws MalformedURLException, IOException {
        
	    URL url = new URL("https://raw.githubusercontent.com/BuddhistDigitalResourceCenter/owl-schema/master/context.jsonld");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();        
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String st="";        
        String line="";
        while ((line = in.readLine()) != null) {
            if(html) {
                st=st+line+"<br>";
            }else {
                st=st+line;
            }
        }
        in.close();        
        return st;
    }
	
	public static boolean isValidMime(String mimeString){
		return mime.contains(mimeString);
	}
	
	public static String getProperty(String key){
		return prop.getProperty(key);
	}
	
	public static String getPrefixes(){
		return sparqlPrefixes;
	}
	
}
