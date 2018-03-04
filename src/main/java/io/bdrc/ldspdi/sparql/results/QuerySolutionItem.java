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

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class QuerySolutionItem {
        
    
    public final static Logger log=LoggerFactory.getLogger(QuerySolutionItem.class.getName());    
    public HashMap<String,Field> dataRow;
    
    public QuerySolutionItem(QuerySolution qs,List<String> headers) {
        
        dataRow=new HashMap<>();
        for(String key:headers) {
            RDFNode node=qs.get(key);
            
            if(node !=null) { 
                
                if(node.isResource()) {
                    Resource res=node.asResource();
                    String Uri=res.getURI();
                    Field f=new Field("resource",Uri);
                    dataRow.put(key, f);                    
                } 
                if(node.isLiteral()) {
                    String lang = node.asNode().getLiteralLanguage();
                    LiteralField lf=new LiteralField(node.asLiteral().getDatatypeURI(),lang,node.asLiteral().getLexicalForm());
                    dataRow.put(key, lf);                    
                }
                if(node.isAnon()) {
                    Field f=new Field("Anonym node",node.toString());
                    dataRow.put(key, f);                    
                }                
            }else {
                Field f=new Field("Null node","");
                dataRow.put(key, f);
            }
        }        
    }
        
    public HashMap<String, Field> getDataRow() {
        return dataRow;
    }
    
    public Field getValue(String key) {
        return dataRow.get(key);
    }

}
