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

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.ontology.service.shapes.OntShapesData;
import io.bdrc.ldspdi.results.ResultsCache;
import io.bdrc.ldspdi.sparql.LdsQueryService;
import io.bdrc.ldspdi.utils.DocFileModel;

public class GitService implements Runnable {

    private static String GIT_LOCAL_PATH = ServiceConfig.LOCAL_QUERIES_DIR;
    private static String GIT_SHAPES_PATH = ServiceConfig.LOCAL_SHAPES_DIR;
    private static String GIT_REMOTE_URL = ServiceConfig.getProperty("git_remote_url");
    private static String GIT_SHAPES_REMOTE_URL = ServiceConfig.getProperty("git_shapes_remote_url");
    private static Repository localRepo;
    public static String QUERIES = "queries";
    public static String SHAPES = "shapes";
    private static String mode;

    final static Logger log = LoggerFactory.getLogger(GitService.class);

    public static void update(String localPath, String remoteUrl) {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        File localGit = new File(localPath + "/.git");
        log.info("LOCAL GIT >> {}", localGit);
        File WlocalGit = new File(localPath);
        log.info("WLOCAL GIT >> {}", WlocalGit);
        boolean isGitRepo = RepositoryCache.FileKey.isGitRepository(localGit, FS.DETECTED);
        log.info("IS GIT >> {}", isGitRepo);
        // init local git dir and clone remote repository if not present locally
        if (!isGitRepo) {
            initRepo(localPath, remoteUrl);
        } else {
            try {
                localRepo = builder.setGitDir(localGit).setWorkTree(WlocalGit).readEnvironment() // scan environment GIT_* variables
                        .build();
            } catch (IOException ex) {
                ex.printStackTrace();
                log.error("Git was unable to setup repository at init time " + localGit.getPath() + " directory ", ex.getMessage());
                return;
            }
            updateRepo(localRepo);
        }
    }

    private static void initRepo(String localPath, String remoteUrl) {
        try {
            log.info("Cloning {} into dir {}", remoteUrl, localPath);
            Git result = Git.cloneRepository().setDirectory(new File(localPath)).setURI(remoteUrl).call();
            result.checkout().setName("master").call();
            result.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error(" Git was unable to pull repository : " + remoteUrl + " directory ", ex.getMessage());
        }
    }

    private static void updateRepo(Repository localRepo) {
        try {
            log.info("LOCAL REPO >> {}", localRepo);
            Git git = new Git(localRepo);
            git.pull().call();
            git.close();
        } catch (Exception ex) {
            log.error(" Git was unable to pull in directory {}, message: {}", localRepo, ex.getMessage());
        }
    }

    public void setMode(String m) {
        mode = m;
    }

    @Override
    public void run() {
        try {
            if (!SHAPES.equals(mode)) {
                DocFileModel.clearCache();
            }
        } catch (RestException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        LdsQueryService.clearCache();
        ResultsCache.clearCache();
        if (mode == null) {
            update(GIT_LOCAL_PATH, GIT_REMOTE_URL);
            update(GIT_SHAPES_PATH, GIT_SHAPES_REMOTE_URL);
        }
        if (QUERIES.equals(mode)) {
            update(GIT_LOCAL_PATH, GIT_REMOTE_URL);
        }
        if (SHAPES.equals(mode)) {
            update(GIT_SHAPES_PATH, GIT_SHAPES_REMOTE_URL);
            log.info("updating Shape Ontology models() >>");
            Thread t = new Thread(new OntShapesData());
            t.start();
        }
    }

}
