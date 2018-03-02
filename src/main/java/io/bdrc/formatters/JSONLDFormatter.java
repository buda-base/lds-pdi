package io.bdrc.formatters;

import java.io.IOException;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.lib.Chars;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFFormat.JSONLDVariant;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.riot.writer.JsonLDWriter;
import org.apache.jena.sparql.core.DatasetGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.utils.JsonUtils;

/*******************************************************************************
 * Copyright (c) 2017 Buddhist Digital Resource Center (BDRC)
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

public class JSONLDFormatter {
    
    protected final static Map<DocType,Object> typeToFrameObject = new EnumMap<>(DocType.class);
    static final ObjectMapper mapper = new ObjectMapper();
    public static final Map<String,Object> jsonldcontext = getJsonLdContext(); // todo: read the thingy
    public static final String BDR = "http://purl.bdrc.io/resource/";
    //public static Writer logWriter = new PrintWriter(new OutputStreamWriter(System.err));	
    public final static Logger log=LoggerFactory.getLogger(JSONLDFormatter.class.getName());
    
    public static enum DocType {
	    CORPORATION,
	    LINEAGE,
	    ETEXT,
	    ETEXTCONTENT,
	    OFFICE,
	    PERSON,
	    PLACE,
	    TOPIC,
	    ITEM,
	    WORK,
	    PRODUCT,
	    TEST
	    ;  
	  }
    
    protected static final HashMap<String,DocType> initialToDocType = new HashMap<>();
    static {
    	initialToDocType.put("P", DocType.PERSON);
    	initialToDocType.put("W", DocType.WORK);
    	initialToDocType.put("G", DocType.PLACE);
    	initialToDocType.put("T", DocType.TOPIC);
    	initialToDocType.put("L", DocType.LINEAGE);
    	initialToDocType.put("C", DocType.CORPORATION);
    	initialToDocType.put("PR", DocType.PRODUCT);
    	initialToDocType.put("I", DocType.ITEM);
    	initialToDocType.put("R", DocType.OFFICE);    	
    }
    
    public static final Map<DocType,Object> typeToRootShortUri = new EnumMap<>(DocType.class);
    static {
        typeToRootShortUri.put(DocType.PERSON, "Person");
        typeToRootShortUri.put(DocType.WORK, "Work");
        typeToRootShortUri.put(DocType.PLACE, "Place");
        typeToRootShortUri.put(DocType.TOPIC, "Topic");
        typeToRootShortUri.put(DocType.LINEAGE, "Lineage");
        typeToRootShortUri.put(DocType.CORPORATION, "Corporation");
        typeToRootShortUri.put(DocType.PRODUCT, "adm:Product");
        typeToRootShortUri.put(DocType.ITEM, Arrays.asList("Item", "ItemImageAsset", "ItemInputEtext", "ItemOCREtext", "ItemPhysicalAsset"));
        typeToRootShortUri.put(DocType.OFFICE, "Role");        
    }
    
    public static Map<String,Object> getJsonLdContext() {
        Map<String, Map<String,Object>> map = null;
        try {
            URL url = new URL("https://raw.githubusercontent.com/BuddhistDigitalResourceCenter/owl-schema/master/context.jsonld");
            map = mapper.readValue(url, new TypeReference<Map<String, Map<String,Object>>>(){});                       
        } catch (Exception e) {
            log.error("Error reading context file :"+ e);
            e.printStackTrace();
            return null;
        }
        return map.get("@context");
    }
    
    public static Object getFrameObject(DocType type, String mainResourceName) {
        // for works, we frame by @id, for cases with outlines
        boolean needsId = (type == DocType.WORK || type == DocType.TEST);  
        if (!needsId && typeToFrameObject.containsKey(type))
            return typeToFrameObject.get(type);
        Map<String,Object> jsonObject = new HashMap<>(); 
        if (needsId) {
            jsonObject.put("@id", BDR+mainResourceName);
        } else {
            jsonObject.put("@type", typeToRootShortUri.get(type));
            typeToFrameObject.put(type, jsonObject);
        }
        jsonObject.put("@context", jsonldcontext);
        return jsonObject;
    }
    
    public static Object getFrameObject(String mainResourceName) {
    	DocType type =initialToDocType.get(mainResourceName.substring(0,1));
        return getFrameObject(type, mainResourceName);
    }
    
    static class MigrationComparator implements Comparator<String>
     {
         public int compare(String s1, String s2)
         {
             if(s1.equals("adm:logEntry")) return 1;
             if(s2.equals("adm:logEntry")) return -1;
             if(s1.startsWith("adm:")) return 1;
             if(s1.startsWith("tbr:")) return 1;
             if(s2.startsWith("adm:")) return -1;
             if(s2.startsWith("tbr:")) return -1;
             if(s1.equals("@context")) return 1;
             if(s1.equals("@graph")) return -1;
             if(s1.equals("rdfs:label")) return -1;
             if(s1.equals("skos:prefLabel")) return -1;
             if(s1.equals("skos:altLabel")) return -1;
             return s1.compareTo(s2);
         }
     }
     
     @SuppressWarnings("unchecked")
     protected static void insertRec(String k, Object v, SortedMap<String,Object> tm) throws IllegalArgumentException {
         if (k.equals("@graph")) {
             if (v instanceof ArrayList) {
                 if (((ArrayList<Object>) v).size() == 0) {
                     tm.put(k,v);
                     //throw new IllegalArgumentException("empty graph, shouldn't happen!");
                     return;
                 }
                 Object o = ((ArrayList<Object>) v).get(0);
                 if (o instanceof Map) {
                     Map<String,Object> orderedo = orderEntries((Map<String,Object>) o);
                     ((ArrayList<Object>) v).set(0, orderedo);
                 }
                 tm.put(k, v);
             } else {// supposing v instance of Map
                 tm.put(k, orderEntries( (Map<String,Object>) v));
             }
         } else {
             tm.put(k, v);
         }
     }
     
     // reorder list
     protected static Map<String,Object> orderEntries(Map<String,Object> input) throws IllegalArgumentException
     {
         SortedMap<String,Object> res = new TreeMap<>(new MigrationComparator());
         // TODO: maybe it should be recursive? at least for outlines...
         input.forEach( (k,v) ->  insertRec(k, v, res) );
         return res; 
     }
     
     public static Map<String,Object> modelToJsonObject(Model m, DocType type, String mainResourceName) {
         return modelToJsonObject(m, type, mainResourceName, RDFFormat.JSONLD_FRAME_PRETTY);
     }
     
     public static Map<String,Object> modelToJsonObject(Model m, String mainResourceName) {
    	 DocType type;
    	 if(mainResourceName.startsWith("PR")) {
    		 type =initialToDocType.get("PR");
    	 }else {
    		 type =initialToDocType.get(mainResourceName.substring(0,1));
    	 }    	 
    	 return modelToJsonObject(m, type, mainResourceName, RDFFormat.JSONLD_FRAME_PRETTY);
     }
     
     @SuppressWarnings("unchecked")
    public static Map<String,Object> modelToJsonObject(Model m, DocType type, String mainResourceName, RDFFormat format) {
         JsonLDWriteContext ctx = new JsonLDWriteContext();
         JSONLDVariant variant;
         if (format.equals(RDFFormat.JSONLD_FRAME_PRETTY)) { 
             Object frameObj = getFrameObject(type, mainResourceName);
             ctx.setFrame(frameObj);
         }
         variant = (RDFFormat.JSONLDVariant) format.getVariant();
         ctx.setJsonLDContext(jsonldcontext);
         JsonLdOptions opts = new JsonLdOptions();
         opts.setUseNativeTypes(true);
         opts.setCompactArrays(true);
         opts.setPruneBlankNodeIdentifiers(true);
         ctx.setOptions(opts);
         DatasetGraph g = DatasetFactory.create(m).asDatasetGraph();
         PrefixMap pm = RiotLib.prefixMap(g);
         String base = null;
         Map<String,Object> tm;
         try {
             tm = (Map<String,Object>) JsonLDWriter.toJsonLDJavaAPI(variant, g, pm, base, ctx);
             // replacing context with URI
             //tm.replace("@context", "http://purl.bdrc.io/context.jsonld");
             tm = orderEntries(tm);
         } catch (JsonLdError | IOException e) {
             e.printStackTrace();
             return null;
         }
         return tm;
     }
     
     public static void jsonObjectToOutputStream(Object jsonObject, OutputStream out) {
         Writer wr = new OutputStreamWriter(out, Chars.charsetUTF8) ;
         try {
             JsonUtils.writePrettyPrint(wr, jsonObject) ;
             wr.write("\n");
         } catch (IOException e) {
             e.printStackTrace();
         }
         IO.flush(wr) ;
     } 
     
}

