package io.bdrc.ldspdi.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.objects.json.QueryTemplate;
import io.bdrc.ldspdi.service.OntPolicies;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.LdsQuery;
import io.bdrc.ldspdi.sparql.LdsQueryService;

public class DocFileModel {

    static List<String> files;
    public final static Logger log = LoggerFactory.getLogger(DocFileModel.class);
    public static Set<String> keys;
    public static ArrayList<String> ontos;
    public static String brandName = ServiceConfig.getProperty("brandName");
    public static String ontName = ServiceConfig.getProperty("ontName");
    static HashMap<String, ArrayList<QueryTemplate>> templ;

    private static final DocFileModel instance = new DocFileModel();

    private DocFileModel() {
        try {
            setContentModel();
        } catch (RestException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("DocFileModel init failed !", e);
        }
    }

    public static DocFileModel getInstance() {
        return instance;
    }

    public static void clearCache() throws RestException {
        setContentModel();
    }

    public static void setContentModel() throws RestException {
        templ = new HashMap<>();
        long deb = System.currentTimeMillis();
        files = getQueryTemplates();
        long deb1 = System.currentTimeMillis();
        log.info("Loaded query templates files in {} ms", (deb1 - deb));
        for (String file : files) {
            String tmp = file.substring(file.lastIndexOf('/') + 1);
            final LdsQuery qfp = LdsQueryService.get(tmp);
            QueryTemplate qt = qfp.getTemplate();
            String queryScope = qt.getQueryScope();

            if (templ.containsKey(queryScope)) {
                templ.get(queryScope).add(qt);
            } else {
                ArrayList<QueryTemplate> qtlist = new ArrayList<>();
                qtlist.add(qt);
                templ.put(queryScope, qtlist);
            }
        }
        long deb2 = System.currentTimeMillis();
        log.info("Loaded query templates themselves in {} ms", (deb2 - deb1));
        ontos = OntPolicies.getValidBaseUri();
        long deb3 = System.currentTimeMillis();
        log.info("Loaded ontologies baseuris in {} ms, onts are {}", (deb3 - deb2), ontos);
        keys = templ.keySet();
    }

    public String getBrandName() {
        return brandName;
    }

    public String getOntName() {
        return ontName;
    }

    public ArrayList<QueryTemplate> getTemplates(String key) {
        return templ.get(key);
    }

    public Set<String> getKeys() {
        return keys;
    }

    public ArrayList<String> getOntos() {
        return ontos;
    }

    public static List<String> getQueryTemplates() throws RestException {
        List<String> files = new ArrayList<>();
        Path dpath = Paths.get(ServiceConfig.LOCAL_QUERIES_DIR + "public");
        Stream<Path> walk;
        try {
            walk = Files.walk(dpath);
            files = walk.map(x -> x.toString()).filter(f -> f.endsWith(".arq")).collect(Collectors.toList());
        } catch (IOException e1) {
            log.error("Error while getting query templates", e1);
            throw new RestException(500, new LdsError(LdsError.MISSING_RES_ERR)
                    .setContext(ServiceConfig.LOCAL_QUERIES_DIR + "public in DocFileModel.getQueryTemplates()"));
        }
        walk.close();
        return files;
    }

    @Override
    public String toString() {
        return "DocFileModel [files=" + files + ", keys=" + keys + ", ontos=" + ontos + ", brandName=" + brandName + ", ontName=" + ontName
                + ", templ=" + templ + "]";
    }

}