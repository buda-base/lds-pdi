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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.objects.json.Output;
import io.bdrc.ldspdi.objects.json.Param;
import io.bdrc.ldspdi.objects.json.QueryTemplate;
import io.bdrc.ldspdi.sparql.QueryConstants;

public class ResultPage {

    public final static Logger log = LoggerFactory.getLogger(ResultPage.class);

    public int pageNumber, numberOfPages, pageSize, numResults, hash;
    public long execTime;
    public String id, query;
    public boolean isLastPage, isFirstPage, isUrlQuery = false;
    public ResultPageLinks pLinks;
    public HashMap<String, List<String>> head;
    public List<String> headrows;
    public ArrayList<QueryMvcSolutionItem> mvc_rows;
    HashMap<String, String> hm;
    private QueryTemplate temp;

    public ResultPage(ResultSetWrapper res, String pageNum, HashMap<String, String> hm, QueryTemplate temp) throws RestException {
        if (pageNum != null) {
            this.pageNumber = Integer.parseInt(pageNum);
        } else {
            this.pageNumber = 1;
        }
        this.hm = hm;
        pageSize = res.getPageSize();
        numResults = res.getNumResults();
        execTime = res.getExecTime();
        hash = res.getHash();
        head = res.getHead();
        headrows = head.get("vars");
        numberOfPages = res.getNumberOfPages();
        id = temp.getId();
        query = hm.get("query");
        String tmp = hm.get(QueryConstants.QUERY_TYPE);
        if (tmp != null) {
            isUrlQuery = tmp.equals(QueryConstants.URL_QUERY);
        }
        int offset = (pageNumber - 1) * pageSize;
        this.temp = temp;
        mvc_rows = new ArrayList<>();
        ArrayList<QueryMvcSolutionItem> allRows = res.getMvc_rows();
        if (pageNumber <= numberOfPages) {
            for (int x = (offset); x < (offset + pageSize); x++) {
                try {
                    mvc_rows.add(allRows.get(x));
                } catch (Exception ex) {
                    // For building the last page
                    break;
                }
            }
        }
        if (pageNumber == 1) {
            isFirstPage = true;
        } else {
            isFirstPage = false;
        }
        if (pageNumber == res.numberOfPages) {
            isLastPage = true;
        } else {
            isLastPage = false;
        }
        pLinks = new ResultPageLinks(this, hm);
        res = null;
    }

    public ArrayList<QueryMvcSolutionItem> getMvc_rows() {
        return mvc_rows;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public List<String> getHeadrows() {
        return headrows;
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

    public boolean isLastPage() {
        return isLastPage;
    }

    public boolean isFirstPage() {
        return isFirstPage;
    }

    public HashMap<String, List<String>> getHead() {
        return head;
    }

    public int getNumberOfPages() {
        return numberOfPages;
    }

    public ResultPageLinks getpLinks() {
        return pLinks;
    }

    public String getId() {
        return id;
    }

    public String getQuery() {
        return query;
    }

    public static Logger getLog() {
        return log;
    }

    public boolean isUrlQuery() {
        return isUrlQuery;
    }

    public QueryTemplate getTemp() {
        return temp;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getParamList() {
        if (temp.getQueryParams() == null) {
            return new ArrayList<String>();
        }
        List<String> list = Arrays.asList(temp.getQueryParams().split(Pattern.compile(",").toString()));
        return list;
    }

    public String getParamValue(String param) {
        String val = hm.get(param);
        if (val != null) {
            return val;
        }
        return "";
    }

    public ArrayList<Param> getParams() {
        return temp.getParams();
    }

    public ArrayList<Output> getOutputs() {
        return temp.getOutputs();
    }

    @Override
    public String toString() {
        return "ResultPage [pageNumber=" + pageNumber + ", numberOfPages=" + numberOfPages + ", pageSize=" + pageSize + ", numResults=" + numResults + ", hash=" + hash + ", execTime=" + execTime + ", id=" + id + ", query=" + query + ", isLastPage="
                + isLastPage + ", isFirstPage=" + isFirstPage + ", isUrlQuery=" + isUrlQuery + ", pLinks=" + pLinks + ", head=" + head + ", hm=" + hm + ", temp=" + temp + "]";
    }

}
