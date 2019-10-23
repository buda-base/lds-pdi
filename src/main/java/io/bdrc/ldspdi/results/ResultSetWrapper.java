package io.bdrc.ldspdi.results;

import java.io.StringWriter;

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
import java.util.List;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVWriter;

import io.bdrc.ldspdi.sparql.QueryConstants;

public class ResultSetWrapper {

    public final static Logger log = LoggerFactory.getLogger(ResultSetWrapper.class);

    public long execTime;
    public int numResults;
    public int pageSize;
    public int numberOfPages;
    public int hash;
    public List<QuerySolution> querySolutions;
    public List<String> vars;
    public HashMap<String, List<String>> head;
    public ArrayList<Row> rows;
    public ArrayList<QueryMvcSolutionItem> mvc_rows;
    public static final String DEL = ",";

    public ResultSetWrapper(ResultSet rs, long execTime, int pageSize) {
        this.pageSize = pageSize;
        vars = rs.getResultVars();
        head = new HashMap<>();
        head.put("vars", rs.getResultVars());
        this.execTime = execTime;
        numResults = 0;
        mvc_rows = new ArrayList<>();
        querySolutions = new ArrayList<>();
        rows = new ArrayList<>();
        while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            querySolutions.add(qs);
            rows.add(new Row(vars, qs));
            QueryMvcSolutionItem mvc_row = new QueryMvcSolutionItem(qs, vars);
            mvc_rows.add(mvc_row);
            numResults++;
        }
        numberOfPages = numResults / pageSize;
        if (numberOfPages * pageSize < numResults) {
            numberOfPages++;
        }
    }

    public void update(int pageSize) {
        this.pageSize = pageSize;
        numberOfPages = numResults / pageSize;
        if (numberOfPages * pageSize < numResults) {
            numberOfPages++;
        }
    }

    public HashMap<String, List<String>> getHead() {
        return head;
    }

    public long getExecTime() {
        return execTime;
    }

    public int getNumResults() {
        return numResults;
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

    public ArrayList<QueryMvcSolutionItem> getMvc_rows() {
        return mvc_rows;
    }

    public ArrayList<Row> getRows() {
        return rows;
    }

    public List<QuerySolution> getQuerySolutions() {
        return querySolutions;
    }

    public void writeCsvHeaders(boolean simple, CSVWriter w) {
        final String[] headers;
        final int factor;
        if (simple) {
            headers = new String[vars.size()];
            factor = 1;
        } else {
            headers = new String[3 * vars.size()];
            factor = 3;
        }
        int varIdx = 0;
        for (String var : vars) {
            headers[factor * varIdx] = var;
            if (!simple) {
                headers[factor * varIdx + 1] = var + "-type";
                headers[factor * varIdx + 2] = var + "-lang";
            }
            varIdx += 1;
        }
        w.writeNext(headers);
    }

    public byte[] getCsvAsBytes(HashMap<String, String> options, boolean simple) {
        final String pageNum = options.getOrDefault(QueryConstants.PAGE_NUMBER, "1");
        final int pageNumber = Integer.parseInt(pageNum);
        final int offset = (pageNumber - 1) * pageSize;
        if (pageNumber > numberOfPages || pageNumber < 0) {
            return null;
        }
        StringWriter sw = new StringWriter();
        final int maxIdx = Math.min(offset + pageSize, numResults);
        CSVWriter csvWriter = new CSVWriter(sw);
        writeCsvHeaders(simple, csvWriter);
        for (int x = offset; x < maxIdx; x++) {
            QuerySolution qs = querySolutions.get(x);
            if (simple)
                QSWriter.writeCsvSimple(qs, vars, csvWriter);
            else
                QSWriter.writeCsvFull(qs, vars, csvWriter);
        }
        csvWriter.flushQuietly();
        return sw.getBuffer().toString().getBytes();
    }

    public HashMap<String, Object> getFusekiResultSet() {
        HashMap<String, Object> res = new HashMap<>();
        HashMap<String, ArrayList<Row>> results = new HashMap<>();
        results.put("bindings", getRows());
        res.put("head", head);
        res.put("results", results);
        return res;
    }

}
