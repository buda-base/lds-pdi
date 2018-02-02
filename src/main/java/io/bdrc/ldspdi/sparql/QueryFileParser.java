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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import io.bdrc.ldspdi.service.ServiceConfig;

public class QueryFileParser {
	
	private File queryFile;	
	private HashMap<String,String> metaInf;
	private String query;
	private String queryName;
	private HashMap<String,String> litLangParams=new HashMap<>();
	public static Logger log=Logger.getLogger(QueryFileParser.class.getName());
	
	
	public QueryFileParser(String filename) {
	    log.addHandler(new ConsoleHandler());
		metaInf= new HashMap<>();
		this.queryFile = new File(ServiceConfig.getProperty(QueryConstants.QUERY_PATH)+"public/"+filename);
		queryName=filename.substring(0,filename.lastIndexOf("."));
		parseFile();		
	}
	
	//For testing purpose only - to be removed//
	public QueryFileParser(String queryPath, String filename) throws IOException{        
        metaInf= new HashMap<>();
        this.queryFile = new File(queryPath+filename);
        queryName=filename.substring(0,filename.lastIndexOf("."));
        parseFile();        
    }
	
	public String getTemplateName() {
	    return this.queryName;
	}
	
	public HashMap<String, String> getMetaInf() {
		return metaInf;
	}

	private void parseFile() {
		try {
    		BufferedReader br = new BufferedReader(new FileReader(queryFile));		
    		String readLine = "";	
    		query=""; 
    	    while ((readLine = br.readLine()) != null) {	            
    	            readLine=readLine.trim();
    	            if(readLine.startsWith("#")) {
    	            	readLine=readLine.substring(1);
    	            	int index=readLine.indexOf("=");
    	            	if(index!=-1) {
    		            	String info0=readLine.substring(0,index);
    		            	String info1=readLine.substring(index+1).trim();	            	
    		            	metaInf.put(info0,info1);
    	            	}
    	            }
    	            else {
    	            	query=query+" "+readLine;
    	            }
    	            	
    	   }
    	   br.close();
	   }
	   catch(IOException ex){
	       log.log(Level.FINEST, "QueryFile parsing error", ex);
	       ex.printStackTrace();
	   }
	    
	}
	
	public String getQuery() {
		return query;
	}
	
	public String checkQueryArgsSyntax() {
		String check="";
		
		String[] args=metaInf.get(QueryConstants.QUERY_PARAMS).split(Pattern.compile(",").toString());
		List<String> params=Arrays.asList(args);
		for(String arg:args) {
		    if(!arg.equals(QueryConstants.QUERY_NO_ARGS)) {
		        //Param is not a Lang param --> if it's not present in the query --> Send error
		        if(!arg.startsWith(QueryConstants.LITERAL_LG_ARGS_PARAMPREFIX) && query.indexOf("?"+arg)==-1) {
		            check="Arg syntax is incorrect : query does not have a ?"+arg+" variable";
		            return check;
		        }
		        //Param is a Lang param --> check if the corresponding lit param is present 
		        if(arg.startsWith(QueryConstants.LITERAL_LG_ARGS_PARAMPREFIX)) {
		           
		            String expectedLiteralParam=QueryConstants.LITERAL_ARGS_PARAMPREFIX+arg.substring(arg.indexOf("_")+1);		            
    		        if(!params.contains(expectedLiteralParam)) {
    		            check="Arg syntax is incorrect : query does not have a literal variable "+
    		                   expectedLiteralParam+" corresponding to lang "+arg+" variable";
    		            return check;
    		        }
    		        else {
    		            litLangParams.put(expectedLiteralParam, arg); 
    		        }
		        }
		    }
		}
		return check;
	}

    public File getQueryFile() {
        return queryFile;
    }

    public String getQueryName() {
        return queryName;
    }

    public HashMap<String, String> getLitLangParams() {
        return litLangParams;
    }

    public static Logger getLog() {
        return log;
    }
	
	

}
