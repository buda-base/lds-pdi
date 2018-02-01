package io.bdrc.ldspdi.sparql.results;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;


public class Results implements Serializable{
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static Logger log=Logger.getLogger(Results.class.getName());
    
    public long execTime;
    public int numResults;
    public int pageSize;
    public int numberOfPages;
    public int hash;
    public List<String> headers;
    public ArrayList<QuerySolutionItem> rows;    
    public HashMap<Integer,String> pagesUrls;
    
    public Results(ResultSet rs, long execTime,int pageSize) {
        this.pageSize=pageSize;
        headers =rs.getResultVars();
        this.execTime=execTime;
        numResults=0;
        rows=new ArrayList<>();
        while(rs.hasNext()) {
            //log.info(headers.toString());
            QuerySolution qs=rs.next();
            //log.info(qs.toString());
            QuerySolutionItem row=new QuerySolutionItem(qs,headers);
            rows.add(row);
            numResults++;
        }
        numberOfPages=(int)(numResults/pageSize);
        if(numberOfPages*pageSize<numResults) {numberOfPages++;}
    }
    
    public List<String> getHeaders() {
        return headers;
    }

    public long getExecTime() {
        return execTime;
    }

    public int getNumResults() {
        return numResults;
    }

    public ArrayList<QuerySolutionItem> getRows() {
        return rows;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public static Logger getLog() {
        return log;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getNumberOfPages() {
        return numberOfPages;
    }

    public int getHash() {
        return hash;
    }

    public void setHash(int hash) {
        this.hash = hash;
    }
    
    
    
    
}
