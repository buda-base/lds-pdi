package io.bdrc.ldspdi.sparql.results;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;


public class ResultSetCopy implements Serializable{
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static Logger log=Logger.getLogger(ResultSetCopy.class.getName());
    
    public long execTime;
    public int numResults;
    public List<String> headers;
    public ArrayList<QuerySolutionItem> rows;
    
    public ResultSetCopy(ResultSet rs, long execTime) {
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
    
    
}
