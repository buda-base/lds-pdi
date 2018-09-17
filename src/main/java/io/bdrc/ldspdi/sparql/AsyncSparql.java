package io.bdrc.ldspdi.sparql;

import java.util.HashMap;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.restapi.exceptions.RestException;

public class AsyncSparql implements Runnable{

    private String fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
    private String file;
    private HashMap<String,String> map;
    private ResultSet res;

    public AsyncSparql(String fusekiUrl, String file, HashMap<String, String> map) {
        super();
        this.fusekiUrl = fusekiUrl;
        this.file = file;
        this.map = map;
    }

    private void getTemplateResults() throws RestException {
        String etext_query=new QueryFileParser(file).getParametizedQuery(map,false);
        QueryExecution qexec=QueryProcessor.getResultSet(etext_query, fusekiUrl);
        res= qexec.execSelect();
    }

    public ResultSet getRes() {
        return res;
    }

    @Override
    public void run() {
        try {
            getTemplateResults();
        } catch (RestException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
