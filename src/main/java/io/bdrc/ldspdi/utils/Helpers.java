package io.bdrc.ldspdi.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.text.StrSubstitutor;
import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.sparql.functions.Wylie;


public class Helpers {

    public final static Logger log = LoggerFactory.getLogger(Helpers.class.getName());

    public static StringBuffer multiChoiceTpl = getTemplateStr("multiChoice.tpl");

    public static InputStream getResourceOrFile(final String baseName) {
        InputStream stream = null;
        stream = Helpers.class.getClassLoader().getResourceAsStream("/"+baseName);
        if (stream != null) {
            return stream;
        }
        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("/"+baseName);
        if (stream != null) {
            return stream;
        }
        final String fileBaseName = "src/main/resources/"+baseName;
        try {
            stream = new FileInputStream(fileBaseName);
            return stream;
        } catch (FileNotFoundException e) {
            log.debug("FileNotFound: "+baseName);
            return null;
        }
    }

    public static String removeAccents(String text) {
        String f=text;
        return f == null ? null :
            Normalizer.normalize(f, Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    public static boolean isTibUni(String s) {
        return s.matches("[\u0f00-\u0fff]+");
    }

    public static boolean isWylie(String s) {
        Wylie wl = new Wylie(true, false, false, true);
        ArrayList<String> warn=new ArrayList<>();
        wl.fromWylie(s, warn);
        return warn.size()==0;
    }

    public static String bdrcEncode(String url) {
        String encoded=url.replace("\"", "%22");
        encoded=encoded.replace(' ', '+');
        encoded=encoded.replace("\'", "%27");
        return encoded;
    }

    public static boolean isValidURI(String uri) {
        String[] schemes = {"http","https"};
        UrlValidator urlValidator = new UrlValidator(schemes);
        return urlValidator.isValid(uri);
    }

    public static HashMap<String,String> convertMulti(MultivaluedMap<String,String> map){
        HashMap<String,String> copy=new HashMap<>();
        Set<String> set=map.keySet();
        for(String key:set) {
            copy.put(key, map.getFirst(key));
        }
        return copy;
    }

    public static StringBuffer getTemplateStr(String tlpPath) {
        final InputStream stream = getResourceOrFile(tlpPath);
        final BufferedReader buffer = new BufferedReader(new InputStreamReader(stream));
        final StringBuffer sb = new StringBuffer();
        try {
            String line=buffer.readLine();
            while(line != null) {
                sb.append(line+System.lineSeparator());
                line = buffer.readLine();

            }
        } catch (IOException e) {
            log.error("Unable to parse the html multi Choices template in Helpers.getMultiChoicesHtml()");
        }
        return sb;
    }

    public static String getMultiChoicesHtml(final String path, final boolean resource) {
        final StringBuilder sb = new StringBuilder();
        for (final Entry<String,MediaType> e: MediaTypeUtils.getResExtensionMimeMap().entrySet()) {
            final String ext = e.getKey();
            final String mimeDesc = e.getValue().toString();
            if(resource) {
                sb.append("<tr><td><a href=\""+path+"."+ext+"\">"+path+"."+ext+"</a><td>"+
                        mimeDesc+"</td></tr>\n");
            } else {
                sb.append("<tr><td><a href=\""+path+"&format="+ext+"\">"+path+"."+ext+"</a><td>"+
                        mimeDesc+"</td></tr>\n");
            }
        }
        final HashMap<String,String> map = new HashMap<>();
        map.put("path", path);
        map.put("rows", sb.toString());
        StrSubstitutor s = new StrSubstitutor(map);
        return s.replace(multiChoiceTpl);
    }
}
