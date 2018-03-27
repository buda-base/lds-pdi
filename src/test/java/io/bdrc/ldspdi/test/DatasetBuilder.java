package io.bdrc.ldspdi.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;

public class DatasetBuilder {
    
    static String Work_chos_dbyings="construct { ?s ?p ?o }\n" + 
            "where {\n" + 
            "  { \n" + 
            "    ?s ?p ?o .\n" + 
            "    ?s a :Work .\n" + 
            "    ?s skos:prefLabel ?l .\n" + 
            "    Filter(contains(?l,\"chos dbyings\"))\n" + 
            "  }\n" + 
            "}";
    
    static String prefixes="PREFIX : <http://purl.bdrc.io/ontology/core/>\n" + 
            " PREFIX bdo: <http://purl.bdrc.io/ontology/core/>\n" + 
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
    
    static String fusekiUrl="http://buda1.bdrc.io:13180/fuseki/bdrcrw/query";
    
    public static void writeModel(String construct,String filename) throws IOException {
        Model model=QueryProcessor.getGraph(prefixes+" "+construct, fusekiUrl);
        FileWriter fw=new FileWriter(new File(TestUtils.TESTDIR+filename));
        model.write(fw, Lang.TURTLE.getName());
        fw.close();
    }
    
    public static void main(String[] args) throws IOException {
        ServiceConfig.initForTests();
        DatasetBuilder.writeModel(Work_chos_dbyings, "W_Chos_Yin.ttl");
    }
}
