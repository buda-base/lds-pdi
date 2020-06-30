package io.bdrc.ldspdi.service;

import java.io.IOException;

import org.eclipse.jgit.errors.RevisionSyntaxException;

import io.bdrc.ldspdi.ontology.service.core.OntData;
import io.bdrc.ldspdi.ontology.service.shapes.OntShapesData;

public class Webhook implements Runnable {

    String payload;
    GitService gs;
    String mode;

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
        String commitId = null;
        if (GitService.SHAPES.equals(mode)) {
            try {
                commitId = gs.update(GitService.GIT_SHAPES_PATH, GitService.GIT_SHAPES_REMOTE_URL);
            } catch (RevisionSyntaxException | IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            OntShapesData osd = new OntShapesData(payload, commitId);
            osd.update();
        }
        if (GitService.ONTOLOGIES.equals(mode)) {
            try {
                commitId = gs.update(GitService.GIT_ONT_PATH, GitService.GIT_ONT_REMOTE_URL);
            } catch (RevisionSyntaxException | IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            OntData od = new OntData(payload, commitId);
            od.init();
        }
        if (GitService.QUERIES.equals(mode)) {
            try {
                commitId = gs.update(GitService.GIT_LOCAL_PATH, GitService.GIT_REMOTE_URL);
                ServiceConfig.setQueriesCommit(commitId);
            } catch (RevisionSyntaxException | IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
