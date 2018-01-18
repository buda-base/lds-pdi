package io.bdrc.ldspdi.test;

import org.apache.jena.fuseki.embedded.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ModelFactory;

public class LdspdiTest {
	
	public static void main(String[] args) {
	    Dataset srvds = DatasetFactory.createTxnMem();
	    srvds.setDefaultModel(ModelFactory.createDefaultModel());
        //Create a fuseki server
	    FusekiServer server = FusekiServer.create()
                .setPort(2244)
                .add("/bdrcrw", srvds)
                .build() ;
        server.start();
	    String fusekiUrl="http://localhost:2244/bdrcrw";
        try {           
            Query q=QueryFactory.create("select ?s where { ?s ?p ?o .}limit 10");
            QueryExecution qe = QueryExecutionFactory.create(q, srvds);
            ResultSet rs=qe.execSelect();
            System.out.println("things work on the dataset directly");
            qe = QueryExecutionFactory.sparqlService(fusekiUrl,q);
            rs=qe.execSelect();
            System.out.println("this is never reached because FusekiServer ");  
        }catch(Exception ex) {
            server.stop();
            ex.printStackTrace();
        }
	}

}
