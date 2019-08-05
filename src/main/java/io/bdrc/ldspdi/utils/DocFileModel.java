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

import io.bdrc.ldspdi.objects.json.QueryTemplate;
import io.bdrc.ldspdi.service.OntPolicies;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.LdsQuery;
import io.bdrc.ldspdi.sparql.LdsQueryService;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

public class DocFileModel {

    List<String> files;
    public final static Logger log = LoggerFactory.getLogger(DocFileModel.class.getName());
    public Set<String> keys;
    public ArrayList<String> ontos;
    public String brandName;
    public String ontName;
    HashMap<String, ArrayList<QueryTemplate>> templ;

    public DocFileModel() throws RestException {
        this.ontName = ServiceConfig.getProperty("ontName");
        this.brandName = ServiceConfig.getProperty("brandName");
        this.files = getQueryTemplates();
        setContentModel();
    }

    public void setContentModel() throws RestException {

        templ = new HashMap<>();

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
        this.ontos = OntPolicies.getValidBaseUri();
        this.keys = templ.keySet();
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
        Path dpath = Paths.get(ServiceConfig.getProperty("queryPath") + "public");
        Stream<Path> walk;
        try {
            walk = Files.walk(dpath);
            files = walk.map(x -> x.toString()).filter(f -> f.endsWith(".arq")).collect(Collectors.toList());
        } catch (IOException e1) {
            log.error("Error while getting query templates", e1);
            e1.printStackTrace();
            throw new RestException(500, new LdsError(LdsError.MISSING_RES_ERR).setContext(ServiceConfig.getProperty(QueryConstants.QUERY_PATH) + "public in DocFileModel.getQueryTemplates()"));
        }
        walk.close();
        return files;
    }

}
