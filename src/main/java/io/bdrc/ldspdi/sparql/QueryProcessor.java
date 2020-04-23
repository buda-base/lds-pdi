package io.bdrc.ldspdi.sparql;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/*******************************************************************************
 * Copyright (c) 2017 Buddhist Digital Resource Center (BDRC)
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

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.results.ResultSetWrapper;
import io.bdrc.ldspdi.results.ResultsCache;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.libraries.Prefixes;

public class QueryProcessor {

    public final static Logger log = LoggerFactory.getLogger(QueryProcessor.class);

    public static Model getCoreResourceGraph(final String URI, String fusekiUrl, String prefixes, String type) throws RestException {
        switch (type) {
        case "graph":
            return getSimpleResourceGraph(URI, "Resgraph.arq", fusekiUrl, prefixes);
        case "describe":
            return getDescribeModel(URI, fusekiUrl, prefixes);
        case "":
            return getSimpleResourceGraph(URI, "ResInfo.arq", fusekiUrl, prefixes);
        default:
            return getDescribeModel(URI, fusekiUrl, prefixes);
        }
    }

    public static Model getSimpleResourceGraph(final String URI, final String queryName) throws RestException {
        return getSimpleResourceGraph(URI, queryName, null, null);
    }

    public static Model getSimpleResourceGraph(final String URI, final String queryName, String fusekiUrl, String prefixes) throws RestException {
        if (prefixes == null) {
            prefixes = Prefixes.getPrefixesString();
        }
        if (fusekiUrl == null) {
            fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }
        int hash = Objects.hashCode(queryName + "::" + URI);
        Model model = (Model) ResultsCache.getObjectFromCache(hash);
        if (model == null) {
            LdsQuery qfp = LdsQueryService.get(queryName, "library");
            final Map<String, String> map = new HashMap<>();
            map.put("R_RES", URI);
            String query = qfp.getParametizedQuery(map, true);
            Query q = QueryFactory.create(query);
            RDFConnection conn = RDFConnectionRemote.create().destination(fusekiUrl).build();
            model = conn.queryDescribe(q);
            ResultsCache.addToCache(model, hash);
        }
        return model;
    }

    public static Model getDescribeModel(final String URI, String fusekiUrl, String prefixes) throws RestException {
        if (prefixes == null) {
            prefixes = Prefixes.getPrefixesString();
        }
        if (fusekiUrl == null) {
            fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }
        Query q = QueryFactory.create(prefixes + " describe " + URI);
        log.debug("getDescribeModel() query : {}", q.toString());
        RDFConnection conn = RDFConnectionRemote.create().destination(fusekiUrl).build();
        Model model = conn.queryDescribe(q);
        return model;
    }

    public static Model getGraph(final String query, String fusekiUrl, String prefixes) throws RestException {
        if (prefixes == null) {
            prefixes = Prefixes.getPrefixesString();
        }
        if (fusekiUrl == null) {
            fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }
        final int hash = Objects.hashCode(query);
        Model model = (Model) ResultsCache.getObjectFromCache(hash);
        if (model == null) {
            log.trace("executing query: {}", query);
            final Query q = QueryFactory.create(prefixes + " " + query);
            RDFConnectionFuseki rvf = RDFConnectionFactory.connectFuseki(fusekiUrl);
            // RDFConnection conn =
            // RDFConnectionRemote.create().destination(fusekiUrl).build();
            model = rvf.queryConstruct(q);
            ResultsCache.addToCache(model, hash);
            rvf.close();
        }
        return model;
    }

    public static Model getGraph(final String query) throws RestException {
        return getGraph(query, null, null);
    }

    public static Model getAuthGraph(String fusekiUrl, String graph) throws RestException {
        if (fusekiUrl == null) {
            fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }
        fusekiUrl = fusekiUrl.substring(0, fusekiUrl.lastIndexOf("/"));
        RDFConnectionFuseki rvf = RDFConnectionFactory.connectFuseki(fusekiUrl);
        Model m = rvf.fetch(ServiceConfig.getProperty(graph));
        return m;
    }

    public static QueryExecution getResultSet(String query, String fusekiUrl) {
        if (fusekiUrl == null) {
            fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }
        RDFConnectionFuseki rvf = RDFConnectionFactory.connectFuseki(fusekiUrl);
        QueryExecution qe = rvf.query(query);
        // QueryExecution qe = QueryExecutionFactory.sparqlService(fusekiUrl,
        // QueryFactory.create(query));
        qe.setTimeout(Long.parseLong(ServiceConfig.getProperty(QueryConstants.QUERY_TIMEOUT)));
        return qe;
    }

    public static void updateOntology(Model mod, String fusekiUrl, String graph, String caller) {
        if (fusekiUrl == null) {
            fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }
        log.info("Service fuseki for caller >> {} and {} Graph >> {} and InfModel Size >> {}", caller, fusekiUrl, graph,
                mod.listStatements().toSet().size());
        RDFConnectionFuseki rvf = RDFConnectionFactory.connectFuseki(fusekiUrl);
        rvf.put(graph, mod);
    }

    public static Model getAnyGraph(String graph, String fusekiUrl) {
        if (fusekiUrl == null) {
            fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }
        RDFConnectionFuseki rvf = RDFConnectionFactory.connectFuseki(fusekiUrl);
        return rvf.fetch(graph);
    }

    public static Model getAnyGraph(String graph) {
        return getAnyGraph(graph, null);
    }

    public static ResultSetWrapper getResults(final String query, String fusekiUrl, final String hash, final String pageSize) {
        if (fusekiUrl == null) {
            fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }
        if (hash != null) {
            return (ResultSetWrapper) ResultsCache.getObjectFromCache(Integer.parseInt(hash));
        }
        int psz = Integer.parseInt(ServiceConfig.getProperty(QueryConstants.PAGE_SIZE));
        if (pageSize != null) {
            psz = Integer.parseInt(pageSize);
        }
        int new_hash = Objects.hashCode(query);
        ResultSetWrapper res = (ResultSetWrapper) ResultsCache.getObjectFromCache(new_hash);
        if (res == null) {
            long start = System.currentTimeMillis();
            final QueryExecution qe = getResultSet(query, fusekiUrl);
            final ResultSet jrs = qe.execSelect();
            long elapsed = System.currentTimeMillis() - start;
            res = new ResultSetWrapper(jrs, elapsed, psz);
            qe.close();
            res.setHash(new_hash);
            ResultsCache.addToCache(res, new_hash);
        } else {
            // Given cached results, set new pageSize parameter
            res.update(psz);
        }
        return res;
    }

    public static ResultSet getResults(final String query, String fusekiUrl) {
        if (fusekiUrl == null) {
            fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }
        int new_hash = Objects.hashCode(query + "RES");
        // ResultSet res = (ResultSet) ResultsCache.getObjectFromCache(new_hash);
        ResultSet res = null;
        if (res == null) {
            final QueryExecution qe = getResultSet(query, fusekiUrl);
            res = qe.execSelect();
            ResultsCache.addToCache(res, new_hash);
        }
        return res;
    }

    public static Model getGraphFromModel(String query, Model model) throws RestException {
        try {
            QueryExecution qexec = QueryExecutionFactory.create(query, model);
            Model m = qexec.execDescribe();
            return m;
        } catch (Exception ex) {
            throw new RestException(500,
                    new LdsError(LdsError.SPARQL_ERR).setContext(" in QueryProcessor.getResultsFromModel(query, model)) \"" + query + "\"", ex));
        }
    }

    public static void main(String[] args) throws RestException, JsonParseException, JsonMappingException, IOException {
        ServiceConfig.initForTests("http://buda1.bdrc.io:13180/fuseki/rfc011rw/query");

        HttpURLConnection connection = (HttpURLConnection) new URL("https://raw.githubusercontent.com/buda-base/owl-schema/master/ont-policy.rdf")
                .openConnection();
        InputStream stream = connection.getInputStream();
        Model tmp = ModelFactory.createDefaultModel();
        tmp.read(stream, RDFLanguages.strLangRDFXML);
        // tmp.write(System.out, "TURTLE");
        stream.close();

        Model m = QueryProcessor.getDescribeModel("bda:AccessOpen", null, null);
        System.out.println("MODEL SIZE : " + m.size());
        m.write(System.out, "TURTLE");

    }
}