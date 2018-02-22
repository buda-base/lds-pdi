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
    public static String QS_PAGE_SIZE="qs_pageSize";
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
	
	//Query Outputs
	public static String OUTPUT="output";
    public static String OUTPUT_NAME="name";
    public static String OUTPUT_TYPE="type";
    public static String OUTPUT_DESC="desc";
	
	//Query Params
	public static String PARAM="param";
	public static String PARAM_NAME="name";
	public static String PARAM_TYPE="type";
	public static String PARAM_LANGTAG="langTag";
	public static String PARAM_LUCENE="isLucene";
	public static String PARAM_EXAMPLE="example";
	public static String PARAM_DESC="desc";
	public static String PARAM_SUBTYPE="subtype";
		
	//Query Params prefixes
	public static String INT_ARGS_PARAMPREFIX="I_";
	public static String LITERAL_ARGS_PARAMPREFIX="L_";
	public static String LITERAL_LG_ARGS_PARAMPREFIX="LG_";
	public static String RES_ARGS_PARAMPREFIX="R_";
	
	//Query Params types
	public static final String INT_PARAM="int";
	public static final String RES_PARAM="resource";
	public static final String STRING_PARAM="string";
	
	//Resources types
	public static final String WORK="Work";
	public static final String PERSON="Person";
	public static final String PLACE="Place";
	public static final String CORPORATION="Corporation";
	public static final String LINEAGE="Lineage";
	public static final String OFFICE="Office";
	public static final String TOPIC="Topic";
	public static final String GENERAL="General";
	
	public static ArrayList<String> queryTypes = new ArrayList<>();;
	public static ArrayList<String> outputs = new ArrayList<>();;
	public static HashMap<String,Boolean> infoTypes = new HashMap<>();
	
	static {
	    
	    outputs.add(OUTPUT_NAME);
	    outputs.add(OUTPUT_TYPE);
	    outputs.add(OUTPUT_DESC);
	    
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
		infoTypes.put(QUERY_URL,true);
		infoTypes.put(PARAM,true);
		infoTypes.put(PARAM_NAME,true);
		infoTypes.put(PARAM_TYPE,true);
		infoTypes.put(PARAM_LANGTAG,true);
		infoTypes.put(PARAM_LUCENE,true);
		infoTypes.put(PARAM_EXAMPLE,true);
		infoTypes.put(PARAM_DESC,true);
		infoTypes.put(PARAM_SUBTYPE,true);
	}
	
	public static boolean isRequired(String info) {
		return infoTypes.get(info);
	}
	
	public static boolean isValidInfoType(String info) {
		return infoTypes.keySet().contains(info);
	}
	public static boolean isValidOutput(String info) {
        return outputs.contains(info);
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
