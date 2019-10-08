package io.bdrc.ldspdi.service;

import java.io.FileInputStream;
import java.io.IOException;
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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.ldspdi.ontology.service.core.OntData;
import io.bdrc.ldspdi.results.ResultsCache;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;
import io.bdrc.taxonomy.TaxModel;

@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@Primary
@ComponentScan(basePackages = { "io.bdrc.ldspdi" })

public class SpringBootLdspdi extends SpringBootServletInitializer {

    public final static Logger log = LoggerFactory.getLogger("default");

    public static void main(String[] args) throws JsonParseException, JsonMappingException, RestException {
        final String configPath = System.getProperty("ldspdi.configpath");
        try {
            ServiceConfig.init();
        } catch (IOException e1) {
            log.error("Primary config could not be load in ServiceConfig", e1);
            throw new RestException(500, new LdsError(LdsError.MISSING_RES_ERR).setContext("Ldspdi startup and initialization", e1));
        }
        Properties props = ServiceConfig.getProperties();
        InputStream is;
        try {
            is = new FileInputStream(configPath + "ldspdi.properties");
            props.load(is);
            is.close();
        } catch (IOException e) {
            log.warn("No custom properties file could be found: using default props");
        }
        if (ServiceConfig.useAuth()) {
            try {
                is = new FileInputStream(configPath + "ldspdi-private.properties");
                props.load(is);
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
                log.warn("No custom properties file could be found: using default props");
            }
            AuthProps.init(props);
            RdfAuthModel.readAuthModel();
        }
        ResultsCache.init();
        GitService.update();
        OntData.init();
        TaxModel.fetchModel();
        log.info("SpringBootLdspdi has been properly initialized");
        SpringApplication.run(SpringBootLdspdi.class, args);
    }

    /*
     * @Override protected SpringApplicationBuilder
     * configure(SpringApplicationBuilder application) { return
     * application.sources(SpringBootLdspdi.class); }
     */

}
