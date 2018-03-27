package io.bdrc.ldspdi.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.riot.resultset.ResultSetReaderFactory;
import org.apache.jena.riot.resultset.ResultSetReaderRegistry;
import org.junit.Test;

public class TemplateJsonOutput {
    
    
    public String Uri_URL="http://localhost:8080/test/IdentifierInfo?R_RES=bdr:V29329_I1KG15042";
    public String Lit3_URL="http://localhost:8080/test/Topic_byAllNames?L_NAME=rgyud&jsonOut";
    public String Lit2_URL="http://localhost:8080/test/Place_byName?L_NAME=dgon+gsar&jsonOut";
    
    
    public InputStream getInputStream(String Url) throws MalformedURLException, IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(Url).openConnection();
        return connection.getInputStream();
    }
    
    @Test
    public void parseJsonResult() throws MalformedURLException, IOException {
        ResultSet rs= ResultSetFactory.fromJSON(getInputStream(Lit3_URL));        
        ResultSetMgr.read(getInputStream(Lit3_URL), ResultSetLang.SPARQLResultSetJSON);
    }
    
}
