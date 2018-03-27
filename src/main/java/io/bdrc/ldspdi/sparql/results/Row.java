package io.bdrc.ldspdi.sparql.results;

import java.util.HashMap;
import java.util.List;

import org.apache.jena.datatypes.xsd.impl.RDFLangString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;

public class Row extends HashMap<String,HashMap<String,String>>{
    
    
    public Row(List<String> headers,QuerySolution qs) {
        //See specs at : https://www.w3.org/TR/sparql11-results-json/ 
        for(String key:headers) {
            RDFNode node=qs.get(key);            
            if(node !=null) {
                if(node.isResource()) {
                    this.put(key, new Field("uri",node.asResource().getURI()));                    
                }                
                if(node.isLiteral()) { 
                    if(node.asNode().getLiteralDatatype() !=null) {
                        if(node.asNode().getLiteralDatatype().equals(RDFLangString.rdfLangString)) {
                            //langString datatype xml:lang is defined (2nd literal case in specs)
                            this.put(key, new LiteralStringField("literal",                                                          
                                                              node.asNode().getLiteralLanguage(),
                                                              node.asLiteral().getLexicalForm()));
                        }                    
                        else {
                            //datatype is found but different from langString (3rd literal case in specs)
                            this.put(key, new LiteralOtherField("literal",
                                    node.asLiteral().getDatatypeURI(),                                
                                    node.asLiteral().getValue().toString()));
                        }
                    }else {
                        //No datatype ?? (Fist literal case in json results specs (Literal S | {"type": "literal","value": "S"})
                        this.put(key,new Field("literal",node.asLiteral().getValue().toString()));
                    }
                }
                if(node.isAnon()) {                    
                    this.put(key, new Field("bnode",node.toString()));                    
                }                
            }
        }        
    }

}
