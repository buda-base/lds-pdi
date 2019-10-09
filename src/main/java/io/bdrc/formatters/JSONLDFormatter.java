package io.bdrc.formatters;

import java.io.IOException;
import java.io.InputStream;
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
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFFormat.JSONLDVariant;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.writer.JsonLDWriter;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.utils.JsonUtils;

import io.bdrc.ldspdi.annotations.AnnotationEndpoint;
import io.bdrc.ldspdi.sparql.Prefixes;
import io.bdrc.ldspdi.utils.Helpers;

/*******************************************************************************
 * Copyright (c) 2017-2018 Buddhist Digital Resource Center (BDRC)
 *
 * If this file is a derivation of another work the license header will appear
 * below; otherwise, this work is licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

public class JSONLDFormatter {

    protected final static Map<DocType, Object> typeToFrameObject = new EnumMap<>(DocType.class);
    static final ObjectMapper mapper = new ObjectMapper();
    public static final Map<String, Object> bdoContextObject = getBDOContext();
    public static final Map<String, Object> annContextObject = getAnnMergedContext();
    public static final Map<String, Object> oaContextObject = getOaMergedContext();
    public static final String BDR = "http://purl.bdrc.io/resource/";
    public final static Logger log = LoggerFactory.getLogger(JSONLDFormatter.class);
    public final static String simpleContext = "http://purl.bdrc.io/context.jsonld";
    public final static String annoContext = "http://www.w3.org/ns/anno.jsonld";
    public final static String oaContext = "http://www.w3.org/ns/oa.jsonld";
    public final static String ldpContext = "http://www.w3.org/ns/ldp.jsonld";
    public static boolean prettyPrint = true;
    public final static JsonLdOptions jsonLdOptions = new JsonLdOptions();
    static {
        jsonLdOptions.setProcessingMode("json-ld-1.1");
        jsonLdOptions.setUseNativeTypes(true);
        jsonLdOptions.setCompactArrays(true);
        jsonLdOptions.setPruneBlankNodeIdentifiers(true);
    }

    static {
        initializeAnnFrameObjects();
    }

    public static enum DocType {
        CORPORATION, LINEAGE, ETEXT, ETEXTCONTENT, ROLE, PERSON, VOLUME, PLACE, TOPIC, ITEM, WORK, PRODUCT, TEST, ANN, ANC, ANP, OA;
    }

    public final static Map<DocType, Object> docTypeToSimpleContext = new HashMap<>();
    static {
        // these are what will appear in the @context property of the output,
        // just URIs replacing the whole context
        docTypeToSimpleContext.put(null, simpleContext);
        docTypeToSimpleContext.put(DocType.PERSON, simpleContext);
        docTypeToSimpleContext.put(DocType.VOLUME, simpleContext);
        docTypeToSimpleContext.put(DocType.WORK, simpleContext);
        docTypeToSimpleContext.put(DocType.PLACE, simpleContext);
        docTypeToSimpleContext.put(DocType.TOPIC, simpleContext);
        docTypeToSimpleContext.put(DocType.LINEAGE, simpleContext);
        docTypeToSimpleContext.put(DocType.CORPORATION, simpleContext);
        docTypeToSimpleContext.put(DocType.PRODUCT, simpleContext);
        docTypeToSimpleContext.put(DocType.ITEM, simpleContext);
        docTypeToSimpleContext.put(DocType.ROLE, simpleContext);
        docTypeToSimpleContext.put(DocType.ANN, Arrays.asList(simpleContext, annoContext));
        // this is what's in the context, so not OrderedCollection
        docTypeToSimpleContext.put(DocType.ANC, Arrays.asList(simpleContext, annoContext, ldpContext));
        docTypeToSimpleContext.put(DocType.ANP, Arrays.asList(simpleContext, annoContext));
        docTypeToSimpleContext.put(DocType.OA, Arrays.asList(simpleContext, oaContext));
    }

    public final static Map<DocType, Object> docTypeToContextObject = new HashMap<>();
    static {
        // these are what will be passed to the json-ld api, the complete context
        // objects
        docTypeToContextObject.put(null, bdoContextObject);
        docTypeToContextObject.put(DocType.PERSON, bdoContextObject);
        docTypeToContextObject.put(DocType.VOLUME, bdoContextObject);
        docTypeToContextObject.put(DocType.WORK, bdoContextObject);
        docTypeToContextObject.put(DocType.PLACE, bdoContextObject);
        docTypeToContextObject.put(DocType.TOPIC, bdoContextObject);
        docTypeToContextObject.put(DocType.LINEAGE, bdoContextObject);
        docTypeToContextObject.put(DocType.CORPORATION, bdoContextObject);
        docTypeToContextObject.put(DocType.PRODUCT, bdoContextObject);
        docTypeToContextObject.put(DocType.ITEM, bdoContextObject);
        docTypeToContextObject.put(DocType.ROLE, bdoContextObject);
        docTypeToContextObject.put(DocType.ANN, annContextObject);
        docTypeToContextObject.put(DocType.ANC, annContextObject);
        docTypeToContextObject.put(DocType.ANP, annContextObject);
        docTypeToContextObject.put(DocType.OA, oaContextObject);
    }

    public static final Map<String, DocType> typeToDocType = new HashMap<>();
    static {
        typeToDocType.put("Person", DocType.PERSON);
        typeToDocType.put("Volume", DocType.VOLUME);
        typeToDocType.put("Work", DocType.WORK);
        typeToDocType.put("Place", DocType.PLACE);
        typeToDocType.put("Topic", DocType.TOPIC);
        typeToDocType.put("Lineage", DocType.LINEAGE);
        typeToDocType.put("Corporation", DocType.CORPORATION);
        typeToDocType.put("Product", DocType.PRODUCT);
        typeToDocType.put("Item", DocType.ITEM);
        typeToDocType.put("Role", DocType.ROLE);
        typeToDocType.put("Annotation", DocType.ANN);
        typeToDocType.put("OrderedCollection", DocType.ANC);
        typeToDocType.put("OrderedCollectionPage", DocType.ANP);
    }

    public static final Map<DocType, Object> typeToRootShortUri = new EnumMap<>(DocType.class);
    static {
        typeToRootShortUri.put(DocType.PERSON, "Person");
        typeToRootShortUri.put(DocType.VOLUME, Arrays.asList("Volume", "VolumeImageAsset", "VolumePhysicalAsset"));
        typeToRootShortUri.put(DocType.WORK, "Work");
        typeToRootShortUri.put(DocType.PLACE, "Place");
        typeToRootShortUri.put(DocType.TOPIC, "Topic");
        typeToRootShortUri.put(DocType.LINEAGE, "Lineage");
        typeToRootShortUri.put(DocType.CORPORATION, "Corporation");
        typeToRootShortUri.put(DocType.PRODUCT, "adm:Product");
        typeToRootShortUri.put(DocType.ITEM, Arrays.asList("Item", "ItemImageAsset", "ItemInputEtext", "ItemOCREtext", "ItemPhysicalAsset"));
        typeToRootShortUri.put(DocType.ROLE, "Role");
        typeToRootShortUri.put(DocType.ANN, "Annotation");
        // this is what's in the context, so not OrderedCollection
        typeToRootShortUri.put(DocType.ANC, "as:OrderedCollection");
        typeToRootShortUri.put(DocType.ANP, "AnnotationPage");
    }

    public static Map<String, Object> getBDOContext() {
        Map<String, Map<String, Object>> map = null;
        try {
            URL url = new URL("https://raw.githubusercontent.com/buda-base/owl-schema/master/context.jsonld");
            map = mapper.readValue(url, new TypeReference<Map<String, Map<String, Object>>>() {
            });
        } catch (Exception e) {
            log.error("Error reading context file :" + e);
            e.printStackTrace();
            return null;
        }
        return map.get("@context");
    }

    // return the object corresponding to the context that needs to
    // be associated with an annotation type for framing. It's a merge
    // of the json file in src/main/resources/context/ and the BDO context
    public static Map<String, Object> getAnnMergedContext() {
        final Map<String, Object> res = new HashMap<>();
        res.putAll(bdoContextObject);
        Map<String, Map<String, Object>> map = null;
        try {
            InputStream is = Helpers.getResourceOrFile("contexts/ldp.jsonld");
            map = mapper.readValue(is, new TypeReference<Map<String, Map<String, Object>>>() {
            });
            res.putAll(map.get("@context"));
            is = Helpers.getResourceOrFile("contexts/anno.jsonld");
            map = mapper.readValue(is, new TypeReference<Map<String, Map<String, Object>>>() {
            });
            res.putAll(map.get("@context"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        // we need to not compact these values, see
        // https://github.com/json-ld/json-ld.org/issues/679
        res.remove(AnnotationEndpoint.ANN_PREFIX_SHORT);
        res.remove(AnnotationEndpoint.ANC_PREFIX_SHORT);
        return res;
    }

    public static Map<String, Object> getOaMergedContext() {
        final Map<String, Object> res = new HashMap<>();
        res.putAll(bdoContextObject);
        Map<String, Map<String, Object>> map = null;
        try {
            InputStream is = Helpers.getResourceOrFile("contexts/oa.jsonld");
            map = mapper.readValue(is, new TypeReference<Map<String, Map<String, Object>>>() {
            });
            res.putAll(map.get("@context"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        res.remove(AnnotationEndpoint.ANN_PREFIX_SHORT);
        res.remove(AnnotationEndpoint.ANC_PREFIX_SHORT);
        return res;
    }

    private static void initializeAnnFrameObjects() {
        Map<String, Object> res = null;
        try {
            InputStream is = Helpers.getResourceOrFile("contexts/annotation_frame.jsonld");
            res = mapper.readValue(is, new TypeReference<Map<String, Object>>() {
            });
            res.put("@context", annContextObject);
            typeToFrameObject.put(DocType.ANN, res);
            typeToFrameObject.put(DocType.OA, res);
            is = Helpers.getResourceOrFile("contexts/collection_frame.jsonld");
            res = mapper.readValue(is, new TypeReference<Map<String, Object>>() {
            });
            res.put("@context", annContextObject);
            typeToFrameObject.put(DocType.ANC, res);
            is = Helpers.getResourceOrFile("contexts/page_frame.jsonld");
            res = mapper.readValue(is, new TypeReference<Map<String, Object>>() {
            });
            res.put("@context", annContextObject);
            typeToFrameObject.put(DocType.ANP, res);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Object getFrameObject(DocType type, String mainResourceUri) {
        // for works, we frame by @id, for cases with outlines
        boolean needsId = (type == DocType.WORK || type == DocType.TEST);
        if (!needsId && typeToFrameObject.containsKey(type))
            return typeToFrameObject.get(type);
        Map<String, Object> jsonObject = new HashMap<>();
        if (needsId) {
            jsonObject.put("@id", mainResourceUri);
        } else {
            jsonObject.put("@type", typeToRootShortUri.get(type));
            typeToFrameObject.put(type, jsonObject);
        }
        jsonObject.put("@context", bdoContextObject);
        return jsonObject;
    }

    static class JsonLDComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            if (s1.equals("adm:logEntry"))
                return 1;
            if (s2.equals("adm:logEntry"))
                return -1;
            if (s1.startsWith("adm:"))
                return 1;
            if (s1.startsWith("tbr:"))
                return 1;
            if (s2.startsWith("adm:"))
                return -1;
            if (s2.startsWith("tbr:"))
                return -1;
            if (s1.equals("@context"))
                return 1;
            if (s1.equals("@graph"))
                return -1;
            if (s1.equals("rdfs:label"))
                return -1;
            if (s1.equals("skos:prefLabel"))
                return -1;
            if (s1.equals("skos:altLabel"))
                return -1;
            return s1.compareTo(s2);
        }
    }

    @SuppressWarnings("unchecked")
    protected static void insertRec(String k, Object v, SortedMap<String, Object> tm) throws IllegalArgumentException {
        if (k.equals("@graph")) {
            if (v instanceof ArrayList) {
                if (((ArrayList<Object>) v).size() == 0) {
                    tm.put(k, v);
                    // throw new IllegalArgumentException("empty graph, shouldn't happen!");
                    return;
                }
                Object o = ((ArrayList<Object>) v).get(0);
                if (o instanceof Map) {
                    Map<String, Object> orderedo = orderEntries((Map<String, Object>) o);
                    ((ArrayList<Object>) v).set(0, orderedo);
                }
                tm.put(k, v);
            } else {// supposing v instance of Map
                tm.put(k, orderEntries((Map<String, Object>) v));
            }
        } else {
            tm.put(k, v);
        }
    }

    // reorder list
    protected static Map<String, Object> orderEntries(Map<String, Object> input) throws IllegalArgumentException {
        SortedMap<String, Object> res = new TreeMap<>(new JsonLDComparator());
        // TODO: maybe it should be recursive? at least for outlines...
        input.forEach((k, v) -> insertRec(k, v, res));
        return res;
    }

    public static DocType getDocType(final Model m, final String mainResourceUri) {
        final NodeIterator ni = m.listObjectsOfProperty(m.getResource(mainResourceUri), RDF.type);
        DocType res = null;
        while (ni.hasNext()) {
            final RDFNode n = ni.next();
            final String t = n.asResource().getLocalName();
            res = typeToDocType.get(t);
            if (res != null)
                return res;
        }
        return res;
    }

    public static Map<String, Object> modelToJsonObject(final Model m, final String mainResourceUri, DocType type) {
        if (type == null) {
            type = getDocType(m, mainResourceUri);
            if (type == null) {
                log.info("not able to determine type of resource {} for frame output, outputting compact", mainResourceUri);
                return modelToJsonObject(m, null, mainResourceUri, RDFFormat.JSONLD_COMPACT_PRETTY, false);
            }
        }
        return modelToJsonObject(m, type, mainResourceUri, RDFFormat.JSONLD_FRAME_PRETTY, false);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> modelToJsonObject(final Model m, final DocType type, final String mainResourceUri, RDFFormat format, final boolean reorder) {
        final JsonLDWriteContext ctx = new JsonLDWriteContext();
        if (format.equals(RDFFormat.JSONLD_FRAME_PRETTY) || format.equals(RDFFormat.JSONLD_FRAME_FLAT)) {
            final Object frameObj = getFrameObject(type, mainResourceUri);
            ctx.setFrame(frameObj);
        }
        final JSONLDVariant variant = (RDFFormat.JSONLDVariant) format.getVariant();
        ctx.setJsonLDContext(docTypeToContextObject.get(type));
        ctx.setOptions(jsonLdOptions);
        final DatasetGraph g = DatasetFactory.create(m).asDatasetGraph();
        final PrefixMap pm = Prefixes.getPrefixMap();
        Map<String, Object> tm;
        try {
            tm = (Map<String, Object>) JsonLDWriter.toJsonLDJavaAPI(variant, g, pm, null, ctx);
            // replacing context with URI
            tm.replace("@context", docTypeToSimpleContext.get(type));
            if (reorder)
                tm = orderEntries(tm);
        } catch (JsonLdError | IOException e) {
            e.printStackTrace();
            return null;
        }
        return tm;
    }

    public static void jsonObjectToOutputStream(Object jsonObject, OutputStream out) {
        Writer wr = new OutputStreamWriter(out, Chars.charsetUTF8);
        try {
            if (prettyPrint) {
                JsonUtils.writePrettyPrint(wr, jsonObject);
                wr.write("\n");
            } else {
                JsonUtils.write(wr, jsonObject);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        IO.flush(wr);
    }

    public static void writeModelAsCompact(Model m, OutputStream out) {
        Object jsonO = modelToJsonObject(m, null, null, RDFFormat.JSONLD_COMPACT_PRETTY, false);
        jsonObjectToOutputStream(jsonO, out);
    }

}
