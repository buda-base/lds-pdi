package io.bdrc.ldspdi.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFWriter;

public class ModelWriteTest {
	
	static String testUrl="https://raw.githubusercontent.com/buda-base/lds-pdi/master/test.ttl";
	
	public static void main(String[] args) throws IOException {
		//loading ont model
		Model m = ModelFactory.createDefaultModel();
		HttpURLConnection connection = (HttpURLConnection) new URL(testUrl).openConnection();
        InputStream stream=connection.getInputStream();
		m.read(stream, "", "TURTLE");
		stream.close();
		
		System.out.println("@base is present when");
		System.out.println("WRITING MODEL m.write(System.out,\"TURTLE\",\"http://purl.bdrc.io/ontology/ext/auth/\")");
		m.write(System.out,"TURTLE","http://purl.bdrc.io/ontology/ext/auth/");
		System.out.println("---------------------------------------------------------------------------------------");
		System.out.println("xml:base is lacking when");
		System.out.println("WRITING MODEL m.write(System.out,\"TURTLE\",\"http://purl.bdrc.io/ontology/ext/auth/\")");
		m.write(System.out,"RDF/XML","http://purl.bdrc.io/ontology/ext/auth/");
		
		System.out.println("---------------------------------------------------------------------------------------");
		System.out.println("@base is present when");
		System.out.println("WRITING MODEL IN TURTLE USING RIOT RDFWRITER");
		RDFWriter.create()
    	.source(m.getGraph())
    	.base("http://purl.bdrc.io/ontology/ext/auth/")
    	.lang(Lang.TURTLE)
    	.build().output(System.out);
		
		System.out.println("---------------------------------------------------------------------------------------");
		System.out.println("xml:base is lacking when");
		System.out.println("WRITING MODEL IN RDF/XML USING RIOT RDFWRITER");
		RDFWriter.create()
    	.source(m.getGraph())
    	.base("http://purl.bdrc.io/ontology/ext/auth/")
    	.lang(Lang.RDFXML)
    	.build().output(System.out);
	}

}
