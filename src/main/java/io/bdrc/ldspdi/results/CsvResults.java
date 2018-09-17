package io.bdrc.ldspdi.results;

import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.restapi.exceptions.RestException;

public class CsvResults {

    public final static Logger log=LoggerFactory.getLogger(Results.class.getName());

    public int pageNumber;
    public int numberOfPages;
    public int pageSize;
    public int numResults;
    public int hash;
    public boolean lastPage;
    public boolean firstPage;
    String csvRes;

    public CsvResults(ResultSetWrapper res,HashMap<String,String> hm)
            throws RestException{
        String profile=hm.get("profile");
        //default is detailed
        boolean simple=false;
        if("simple".equals(profile)) {
            simple=true;
        }
        String csvCols=null;
        String pageNum=hm.get(QueryConstants.PAGE_NUMBER);
        if(pageNum!=null) {
            this.pageNumber=Integer.parseInt(pageNum);
        }else {
            this.pageNumber=1;
        }
        pageSize=res.getPageSize();
        numResults=res.getNumResults();
        hash=res.getHash();
        numberOfPages=res.getNumberOfPages();
        int offset=(pageNumber-1)*pageSize;
        ArrayList<CsvRow> csvrows=res.getCsvRows();
        if(pageNumber<=numberOfPages) {
            for (int x=(offset); x<(offset+pageSize);x++) {
                try {
                    CsvRow csv=csvrows.get(x);
                    if(csvCols==null) {
                        if(simple) {
                            csvCols=csv.getSimpleCols();
                            csvRes=csvCols+System.lineSeparator();
                        }
                        else {
                            csvCols=csv.getCsvCols();
                            csvRes=csvCols+System.lineSeparator();
                        }
                    }
                    if(simple) {
                        csvRes=csvRes+csv.getSimpleCsv()+System.lineSeparator();
                    }else {
                        csvRes=csvRes+csv.getCsv()+System.lineSeparator();
                    }
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

    public int getHash() {
        return hash;
    }

    public boolean isLastPage() {
        return lastPage;
    }

    public boolean isFirstPage() {
        return firstPage;
    }

    public String getCsvRes() {
        return csvRes;
    }

    @Override
    public String toString() {
        return "CsvResults [pageNumber=" + pageNumber + ", numberOfPages=" + numberOfPages + ", pageSize=" + pageSize
                + ", numResults=" + numResults + ", hash=" + hash + ", lastPage=" + lastPage + ", firstPage="
                + firstPage + ", csvRes=" + csvRes + "]";
    }





}
