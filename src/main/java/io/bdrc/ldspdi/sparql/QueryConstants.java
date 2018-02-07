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
import java.util.ArrayList;
import java.util.HashMap;


public class QueryConstants {
    
    /********* Results Objects Constants ****************/
    public static String RES_URI="URI";
    public static String PAGE_SIZE="pageSize";
    public static String PAGE_NUMBER="pageNumber";
    public static String RESULT_HASH="hash";
    public static String JSON_OUT="jsonOut";
    public static String REQ_METHOD="method";
    public static String REQ_URI="reqUri";
    public static String QUERY_TYPE="queryType";
    
    //****** URL QUERY TEMPLATE****************/
    public static String URL_TEMPLATE="urlTemplate";
    public static String URL_TEMPLATE_EXACT="urlTemplateExact";
    public static String URL_QUERY="urlQuery";
    
    
    public static String QUERY_ERROR="ERROR";
    public static String QUERY_PATH="queryPath";
	
	public static String QUERY_SCOPE="QueryScope";
	public static String QUERY_PUBLIC_DOMAIN="public";
	public static String QUERY_RETURN_TYPE="QueryReturnType";
	public static String QUERY_RESULTS="QueryResults";
	public static String QUERY_PARAMS="QueryParams";	
	public static String QUERY_URL="QueryUrl";
	public static String SEARCH_TYPE="searchType";
	
	public static String QUERY_NO_ARGS="NONE";
	public static String INT_ARGS_PARAMPREFIX="I_";
	public static String LITERAL_ARGS_PARAMPREFIX="L_";
	public static String LITERAL_LG_ARGS_PARAMPREFIX="LG_";
	public static String RES_ARGS_PARAMPREFIX="R_";	
	
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
