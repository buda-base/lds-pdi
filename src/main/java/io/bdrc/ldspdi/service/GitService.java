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
import java.util.concurrent.TimeUnit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class GitService /* implements Runnable */ {

    static String GIT_LOCAL_PATH = ServiceConfig.LOCAL_QUERIES_DIR;
    static String GIT_SHAPES_PATH = ServiceConfig.LOCAL_SHAPES_DIR;
    static String GIT_ONT_PATH = ServiceConfig.LOCAL_ONT_DIR;
    static String GIT_REMOTE_URL = ServiceConfig.getProperty("git_remote_url");
    static String GIT_SHAPES_REMOTE_URL = ServiceConfig.getProperty("git_shapes_remote_url");
    static String GIT_ONT_REMOTE_URL = ServiceConfig.getProperty("git_ontologies_remote_url");
    static Repository localRepo;
    public static String QUERIES = "queries";
    public static String SHAPES = "shapes";
    public static String ONTOLOGIES = "ontologies";
    private static String mode;
    private int delayInSeconds;

    public GitService(int delayInSeconds) {
        super();
        GIT_LOCAL_PATH = ServiceConfig.LOCAL_QUERIES_DIR;
        GIT_SHAPES_PATH = ServiceConfig.LOCAL_SHAPES_DIR;
        GIT_REMOTE_URL = ServiceConfig.getProperty("git_remote_url");
        GIT_SHAPES_REMOTE_URL = ServiceConfig.getProperty("git_shapes_remote_url");
        GIT_ONT_PATH = ServiceConfig.LOCAL_ONT_DIR;
        GIT_ONT_REMOTE_URL = ServiceConfig.getProperty("git_ontologies_remote_url");
        this.delayInSeconds = delayInSeconds;
    }

    final static Logger log = LoggerFactory.getLogger(GitService.class);

    public String update(String localPath, String remoteUrl)
            throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException, InterruptedException {
        TimeUnit.SECONDS.sleep(delayInSeconds);
        String commit = null;
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
            }
            commit = updateRepo(localRepo);
        }
        return commit;
    }

    private void initRepo(String localPath, String remoteUrl) {
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

    private String getHead(Repository localRepo) {
        try {
            ObjectId id = localRepo.resolve(Constants.HEAD);
            return id.getName().substring(0, 7);
          } catch (IOException e) {
            log.error("can't get commit of repo");
          }
        return null;
    }
    
    private String updateRepo(Repository localRepo)
            throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException {
        String commitId = null;
        try {
            log.info("UPDATING LOCAL REPO >> {}", localRepo);
            Git git = new Git(localRepo);
            git.pull().call();
            git.close();
            commitId = localRepo.resolve(Constants.HEAD).getName().substring(0, 7);
            log.info("LOCAL REPO >> {} was updated with commit {}", localRepo, commitId);
        } catch (Exception ex) {
            log.error(" Git was unable to pull in directory {}, message: {}", localRepo, ex.getMessage());
            return getHead(localRepo);
        }
        return commitId;
    }

    public void setMode(String m) {
        mode = m;
    }

    public static void main(String... args)
            throws JsonParseException, JsonMappingException, IOException, RevisionSyntaxException, InterruptedException {
        ServiceConfig.init();
        GitService gs = new GitService(0);
        gs.setMode(GitService.ONTOLOGIES);
        System.out.println("DIR >>" + ServiceConfig.LOCAL_ONT_DIR);
        String commitId = gs.update(GIT_ONT_PATH, GIT_ONT_REMOTE_URL);
        System.out.println(commitId);
    }

}
