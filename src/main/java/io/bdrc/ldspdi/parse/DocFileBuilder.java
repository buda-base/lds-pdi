package io.bdrc.ldspdi.parse;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StrSubstitutor;

import io.bdrc.ldspdi.rest.resources.PublicDataResource;
import io.bdrc.ldspdi.service.ServiceConfig;

public class DocFileBuilder {
	
	public static String content="";
	public static HashMap<String,String> specs;
	
	static {
		try {
			File html = new File(PublicDataResource.class.getClassLoader().getResource("welcome.html").getFile());
			content=FileUtils.readFileToString(html, "utf-8");
			
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public static String getContent() {	
		ArrayList<String> files=getQueryFiles();
		String contents="";
		specs=new HashMap<>();
		String LS=System.lineSeparator();
		try {
			for(String file:files) {
				System.out.println("Processing :"+file);
				QueryFileParser qfp=new QueryFileParser(file);
				HashMap<String,String> info=qfp.getMetaInf();
				String queryScope=info.get(ParserConfig.QUERY_SCOPE);
				System.out.println("QueryScope="+queryScope);
				if(specs.containsKey(queryScope)) {
					String tmp=specs.get(queryScope);
					tmp=tmp+"<tr><td><b>"+file.substring(0, file.indexOf("."))+"</b></td>"+LS
							+"<td>"+info.get(ParserConfig.QUERY_RETURN_TYPE)+"</td>"+LS
							+"<td style=\"width:400px;\">"+info.get(ParserConfig.QUERY_RESULTS)+"</td>"+LS
							+"<td>"+info.get(ParserConfig.QUERY_PARAMS)+"</td>"+LS
							+"<td>"+info.get(ParserConfig.QUERY_URL)+"</td></tr>"+LS;
					specs.put(queryScope, tmp);
					System.out.println("Added QueryScope="+queryScope+ " value="+tmp);
					
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
							+"<td>"+info.get(ParserConfig.QUERY_URL)+"</td></tr>"+LS;
					System.out.println("Added QueryScope="+queryScope+ " value="+scope);
					specs.put(queryScope, scope);
				}
			}
			Set<String> set=specs.keySet();
			for(String key:set) {
				String val=specs.get(key)+"</table>";
				specs.put(key, val);
				contents=contents+" "+val;
			}
			
		}
		catch(PdiQueryParserException ex) {
			return ex.getMessage();
		}
		catch(IOException ex) {
			return ex.getMessage();
		}
		System.out.println("In Builder: "+contents);
		return contents;
	}
	
	public static ArrayList<String> getQueryFiles() {
		ArrayList<String> files=new ArrayList<>();
		Path directoryPath = Paths.get(ServiceConfig.getProperty("queryPath"));
		System.out.println("Directory :"+directoryPath.toAbsolutePath());
        if (Files.isDirectory(directoryPath)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath)) {
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
	
	public static void main(String[] args) {
		//System.out.println(DocFileBuilder.getQueryFiles());
		System.out.println(DocFileBuilder.getContent());
	}

}
