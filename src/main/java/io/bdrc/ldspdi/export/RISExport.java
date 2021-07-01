package io.bdrc.ldspdi.export;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.bdrc.ldspdi.export.CSLJsonExport.CSLResObj;

public class RISExport {
    private final static String TAG_SEPARATOR = "  - ";
    private final static String LINE_SEPARATOR = "\n";
    
    
    public static final class RISObject {
        public String type = "BOOK";
        public Map<String,List<String>> fields = new HashMap<>();
        
        public void addFieldValue(final String field, final String value) {
            List<String> values = fields.computeIfAbsent(field, f -> new ArrayList<>());
            values.add(value);
        }
        
        public void writeTo(OutputStream out) throws IOException {
            StringBuilder builder = new StringBuilder();
            builder.append("TY  - ");
            builder.append(this.type);
            builder.append(LINE_SEPARATOR);
            for (Entry<String,List<String>> e : this.fields.entrySet()) {
                final String fieldName = e.getKey();
                final List<String> values = e.getValue();
                for (final String value : values) {
                    builder.append(fieldName);
                    builder.append(TAG_SEPARATOR);
                    builder.append(value);
                    builder.append(LINE_SEPARATOR);
                }
                builder.append("ER");
                builder.append(TAG_SEPARATOR);
                builder.append(LINE_SEPARATOR);
                builder.append(LINE_SEPARATOR);
            }
            out.write(builder.toString().getBytes());
        }
    }
    
    public static void simpleMapping(final RISObject ris, final CSLResObj csl, final String lang, final String risField, final String cslField) {
        final ObjectNode on = csl.getObjectNode(lang);
        if (on.has(cslField)) {
            final String val = on.get(cslField).asText();
            ris.addFieldValue(risField, val);
        }
    }
    
    public static void dateMapping(final RISObject ris, final CSLResObj csl, final String lang, final String risField, final String cslField) {
        
    }
    
    public static void nameMapping(final RISObject ris, final CSLResObj csl, final String lang, final String risField, final String cslField) {

    }
    
    public static RISObject RISFromCSL(final CSLResObj csl, final String lang) {
        RISObject ris = new RISObject();
        if ("chapter".equals(csl.bo.get("type").asText()))
            ris.type = "CHAP";
        simpleMapping(ris, csl, lang, "ET", "edition");
        simpleMapping(ris, csl, lang, "IS", "collection-number");
        simpleMapping(ris, csl, lang, "NV", "number-of-volumes");
        simpleMapping(ris, csl, lang, "PB", "publisher");
        simpleMapping(ris, csl, lang, "CY", "publisher-place"); // or perhaps PP?
        simpleMapping(ris, csl, lang, "SE", "section");
        simpleMapping(ris, csl, lang, "UR", "url");
        simpleMapping(ris, csl, lang, "T3", "collection-title");
        simpleMapping(ris, csl, lang, "ID", "id");
        simpleMapping(ris, csl, lang, "TI", "title");
        simpleMapping(ris, csl, lang, "SN", "ISBN");
        dateMapping(ris, csl, lang, "Y2", "accessed");
        dateMapping(ris, csl, lang, "PY", "issued");
        simpleMapping(ris, csl, lang, "BT", "container-title");
        nameMapping(ris, csl, lang, "AU", "author");
        nameMapping(ris, csl, lang, "A4", "translator");
        nameMapping(ris, csl, lang, "ED", "editor");
        nameMapping(ris, csl, lang, "C2", "container-author");
        return ris;
    }
    
}
