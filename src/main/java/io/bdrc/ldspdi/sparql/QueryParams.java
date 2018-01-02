package io.bdrc.ldspdi.sparql;

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

import java.util.HashMap;


public class QueryParams {
	
	//Variable names in SPARQL request
	public static final String RES_ID="ID";
	public static final String PREF_LABEL="Preferred_Name";
	public static final String ALT_LABEL="Alt_Name";
	public static final String NAME_TYPE="Name_Type";
	public static final String NAME_LABEL="Name";
	public static final String STUDENT="Student";
	public static final String TEACHER="Teacher";
	public static final String EVENT_TYPE="Event_Type";
	public static final String TITLE_TYPE="Title_Type";
	public static final String CAT_INFO="cat_Info";
	public static final String NOTE="note";
	public static final String NUM_VOLUMES="num_Vol";
	public static final String ACCESS="access";
	public static final String LICENSE="license";
	public static final String STATUS="status";
	
	
	//Parameters coming from the query
	public static final String NAME="name";
	public static final String SEARCH_TYPE="searchType";
	public static final String EXACT_SEARCH="exactSearch";
	public static final String RES_TYPE="Res_type";	
	public static final String SEARCH_LANG="searchLang";
	
	//***************Search Types****************************/
					//Person
	public static final String P_LIST="p_list";
	public static final String P_ALL_NAMES="p_list_all";
	public static final String P_EVENT="p_event";
	public static final String P_TEACH_STUD="p_teach_stud";
	
					//Place
	public static final String PL_LIST="pl_list";
	public static final String PL_ALL_NAMES="pl_list_all";
	public static final String PL_EVENT="pl_event";
	public static final String PL_DETAILS="pl_details";
					
					//Work
	public static final String WK_TITLE="wktitle_list";
	public static final String WK_BIBLIO="wk_bib";
	public static final String WK_ID="wk_id";
	
					//Topic
	public static final String T_LIST="t_list";
	public static final String T_LIST_NAME="t_list_name";
	public static final String T_LIST_ID="t_list_id";
	public static final String TW_LIST_ID="tw_list_id";
	public static final String TW_LIST_NAME="tw_list_name";
	public static final String T_ID="t_id";
	
					//Global (Resource)
	public static final String R_LIST="res_list";
	
					//Alternative techniques
	public static final String ALT_LIST="alt_list";
	
	//************* PDI SEARCH TYPES**********************/
					
	//PDI Person
	public static final String PDI_P_HOLDERS="pdi_p_holders";
	public static final String PDI_P_DOC="pdi_p_doc";
					
	//PDI LINEAGE
	public static final String PDI_L_LIST="pdi_l_list";
	
		
	//Resource Types
	public static final String ALL_RES="all_res";
	public static final String WORK="Work";
	public static final String PERSON="Person";
	public static final String PLACE="Place";
	public static final String CORPORATION="Corporation";
	public static final String LINEAGE="Lineage";
	public static final String OFFICE="Office";
	public static final String TOPIC="Topic";
	
	//Languages
	public static final String ANY_LANG="any_lang";
	public static final String TIB="bo";
	public static final String TIB_EWTS="bo-x-ewts";
	public static final String SKT_IAST="sa-x-iast";
	public static final String SKT_DEVA="sa-Deva";
	public static final String SKT_NDIA="sa-x-ndia";
	public static final String CH_SIMPL="zh-Hans";
	public static final String CH_PINYIN="zh-Latn-pinyin";
	public static final String ENGLISH="en";
	
	//functions
	public static String LEVENSHTEIN="LevenshteinFunction";
	public static String JAROWINKLER="JaroWinklerFunction";
	public static String TO_UNICODE="ToUnicodeFunction";
	public static String TO_WYLIE="ToWylieFunction";
	public static String FUNCTION_PREFIX="http://io.bdrc.org/functions/";
	
	@SuppressWarnings("rawtypes")
	public static final HashMap<String,Class> functionMap = new HashMap<>();
	public static final HashMap<String,QueryType> queryTypeMap = new HashMap<>();
	 
	public static enum QueryType {
	    PERSON_LIST,
	    PERSON_LIST_ALL,
	    PERSON_TEACH_STUD,
	    PERSON_EVENT,
		
	    PLACE_LIST,
	    PLACE_LIST_ALL,
	    PLACE_EVENT,
	    PLACE_DETAILS,
	    
	    WORK_TITLE_LIST,
	    WORK_BIBLIO,
	    
	    TOPIC_LIST,
	    TOPIC_LIST_NAME,
	    TOPIC_LIST_ID,
	    TOPIC_WORK_LIST_ID,
	    TOPIC_WORK_LIST_NAME,
	    
	    RES_LIST,
	    ALTER_LIST,
	    	    
	    GENERAL
	    ;  
	}
	
	//Map of SearchType/QueryType
	static {
		
		//Map of SearchType/QueryType
    	queryTypeMap.put(P_LIST, QueryType.PERSON_LIST);
    	queryTypeMap.put(P_ALL_NAMES, QueryType.PERSON_LIST_ALL);
    	queryTypeMap.put(P_TEACH_STUD, QueryType.PERSON_TEACH_STUD);
    	queryTypeMap.put(P_EVENT, QueryType.PERSON_EVENT); 
    	
    	queryTypeMap.put(PL_LIST, QueryType.PLACE_LIST);
    	queryTypeMap.put(PL_ALL_NAMES, QueryType.PLACE_LIST_ALL);
    	queryTypeMap.put(PL_EVENT, QueryType.PLACE_EVENT); 
    	queryTypeMap.put(PL_DETAILS, QueryType.PLACE_DETAILS);
    	
    	queryTypeMap.put(WK_TITLE, QueryType.WORK_TITLE_LIST); 
    	queryTypeMap.put(WK_BIBLIO, QueryType.WORK_BIBLIO); 
    	
    	queryTypeMap.put(T_LIST, QueryType.TOPIC_LIST); 
    	queryTypeMap.put(T_LIST_NAME, QueryType.TOPIC_LIST_NAME);
    	queryTypeMap.put(T_LIST_ID, QueryType.TOPIC_LIST_ID);
    	queryTypeMap.put(TW_LIST_ID, QueryType.TOPIC_WORK_LIST_ID);
    	queryTypeMap.put(TW_LIST_NAME, QueryType.TOPIC_WORK_LIST_NAME);
    	
    	queryTypeMap.put(R_LIST, QueryType.RES_LIST);
    	queryTypeMap.put(ALT_LIST, QueryType.ALTER_LIST);
    	
    }
	
	public static QueryType getQueryType(String searchType) {		
		return queryTypeMap.get(searchType);
	}
}
