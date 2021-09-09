package io.bdrc.ldspdi.service;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.results.ResultsCache;
import io.bdrc.ldspdi.results.library.WorkResults;

@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@Primary
@ComponentScan(basePackages = {"io.bdrc.ldspdi"})

public class SpringBootLdspdi extends SpringBootServletInitializer {

    // public final static Logger log =
    // LoggerFactory.getLogger(SpringBootLdspdi.class);
    public final static Logger log = LoggerFactory.getLogger("default");

    public static void main(String[] args) throws RestException,
            RevisionSyntaxException, AmbiguousObjectException,
            IncorrectObjectTypeException, IOException, InterruptedException {
        try {
            ServiceConfig.init();
        } catch (IOException e1) {
            log.error("Primary config could not be load in ServiceConfig", e1);
            throw new RestException(500, new LdsError(LdsError.MISSING_RES_ERR)
                    .setContext("Ldspdi startup and initialization", e1));
        }
        final Properties props = ServiceConfig.getProperties();
        AuthProps.init(props);
        if (ServiceConfig.useAuth()) {
            RdfAuthModel.readAuthModel();
        }
        ResultsCache.init();
        // Pull lds-queries repo from git if not in China
        if (!ServiceConfig.isInChina()) {
            GitService gs = new GitService(0);
            String commit = gs.update(GitService.GIT_LOCAL_PATH,
                    GitService.GIT_REMOTE_URL);
            ServiceConfig.setQueriesCommit(commit);
        }
        if (ServiceConfig.getProperty("taxonomyRoot") != null) {
            log.info("initialize taxonomies");
            WorkResults.initForProd();
        }
        log.info("SpringBootLdspdi has been properly initialized");
        SpringApplication app = new SpringApplication(SpringBootLdspdi.class);
        app.setDefaultProperties(props);
        app.run(args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup()
            throws InterruptedException, ExecutionException {

        Webhook wh_ont = new Webhook(null, GitService.ONTOLOGIES, 0);
        Thread t_ont = new Thread(wh_ont);
        t_ont.start();

        if (!ServiceConfig.isInChina()) {
            // OntShapesData.init();
            Webhook wh = new Webhook(null, GitService.SHAPES, 0);
            Thread t = new Thread(wh);
            t.start();
            if ("true".equals(AuthProps.getProperty("useAuth"))) {
                log.info("SpringBootLdspdi uses auth, updating auth data...");
                RdfAuthModel.init();
                RdfAuthModel.updateAuthData(
                        AuthProps.getProperty("fusekiAuthData"));
            }
        }
        log.info("SERVER IS IN CHINA {}", ServiceConfig.isInChina());

    }

}
