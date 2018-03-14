package io.bdrc.ldspdi.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.junit.Test;

public class TestOntModelQuery {
    
    public static final String owlURL="https://raw.githubusercontent.com/BuddhistDigitalResourceCenter/owl-schema/master/bdrc.owl";
    
    @Test
    public void testSpeed() throws IOException{
        
        /** Creating the model from the ontology owl file */
        URL url = new URL(owlURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream stream=connection.getInputStream();
        Model m = ModelFactory.createDefaultModel();
        m.read(stream, null, "RDF/XML");
        stream.close();
        Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
        InfModel infMod = ModelFactory.createInfModel(reasoner, m);
        OntModel ontMod = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, m);
        
        /** Querying the model **/
        String query=" select distinct ?s where {\n" + 
                "    ?s <http://www.w3.org/2000/01/rdf-schema#domain> <http://purl.bdrc.io/ontology/core/Work> .    \n" + 
                "} order by ?s";
        long start=System.currentTimeMillis();
        QueryExecution qexec = QueryExecutionFactory.create(query, m);
        ResultSet rs=qexec.execSelect();
        System.out.println("Exec time on simple model: "+(System.currentTimeMillis()-start));
        System.out.println(ResultSetFormatter.asText(rs));
        System.out.println("Exec time on simple model for processing result set: "+(System.currentTimeMillis()-start));
        
        start=System.currentTimeMillis();
        qexec = QueryExecutionFactory.create(query, infMod);
        rs=qexec.execSelect() ;
        System.out.println("Exec time on inferred model: "+(System.currentTimeMillis()-start));
        System.out.println(ResultSetFormatter.asText(rs));
        System.out.println("Exec time on inferred model for processing result set: "+(System.currentTimeMillis()-start));
        
        start=System.currentTimeMillis();
        qexec = QueryExecutionFactory.create(query, ontMod);
        rs=qexec.execSelect() ;
        System.out.println("Exec time on Ont model: "+(System.currentTimeMillis()-start));
        System.out.println(ResultSetFormatter.asText(rs));
        System.out.println("Exec time on Ont model for processing result set: "+(System.currentTimeMillis()-start));
    }

}
