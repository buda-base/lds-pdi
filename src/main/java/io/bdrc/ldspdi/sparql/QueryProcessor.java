package io.bdrc.ldspdi.sparql;

import java.util.List;

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

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;

import io.bdrc.ldspdi.service.ServiceConfig;

public class QueryProcessor {
	
	String prefixes=ServiceConfig.getPrefixes();
	
	String baseUri=null;
	
	public Model getResource(String resID,String fusekiUrl,String pref){
		System.out.println("Processor url:" +fusekiUrl);
		if(pref != null){
			prefixes=pref;
		}
		Query q=QueryFactory.create(prefixes+" DESCRIBE <http://purl.bdrc.io/resource/"+resID.trim()+">");
		System.out.println("Processor query describe:" +q);
		QueryExecution qe = QueryExecutionFactory.sparqlService(fusekiUrl,q);
		Model model = qe.execDescribe();		
		return model;		
	}
	
	public String getResource(String query,String fusekiUrl,boolean html,String baseUri){
		System.out.println("Processor query select:" +query);
		this.baseUri=baseUri;
	    String ret="";
		if(fusekiUrl == null) {
			fusekiUrl=ServiceConfig.getProperty("fuseki");
		}
		Query q=QueryFactory.create(prefixes+" "+query);
		long start=System.currentTimeMillis();
		QueryExecution qe = QueryExecutionFactory.sparqlService(fusekiUrl,q);
		ResultSet rs = qe.execSelect();
		long end=System.currentTimeMillis();
		long elapsed=end-start;
		if(html) {
			ret=toHtmlTable(rs,elapsed);
		}else {
			ret=toTable(rs);			
		}
		return ret;		
	}
	
	private String toTable(ResultSet rs) {
		String table="";		
		List<String> l=rs.getResultVars();	
		for(String st:l) {
			table=table+"\t"+st;			
		}
		table=table+"\t"+System.lineSeparator();
		while(rs.hasNext()) {
			QuerySolution qs=rs.next();
			table=table+System.lineSeparator();
			for(String str:l) {
				
				RDFNode node=qs.get(str);
				if(node !=null) {
					if(node.isResource()) {
						table=table+qs.get(str).asNode().getLocalName()+"\t";
					}
					else if(node.isLiteral()) {
						table=table+qs.get(str)+"\t";	
					}
				}else {
					table=table+"\t";
				}
			}
		}		
		return table;
	}
	
	private String toHtmlTable(ResultSet rs,long elapsed) {
		int num_res=0;
		String table="<table style=\"width: 80%\" border=\"0\"><tr >";		
		List<String> l=rs.getResultVars();
		for(String st:l) {
			table=table+"<td style=\"background-color: #f7f7c5;\">"+st+"</td>";			
		}
		table=table+"</tr>";
		boolean changeColor=false;
		while(rs.hasNext()) {
			num_res++;
			QuerySolution qs=rs.next();	
			table=table+"<tr>";	
			int index=0;
			for(String str:l) {				
				RDFNode node=qs.get(str);
				if(node !=null) {
					if(node.isResource() ) {
						table=table+"<td";
						if(changeColor) {
							table=table+" style=\"background-color: #f2f2f2;\"";
						}
						if(index==0) {
							if(baseUri==null) {
							table=table+"><a href=\"lookup/query/";
							}else {
								table=table+"><a href=\""+baseUri;	
							}
							if(qs.get(str).asNode().isBlank()) {
								table=table+qs.get(str).asNode().getBlankNodeLabel()+"\"> "
								+qs.get(str).asNode().getBlankNodeLabel()+"</a></td>";
							}else {
								table=table+qs.get(str).asNode().getLocalName()+"\"> "
								+qs.get(str).asNode().getLocalName()+"</a></td>";
							}
						}
						else {
							if(qs.get(str).asNode().isBlank()) {
								table=table+">_b"+qs.get(str).asNode().getBlankNodeLabel().substring(0, 5)+"</td>";
							}else {
								table=table+">"+qs.get(str).asNode().getLocalName()+"</td>";
							}
						}
					}
					else if(node.isLiteral()) {
						table=table+"<td";
						if(changeColor) {
							table=table+" style=\"background-color: #f2f2f2;\"";
						}
						table=table+">"+qs.get(str)+"</td>";	
					}
				}else {
					table=table+"<td";
					if(changeColor) {
						table=table+" style=\"background-color: #f2f2f2;\"";
					}
					table=table+"></td>";
					
				}
				index++;
			}
			changeColor=!changeColor;
			table=table+"</tr>";
		}	
		table=table+"</table>";
		String time="<br><span><b> Returned "+num_res+" results in "+elapsed+" ms</b></span><br><br>";
		return time+table;
	}

}
