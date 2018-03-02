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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.objects.json.IntParam;
import io.bdrc.ldspdi.objects.json.Output;
import io.bdrc.ldspdi.objects.json.Param;
import io.bdrc.ldspdi.objects.json.QueryTemplate;
import io.bdrc.ldspdi.objects.json.ResParam;
import io.bdrc.ldspdi.objects.json.StringParam;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.restapi.exceptions.RestException;

public class QueryFileParser {
	
	private File queryFile;	
	private HashMap<String,String> metaInf;
	private String query;
	private String queryHtml;
	private String queryName;
	private HashMap<String,String> litLangParams=new HashMap<>();
	private QueryTemplate template;
	private ArrayList<Param> params;
	private ArrayList<Output> outputs;
	
	public final static Logger log=LoggerFactory.getLogger(QueryFileParser.class.getName());
	
	
	public QueryFileParser(String filename) throws RestException{
	    
		metaInf= new HashMap<>();
		this.queryFile = new File(ServiceConfig.getProperty(QueryConstants.QUERY_PATH)+"public/"+filename);
		queryName=filename.substring(0,filename.lastIndexOf("."));
		parseTemplate();
		template= new QueryTemplate(
                getTemplateName(),
                QueryConstants.QUERY_PUBLIC_DOMAIN,
                Helpers.bdrcEncode(ServiceConfig.getProperty("urlTemplatePath")+metaInf.get(QueryConstants.QUERY_URL)),
                metaInf.get(QueryConstants.QUERY_SCOPE),
                metaInf.get(QueryConstants.QUERY_RESULTS),
                metaInf.get(QueryConstants.QUERY_RETURN_TYPE),
                metaInf.get(QueryConstants.QUERY_PARAMS),
                params, 
                outputs,
                getQuery());
	}
	
	//For testing purpose only - to be removed//
	public QueryFileParser(String queryPath, String filename) throws RestException{        
        metaInf= new HashMap<>();
        this.queryFile = new File(queryPath+filename);
        queryName=filename.substring(0,filename.lastIndexOf("."));
        parseTemplate();
    }
	
	public String getTemplateName() {
	    return this.queryName;
	}
	
	private void parseTemplate() throws RestException{
    	try { 
    	    HashMap<String,HashMap<String,String>> p_map=new HashMap<>();
    	    HashMap<String,HashMap<String,String>> o_map=new HashMap<>();    	       
            String readLine = "";   
            query=""; 
            queryHtml=""; 
            BufferedReader brd = new BufferedReader(new FileReader(queryFile)); 
            while ((readLine = brd.readLine()) != null) {                
                readLine=readLine.trim();
                boolean processed=false;
                if(readLine.startsWith("#")) {
                    
                    readLine=readLine.substring(1);
                    int index=readLine.indexOf("=");
                    if(index!=-1) {
                        String info0=readLine.substring(0,index);
                        String info1=readLine.substring(index+1).trim();
                        if(info0.startsWith(QueryConstants.PARAM)) {                            
                            List<String> parsed=Arrays.asList(info0.split(Pattern.compile("\\.").toString()));                            
                            if(parsed.size()==3 && QueryConstants.isValidInfoType(parsed.get(2))) {
                                HashMap<String,String> mp=p_map.get(parsed.get(1));
                                if(mp==null) {
                                    mp=new HashMap<>();
                                }
                                mp.put(parsed.get(2), info1);
                                p_map.put(parsed.get(1), mp);
                            }else {
                                throw new RestException(500,RestException.GENERIC_APP_ERROR_CODE,"Query template parsing failed, invalid param declaration :"+info0);
                            }
                            processed=true;
                        }
                        if(info0.startsWith(QueryConstants.OUTPUT)) {                            
                            List<String> parsed=Arrays.asList(info0.split(Pattern.compile("\\.").toString()));                            
                            if(parsed.size()==3 && QueryConstants.isValidOutput(parsed.get(2))) {
                                HashMap<String,String> op=o_map.get(parsed.get(1));
                                if(op==null) {
                                    op=new HashMap<>();
                                }
                                op.put(parsed.get(2), info1);
                                o_map.put(parsed.get(1), op);
                            }
                            processed=true;
                        }
                        if(!processed) {
                            metaInf.put(info0,info1);
                        }
                    }
                }
                else {
                    query=query+" "+readLine;
                    queryHtml=queryHtml+" "+readLine+"<br>";
                }
            }
            brd.close();
            queryHtml=queryHtml.substring(15);            
            params=buildParams(p_map);
            outputs=buildOutputs(o_map);            
    	}
        catch(Exception ex){
            log.error("QueryFile parsing error", ex);
            throw new RestException(500,RestException.GENERIC_APP_ERROR_CODE,"Query template parsing failed :"+ex.getMessage());
        }
	}
	
	private ArrayList<Param> buildParams(HashMap<String,HashMap<String,String>> p_map) throws RestException{
	    ArrayList<Param> p=new ArrayList<>();
	    Set<String> names=p_map.keySet();
	    for(String name:names) {
	        HashMap<String,String> mp=p_map.get(name);
	        
    	    switch (mp.get(QueryConstants.PARAM_TYPE)) {
    	        case QueryConstants.STRING_PARAM:
    	            StringParam stp=new StringParam(name);    	            
    	            stp.setLangTag(mp.get(QueryConstants.PARAM_LANGTAG));
    	            stp.setIsLuceneParam(mp.get(QueryConstants.PARAM_LUCENE));
    	            stp.setExample(mp.get(QueryConstants.PARAM_EXAMPLE));
    	            p.add(stp);
    	            break;
    	        case QueryConstants.INT_PARAM:
    	            IntParam intp=new IntParam(name);
    	            intp.setDescription(mp.get(QueryConstants.PARAM_DESC));
    	            p.add(intp);
    	            break;
    	        case QueryConstants.RES_PARAM:    	            
    	            ResParam rtp=new ResParam(name,mp.get(QueryConstants.PARAM_SUBTYPE));
    	            rtp.setDescription(mp.get(QueryConstants.PARAM_DESC));
    	            p.add(rtp);
    	            break;
    	    }
	    }
	    return p;
	}
	
	private ArrayList<Output> buildOutputs(HashMap<String,HashMap<String,String>> o_map) throws RestException{
	    ArrayList<Output> o=new ArrayList<>();
        Set<String> names=o_map.keySet();
        for(String name:names) {
            HashMap<String,String> op=o_map.get(name);
            Output output=new Output(name,
                                    op.get(QueryConstants.OUTPUT_TYPE),
                                    op.get(QueryConstants.OUTPUT_DESC));
            o.add(output);
        }
        return o;
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
                    litLangParams.put(expectedLiteralParam, arg); 
                }
		    }
		}
		return "";
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

    public String getQueryHtml() {
        return queryHtml;
    }

    public QueryTemplate getTemplate() {
        return template;
    }

}
