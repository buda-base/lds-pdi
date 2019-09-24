package io.bdrc.ldspdi.service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.ldspdi.ontology.service.core.OntData;
import io.bdrc.ldspdi.results.ResultsCache;
import io.bdrc.taxonomy.TaxModel;

@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@Primary
@ComponentScan(basePackages = { "io.bdrc.ldspdi" })

public class SpringBootLdspdi extends SpringBootServletInitializer {

	public final static Logger log = LoggerFactory.getLogger(SpringBootLdspdi.class.getName());

	public static void main(String[] args) throws Exception {
		final String configPath = System.getProperty("ldspdi.configpath");
		ServiceConfig.init();
		ResultsCache.init();
		GitService.update(ServiceConfig.getProperty("queryPath"));
		OntData.init();
		TaxModel.fetchModel();
		Properties props = null;
		if (ServiceConfig.useAuth()) {
			// RdfAuthModel.updateAuthData(fuseki);
			// For applications
			InputStream is = new FileInputStream(configPath + "ldspdi.properties");
			props = ServiceConfig.getProperties();
			props.load(is);
			is.close();
			is = new FileInputStream(configPath + "ldspdi-private.properties");
			props.load(is);
			is.close();
			AuthProps.init(props);
			RdfAuthModel.readAuthModel();
		}
		log.info("SpringBootLdspdi has been properly initialized");
		SpringApplication.run(SpringBootLdspdi.class, args);
	}

	/*
	 * @Override protected SpringApplicationBuilder
	 * configure(SpringApplicationBuilder application) { return
	 * application.sources(SpringBootLdspdi.class); }
	 */

}
