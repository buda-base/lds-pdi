package io.bdrc.ldspdi.parse;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;




import io.bdrc.ldspdi.service.ServiceConfig;

public class DocFileBuilder {
	
	public static String content="";
	public static HashMap<String,String> specs;
	
	
	
	public static String getContent(String base) throws IOException{	
		ArrayList<String> files=getQueryFiles();
		
		String contents="";
		specs=new HashMap<>();
		String LS=System.lineSeparator();
		
			for(String file:files) {
				
				QueryFileParser qfp=new QueryFileParser(ServiceConfig.getProperty(ParserConfig.QUERY_PATH)+file);
				HashMap<String,String> info=qfp.getMetaInf();
				String queryScope=info.get(ParserConfig.QUERY_SCOPE);
				
				if(specs.containsKey(queryScope)) {
					String tmp=specs.get(queryScope);
					tmp=tmp+"<tr><td><b>"+file.substring(0, file.indexOf("."))+"</b></td>"+LS
							+"<td>"+info.get(ParserConfig.QUERY_RETURN_TYPE)+"</td>"+LS
							+"<td style=\"width:400px;\">"+info.get(ParserConfig.QUERY_RESULTS)+"</td>"+LS
							+"<td>"+info.get(ParserConfig.QUERY_PARAMS)+"</td>"+LS
							+"<td><a href=\""+base+info.get(ParserConfig.QUERY_URL)+"\">"+
							base+info.get(ParserConfig.QUERY_URL)+"</a></td></tr>"+LS;
					specs.put(queryScope, tmp);
					
					
				}else {
					String scope="<h2>"+queryScope+"</h2><table id=\"specs\"><tr>"+LS
							+ "<th>Search type</th>"+LS
							+ "<th>Return type</th>"+LS
							+ "<th>Result set</th>"+LS
							+ "<th>Parameter(s)</th>"+LS
							+ "<th>Url format</th>"+LS
							+ "</tr>"+LS;
					scope=scope+"<tr><td><b>"+file.substring(0, file.indexOf("."))+"</b></td>"+LS
							+"<td>"+info.get(ParserConfig.QUERY_RETURN_TYPE)+"</td>"+LS
							+"<td style=\"width:400px;\">"+info.get(ParserConfig.QUERY_RESULTS)+"</td>"+LS
							+"<td>"+info.get(ParserConfig.QUERY_PARAMS)+"</td>"+LS
							+"<td><a href=\""+base+info.get(ParserConfig.QUERY_URL)+"\">"+
							base+info.get(ParserConfig.QUERY_URL)+"</a></td></tr>"+LS;
					
					specs.put(queryScope, scope);
				}
			}
			Set<String> set=specs.keySet();
			for(String key:set) {
				String val=specs.get(key)+"</table>";
				specs.put(key, val);
				contents=contents+" "+val;
			}		
		
		return contents;
	}
	
	public static ArrayList<String> getQueryFiles() {
		ArrayList<String> files=new ArrayList<>();
		Path dpath = Paths.get(ServiceConfig.getProperty(ParserConfig.QUERY_PATH));		
		if (Files.isDirectory(dpath)) {        
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dpath)) {
                for (Path path : stream) {
                    String tmp=path.toString();
                    files.add(tmp.substring(tmp.lastIndexOf("/")+1));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return files;		
	}

}
