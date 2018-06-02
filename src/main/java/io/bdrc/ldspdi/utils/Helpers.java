package io.bdrc.ldspdi.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.text.StrSubstitutor;
import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedMap;

import io.bdrc.ldspdi.sparql.functions.Wylie;
import io.bdrc.restapi.exceptions.RestException;


public class Helpers {
    
    public final static Logger log=LoggerFactory.getLogger(Helpers.class.getName());
	
	public static String removeAccents(String text) {		
		String f=text;
		return f == null ? null :
	        Normalizer.normalize(f, Form.NFD)
	            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}
	public static boolean isTibUni(String s) {		
		return s.matches("[\u0f00-\u0fff]+");
	}
	
	public static boolean isWylie(String s) {		
		 Wylie wl = new Wylie(true, false, false, true);
		 ArrayList<String> warn=new ArrayList<>();
		 wl.fromWylie(s, warn);
		 return warn.size()==0;
	}
	
	public static String bdrcEncode(String url) {
		String encoded=url.replace("\"", "%22");
		encoded=encoded.replace(' ', '+');
		encoded=encoded.replace("\'", "%27");
		return encoded;
	}
	
	public static boolean isValidURI(String uri) {
	    String[] schemes = {"http","https"};
	    UrlValidator urlValidator = new UrlValidator(schemes);
	    return urlValidator.isValid(uri);                
    }
	
	public static HashMap<String,String> convertMulti(MultivaluedMap<String,String> map){
        HashMap<String,String> copy=new HashMap<>();
        Set<String> set=map.keySet();
        for(String key:set) {
            copy.put(key, map.getFirst(key));
        }
        return copy;
    }
	
	public static String relativizeURL(String uri) {
        uri=uri.substring(8);
        return uri.substring(uri.indexOf("/"));
    }
	
	public static String getMultiChoicesHtml(String path) throws RestException {
	    InputStream stream = Helpers.class.getClassLoader().getResourceAsStream("multiChoice.tpl");
	    BufferedReader buffer = new BufferedReader(new InputStreamReader(stream));
	    StringBuffer sb=new StringBuffer();
	    try {
    	    String line=buffer.readLine();
    	    while(line!=null) {
    	        sb.append(line+System.lineSeparator());
    	        line=buffer.readLine();
                
    	    }
	    } catch (IOException e) {
	        throw new RestException(500,RestException.GENERIC_APP_ERROR_CODE,
	                "Unable to parse the html multi Choices template"+e.getMessage());         
        }
	    String rows="";
	    for(String k:MediaTypeUtils.getExtensionMimeMap().keySet()) {
	        rows=rows+"<tr><td><a href=\""+path+"."+k+"\">"+path+"."+k+"</a><td>"+
	                MediaTypeUtils.getExtensionMimeMap().get(k)+"</td></tr>"+System.lineSeparator();
	    }
	    HashMap<String,String> map=new HashMap<>();
	    map.put("path", path);
	    map.put("rows",rows);
	    StrSubstitutor s=new StrSubstitutor(map);
	    return s.replace(sb);
	}
}
