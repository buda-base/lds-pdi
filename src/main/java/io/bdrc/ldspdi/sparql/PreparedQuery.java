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

import org.apache.commons.text.StrSubstitutor;

public class PreparedQuery {
	
	private boolean lucene=false;
	private String prep_query="";
	
	public PreparedQuery(String query, MultivaluedMap<String,String> mvm) {
		if(query.contains("text:query")) {
			lucene=true;
		}
		HashMap<String,String> converted=new HashMap<>();
		Set<String> set=mvm.keySet();
		for(String st:set) {
			List<String> str=mvm.get(st);
			for(String ss:str) {
				if(lucene) {
					converted.put(st, ss.replace("\'", "\\'"));
				}
				else {
					converted.put(st, ss);
				}
			}
		}
		StrSubstitutor sub = new StrSubstitutor(converted);
		prep_query = sub.replace(query);		
	}
	
	public String getPreparedQuery() {
		return prep_query;
	}

}
