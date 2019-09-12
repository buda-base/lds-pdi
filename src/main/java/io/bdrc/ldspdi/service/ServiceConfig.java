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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.bdrc.ldspdi.results.ResultsCache;

public class ServiceConfig {

	static Properties prop = new Properties();
	public final static String FUSEKI_URL = "fusekiUrl";
	public final static Logger log = LoggerFactory.getLogger(ServiceConfig.class.getName());
	// static PrometheusMeterRegistry prometheusRegistry;

	public static void init() throws JsonParseException, JsonMappingException, IOException {

		try {
			final String configPath = System.getProperty("ldspdi.configpath");
			InputStream input = new FileInputStream(new File(configPath + "ldspdi.properties"));
			// load a properties file
			prop.load(input);
			input.close();

		} catch (IOException ex) {
			log.error("ServiceConfig init error", ex);
			ex.printStackTrace();
		}
		initMonitoring();
		OntPolicies.init();
	}

	private static void initMonitoring() {
		/*
		 * prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
		 * prometheusRegistry.config().meterFilter(new PrometheusRenameFilter()); Timer
		 * timer = prometheusRegistry.timer("resource"); timer.record(() -> { try {
		 * TimeUnit.MILLISECONDS.sleep(1000); } catch (InterruptedException ignored) { }
		 * }); Timer timer1 = prometheusRegistry.timer("query.table.get"); Counter
		 * counter = Counter.builder("instance").
		 * description("indicates instance count of the object").tags("dev",
		 * "performance").register(prometheusRegistry); try { HttpServer server =
		 * HttpServer.create(new InetSocketAddress(8088), 0);
		 * server.createContext("/prometheus", httpExchange -> { String response =
		 * prometheusRegistry.scrape(); List<Meter> list =
		 * prometheusRegistry.getMeters();
		 * System.out.println(">>>>>>>>> REGISTRY METERS " + list); for (Meter m : list)
		 * { System.out.println("ID > " + m.getId()); System.out.println("m > " +
		 * m.toString()); } httpExchange.sendResponseHeaders(200,
		 * response.getBytes().length); try (OutputStream os =
		 * httpExchange.getResponseBody()) { os.write(response.getBytes()); } });
		 * prometheusRegistry.config().commonTags("application", "LDSPDI"); new
		 * Thread(server::start).start(); } catch (IOException e) { throw new
		 * RuntimeException(e); }
		 */
	}

	public static void initForTests(String fusekiUrl) throws JsonParseException, JsonMappingException, IOException {
		try {
			InputStream input = new FileInputStream(new File("src/test/resources/ldspdiTest.properties"));
			// load a properties file
			prop.load(input);
			input.close();
			ResultsCache.init();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		prop.setProperty(FUSEKI_URL, fusekiUrl);
		OntPolicies.init();
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