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

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

public class ResultPageLinks {

    public String prevGet;
    public String nextGet;
    public String currJsonParams;
    public String prevJsonParams;
    public String nextJsonParams;

    public final static Logger log=LoggerFactory.getLogger(ResultPageLinks.class.getName());

    public ResultPageLinks(ResultPage page, HashMap<String,String> hm) throws RestException{
        ObjectMapper mapper = new ObjectMapper();
        String method=hm.get(QueryConstants.REQ_METHOD);
        if(method !=null) {
            if(method.equals("GET")) {
                String URL=hm.get(QueryConstants.REQ_URI);
                if( page.numberOfPages>1) {
                    if(page.getPageNumber()==1 ) {
                        if(!URL.contains("&pageNumber=")) {
                            if(URL.indexOf("?")!=-1) {
                                nextGet=URL+"&pageNumber=2&hash="+page.getHash();
                            }else {
                                nextGet=URL+"?pageNumber=2&hash="+page.getHash();
                            }
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
        try {
            currJsonParams=mapper.writer().writeValueAsString(hm);
            hm.put(QueryConstants.RESULT_HASH, Integer.toString(page.getHash()));
            hm.put(QueryConstants.PAGE_NUMBER, Integer.toString(page.getPageNumber()+1));
            nextJsonParams=mapper.writer().writeValueAsString(hm);
            if(page.getPageNumber()!=1) {
                hm.put(QueryConstants.PAGE_NUMBER, Integer.toString(page.getPageNumber()-1));
                prevJsonParams=mapper.writer().writeValueAsString(hm);
            }
        }
        catch(JsonProcessingException ex) {
            throw new RestException(5001,new LdsError(LdsError.JSON_ERR).
                    setContext(" in ResultPageLinks constructor ",ex));
        }
    }

    public ResultPageLinks(Results page, HashMap<String,String> hm) throws RestException{
        ObjectMapper mapper = new ObjectMapper();
        String method=hm.get(QueryConstants.REQ_METHOD);
        if(method !=null) {
            if(method.equals("GET")) {
                String URL=hm.get(QueryConstants.REQ_URI);
                System.out.println("PAGE in RES_LINKS >> "+page.getPageNumber());
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
                        System.out.println("prevGet in RES_LINKS >> "+prevGet);
                        if(page.getPageNumber()!=page.getNumberOfPages()) {
                            int next=page.getPageNumber()+1;
                            nextGet=URL.replace("pageNumber="+page.getPageNumber(), "pageNumber="+next);
                            System.out.println("nextGet in RES_LINKS >> "+nextGet);
                        }
                    }
                }
            }
        }
        try {
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
        catch(JsonProcessingException ex) {
            throw new RestException(5001,new LdsError(LdsError.JSON_ERR).
                    setContext(" in ResultPageLinks(Results, HashMap<String,String>) constructor ",ex));
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

    @Override
    public String toString() {
        return "ResultPageLinks [prevGet=" + prevGet + ", nextGet=" + nextGet + ", currJsonParams=" + currJsonParams
                + ", prevJsonParams=" + prevJsonParams + ", nextJsonParams=" + nextJsonParams + "]";
    }

}
