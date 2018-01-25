package io.bdrc.ldspdi.Utils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import io.bdrc.ldspdi.Utils.StringHelpers;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ldspdi.sparql.QueryFileParser;

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

public class DocFileBuilder {
	
	public static String content="";
	public static HashMap<String,String> specs;
	
	
	
	public static String getContent(String base) throws IOException{	
		ArrayList<String> files=getQueryFiles();
		
		String contents="";
		specs=new HashMap<>();
		String LS=System.lineSeparator();
		
			for(String file:files) {
				
				QueryFileParser qfp=new QueryFileParser(file);
				HashMap<String,String> info=qfp.getMetaInf();
				String queryScope=info.get(QueryConstants.QUERY_SCOPE);
				String url=info.get(QueryConstants.QUERY_URL);				
				String return_type=info.get(QueryConstants.QUERY_RETURN_TYPE);
				String query_params=info.get(QueryConstants.QUERY_PARAMS);
				String query_results=info.get(QueryConstants.QUERY_RESULTS);	
				if(specs.containsKey(queryScope)) {
				    //adds a row to the existing table
					String tmp=specs.get(queryScope);
					tmp=tmp+"<tr><td><b>"+file.substring(0, file.indexOf("."))+"</b></td>"+LS
							+"<td>"+return_type+"</td>"+LS
							+"<td style=\"width:400px;\">"+query_results+"</td>"+LS
							+"<td>"+query_params+"</td>"+LS
							+"<td><a href=\""+base+StringHelpers.bdrcEncode(url)+"\">"+
							base+url+"</a></td></tr>"+LS;
					specs.put(queryScope, tmp);
				}else {
				    //creates the table for the given scope and add the first row
					String scope="<h2>"+queryScope+"</h2><table id=\"specs\"><tr>"+LS
							+ "<th>Search type</th>"+LS
							+ "<th>Return type</th>"+LS
							+ "<th>Result set</th>"+LS
							+ "<th>Parameter(s)</th>"+LS
							+ "<th>Url format</th>"+LS
							+ "</tr>"+LS;
					scope=scope+"<tr><td><b>"+file.substring(0, file.indexOf("."))+"</b></td>"+LS
							+"<td>"+return_type+"</td>"+LS
							+"<td style=\"width:400px;\">"+query_results+"</td>"+LS
							+"<td>"+query_params+"</td>"+LS
							+"<td><a href=\""+base+StringHelpers.bdrcEncode(url)+"\">"+
							base+url+"</a></td></tr>"+LS;
					
					specs.put(queryScope, scope);
				}
			}
			Set<String> set=specs.keySet();
			for(String key:set) {
			    //ends each scope table
				String val=specs.get(key)+"</table>";
				specs.put(key, val);
				contents=contents+" "+val;
			}		
		
		return contents;
	}
	
	public static ArrayList<String> getQueryFiles() {
		ArrayList<String> files=new ArrayList<>();
		Path dpath = Paths.get(ServiceConfig.getProperty(QueryConstants.QUERY_PATH)+"public");		
		if (Files.isDirectory(dpath)) {        
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dpath)) {
                for (Path path : stream) {
                    String tmp=path.toString();
                    //Filtering arq files
                    if(tmp.endsWith(".arq")) {
                    	files.add(tmp.substring(tmp.lastIndexOf("/")+1));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return files;		
	}

}