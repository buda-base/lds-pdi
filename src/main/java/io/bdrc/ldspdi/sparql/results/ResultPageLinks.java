package io.bdrc.ldspdi.sparql.results;

import java.util.HashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.ldspdi.sparql.QueryConstants;

public class ResultPageLinks {
    
    public String prevGet;
    public String nextGet;
    public String currJsonParams;
    public String prevJsonParams;
    public String nextJsonParams;    
    
    
    public ResultPageLinks(ResultPage page, HashMap<String,String> hm) throws JsonProcessingException{
        ObjectMapper mapper = new ObjectMapper();
        String method=hm.get(QueryConstants.REQ_METHOD);
        if(method !=null) {
            if(method.equals("GET")) {
                String URL=hm.get(QueryConstants.REQ_URI);
                if( page.numberOfPages>1) {
                    if(page.getPageNumber()==1 ) {
                        if(!URL.contains("&pageNumber=")) {
                        nextGet=URL+"&pageNumber=2&hash="+page.getHash();
                        }
                        else {
                            int next=page.getPageNumber()+1;
                            nextGet=URL.replace("pageNumber="+page.getPageNumber(), "pageNumber="+next);                           
                        }
                    }else {
                        int prev=page.getPageNumber()-1;
                        prevGet=URL.replace("pageNumber="+page.getPageNumber(), "pageNumber="+prev);
                        
                        if(!page.isLastPage()) {
                            int next=page.getPageNumber()+1;
                            nextGet=URL.replace("pageNumber="+page.getPageNumber(), "pageNumber="+next);                        
                        }
                    }
                }
            }
        }
        currJsonParams=mapper.writer().writeValueAsString(hm);
        hm.put(QueryConstants.RESULT_HASH, Integer.toString(page.getHash()));
        hm.put(QueryConstants.PAGE_NUMBER, Integer.toString(page.getPageNumber()+1));
        nextJsonParams=mapper.writer().writeValueAsString(hm);
        if(page.getPageNumber()!=1) {
            hm.put(QueryConstants.PAGE_NUMBER, Integer.toString(page.getPageNumber()-1));
            prevJsonParams=mapper.writer().writeValueAsString(hm);
        }
    }
    
    public ResultPageLinks(JsonResult page, HashMap<String,String> hm) throws JsonProcessingException{
        ObjectMapper mapper = new ObjectMapper();
        String method=hm.get(QueryConstants.REQ_METHOD);
        if(method !=null) {
            if(method.equals("GET")) {
                String URL=hm.get(QueryConstants.REQ_URI);
                if( page.numberOfPages>1) {
                    if(page.getPageNumber()==1 ) {
                        if(!URL.contains("&pageNumber=")) {
                        nextGet=URL+"&pageNumber=2&hash="+page.getHash();
                        }
                        else {
                            int next=page.getPageNumber()+1;
                            nextGet=URL.replace("pageNumber="+page.getPageNumber(), "pageNumber="+next);                           
                        }
                    }else {
                        int prev=page.getPageNumber()-1;
                        prevGet=URL.replace("pageNumber="+page.getPageNumber(), "pageNumber="+prev);
                        
                        if(page.getPageNumber()!=page.getNumberOfPages()) {
                            int next=page.getPageNumber()+1;
                            nextGet=URL.replace("pageNumber="+page.getPageNumber(), "pageNumber="+next);                        
                        }
                    }
                }
            }
        }
        currJsonParams=mapper.writer().writeValueAsString(hm);
        hm.put(QueryConstants.RESULT_HASH, Integer.toString(page.getHash()));
        if(page.getPageNumber()>1 && (page.getPageNumber()<page.getNumberOfPages())) {
            hm.put(QueryConstants.PAGE_NUMBER, Integer.toString(page.getPageNumber()+1));
            nextJsonParams=mapper.writer().writeValueAsString(hm);
        }
        if(page.getPageNumber()!=1) {
            hm.put(QueryConstants.PAGE_NUMBER, Integer.toString(page.getPageNumber()-1));
            prevJsonParams=mapper.writer().writeValueAsString(hm);
        }
    }
    
    public String getPrevGet() {
        return prevGet;
    }
    public String getNextGet() {
        return nextGet;
    }
    public String getCurrJsonParams() {
        return currJsonParams;
    }
    public String getPrevJsonParams() {
        return prevJsonParams;
    }
    public String getNextJsonParams() {
        return nextJsonParams;
    }

    
    
}
