package io.bdrc.ldspdi.parse;

import java.util.ArrayList;
import java.util.HashMap;

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

public class ParserConfig {
	
	public static String QUERY_PATH="queryPath";
	
	public static String QUERY_SCOPE="QueryScope";
	public static String QUERY_RETURN_TYPE="QueryReturnType";
	public static String QUERY_RESULTS="QueryResults";
	public static String QUERY_PARAMS="QueryParams";	
	public static String QUERY_URL="QueryUrl";
	
	public static String QUERY_NO_ARGS="NONE";
	
	public static final String WORK="Work";
	public static final String PERSON="Person";
	public static final String PLACE="Place";
	public static final String CORPORATION="Corporation";
	public static final String LINEAGE="Lineage";
	public static final String OFFICE="Office";
	public static final String TOPIC="Topic";
	public static final String GENERAL="General";
	
	public static ArrayList<String> queryTypes = new ArrayList<>();;
	public static HashMap<String,Boolean> infoTypes = new HashMap<>();
	
	static {
		queryTypes.add(WORK);
		queryTypes.add(PERSON);
		queryTypes.add(PLACE);
		queryTypes.add(CORPORATION);
		queryTypes.add(LINEAGE);
		queryTypes.add(OFFICE);
		queryTypes.add(TOPIC);
		queryTypes.add(GENERAL);
		
		infoTypes.put(QUERY_SCOPE,true);
		infoTypes.put(QUERY_RETURN_TYPE,true);
		infoTypes.put(QUERY_RESULTS,true);
		infoTypes.put(QUERY_PARAMS,true);		
		infoTypes.put(QUERY_URL,true);
	}	
	
	public static boolean isRequired(String info) {
		return infoTypes.get(info);
	}
	
	public static boolean isValidInfoType(String info) {
		return infoTypes.keySet().contains(info);
	}
	
	public static boolean isValidQueryType(String type) {
		return queryTypes.contains(type);
	}
	
	public static ArrayList<String> requiredInfoType(){
		ArrayList<String> required=new ArrayList<>();
		for(String st:infoTypes.keySet()) {
			if (isRequired(st)) {
				required.add(st);
			}
		}
		return required;
	}
	
	
}
