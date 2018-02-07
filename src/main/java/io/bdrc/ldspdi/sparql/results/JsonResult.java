package io.bdrc.ldspdi.sparql.results;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.bdrc.ldspdi.sparql.QueryConstants;

public class JsonResult {
    
public static Logger log=Logger.getLogger(ResultPage.class.getName());
    
    public int pageNumber;
    public int numberOfPages;
    public int pageSize;
    public int numResults;
    public long execTime;
    public int hash;
    public boolean lastPage;
    public boolean firstPage;    
    public ResultPageLinks pLinks;
    public List<String> headers;
    public ArrayList<QuerySolutionItem> rows;
    
    public JsonResult(Results res,HashMap<String,String> hm) 
            throws JsonProcessingException,NumberFormatException{
        String pageNum=hm.get(QueryConstants.PAGE_NUMBER);
        if(pageNum!=null) {
            this.pageNumber=Integer.parseInt(pageNum);
        }else {
            this.pageNumber=1;
        }
        pageSize=res.getPageSize();
        numResults=res.getNumResults();
        execTime=res.getExecTime();
        hash=res.getHash();
        headers=res.getHeaders();
        numberOfPages=res.getNumberOfPages();        
        int offset=(pageNumber-1)*pageSize; 
        rows=new ArrayList<>();
        ArrayList<QuerySolutionItem> allRows=res.getRows();        
        if(pageNumber<=numberOfPages) {
            for (int x=(offset); x<(offset+pageSize);x++) {
                try {
                rows.add(allRows.get(x));
                }
                catch(Exception ex) {                    
                    break;
                }
            }
        }
        if(pageNumber==1) {
            firstPage=true;
        }
        else {
            firstPage=false;
        }
        if(pageNumber==res.numberOfPages) {
            lastPage=true;
        }else {
            lastPage=false;
        }
        pLinks=new ResultPageLinks(this,hm);
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getNumberOfPages() {
        return numberOfPages;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getNumResults() {
        return numResults;
    }

    public long getExecTime() {
        return execTime;
    }

    public int getHash() {
        return hash;
    }

    public boolean lastPage() {
        return lastPage;
    }

    public boolean firstPage() {
        return firstPage;
    }

    public ResultPageLinks getpLinks() {
        return pLinks;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public ArrayList<QuerySolutionItem> getRows() {
        return rows;
    }
    
}
