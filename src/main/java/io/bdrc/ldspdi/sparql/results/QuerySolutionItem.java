package io.bdrc.ldspdi.sparql.results;

/*******************************************************************************
 * Copyright (c) 2018 Buddhist Digital Resource Center (BDRC)
 * 
 * If this file is a derivation of another work the license header will appear below; 
 * otherwise, this work is licensed under the Apache License, Version 2.0 
 * (the "License"); you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/


import java.util.HashMap;
import java.util.List;

import org.apache.jena.datatypes.xsd.impl.RDFLangString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class QuerySolutionItem {
        
    
    public final static Logger log=LoggerFactory.getLogger(QuerySolutionItem.class.getName());    
    public HashMap<String,Field> bindings;
    
    public QuerySolutionItem(QuerySolution qs,List<String> headers) {
        
        bindings=new HashMap<>();
        for(String key:headers) {
            RDFNode node=qs.get(key);            
            if(node !=null) {
                if(node.isResource()) {
                    bindings.put(key, new Field("uri",node.asResource().getURI()));                    
                } 
                if(node.isLiteral()) {                    
                    if(node.asNode().getLiteralDatatype().equals(RDFLangString.rdfLangString)) {
                        bindings.put(key, new LiteralStringField("literal",
                                                          node.asLiteral().getDatatypeURI(),
                                                          node.asNode().getLiteralLanguage(),
                                                          node.asLiteral().getLexicalForm()));
                    }                    
                    else {
                        bindings.put(key, new LiteralOtherField("literal",
                                node.asLiteral().getDatatypeURI(),                                
                                node.asLiteral().getValue().toString()));
                    }
                }
                if(node.isAnon()) {                    
                    bindings.put(key, new Field("bnode",node.toString()));                    
                }                
            }
        }        
    }
        
    public HashMap<String, Field> getBindings() {
        return bindings;
    }
    
    public Field getValue(String key) {
        return bindings.get(key);
    }

}
