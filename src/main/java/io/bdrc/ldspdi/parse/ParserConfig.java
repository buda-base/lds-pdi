package io.bdrc.ldspdi.parse;

import java.util.ArrayList;
import java.util.HashMap;

public class ParserConfig {
	
	public static String QUERY_DIR="queryDir";
	
	public static String QUERY_SCOPE="QueryScope";
	public static String QUERY_RETURN_TYPE="QueryReturnType";
	public static String QUERY_RESULTS="QueryResults";
	public static String QUERY_PARAMS="QueryParams";
	public static String QUERY_TYPE_DESC="Allowed QueryTypes are :";
	
	public static String QUERY_NO_ARGS="NONE";
	
	public static final String WORK="Work";
	public static final String PERSON="Person";
	public static final String PLACE="Place";
	public static final String CORPORATION="Corporation";
	public static final String LINEAGE="Lineage";
	public static final String OFFICE="Office";
	public static final String TOPIC="Topic";
	public static final String GENERAL="Topic";
	
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
		infoTypes.put(QUERY_TYPE_DESC,false);
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
