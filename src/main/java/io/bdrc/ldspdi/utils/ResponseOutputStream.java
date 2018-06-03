package io.bdrc.ldspdi.utils;

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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.writer.TriGWriter;
import org.apache.jena.sparql.core.DatasetImpl;
import org.apache.jena.sparql.util.Context;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.formatters.JSONLDFormatter;
import io.bdrc.formatters.TTLRDFWriter;

public class ResponseOutputStream {

      
    public static StreamingOutput getJsonResponseStream(Object toJson) {        
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                ObjectMapper mapper = new ObjectMapper();
                mapper.writerWithDefaultPrettyPrinter().writeValue(os , toJson);                    
            }
        };
        return stream;
    }
    
    public static StreamingOutput getJsonLDResponseStream(Object toJson) {
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                JSONLDFormatter.jsonObjectToOutputStream(toJson, os);                   
            }
        };
        return stream;
    }
    
    public static StreamingOutput getModelStream(final Model model, String format, final String res) {
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {                
                 
                if(MediaTypeUtils.getJenaFromExtension(format)!=null && !format.equalsIgnoreCase("ttl")){
                    if(format.equalsIgnoreCase("jsonld")) {      
                        Object json = JSONLDFormatter.modelToJsonObject(model, res);
                        JSONLDFormatter.jsonObjectToOutputStream(json, os);
                    } else {
                        model.write(os,MediaTypeUtils.getJenaFromExtension(format));
                    }
                } else {
                    RDFWriter writer=TTLRDFWriter.getSTTLRDFWriter(model);                   
                    writer.output(os);                                      
                }                
            }
        };
        return stream;
    }
    
    public static StreamingOutput getModelStream(Model model, String format) {        
        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {                
                System.out.println("getJenaFromExtension :"+format+" >>> "+MediaTypeUtils.getJenaFromExtension(format)); 
                if(MediaTypeUtils.getJenaFromExtension(format)!=null && !format.equalsIgnoreCase("ttl")){
                    if(format.equalsIgnoreCase("jsonld")) {                        
                        JSONLDFormatter.writeModelAsCompact(model, os);
                    } 
                    else {
                        model.write(os,MediaTypeUtils.getJenaFromExtension(format)); 
                    }
                } else {
                    RDFWriter writer=TTLRDFWriter.getSTTLRDFWriter(model);                   
                    writer.output(os);                                      
                }                
            }
        };
        return stream;
    }
    
    public static StreamingOutput getModelStream(Model model) {        
        StreamingOutput stream = new StreamingOutput() {            
            public void write(OutputStream os) throws IOException, WebApplicationException {             
                RDFWriter writer=TTLRDFWriter.getSTTLRDFWriter(model); 
                writer.output(os); 
            }
        };
        return stream;
    }
    
    public static StreamingOutput getStringStream(String toWrite) {        
        StreamingOutput stream = new StreamingOutput() {            
            public void write(OutputStream os) throws IOException, WebApplicationException {             
                Writer w = new OutputStreamWriter(os); 
                w.write(toWrite);
            }
        };
        return stream;
    }

}
