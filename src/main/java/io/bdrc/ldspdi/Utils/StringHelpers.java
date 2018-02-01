package io.bdrc.ldspdi.Utils;

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

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;

import io.bdrc.ldspdi.sparql.functions.Wylie;
import io.bdrc.ldspdi.sparql.results.QuerySolutionItem;
import io.bdrc.ldspdi.sparql.results.ResultPage;

public class StringHelpers {
	
	public static String removeAccents(String text) {		
		String f=text;
		return f == null ? null :
	        Normalizer.normalize(f, Form.NFD)
	            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}
	public static boolean isTibUni(String s) {		
		return s.matches("[\u0f00-\u0fff]+");
	}
	public static boolean isWylie(String s) {		
		 Wylie wl = new Wylie(true, false, false, true);
		 ArrayList<String> warn=new ArrayList<>();
		 wl.fromWylie(s, warn);
		 return warn.size()==0;
	}
	
	public static String bdrcEncode(String url) {
		String encoded=url.replace("\"", "%22");
		encoded=encoded.replace(' ', '+');
		encoded=encoded.replace("\'", "%27");
		return encoded;
	}
	
	public static String renderHtmlResultPage(ResultPage page,String URL) {
	    String table="<br><span><b> Returned "+page.numResults+" results in "+page.getExecTime()+" ms</b></span><br>";
	    table=table+"<span><b> Page number : "+page.getPageNumber()+"</b></span><br>";
	    table=table+"<span><b> Total pages : "+page.getNumberOfPages()+"</b></span><br>";
	    table=table+"<span><b> ResultSet Hash="+page.getHash()+"</b></span><br>";
	    if( page.numberOfPages>1) {
    	    if(page.getPageNumber()==1 ) {
    	        if(!URL.contains("&pageNumber=")) {
    	        table=table+"<br><a href=\""+URL+"&pageNumber=2&hash="+page.getHash()+"\">Next</a><br><br>";
    	        }
    	        else {
    	            int next=page.getPageNumber()+1;
                    String nextUrl=URL.replace("pageNumber="+page.getPageNumber(), "pageNumber="+next);
                    table=table+"<br><a href=\""+nextUrl+"\">Next</a>";   
    	        }
    	    }else {
    	        int prev=page.getPageNumber()-1;
    	        String prevUrl=URL.replace("pageNumber="+page.getPageNumber(), "pageNumber="+prev);
    	        table=table+"<br><a href=\""+prevUrl+"\">Prev</a>";
    	        if(!page.isLastPage()) {
    	            int next=page.getPageNumber()+1;
    	            String nextUrl=URL.replace("pageNumber="+page.getPageNumber(), "pageNumber="+next);
    	            table=table+"&nbsp;&nbsp;&nbsp;&nbsp;<a href=\""+nextUrl+"\">&nbsp;Next</a>";
    	        }
    	    }
	    }
	    table=table+"<br><br><table style=\"width: 80%\" border=\"0\"><tr >";
	    List<String> headers=page.getHeaders();
	    ArrayList<QuerySolutionItem> rows=page.getRows();
	    for(String st:headers) {
	        table=table+"<td style=\"background-color: #f7f7c5;\">"+st+"</td>";            
        }
	    table=table+"</tr>";
        boolean changeColor=false;
        for(QuerySolutionItem qsi:rows) {
            table=table+"<tr>"; 
            int index=0;
            for(String st:headers) {
                table=table+"<td";
                if(changeColor) {
                    table=table+" style=\"background-color: #f2f2f2;\">"+qsi.getValue(st)+"</td>";
                }else {
                    table=table+">"+qsi.getValue(st)+"</td>";
                }
            }
            table=table+"</tr>";
            changeColor=!changeColor;
        }
        table=table+"</table>";
        if( page.numberOfPages>1) {
            if(page.getPageNumber()==1 ) {
                if(!URL.contains("&pageNumber=")) {
                table=table+"<br><a href=\""+URL+"&pageNumber=2&hash="+page.getHash()+"\">Next</a><br><br>";
                }
                else {
                    int next=page.getPageNumber()+1;
                    String nextUrl=URL.replace("pageNumber="+page.getPageNumber(), "pageNumber="+next);
                    table=table+"<br><a href=\""+nextUrl+"\">Next</a>";   
                }
            }else {
                int prev=page.getPageNumber()-1;
                String prevUrl=URL.replace("pageNumber="+page.getPageNumber(), "pageNumber="+prev);
                table=table+"<br><a href=\""+prevUrl+"\">Prev</a>";
                if(!page.isLastPage()) {
                    int next=page.getPageNumber()+1;
                    String nextUrl=URL.replace("pageNumber="+page.getPageNumber(), "pageNumber="+next);
                    table=table+"&nbsp;&nbsp;&nbsp;&nbsp;<a href=\""+nextUrl+"\">&nbsp;Next</a>";
                }
            }
        }
	    return table;
	}
	
	
}
