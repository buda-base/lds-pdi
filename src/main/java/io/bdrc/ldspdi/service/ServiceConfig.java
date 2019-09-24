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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.bdrc.ldspdi.results.ResultsCache;

public class ServiceConfig {

	static Properties prop = new Properties();
	public final static String FUSEKI_URL = "fusekiUrl";
	public static String LOCAL_QUERIES_DIR;
	public final static org.slf4j.Logger log = LoggerFactory.getLogger(ServiceConfig.class.getName());

	public static void init() throws JsonParseException, JsonMappingException, IOException {

		try {
			String tmp = System.getProperty("user.dir");
			// we need to go up from the current dir so we don't have git conflicts (nested
			// repo in the dev environnement)
			LOCAL_QUERIES_DIR = tmp.substring(0, tmp.lastIndexOf("/") + 1) + "/lds-queries/";
			final String configPath = System.getProperty("ldspdi.configpath");
			log.info("getting properties from {}", configPath);
			InputStream input = ServiceConfig.class.getClassLoader().getResourceAsStream("ldspdi.properties");
			prop.load(input);
			input.close();
			initLogs();

		} catch (IOException ex) {
			log.error("ServiceConfig init error", ex);
		}
		OntPolicies.init();
	}

	public static void initForTests(String fusekiUrl) throws JsonParseException, JsonMappingException, IOException {
		try {
			InputStream input = new FileInputStream(new File("src/test/resources/ldspdiTest.properties"));
			// load a properties file
			prop.load(input);
			input.close();
			initLogs();
			ResultsCache.init();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		prop.setProperty(FUSEKI_URL, fusekiUrl);
		OntPolicies.init();
	}

	private static void initLogs() {
		Set<String> loggers = new HashSet<>(Arrays.asList("org.apache.http", "org.apache.jena"));
		for (String log : loggers) {
			Logger logger = (Logger) LoggerFactory.getLogger(log);
			logger.setLevel(Level.toLevel(Integer.parseInt(getProperty("logLevel"))));
			logger.setAdditive(false);
		}
	}

	public static boolean useAuth() {
		return Boolean.parseBoolean(prop.getProperty("useAuth"));
	}

	public static boolean isBRDCBrand() {
		return prop.getProperty("brand").equals("BDRC");
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