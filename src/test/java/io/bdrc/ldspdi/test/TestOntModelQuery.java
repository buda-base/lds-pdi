package io.bdrc.ldspdi.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;

public class TestOntModelQuery {
    
    public static String prefixes="PREFIX : <http://purl.bdrc.io/ontology/core/>\n" + 
            " PREFIX adm: <http://purl.bdrc.io/ontology/admin/>\n" + 
            " PREFIX bdr: <http://purl.bdrc.io/resource/>\n" + 
            " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" + 
            " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + 
            " PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" + 
            " PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" + 
            " PREFIX bf: <http://id.loc.gov/ontologies/bibframe/>\n" + 
            " PREFIX tbr: <http://purl.bdrc.io/ontology/toberemoved/>\n" + 
            " PREFIX vcard: <http://www.w3.org/2006/vcard/ns#>\n" + 
            " PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" + 
            " PREFIX text: <http://jena.apache.org/text#>\n" + 
            " PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" + 
            " PREFIX dcterms: <http://purl.org/dc/terms/>\n" + 
            " PREFIX f: <java:io.bdrc.ldspdi.sparql.functions.>";
    
    public static final String owlURL="https://raw.githubusercontent.com/BuddhistDigitalResourceCenter/owl-schema/master/bdrc.owl";
    
    public static void main(String[] args) throws IOException{
        
        /** Creating the model from the ontology owl file */
        URL url = new URL(owlURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream stream=connection.getInputStream();
        Model m = ModelFactory.createDefaultModel();
        m.read(stream, "", "RDF/XML");
        stream.close();
        Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
        InfModel infMod=ModelFactory.createInfModel(reasoner, m);
        
        /** Querying the model **/
        String query=prefixes+ " select distinct ?s where {\n" + 
                "    ?s rdfs:domain <http://purl.bdrc.io/ontology/core/Work> .    \n" + 
                "} order by ?s";
        QueryExecution qexec = QueryExecutionFactory.create(query, infMod);
        long start=System.currentTimeMillis();
        ResultSet res = qexec.execSelect() ;
        System.out.println("Exec time for producing a result set >>"+(System.currentTimeMillis()-start));
        System.out.println(ResultSetFormatter.asText(res));
        System.out.println("Exec time fro processing and displaying the result set >>"+(System.currentTimeMillis()-start));
    }

}
