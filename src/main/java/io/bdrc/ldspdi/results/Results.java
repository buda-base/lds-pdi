package io.bdrc.ldspdi.results;

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

import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.restapi.exceptions.RestException;

public class Results {

    public final static Logger log=LoggerFactory.getLogger(Results.class.getName());

    public int pageNumber;
    public int numberOfPages;
    public int pageSize;
    public int numResults;
    public long execTime;
    public int hash;
    public boolean lastPage;
    public boolean firstPage;
    public ResultPageLinks pLinks;
    public Head head;
    public HashMap<String,ArrayList<Row>> results;

    public Results(ResultSetWrapper res,HashMap<String,String> hm)
            throws RestException{
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
        head=new Head(res.getHead());
        numberOfPages=res.getNumberOfPages();
        int offset=(pageNumber-1)*pageSize;
        results=new HashMap<>();
        ArrayList<Row> rows=res.getRows();
        ArrayList<Row> bindings=new ArrayList<>();;
        if(pageNumber<=numberOfPages) {
            for (int x=(offset); x<(offset+pageSize);x++) {
                try {
                    bindings.add(rows.get(x));
                }
                catch(Exception ex) {
                    break;
                }
            }
        }
        results.put("bindings", bindings);
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
        res=null;
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

    public Head getHead() {
        return head;
    }

    public HashMap<String, ArrayList<Row>> getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "Results [pageNumber=" + pageNumber + ", numberOfPages=" + numberOfPages + ", pageSize=" + pageSize
                + ", numResults=" + numResults + ", execTime=" + execTime + ", hash=" + hash + ", lastPage=" + lastPage
                + ", firstPage=" + firstPage + ", pLinks=" + pLinks + ", head=" + head + ", results=" + results + "]";
    }




}
