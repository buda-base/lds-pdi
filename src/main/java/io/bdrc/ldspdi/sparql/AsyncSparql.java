package io.bdrc.ldspdi.sparql;

import java.util.HashMap;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.restapi.exceptions.RestException;

public class AsyncSparql implements Runnable{
    
    public String fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
    public String file;
    public HashMap<String,String> map;
    public ResultSet res;
    
    public AsyncSparql(String fusekiUrl, String file, HashMap<String, String> map) {
        super();
        this.fusekiUrl = fusekiUrl;
        this.file = file;
        this.map = map;
    }

    public void getTemplateResults() throws RestException {        
        String etext_query=new QueryFileParser(file).getParametizedQuery(map,false);
        QueryExecution qexec=QueryProcessor.getResultSet(etext_query, fusekiUrl);
        res= qexec.execSelect();        
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
