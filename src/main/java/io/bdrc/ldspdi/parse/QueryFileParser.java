package io.bdrc.ldspdi.parse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StrSubstitutor;

import io.bdrc.ldspdi.rest.resources.PublicDataResource;
import io.bdrc.ldspdi.service.ServiceConfig;

public class QueryFileParser {
	
	private File queryFile;
	private HashMap<String,String> queryMap;
	private HashMap<String,String> metaInf;
	private String query;
	
	public QueryFileParser(HashMap<String,String> queryMap) throws IOException,PdiQueryParserException{
		this.queryMap=queryMap;
		metaInf= new HashMap<>();
		String queryFilename=ServiceConfig.getProperty(ParserConfig.QUERY_DIR)+queryMap.get("searchType")+".arq";
		try {
		this.queryFile = new File(PublicDataResource.class.getClassLoader().getResource(queryFilename).getFile());
		}
		catch(Exception ex) {
			throw new PdiQueryParserException("PdiQueryParserException : parser was unable to read File->"+ queryMap.get("searchType")+".arq  ");
			   
		}
		parseFile();		
	}
	
	public QueryFileParser(String filename) throws IOException,PdiQueryParserException{		
		metaInf= new HashMap<>();
		String queryFilename=ServiceConfig.getProperty(ParserConfig.QUERY_DIR)+filename;
		try {
		this.queryFile = new File(PublicDataResource.class.getClassLoader().getResource(queryFilename).getFile());
		}
		catch(Exception ex) {
			throw new PdiQueryParserException("PdiQueryParserException : parser was unable to read File->"+ queryMap.get("searchType")+".arq  ");
			   
		}
		parseFile();		
	}
	
	
	public HashMap<String, String> getMetaInf() {
		return metaInf;
	}

	private void parseFile() throws IOException,PdiQueryParserException{
		
		BufferedReader br = new BufferedReader(new FileReader(queryFile));		
		String readLine = "";	
		query=""; 
	    while ((readLine = br.readLine()) != null) {	            
	            readLine=readLine.trim();
	            if(readLine.startsWith("#")) {
	            	readLine=readLine.substring(1);
	            	int index=readLine.indexOf("=");
	            	String info0=readLine.substring(0,index);
	            	String info1=readLine.substring(index+1);	            	
	            	metaInf.put(info0,info1);	            	
	            }
	            else {
	            	query=query+" "+readLine;
	            }
	            	
	   }
	   br.close();
	   String missingInfo=missingRequiredInfo();
	   
	   if(missingInfo.length()>0) {
	       String msg=missingInfo+" is missing but is a required information field";
	       throw new PdiQueryParserException("PdiQueryParserException : File->"+ queryMap.get("searchType")+".arq; ERROR: "+msg);
	   }
	}		
	
	
	private String missingRequiredInfo() {
		ArrayList<String> required=ParserConfig.requiredInfoType();
		String test="";
		for(String st:required) {
			
			if(!metaInf.keySet().contains(st)) {
				test=st;
			}
		}
		return test;
	}
	
	public String getQuery() {
		return query;
	}
	
	public String checkQueryArgsSyntax() {
		String check="";
		
		String[] args=metaInf.get(ParserConfig.QUERY_PARAMS).split(Pattern.compile(",").toString());
		for(String arg:args) {
			if((!arg.trim().equals(ParserConfig.QUERY_NO_ARGS)) && query.indexOf("${"+arg.trim()+"}")==-1) {
				check="Arg syntax is incorrect : query does not have a ${"+arg.trim()+"} placeholder";
				return check;
			}
		}
		return check;
	}
	

}
