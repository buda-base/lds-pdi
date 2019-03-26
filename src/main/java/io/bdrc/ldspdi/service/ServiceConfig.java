package io.bdrc.ldspdi.service;

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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.bdrc.ldspdi.results.ResultsCache;

public class ServiceConfig {

    static Properties prop = new Properties();
    public static HashMap<String, String> params;
    public static Config config;
    public final static String FUSEKI_URL = "fusekiUrl";
    public final static Logger log = LoggerFactory.getLogger(ServiceConfig.class.getName());

    public static void init(HashMap<String, String> params) throws JsonParseException, JsonMappingException, IOException {
        ServiceConfig.params = params;

        try {
            InputStream input = ServiceConfig.class.getClassLoader().getResourceAsStream("ldspdi.properties");
            // load a properties file
            prop.load(input);
            input.close();
            /**
             * sets the PROD values of fuseki and queryPath properties Overrides test
             * queryPath value *
             **/
            Set<String> set = params.keySet();
            for (String st : set) {
                prop.setProperty(st, params.get(st));
            }

        } catch (IOException ex) {
            log.error("ServiceConfig init error", ex);
            ex.printStackTrace();
        }
        loadOntologies();
    }

    @SuppressWarnings("unchecked")
    public static Config loadOntologies() throws JsonParseException, JsonMappingException, IOException {
        // InputStream input =
        // ServiceConfig.class.getClassLoader().getResourceAsStream("ontologies.yml");
        InputStream input = ServiceConfig.class.getClassLoader().getResourceAsStream("ontologiesLocal.yml");
        Yaml yaml = new Yaml();
        config = yaml.loadAs(input, Config.class);
        input.close();
        return config;
    }

    public static void initForTests(String fusekiUrl) throws JsonParseException, JsonMappingException, IOException {
        try {
            InputStream input = ServiceConfig.class.getClassLoader().getResourceAsStream("ldspdi.properties");
            // load a properties file
            prop.load(input);
            input.close();
            ResultsCache.init();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        prop.setProperty(FUSEKI_URL, fusekiUrl);
        loadOntologies();
    }

    public static boolean useAuth() {
        return Boolean.parseBoolean(prop.getProperty("useAuth"));
    }

    public static String getProperty(String key) {
        return prop.getProperty(key);
    }

    public static Config getConfig() {
        return config;
    }

    public static String getRobots() {
        return "User-agent: *" + System.lineSeparator() + "Disallow: /";
    }

    public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
        System.out.println(ServiceConfig.loadOntologies().getOntology("core-shapes"));
    }

}