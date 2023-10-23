package io.bdrc.ldspdi.service;

import java.io.IOException;

import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.ontology.service.core.OntData;
import io.bdrc.ldspdi.ontology.service.shapes.OntShapesData;
import io.bdrc.ldspdi.results.ResultsCache;
import io.bdrc.ldspdi.sparql.LdsQueryService;

public class Webhook implements Runnable {

    String payload;
    GitService gs;
    String mode;
    public final static Logger log = LoggerFactory.getLogger(Webhook.class);

    public Webhook(String payload, String mode) {
        super();
        this.payload = payload;
        this.mode = mode;
        this.gs = new GitService(15);
        gs.setMode(mode);
    }

    public Webhook(String payload, String mode, int delayInSeconds) {
        super();
        this.payload = payload;
        this.mode = mode;
        this.gs = new GitService(delayInSeconds);
        gs.setMode(mode);
    }

    @Override
    public void run() {
        log.info("run webhook mode {}", mode);
        String commitId = null;
        ResultsCache.clearCache();
        LdsQueryService.clearCache();
        if (GitService.ONTOLOGIES.equals(mode)) {
            try {
                commitId = gs.update(GitService.GIT_ONT_PATH, GitService.GIT_ONT_REMOTE_URL);
            } catch (RevisionSyntaxException | IOException | InterruptedException e) {
                log.error("error getting latest commit for ontology", e);
            }
            OntData od = new OntData(payload, commitId);
            od.init();
        }
        if (GitService.QUERIES.equals(mode)) {
            try {
                commitId = gs.update(GitService.GIT_LOCAL_PATH, GitService.GIT_REMOTE_URL);
                ServiceConfig.setQueriesCommit(commitId);
            } catch (RevisionSyntaxException | IOException | InterruptedException e) {
                log.error("error getting latest commit for queries", e);
            }
        }
        // loading shapes after ontology to add the labels
        if (GitService.SHAPES.equals(mode)) {
            try {
                commitId = gs.update(GitService.GIT_SHAPES_PATH, GitService.GIT_SHAPES_REMOTE_URL);
            } catch (RevisionSyntaxException | IOException | InterruptedException e) {
                log.error("error getting latest commit for shapes", e);
            }
            OntShapesData osd = new OntShapesData(payload, commitId);
            osd.update();
        }
    }

}
