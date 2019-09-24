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
	public final static String RES_URI = "URI";
	public final static String PAGE_SIZE = "pageSize";
	public final static String QS_PAGE_SIZE = "qs_pageSize";
	public final static String PAGE_NUMBER = "pageNumber";
	public final static String RESULT_HASH = "hash";
	public final static String FORMAT = "format";
	public final static String REQ_METHOD = "method";
	public final static String REQ_URI = "reqUri";
	public final static String QUERY_TYPE = "queryType";
	public final static String LIMIT = "limit";
	public final static String QUERY_TIMEOUT = "timeout";

	// ****** URL QUERY TEMPLATE****************/
	public final static String URL_TEMPLATE = "urlTemplate";
	public final static String URL_TEMPLATE_EXACT = "urlTemplateExact";
	public final static String URL_QUERY = "urlQuery";

	public final static String QUERY_ERROR = "ERROR";

	public final static String QUERY_SCOPE = "QueryScope";
	public final static String QUERY_PUBLIC_DOMAIN = "public";
	public final static String QUERY_RETURN_TYPE = "QueryReturnType";
	public final static String QUERY_RESULTS = "QueryResults";
	public final static String QUERY_PARAMS = "QueryParams";
	public final static String QUERY_NO_ARGS = "NONE";
	public final static String QUERY_URL = "QueryUrl";
	public final static String SEARCH_TYPE = "searchType";

	// Query return types
	public final static String TABLE = "Table";
	public final static String VALUE = "Value";
	public final static String GRAPH = "Graph";

	// Query Outputs
	public final static String OUTPUT = "output";
	public final static String OUTPUT_NAME = "name";
	public final static String OUTPUT_TYPE = "type";
	public final static String OUTPUT_DESC = "desc";

	// Query Params
	public final static String PARAM = "param";
	public final static String PARAM_NAME = "name";
	public final static String PARAM_TYPE = "type";
	public final static String PARAM_LANGTAG = "langTag";
	public final static String PARAM_LUCENE = "isLucene";
	public final static String PARAM_EXAMPLE = "example";
	public final static String PARAM_DESC = "desc";
	public final static String PARAM_SUBTYPE = "subtype";

	// Query Params prefixes
	public final static String INT_ARGS_PARAMPREFIX = "I_";
	public final static String LITERAL_ARGS_PARAMPREFIX = "L_";
	public final static String LITERAL_LG_ARGS_PARAMPREFIX = "LG_";
	public final static String LITERAL_LIMITPREFIX = "LI_";
	public final static String RES_ARGS_PARAMPREFIX = "R_";

	// Query Params types
	public static final String INT_PARAM = "int";
	public static final String RES_PARAM = "resource";
	public static final String STRING_PARAM = "string";

	// Resources types
	public static final String WORK = "Work";
	public static final String PERSON = "Person";
	public static final String PLACE = "Place";
	public static final String CORPORATION = "Corporation";
	public static final String LINEAGE = "Lineage";
	public static final String OFFICE = "Office";
	public static final String TOPIC = "Topic";
	public static final String GENERAL = "General";

	final static ArrayList<String> queryTypes = new ArrayList<>();
	final static ArrayList<String> returnTypes = new ArrayList<>();
	final static ArrayList<String> outputs = new ArrayList<>();;
	final static HashMap<String, Boolean> infoTypes = new HashMap<>();

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

		infoTypes.put(QUERY_SCOPE, true);
		infoTypes.put(QUERY_RETURN_TYPE, true);
		infoTypes.put(QUERY_RESULTS, true);
		infoTypes.put(QUERY_URL, true);
		infoTypes.put(PARAM, true);
		infoTypes.put(PARAM_NAME, true);
		infoTypes.put(PARAM_TYPE, true);
		infoTypes.put(PARAM_LANGTAG, true);
		infoTypes.put(PARAM_LUCENE, true);
		infoTypes.put(PARAM_EXAMPLE, true);
		infoTypes.put(PARAM_DESC, true);
		infoTypes.put(PARAM_SUBTYPE, true);

		returnTypes.add(TABLE);
		returnTypes.add(VALUE);
		returnTypes.add(GRAPH);
	}

	public static boolean isValidReturnType(String info) {
		return returnTypes.contains(info);
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

	public static ArrayList<String> requiredInfoType() {
		ArrayList<String> required = new ArrayList<>();
		for (String st : infoTypes.keySet()) {
			if (isRequired(st)) {
				required.add(st);
			}
		}
		return required;
	}
}
