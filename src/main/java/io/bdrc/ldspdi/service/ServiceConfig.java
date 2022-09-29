package io.bdrc.ldspdi.service;

import java.io.File;
import java.io.FileInputStream;

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
import java.util.Properties;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.bdrc.auth.AuthProps;
import io.bdrc.ldspdi.results.ResultsCache;
import io.bdrc.libraries.LibProps;
import io.bdrc.libraries.Prefix;

public class ServiceConfig {

    static Properties prop = new Properties();
    public final static String FUSEKI_URL = "fusekiUrl";
    public static String LOCAL_QUERIES_DIR;
    public static String LOCAL_SHAPES_DIR;
    public static String LOCAL_ONT_DIR;
    public static String SERVER_ROOT;
    public static Prefix PREFIX;
    public static String queriesCommit = RandomStringUtils
            .randomAlphanumeric(10);
    public final static Logger log = LoggerFactory
            .getLogger(ServiceConfig.class);

    // getting the default properties from ldspdi.properties that is packaged
    // with
    // the jar
    public static void init()
            throws JsonParseException, JsonMappingException, IOException {
        log.info("user.dir = " + System.getProperty("user.dir"));
        LOCAL_QUERIES_DIR = System.getProperty("user.dir")
                + "/lds-queries/";
        log.info("LOCAL_QUERIES_DIR = " + LOCAL_QUERIES_DIR);
        LOCAL_SHAPES_DIR = System.getProperty("user.dir")
                + "/editor-templates/";
        LOCAL_ONT_DIR = System.getProperty("user.dir") + "/owl-schema/";
        log.info("getting properties from packaged ldspdi.properties");
        final String configPath = System.getProperty("ldspdi.configpath");
        if (configPath != null) {
            log.info("getting properties from {}",
                    configPath + "ldspdi.properties");
            try {
                InputStream is = new FileInputStream(
                        configPath + "ldspdi.properties");
                prop.load(is);
                is = new FileInputStream(
                        configPath + "ldspdi-private.properties");
                prop.load(is);
                is.close();

            } catch (IOException e) {
                log.warn(
                        "No custom properties file could be found: using default props");
            }
        } else {
            log.info("using default properties");
        }
        prop.setProperty("jsonldContextFile",
                LOCAL_ONT_DIR + "context.jsonld");
        LibProps.init(prop);
        SERVER_ROOT = prop.getProperty("serverRoot");
        PREFIX = new Prefix(LOCAL_QUERIES_DIR + "public/prefixes.txt");
        OntPolicies.init();
    }

    public static String getQueriesCommit() {
        return queriesCommit;
    }

    public static void setQueriesCommit(String queriesCommit) {
        ServiceConfig.queriesCommit = queriesCommit;
    }

    public static void loadPrefixes() {
        PREFIX = new Prefix(LOCAL_QUERIES_DIR + "public/prefixes.txt");
    }

    public static void initForTests(String fusekiUrl)
            throws JsonParseException, JsonMappingException, IOException {
        try {
            LOCAL_QUERIES_DIR = System.getProperty("user.dir")
                    + "/src/test/resources/arq/";
            InputStream input = new FileInputStream(
                    new File("src/test/resources/ldspdiTest.properties"));
            // load a properties file
            prop.load(input);
            input.close();
            ResultsCache.init();
            /*
             * if (ServiceConfig.useAuth()) { try { InputStream is = new
             * FileInputStream("/etc/buda/share/shared-private.properties");
             * prop.load(is); is.close(); } catch (IOException e) {
             * e.printStackTrace(); log.
             * warn("No custom properties file could be found: using default props"
             * ); } AuthProps.init(prop); }
             */
            AuthProps.init(prop);
            LibProps.init(prop);
            PREFIX = new Prefix("src/test/resources/prefixes.txt");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (fusekiUrl != null) {
            prop.setProperty(FUSEKI_URL, fusekiUrl);
        }
        // OntPolicies.init();
    }

    public static boolean useAuth() {
        return Boolean.parseBoolean(prop.getProperty("useAuth"));
    }

    public static boolean isBRDCBrand() {
        return prop.getProperty("brand").equals("BDRC");
    }

    private static Boolean inChina = null;
    public static boolean isInChina() {
        if (inChina != null) return inChina;
        String val = prop.getProperty("serverLocation");
        if (val != null) {
            if (val.equals("china")) {
                inChina = true;
                return true;
            }
        }
        inChina = false;
        return false;
    }

    public static String getProperty(String key) {
        return prop.getProperty(key);
    }

    public static Properties getProperties() {
        return prop;
    }

    public static String getRobots() {
        return "User-agent: *" + System.lineSeparator() + "Disallow: /";
    }

}