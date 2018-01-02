package io.bdrc.ldspdi.parse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StrSubstitutor;

import io.bdrc.ldspdi.rest.resources.PublicDataResource;
import io.bdrc.ldspdi.service.ServiceConfig;

public class QueryFileParser {
	
	private File queryFile;
	private HashMap<String,String> queryMap;
	
	public QueryFileParser(HashMap<String,String> queryMap) {
		this.queryMap=queryMap;
		String queryFilename=ServiceConfig.getProperty(ParserConfig.QUERY_DIR)+queryMap.get("searchType")+".arq";
		this.queryFile = new File(PublicDataResource.class.getClassLoader().getResource(queryFilename).getFile());
		parseFile();
	}
	
	private void parseFile() {
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(queryFile));		
			String readLine = "";	
	
	        while ((readLine = br.readLine()) != null) {
	            System.out.println("Line : "+readLine);
	        }
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public String getQuery() {
		String query="";
		try {
			String tmp= FileUtils.readFileToString(queryFile,"utf-8");
			StrSubstitutor sub = new StrSubstitutor(queryMap);
		    query = sub.replace(tmp);
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}
		return query;
	}
	

}
