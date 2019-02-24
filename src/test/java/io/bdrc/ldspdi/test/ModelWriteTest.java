package io.bdrc.ldspdi.test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFWriter;
import org.apache.jena.riot.Lang;
//import org.apache.jena.riot.RDFWriter;

public class ModelWriteTest {
	
	static String testUrl="https://raw.githubusercontent.com/buda-base/lds-pdi/master/test.ttl";
	//static String testUrl="https://raw.githubusercontent.com/buda-base/owl-schema/master/global.ttl";
	
	public static void main(String[] args) throws IOException {
		//loading ont model
		Model m = ModelFactory.createDefaultModel();
		HttpURLConnection connection = (HttpURLConnection) new URL(testUrl).openConnection();
        InputStream stream=connection.getInputStream();
		m.read(stream, "", "TURTLE");
		stream.close();
		OntModel ontMod=ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, m);
		
		RDFWriter wr=ontMod.getWriter("TURTLE");
		wr.setProperty("xmlbase", "http://purl.bdrc.io/ontology/ext/auth/");
		System.out.println("@base is present when");
		System.out.println("WRITING MODEL using wr.write(ontMod, System.out, \"http://purl.bdrc.io/ontology/ext/auth/\");");
		System.out.println("and wr=ontMod.getWriter(\"TURTLE\") and wr.setProperty(\"xmlbase\", \"http://purl.bdrc.io/ontology/ext/auth/\");");
		wr.write(ontMod, System.out, "http://purl.bdrc.io/ontology/ext/auth/");
		
		
		wr=ontMod.getWriter("RDF/XML");
		wr.setProperty("xmlbase", "http://purl.bdrc.io/ontology/ext/auth/");
		System.out.println("---------------------------------------------------------------------------------------");
		System.out.println("xml:base is present when");
		System.out.println("WRITING MODEL using wr.write(ontMod, System.out, \"http://purl.bdrc.io/ontology/ext/auth/\");");
		System.out.println("and wr=ontMod.getWriter(\"RDF/XML\"); and wr.setProperty(\"xmlbase\", \"http://purl.bdrc.io/ontology/ext/auth/\");");
		wr.write(ontMod, System.out, "http://purl.bdrc.io/ontology/ext/auth/");
		
	}

}
