package io.bdrc.ldspdi.sparql.results;

/*******************************************************************************
 * Copyright (c) 2018 Buddhist Digital Resource Center (BDRC)
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;


public class ResultSetWrapper implements Serializable{
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static Logger log=LoggerFactory.getLogger(ResultSetWrapper.class.getName());
    
    public long execTime;
    public int numResults;
    public int pageSize;
    public int numberOfPages;
    public int hash;
    public List<String> headers;
    public ArrayList<QuerySolutionItem> rows;    
    public HashMap<Integer,String> pagesUrls;
    
    public ResultSetWrapper(ResultSet rs, long execTime,int pageSize) {
        this.pageSize=pageSize;
        headers =rs.getResultVars();
        this.execTime=execTime;
        numResults=0;
        rows=new ArrayList<>();
        while(rs.hasNext()) {            
            QuerySolution qs=rs.next();            
            QuerySolutionItem row=new QuerySolutionItem(qs,headers);
            rows.add(row);
            numResults++;
        }
        numberOfPages=(numResults/pageSize);
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
