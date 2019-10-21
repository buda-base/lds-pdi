package io.bdrc.ldspdi.service;

import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.SortedMap;

import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.vocabulary.SKOS;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.model.User;
import io.bdrc.jena.sttl.CompareComplex;
import io.bdrc.jena.sttl.ComparePredicates;
import io.bdrc.jena.sttl.STTLWriter;
import io.bdrc.jena.sttl.STriGWriter;
import io.bdrc.ldspdi.sparql.Prefixes;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.users.BudaUser;

public class UserDataService {

    public final static Logger log = LoggerFactory.getLogger(UserDataService.class.getName());
    public static final String gitignore = "# Ignore everything\n" + "*\n" + "# Don't ignore directories, so we can recurse into them\n" + "!*/\n" + "# Don't ignore .gitignore and *.foo files\n" + "!.gitignore\n" + "!*.trig\n" + "";

    public static RevCommit addNewBudaUser(User user) {
        RevCommit rev = null;
        Model[] mod = BudaUser.createBudaUserModels(user);
        log.info("public model for user {} is {}", user, mod[0]);
        log.info("private model for user {} is {}", user, mod[1]);
        Model pub = mod[0];
        Model priv = mod[1];
        String userId = null;
        ResIterator rit = priv.listSubjectsWithProperty(ResourceFactory.createProperty(BudaUser.BDOU_PFX + "hasUserProfile"));
        if (rit.hasNext()) {
            Resource r = rit.next();
            userId = r.getLocalName();
        } else {
            log.error("Invalid user model for {}", user);
            return null;
        }
        ensureUserGitRepo();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(System.getProperty("user.dir") + "/users/" + userId + ".trig");
            DatasetGraph dsg = DatasetFactory.create().asDatasetGraph();
            dsg.addGraph(ResourceFactory.createResource(BudaUser.PUBLIC_PFX + userId).asNode(), pub.getGraph());
            dsg.addGraph(ResourceFactory.createResource(BudaUser.PRIVATE_PFX + userId).asNode(), priv.getGraph());
            new STriGWriter().write(fos, dsg, Prefixes.getPrefixMap(), "", createWriterContext());
            Repository r = ensureUserGitRepo();
            Git git = new Git(r);
            if (!git.status().call().isClean()) {
                git.add().addFilepattern(".").call();
                rev = git.commit().setMessage("User " + user.getName() + " was created").call();
            }
            git.close();
            RDFConnectionRemoteBuilder builder = RDFConnectionFuseki.create().destination(ServiceConfig.getProperty("fusekiAuthData"));
            RDFConnectionFuseki fusConn = ((RDFConnectionFuseki) builder.build());
            QueryProcessor.putModel(fusConn, BudaUser.PUBLIC_PFX + userId, pub);
            QueryProcessor.putModel(fusConn, BudaUser.PRIVATE_PFX + userId, priv);
            fusConn.close();

        } catch (Exception e) {

        }
        return rev;
    }

    public static Repository ensureUserGitRepo() {
        Repository repository = null;
        String dirpath = System.getProperty("user.dir") + "/users/";
        createDirIfNotExists(dirpath);
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        File gitDir = new File(dirpath + "/.git");
        File wtDir = new File(dirpath);
        try {
            repository = builder.setGitDir(gitDir).setWorkTree(wtDir).readEnvironment() // scan environment GIT_* variables
                    .build();
            if (!repository.getObjectDatabase().exists()) {
                log.info("create git repository in {}", dirpath);
                repository.create();
                PrintWriter out = new PrintWriter(dirpath + ".gitignore");
                out.println(gitignore);
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return repository;
    }

    public static void createDirIfNotExists(String dir) {
        File theDir = new File(dir);
        if (!theDir.exists()) {
            try {
                theDir.mkdir();
            } catch (SecurityException se) {
                System.err.println("could not create directory, please fasten your seat belt");
            }
        }
    }

    private static Context createWriterContext() {
        SortedMap<String, Integer> nsPrio = ComparePredicates.getDefaultNSPriorities();
        nsPrio.put(SKOS.getURI(), 1);
        nsPrio.put("http://purl.bdrc.io/ontology/admin/", 5);
        nsPrio.put("http://purl.bdrc.io/ontology/toberemoved/", 6);
        List<String> predicatesPrio = CompareComplex.getDefaultPropUris();
        predicatesPrio.add(ADM + "logDate");
        predicatesPrio.add(BDO + "seqNum");
        predicatesPrio.add(BDO + "onYear");
        predicatesPrio.add(BDO + "notBefore");
        predicatesPrio.add(BDO + "notAfter");
        predicatesPrio.add(BDO + "noteText");
        predicatesPrio.add(BDO + "noteWork");
        predicatesPrio.add(BDO + "noteLocationStatement");
        predicatesPrio.add(BDO + "volumeNumber");
        predicatesPrio.add(BDO + "eventWho");
        predicatesPrio.add(BDO + "eventWhere");
        Context ctx = new Context();
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsPriorities"), nsPrio);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsDefaultPriority"), 2);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "complexPredicatesPriorities"), predicatesPrio);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "indentBase"), 4);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "predicateBaseWidth"), 18);
        return ctx;
    }

}
