package io.bdrc.ldspdi.test;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TestUtils {
	
	final static String TESTDIR = "src/test/";
	final static String TmpDIR = "/tmp";
	
	public static String prefixes=
			 "PREFIX : <http://purl.bdrc.io/ontology/core/> "
			+" PREFIX adm: <http://purl.bdrc.io/ontology/admin/> "
			+" PREFIX bdr: <http://purl.bdrc.io/resource/> "
			+" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
			+" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"				
			+" PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "
			+" PREFIX tbr: <http://purl.bdrc.io/ontology/toberemoved/>"
			+" PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>";
	
	public static String placePrefixes=
			 "PREFIX : <http://purl.bdrc.io/ontology/core/> "
			+" PREFIX adm: <http://purl.bdrc.io/ontology/admin/> "
			+" PREFIX bdr: <http://purl.bdrc.io/resource/> "
			+" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
			+" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"				
			+" PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "
			+" PREFIX tbr: <http://purl.bdrc.io/ontology/toberemoved/>"
			+" PREFIX vcard: <http://www.w3.org/2006/vcard/ns#>"
			+" PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>";
	
	public static String convertToString(Map<String,String> prefixMap){
		String pref="";
		Set<String> keys=prefixMap.keySet();
		for (String key : keys){
			String tmp=" PREFIX "+key+ ": <"+prefixMap.get(key)+"> ";//+System.lineSeparator();
			pref=pref+tmp;
		}
		return pref;
	}
	
	static HashMap<String,String> getContentTypes(){
		HashMap<String,String> map=new HashMap<>();
		map.put("ttl", "text/turtle");
		map.put("nt", "application/n-triples");
		map.put("nq", "application/n-quads");
		map.put("trig", "text/trig");
		map.put("rdf", "application/rdf+xml");
		map.put("owl", "application/owl+xml");
		map.put("jsonld", "application/ld+json");
		map.put("rt", "application/rdf+thrift");
		map.put("trdf", "application/rdf+thrift");
		map.put("rj", "application/json");
		map.put("json", "application/json");
		map.put("trix", "application/trix+xml");
		return map;
	}
	
	static ArrayList<String> getResourcesList(){
		ArrayList<String> res=new ArrayList<>();
		res.add("R8LS12819");
		res.add("C68");
		
		res.add("I00KG01506_I001");
		res.add("L1RKL907");
		res.add("P1AG29");
		res.add("G1KR1355");
		res.add("W1FPL3356");
		//res.add("UT23637_006_0000");
		return res;
	}

}
