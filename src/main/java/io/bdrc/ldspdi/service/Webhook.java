package io.bdrc.ldspdi.service;

import java.io.IOException;

import org.eclipse.jgit.errors.RevisionSyntaxException;

import io.bdrc.ldspdi.ontology.service.shapes.OntShapesData;

public class Webhook implements Runnable {

    String payload;
    GitService gs;
    String mode;

    public Webhook(String payload, String mode) {
        super();
        this.payload = payload;
        this.mode = mode;
        this.gs = new GitService();
        gs.setMode(mode);
    }

    @Override
    public void run() {
        String commitId = null;
        if (GitService.SHAPES.equals(mode)) {
            try {
                commitId = gs.update(GitService.GIT_SHAPES_PATH, GitService.GIT_SHAPES_REMOTE_URL);
            } catch (RevisionSyntaxException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            OntShapesData osd = new OntShapesData(payload, commitId);
            osd.update();
        }
    }

}
