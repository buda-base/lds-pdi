package io.bdrc.ldspdi.sparql;

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
