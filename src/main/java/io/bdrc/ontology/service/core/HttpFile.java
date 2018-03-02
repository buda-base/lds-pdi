package io.bdrc.ontology.service.core;

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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class HttpFile {
    
    public final static Logger log=LoggerFactory.getLogger(HttpFile.class.getName());

    public static Reader reader(String link) 
            throws MalformedURLException, IOException
    {
        InputStream stream = stream(link);
        if (stream != null) {
            try {
                return new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                log.error("HttpFile reader error", ex);
                return null;
            }
        } 
        return null;        
    }

    public static InputStream stream(String link) 
            throws MalformedURLException, IOException
    {
        
        URL url = new URL(link);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        Map<String, List<String>> headers = connection.getHeaderFields();

        // Follow 301 and 302 redirects
        for (String header : headers.get(null)) {
            if (header.contains(" 302 ") || header.contains(" 301 ")) {
                link = headers.get("Location").get(0);
                url = new URL(link);
                connection = (HttpURLConnection) url.openConnection();
                headers = connection.getHeaderFields();
            }
        }
        
        return connection.getInputStream();
    }
}

