package io.bdrc.ldspdi.results;

import java.util.HashMap;
import java.util.List;

import org.apache.jena.datatypes.xsd.impl.RDFLangString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.Util;

@SuppressWarnings("serial")
public class Row extends HashMap<String,HashMap<String,String>>{


    String lang = null;
    String value = null;

    public Row(List<String> headers, QuerySolution qs) {
        //See specs at : https://www.w3.org/TR/sparql11-results-json/
        for(String key: headers) {
            RDFNode node = qs.get(key);
            if (node !=null) {
                if (node.isResource()) {
                    this.put(key, new Field("uri",node.asResource().getURI()));
                }
                if (node.isLiteral()) {
                    Literal lit = node.asLiteral();
                    if (Util.isSimpleString(lit) || Util.isLangString(lit)) {
                        if (node.asNode().getLiteralDatatype().equals(RDFLangString.rdfLangString)) {
                            this.put(key, new LiteralStringField("literal",
                                    node.asNode().getLiteralLanguage(),
                                    node.asLiteral().getValue().toString()));
                        }else {
                            this.put(key,new Field("literal",node.asLiteral().getValue().toString()));
                        }
                    }else {
                        this.put(key, new LiteralOtherField("literal",
                                node.asLiteral().getDatatypeURI(),
                                node.asLiteral().getValue().toString()));
                    }
                }
                if(node.isAnon()) {
                    this.put(key, new Field("bnode",node.asNode().getBlankNodeLabel()));
                }
            }
        }
    }
}
