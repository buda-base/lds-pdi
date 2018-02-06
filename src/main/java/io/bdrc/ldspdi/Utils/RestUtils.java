package io.bdrc.ldspdi.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.vocabulary.SKOS;

import io.bdrc.jena.sttl.CompareComplex;
import io.bdrc.jena.sttl.ComparePredicates;
import io.bdrc.jena.sttl.STTLWriter;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.sparql.results.QuerySolutionItem;
import io.bdrc.ldspdi.sparql.results.ResultPage;
import io.bdrc.ldspdi.sparql.results.Results;
import io.bdrc.ldspdi.sparql.results.ResultsCache;

public class RestUtils {
    
    public static Logger log=Logger.getLogger(RestUtils.class.getName());
    
    public static boolean getJsonOutput(String param) {
        boolean json;
        try {
            json=Boolean.parseBoolean(param);            
        }catch(Exception ex){
            json=false;            
        }
        return json;
    }
    
    public static int getHash(String param) {
        int hash;
        try {
            hash=Integer.parseInt(param);            
        }catch(Exception ex){
            hash= -1;            
        }
        return hash;
    }
    
    public static int getPageSize(String param) {
        int pageSize;
        try {
            pageSize=Integer.parseInt(param);
            
        }catch(Exception ex){
            pageSize= Integer.parseInt(ServiceConfig.getProperty(QueryConstants.PAGE_SIZE));            
        }
        return pageSize;
    }
    
    public static int getPageNumber(String param) {
        int pageNumber;
        try {
            pageNumber=Integer.parseInt(param);
            
        }catch(Exception ex){
            pageNumber= 1;            
        }
        return pageNumber;
    }
    
    public static MediaType getMediaType(String format){
        String[] parts=format.split(Pattern.quote("/"));
        return new MediaType(parts[0],parts[1]);        
    }
    
    public static boolean isValidExtension(String ext){
        return (ServiceConfig.getProperty(ext)!=null);
    }
    
    public static RDFWriter getSTTLRDFWriter(Model m) throws IOException{
        Lang sttl = STTLWriter.registerWriter();
        SortedMap<String, Integer> nsPrio = ComparePredicates.getDefaultNSPriorities();
        nsPrio.put(SKOS.getURI(), 1);
        nsPrio.put("http://purl.bdrc.io/ontology/admin/", 5);
        nsPrio.put("http://purl.bdrc.io/ontology/toberemoved/", 6);
        List<String> predicatesPrio = CompareComplex.getDefaultPropUris();
        predicatesPrio.add("http://purl.bdrc.io/ontology/admin/logWhen");
        predicatesPrio.add("http://purl.bdrc.io/ontology/onOrAbout");
        predicatesPrio.add("http://purl.bdrc.io/ontology/noteText");
        org.apache.jena.sparql.util.Context ctx = new org.apache.jena.sparql.util.Context();
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsPriorities"), nsPrio);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsDefaultPriority"), 2);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "complexPredicatesPriorities"), predicatesPrio);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "indentBase"), 3);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "predicateBaseWidth"), 12);
        RDFWriter w = RDFWriter.create().source(m.getGraph()).context(ctx).lang(sttl).build();
        return w;
    }    
       
    public static Results getResults(String query, String fuseki, int hash, int pageSize) {
        Results res;
        if(hash ==-1) {
            long start=System.currentTimeMillis();
            QueryProcessor processor=new QueryProcessor();
            ResultSet jrs=processor.getResultSet(query, fuseki);
            long end=System.currentTimeMillis();
            long elapsed=end-start;
            res=new Results(jrs,elapsed,pageSize);                    
            int new_hash=Objects.hashCode(res);                    
            res.setHash(new_hash);                    
            ResultsCache.addToCache(res, Objects.hashCode(res));
            log.info("New Results object loaded into cache with hash:"+new_hash);
        }
        else {
            res=ResultsCache.getResultsFromCache(hash);
            log.info("Got Results object from cache with hash:"+hash);
        }
        return res;
    }
    
    public static String renderHtmlResultPage(ResultPage page,String URL) {
        String table=getHtmlResultsHeader();
        table=table+"<br><span><b> Returned "+page.numResults+" results in "+page.getExecTime()+" ms</b></span><br>";
        table=table+"<span><b> Page number : "+page.getPageNumber()+"</b></span><br>";
        table=table+"<span><b> Total pages : "+page.getNumberOfPages()+"</b></span><br>";
        table=table+"<span><b> ResultSet Hash="+page.getHash()+"</b></span><br>";
        table=table+"<span><b> Template name="+page.getId()+"  </b><a href=\"javascript:showHide()\">(view/hide query)"+"</a></span>";
        table=table+"<div id=\"query\" style=\"display:none\">"+page.getQuery()+"</div><br>";
        if( page.numberOfPages>1) {
            if(page.getPageNumber()==1 ) {
                if(!URL.contains("&pageNumber=")) {
                table=table+"<br><a href=\""+URL+"&pageNumber=2&hash="+page.getHash()+"\">Next</a><br>";
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
        table=table+"<br><br><table style=\"width: 80%\" border=\"0\"><tr>";
        List<String> headers=page.getHeaders();
        ArrayList<QuerySolutionItem> rows=page.getRows();
        for(String st:headers) {
            table=table+"<td style=\"background-color: #f7f7c5;\">"+st+"</td>";            
        }
        table=table+"</tr>";
        boolean changeColor=false;
        for(QuerySolutionItem qsi:rows) {
            table=table+"<tr>";             
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
        table=table+getHtmlResultsFooter();
        return table;
    }
    
    public static String renderSingleHtmlResultPage(ResultPage page,String URL) {
        String table=getHtmlResultsHeader();
        table=table+"<br><span><b> Returned "+page.numResults+" results in "+page.getExecTime()+" ms</b></span><br>";
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
        table=table+getHtmlResultsFooter();
        return table;
    }
    
    public static String getHtmlResultsHeader() {
        return "<!DOCTYPE html>\n" + 
                "<html>\n" + 
                "<head>\n" + 
                "<meta charset=\"UTF-8\">\n" + 
                "<script type=\"text/javascript\"> \n" + 
                "function showHide() {" + 
                "    var x = document.getElementById(\"query\");" + 
                "    if (x.style.display === \"none\") {" + 
                "        x.style.display = \"block\";" + 
                "    } else {\n" + 
                "        x.style.display = \"none\";" + 
                "    }" + 
                "}   \n" + 
                "</script>\n" + 
                "<title>BDRC Public data results</title>\n" + 
                "</head>\n" + 
                "<body>";
        
    }
    public static String getHtmlResultsFooter() {
        
        return "</body></html>";
    }
    
    public static HashMap<String,String> convertMulti(MultivaluedMap<String,String> map){
        HashMap<String,String> copy=new HashMap<>();
        Set<String> set=map.keySet();
        for(String key:set) {
            copy.put(key, map.getFirst(key));
        }
        return copy;
    }
    
    public static String relativizeURL(String uri) {
        uri=uri.substring(8);
        return uri.substring(uri.indexOf("/"));
    }

}
