package io.bdrc.ldspdi.sparql.results;

import java.util.ArrayList;
import java.util.List;

public class ResultPage {
    
    public int pageNumber;
    public int numberOfPages;
    public int pageSize;
    public int numResults;
    public long execTime;
    public int hash;
    public boolean isLastPage;
    public String jsonParamsString;
    public List<String> headers;
    public ArrayList<QuerySolutionItem> rows;
    

    public ResultPage(Results res,int pageNumber,String jsonParamsString) {
        
        this.jsonParamsString=jsonParamsString;
        this.pageNumber=pageNumber;
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
                    //For building the last page
                    break;
                }
            }
        }
        if(pageNumber==res.numberOfPages) {
            isLastPage=true;
        }else {
            isLastPage=false;
        }
    }

    public ArrayList<QuerySolutionItem> getRows() {
        return rows;
    }

    public int getPageNumber() {
        return pageNumber;
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

    public String getJsonParamsString() {
        return jsonParamsString;
    }

    public int getHash() {
        return hash;
    }

    public boolean isLastPage() {
        return isLastPage;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public int getNumberOfPages() {
        return numberOfPages;
    }
    
    
    
    
}
