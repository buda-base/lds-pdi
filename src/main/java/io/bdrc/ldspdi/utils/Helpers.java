package io.bdrc.ldspdi.utils;

import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDO;

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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.bdrc.jena.sttl.CompareComplex;
import io.bdrc.jena.sttl.ComparePredicates;
import io.bdrc.jena.sttl.STTLWriter;
import io.bdrc.libraries.BudaMediaTypes;

public class Helpers {

    public final static Logger log = LoggerFactory.getLogger(Helpers.class);

    public static StringBuffer multiChoiceTpl = getTemplateStr("multiChoice.tpl");

    public static InputStream getResourceOrFile(final String baseName) {
        InputStream stream = null;
        stream = Helpers.class.getClassLoader().getResourceAsStream("/" + baseName);
        if (stream != null) {
            return stream;
        }
        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("/" + baseName);
        if (stream != null) {
            return stream;
        }
        final String fileBaseName = "src/main/resources/" + baseName;
        try {
            stream = new FileInputStream(fileBaseName);
            return stream;
        } catch (FileNotFoundException e) {
            log.debug("FileNotFound: " + baseName);
            return null;
        }
    }

    public static String bdrcEncode(String url) {
        String encoded = url.replace("\"", "%22");
        encoded = encoded.replace(' ', '+');
        encoded = encoded.replace("\'", "%27");
        return encoded;
    }

    public static boolean isValidURI(String uri) {
        String[] schemes = { "http", "https" };
        UrlValidator urlValidator = new UrlValidator(schemes);
        return urlValidator.isValid(uri);
    }

    public static HashMap<String, String> convertMulti(Map<String, String[]> map) {
        HashMap<String, String> copy = new HashMap<>();
        Set<String> set = map.keySet();
        for (String key : set) {
            copy.put(key, map.get(key)[0]);
        }
        return copy;
    }

    public static StringBuffer getTemplateStr(String tlpPath) {
        final InputStream stream = getResourceOrFile(tlpPath);
        final BufferedReader buffer = new BufferedReader(new InputStreamReader(stream));
        final StringBuffer sb = new StringBuffer();
        try {
            String line = buffer.readLine();
            while (line != null) {
                sb.append(line + System.lineSeparator());
                line = buffer.readLine();

            }
        } catch (IOException e) {
            log.error("Unable to parse the html multi Choices template in Helpers.getMultiChoicesHtml()");
        }
        return sb;
    }

    public static String getMultiChoicesHtml(final String path, final boolean resource) {
        final StringBuilder sb = new StringBuilder();
        for (final Entry<String, MediaType> e : BudaMediaTypes.getResExtensionMimeMap().entrySet()) {
            final String ext = e.getKey();
            final String mimeDesc = e.getValue().toString();
            if (resource) {
                sb.append("<tr><td><a href=\"" + path + "." + ext + "\">" + path + "." + ext + "</a><td>" + mimeDesc + "</td></tr>\n");
            } else {
                sb.append("<tr><td><a href=\"" + path + "&format=" + ext + "\">" + path + "." + ext + "</a><td>" + mimeDesc + "</td></tr>\n");
            }
        }
        final HashMap<String, String> map = new HashMap<>();
        map.put("path", path);
        map.put("rows", sb.toString());
        StringSubstitutor s = new StringSubstitutor(map);
        return s.replace(multiChoiceTpl);
    }

    public static void test() {

    }

    public static StreamingResponseBody getResultSetAsXml(ResultSet rs) {
        return new StreamingResponseBody() {
            @Override
            public void writeTo(OutputStream os) throws IOException {
                ResultSetFormatter.outputAsXML(os, rs);
            }
        };
    }

    public static boolean equals(MediaType mt, MediaType mt1) {
        return (mt.getType().equals(mt1.getType()) && mt.getSubtype().equals(mt1.getSubtype()));
    }

    public static String getTwoLettersBucket(String st) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.reset();
        md.update(st.getBytes(Charset.forName("UTF8")));
        return new String(Hex.encodeHex(md.digest())).substring(0, 2);
    }

    public static Context createWriterContext() {
        SortedMap<String, Integer> nsPrio = ComparePredicates.getDefaultNSPriorities();
        nsPrio.put(SKOS.getURI(), 1);
        nsPrio.put("http://purl.bdrc.io/ontology/admin/", 5);
        nsPrio.put("http://purl.bdrc.io/ontology/toberemoved/", 6);
        List<String> predicatesPrio = CompareComplex.getDefaultPropUris();
        predicatesPrio.add(ADM + "logDate");
        predicatesPrio.add(BDO + "seqNum");
        predicatesPrio.add(BDO + "onYear");
        predicatesPrio.add(BDO + "notBefore");
        predicatesPrio.add(BDO + "notAfter");
        predicatesPrio.add(BDO + "noteText");
        predicatesPrio.add(BDO + "noteWork");
        predicatesPrio.add(BDO + "noteLocationStatement");
        predicatesPrio.add(BDO + "volumeNumber");
        predicatesPrio.add(BDO + "eventWho");
        predicatesPrio.add(BDO + "eventWhere");
        Context ctx = new Context();
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsPriorities"), nsPrio);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsDefaultPriority"), 2);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "complexPredicatesPriorities"), predicatesPrio);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "indentBase"), 4);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "predicateBaseWidth"), 18);
        return ctx;
    }

}