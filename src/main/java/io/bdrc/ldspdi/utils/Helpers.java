package io.bdrc.ldspdi.utils;

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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.HttpHeaders;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.libraries.BudaMediaTypes;
import io.bdrc.libraries.Models;

public class Helpers {

    public final static Logger log = LoggerFactory.getLogger(Helpers.class);

    public static StringBuffer multiChoiceTpl = getTemplateStr("multiChoice.tpl");
    public static String MAX_AGE_VALUE = ServiceConfig.getProperty("Max-Age");
    public static Property IS_IN_ROOT_INSTANCE = ResourceFactory.createProperty("http://purl.bdrc.io/ontology/core/inRootInstance");

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

    public static void setCacheControl(HttpServletResponse resp, String pubpriv) {
        resp.setHeader(HttpHeaders.CACHE_CONTROL, pubpriv + ",max-age=" + MAX_AGE_VALUE);
    }

    public static void createDirIfNotExists(String dir) {
        File theDir = new File(dir);
        if (!theDir.exists()) {
            try {
                theDir.mkdir();
            } catch (SecurityException se) {
                log.error("Could not create " + dir, se);
            }
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

    public static StreamingResponseBody getResultSetAsXml(ResultSet rs) {
        return new StreamingResponseBody() {
            @Override
            public void writeTo(OutputStream os) throws IOException {
                ResultSetFormatter.outputAsXML(os, rs);
            }
        };
    }

    public static String tofullResourceUri(String prefixedUri) {
        String res = prefixedUri.substring(prefixedUri.lastIndexOf(":") + 1);
        return Models.BDR + res;
    }

    public static String toPrefixedResourceUri(String fullUri) {
        String res = fullUri.substring(fullUri.lastIndexOf("/") + 1);
        return "bdr:" + res;
    }

    public static String getRootInstanceUri(String prefixedUri, Model m) {
        NodeIterator it = m.listObjectsOfProperty(ResourceFactory.createResource(tofullResourceUri(prefixedUri)), IS_IN_ROOT_INSTANCE);
        if (it.hasNext()) {
            RDFNode st = it.next();
            return toPrefixedResourceUri(st.asResource().getURI());
        }
        return null;
    }

    public static boolean equals(MediaType mt, MediaType mt1) {
        return (mt.getType().equals(mt1.getType()) && mt.getSubtype().equals(mt1.getSubtype()));
    }

    public static void writeModelToFile(Model m, String filename) throws IOException {
        log.info("Writing model to file {}", filename);
        FileOutputStream fos = new FileOutputStream(filename);
        m.write(fos, "TURTLE");
        fos.close();
    }

}