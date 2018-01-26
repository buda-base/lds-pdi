package io.bdrc.ldspdi.service;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GitService {
    
    private static String GIT_LOCAL_PATH;
    private static String GIT_REMOTE_URL="https://github.com/BuddhistDigitalResourceCenter/lds-queries.git";
    private static Repository localRepo;
   
    
    static Logger log = LoggerFactory.getLogger(GitService.class);
    
    public static void update(String localPath) {
        GIT_LOCAL_PATH=localPath;
        FileRepositoryBuilder builder=new FileRepositoryBuilder();
        File localGit=new File(GitService.GIT_LOCAL_PATH+"/.git");
        File WlocalGit=new File(GitService.GIT_LOCAL_PATH); 
        boolean isGitRepo=RepositoryCache.FileKey.isGitRepository(localGit, FS.DETECTED);
        
        //init local git dir and clone remote repository if not present locally
        if(!isGitRepo) {            
            initRepo();            
        }
        else {
            try {
                localRepo = builder.setGitDir(localGit)                        
                        .setWorkTree(WlocalGit)                                        
                        .readEnvironment() // scan environment GIT_* variables
                        .build();
               
            }
            catch(IOException ex) {
                log.error("Git was unable to setup repository at "+localGit.getPath()+" directory : "+ex.getMessage());
                ex.printStackTrace();
            }
            updateRepo();            
        }
        
    }
    
    private static void initRepo() {        
        try {
            
            Git result = Git.cloneRepository()
                    .setDirectory(new File(GitService.GIT_LOCAL_PATH))                    
                    .setURI(GitService.GIT_REMOTE_URL)
                    .setProgressMonitor(new TextProgressMonitor()).call();
            result.checkout().setName("master").call(); 
            result.close();

        }
        catch(Exception ex) {
            log.error(ex.getClass()+ " Git was unable to pull repository : "+GitService.GIT_REMOTE_URL+ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    private static void updateRepo() {        
        try {
             
            Git git=new Git(localRepo);
            git.pull().setProgressMonitor(new TextProgressMonitor()).call();                     
            git.close();

        }
        catch(Exception ex) {
            log.error(ex.getClass()+ " Git was unable to pull repository : "+GitService.GIT_REMOTE_URL+ex.getMessage());
            ex.printStackTrace();
        }
    }

}
