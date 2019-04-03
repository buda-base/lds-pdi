package io.bdrc.ldspdi.sparql;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.bdrc.ldspdi.results.ResultSetWrapper;
import io.bdrc.ldspdi.results.ResultsCache;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

public class QueryProcessor {

    public final static Logger log = LoggerFactory.getLogger(QueryProcessor.class.getName());

    public static Model getCoreResourceGraph(final String URI, String fusekiUrl, String prefixes) throws RestException {
        return getSimpleResourceGraph(URI, "Resgraph.arq", fusekiUrl, prefixes);
    }

    public static Model getSimpleResourceGraph(final String URI, final String queryName) throws RestException {
        return getSimpleResourceGraph(URI, queryName, null, null);
    }

    public static Model getSimpleResourceGraph(final String URI, final String queryName, String fusekiUrl, String prefixes) throws RestException {
        if (prefixes == null) {
            prefixes = getPrefixes();
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
            String query = qfp.getParametizedQuery(map, false);
            Query q = QueryFactory.create(query);
            QueryExecution qe = QueryExecutionFactory.sparqlService(fusekiUrl, q);
            qe.setTimeout(Long.parseLong(ServiceConfig.getProperty(QueryConstants.QUERY_TIMEOUT)));
            model = qe.execDescribe();
            qe.close();
            ResultsCache.addToCache(model, hash);
        }
        return model;
    }

    public static Model getGraph(final String query, String fusekiUrl, String prefixes) throws RestException {
        if (prefixes == null) {
            prefixes = getPrefixes();
        }
        if (fusekiUrl == null) {
            fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }
        final int hash = Objects.hashCode(query);
        Model model = (Model) ResultsCache.getObjectFromCache(hash);
        if (model == null) {
            log.trace("executing query: {}", query);
            final Query q = QueryFactory.create(prefixes + " " + query);
            final QueryExecution qe = QueryExecutionFactory.sparqlService(fusekiUrl, q);
            qe.setTimeout(Long.parseLong(ServiceConfig.getProperty(QueryConstants.QUERY_TIMEOUT)));
            model = qe.execConstruct();
            qe.close();
            ResultsCache.addToCache(model, hash);
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
        DatasetAccessor access = DatasetAccessorFactory.createHTTP(fusekiUrl);
        // Model m=access.getModel(AuthProps.getProperty("authDataGraph"));
        Model m = access.getModel(ServiceConfig.getProperty(graph));
        return m;
    }

    public static QueryExecution getResultSet(String query, String fusekiUrl) {
        if (fusekiUrl == null) {
            fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }
        QueryExecution qe = QueryExecutionFactory.sparqlService(fusekiUrl, QueryFactory.create(query));
        qe.setTimeout(Long.parseLong(ServiceConfig.getProperty(QueryConstants.QUERY_TIMEOUT)));
        return qe;
    }

    public static void updateOntology(Model mod, String fusekiUrl, String graph) {
        if (fusekiUrl == null) {
            fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }
        log.info("Service fuseki >> " + fusekiUrl);
        log.info("Graph >> " + graph);
        log.info("InfModel Size >> " + mod.listStatements().toSet().size());
        DatasetAccessor access = DatasetAccessorFactory.createHTTP(fusekiUrl);
        access.putModel(graph, mod);
    }

    public static ResultSetWrapper getResults(final String query, String fusekiUrl, final String hash, final String pageSize) {

        if (hash != null) {
            return (ResultSetWrapper) ResultsCache.getObjectFromCache(Integer.parseInt(hash));
        }
        if (fusekiUrl == null) {
            fusekiUrl = ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);
        }
        long start = System.currentTimeMillis();
        final QueryExecution qe = getResultSet(query, fusekiUrl);
        final ResultSet jrs = qe.execSelect();
        long elapsed = System.currentTimeMillis() - start;
        int psz = Integer.parseInt(ServiceConfig.getProperty(QueryConstants.PAGE_SIZE));
        if (pageSize != null) {
            psz = Integer.parseInt(pageSize);
        }
        final ResultSetWrapper res = new ResultSetWrapper(jrs, elapsed, psz);
        qe.close();
        final int new_hash = Objects.hashCode(res);
        res.setHash(new_hash);
        ResultsCache.addToCache(res, Objects.hashCode(res));
        return res;
    }

    public static Model getGraphFromModel(String query, Model model) throws RestException {
        try {
            QueryExecution qexec = QueryExecutionFactory.create(query, model);
            Model m = qexec.execDescribe();
            return m;
        } catch (Exception ex) {
            throw new RestException(500, new LdsError(LdsError.SPARQL_ERR).setContext(" in QueryProcessor.getResultsFromModel(query, model)) \"" + query + "\"", ex));
        }
    }

    public static ResultSet getResultsFromModel(String query, Model model) throws RestException {
        try {
            QueryExecution qexec = QueryExecutionFactory.create(query, model);
            ResultSet res = qexec.execSelect();
            return res;
        } catch (Exception ex) {
            throw new RestException(500, new LdsError(LdsError.SPARQL_ERR).setContext(" in QueryProcessor.getResultsFromModel(query, model)) \"" + query + "\"", ex));
        }
    }

    public static String getPrefixes() throws RestException {
        String pref = Prefixes.getPrefixesString();
        if (pref != null) {
            return pref;
        } else {
            return "prefix :      <http://purl.bdrc.io/ontology/core/>\n" + "prefix adm:   <http://purl.bdrc.io/ontology/admin/>\n" + "prefix bdd:   <http://purl.bdrc.io/data/>\n" + "prefix bdo:   <http://purl.bdrc.io/ontology/core/>\n"
                    + "prefix bdr:   <http://purl.bdrc.io/resource/>\n" + "prefix bdan:  <http://purl.bdrc.io/annotation/>\n" + "prefix bdac:  <http://purl.bdrc.io/anncollection/>\n" + "prefix tbr:   <http://purl.bdrc.io/ontology/toberemoved/>\n"
                    + "prefix tmp:   <http://purl.bdrc.io/ontology/tmp/>\n" + "prefix aut:   <http://purl.bdrc.io/ontology/ext/auth/>\n" + "prefix adr:   <http://purl.bdrc.io/resource-auth/>\n"
                    + "prefix bf:    <http://id.loc.gov/ontologies/bibframe/>\n" + "prefix dcterms: <http://purl.org/dc/terms/>\n" + "prefix f:     <java:io.bdrc.ldspdi.sparql.functions.>\n" + "prefix foaf:  <http://xmlns.com/foaf/0.1/>\n"
                    + "prefix iiif2: <http://iiif.io/api/presentation/2#>\n" + "prefix iiif3: <http://iiif.io/api/presentation/3#>\n" + "prefix owl:   <http://www.w3.org/2002/07/owl#>\n"
                    + "prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" + "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" + "prefix skos:  <http://www.w3.org/2004/02/skos/core#>\n"
                    + "prefix vcard: <http://www.w3.org/2006/vcard/ns#>\n" + "prefix xsd:   <http://www.w3.org/2001/XMLSchema#>\n" + "prefix text:  <http://jena.apache.org/text#>\n" + "prefix oa:    <http://www.w3.org/ns/oa#>\n"
                    + "prefix as:    <http://www.w3.org/ns/activitystreams#>\n" + "prefix ldp:   <http://www.w3.org/ns/ldp#>\n" + "prefix sh: <http://www.w3.org/ns/shacl#>\n" + "prefix rsh: <http://purl.bdrc.io/shacl/core/shape/>";
        }
    }

    public static void main(String[] args) throws RestException, JsonParseException, JsonMappingException, IOException {
        ServiceConfig.initForTests("http://buda1.bdrc.io:13180/fuseki/bdrcrw/query");
        String query = "PREFIX  :     <http://purl.bdrc.io/ontology/core/>\n" + "PREFIX  bdan: <http://purl.bdrc.io/annotation/>\n" + "PREFIX  aut:  <http://purl.bdrc.io/ontology/ext/auth/>\n"
                + "PREFIX  bf:   <http://id.loc.gov/ontologies/bibframe/>\n" + "PREFIX  tbr:  <http://purl.bdrc.io/ontology/toberemoved/>\n" + "PREFIX  owl:  <http://www.w3.org/2002/07/owl#>\n"
                + "PREFIX  rsh:  <http://purl.bdrc.io/shacl/core/shape/>\n" + "PREFIX  xsd:  <http://www.w3.org/2001/XMLSchema#>\n" + "PREFIX  skos: <http://www.w3.org/2004/02/skos/core#>\n" + "PREFIX  bdac: <http://purl.bdrc.io/anncollection/>\n"
                + "PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + "PREFIX  oa:   <http://www.w3.org/ns/oa#>\n" + "PREFIX  sh:   <http://www.w3.org/ns/shacl#>\n" + "PREFIX  tmp:  <http://purl.bdrc.io/ontology/tmp/>\n"
                + "PREFIX  dcterms: <http://purl.org/dc/terms/>\n" + "PREFIX  text: <http://jena.apache.org/text#>\n" + "PREFIX  foaf: <http://xmlns.com/foaf/0.1/>\n" + "PREFIX  bdd:  <http://purl.bdrc.io/data/>\n"
                + "PREFIX  f:    <java:io.bdrc.ldspdi.sparql.functions.>\n" + "PREFIX  adm:  <http://purl.bdrc.io/ontology/admin/>\n" + "PREFIX  vcard: <http://www.w3.org/2006/vcard/ns#>\n" + "PREFIX  bdo:  <http://purl.bdrc.io/ontology/core/>\n"
                + "PREFIX  iiif2: <http://iiif.io/api/presentation/2#>\n" + "PREFIX  adr:  <http://purl.bdrc.io/resource-auth/>\n" + "PREFIX  iiif3: <http://iiif.io/api/presentation/3#>\n" + "PREFIX  bdr:  <http://purl.bdrc.io/resource/>\n"
                + "PREFIX  as:   <http://www.w3.org/ns/activitystreams#>\n" + "PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" + "PREFIX  ldp:  <http://www.w3.org/ns/ldp#>\n"
                + "PREFIX ad: <http://schemas.talis.com/2005/address/schema#>\n" + "PREFIX bd: <http://www.bigdata.com/rdf#>\n" + "\n" + "construct {?s ?p ?o} where {\n" + "graph <http://purl.bdrc.io/ontology/core/ontologySchema>\n"
                + "  {?s ?p ?o}\n" + "}";
        // final Query q = QueryFactory.create(query);

        // final QueryExecution qe =
        // QueryExecutionFactory.sparqlService("http://buda1.bdrc.io:13180/fuseki/bdrcrw/query",
        // q);
        // qe.setTimeout(Long.parseLong(ServiceConfig.getProperty(QueryConstants.QUERY_TIMEOUT)));
        // Model m = qe.execConstruct();
        // System.out.println(m.size());
        // qe.close();
        // m.write(System.out, "TURTLE");

        // DatasetAccessor access =
        // DatasetAccessorFactory.createHTTP("http://buda1.bdrc.io:13180/fuseki/bdrcrw/data");
        // access.deleteModel("http://purl.bdrc.io/ontology/ext/authSchema");

        /*
         * String update = "PREFIX  :     <http://purl.bdrc.io/ontology/core/>\n" +
         * "PREFIX  bdan: <http://purl.bdrc.io/annotation/>\n" +
         * "PREFIX  aut:  <http://purl.bdrc.io/ontology/ext/auth/>\n" +
         * "PREFIX  bf:   <http://id.loc.gov/ontologies/bibframe/>\n" +
         * "PREFIX  tbr:  <http://purl.bdrc.io/ontology/toberemoved/>\n" +
         * "PREFIX  owl:  <http://www.w3.org/2002/07/owl#>\n" +
         * "PREFIX  rsh:  <http://purl.bdrc.io/shacl/core/shape/>\n" +
         * "PREFIX  xsd:  <http://www.w3.org/2001/XMLSchema#>\n" +
         * "PREFIX  skos: <http://www.w3.org/2004/02/skos/core#>\n" +
         * "PREFIX  bdac: <http://purl.bdrc.io/anncollection/>\n" +
         * "PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
         * "PREFIX  oa:   <http://www.w3.org/ns/oa#>\n" +
         * "PREFIX  sh:   <http://www.w3.org/ns/shacl#>\n" +
         * "PREFIX  tmp:  <http://purl.bdrc.io/ontology/tmp/>\n" +
         * "PREFIX  dcterms: <http://purl.org/dc/terms/>\n" +
         * "PREFIX  text: <http://jena.apache.org/text#>\n" +
         * "PREFIX  foaf: <http://xmlns.com/foaf/0.1/>\n" +
         * "PREFIX  bdd:  <http://purl.bdrc.io/data/>\n" +
         * "PREFIX  f:    <java:io.bdrc.ldspdi.sparql.functions.>\n" +
         * "PREFIX  adm:  <http://purl.bdrc.io/ontology/admin/>\n" +
         * "PREFIX  vcard: <http://www.w3.org/2006/vcard/ns#>\n" +
         * "PREFIX  bdo:  <http://purl.bdrc.io/ontology/core/>\n" +
         * "PREFIX  iiif2: <http://iiif.io/api/presentation/2#>\n" +
         * "PREFIX  adr:  <http://purl.bdrc.io/resource-auth/>\n" +
         * "PREFIX  iiif3: <http://iiif.io/api/presentation/3#>\n" +
         * "PREFIX  bdr:  <http://purl.bdrc.io/resource/>\n" +
         * "PREFIX  as:   <http://www.w3.org/ns/activitystreams#>\n" +
         * "PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
         * "PREFIX  ldp:  <http://www.w3.org/ns/ldp#>\n" +
         * "PREFIX ad: <http://schemas.talis.com/2005/address/schema#>\n" +
         * "PREFIX bd: <http://www.bigdata.com/rdf#>\n" + "\n" +
         * "DELETE {?s adm:access bdr:AccessTemporarilyRestricted .}\n" +
         * "INSERT {?s adm:access bdr:AccessRestrictedTemporarily .}\n" + "where {\n" +
         * "{?s adm:access bdr:AccessTemporarilyRestricted .}\n" + "}";
         * 
         * RDFConnection conn = RDFConnectionFactory.connectFuseki(
         * "http://buda1.bdrc.io:13180/fuseki/bdrcrw/update"); conn.update(update);
         */
    }
}
