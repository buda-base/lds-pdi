package io.bdrc.ldspdi.service;

/*******************************************************************************
 * Copyright (c) 2017 Buddhist Digital Resource Center (BDRC)
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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;


public class ServiceConfig {
	
	static Properties prop = new Properties();
	static ArrayList<String> mime=new ArrayList<String>();
	public static Writer logWriter;
	
	static{
		try {
			InputStream input = ServiceConfig.class.getClassLoader().getResourceAsStream("ldspdi.properties");
			// load a properties file
			prop.load(input);
			logWriter = new PrintWriter(new OutputStreamWriter(System.err, "UTF-8"));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		String mimes=prop.getProperty("mime");
		StringTokenizer st=new StringTokenizer(mimes,",");
		while(st.hasMoreTokens()){
			mime.add(st.nextToken());
		}
	}
	
	public static boolean isValidMime(String mimeString){
		return mime.contains(mimeString);
	}
	
	public static String getProperty(String key){
		return prop.getProperty(key);
	}		
	
}
