package io.bdrc.ldspdi.test;

import java.io.IOException;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.results.library.WorkResults;
import io.bdrc.libraries.Models;
import io.bdrc.taxonomy.Taxonomy;

public class TaxResultTest {
    @BeforeClass
    public static void init() throws JsonParseException, JsonMappingException, IOException {
        final Model topicTaxModel = Utils.getModelFromFileName(Utils.TESTDIR + "O9TAXTBRC201605.ttl", Lang.TURTLE);
        final Model femcTaxModel = Utils.getModelFromFileName(Utils.TESTDIR + "FEMCScheme.ttl", Lang.TURTLE);
        final Model genreTaxModel = Utils.getModelFromFileName(Utils.TESTDIR + "O3JW5309.ttl", Lang.TURTLE);
        final Taxonomy genreTaxonomy = new Taxonomy(Models.BDR+"O3JW5309", genreTaxModel);
        final Taxonomy topicTaxonomy = new Taxonomy(Models.BDR+"O9TAXTBRC201605", topicTaxModel);
        final Taxonomy femcTaxonomy = new Taxonomy(Models.BDR+"FEMCScheme", femcTaxModel);
        //WorkResults.initForTests(genreTaxonomy, topicTaxonomy);
        WorkResults.initForTests(femcTaxonomy, femcTaxonomy);
    }
    
    @Test
    public void mainTest() throws RestException, JsonGenerationException, JsonMappingException, IOException {
        //final Model resModel = Utils.getModelFromFileName(Utils.TESTDIR + "taxtest-resmodel.ttl", Lang.TURTLE);
        final Model resModel = Utils.getModelFromFileName(Utils.TESTDIR + "taxtest-femc.ttl", Lang.TURTLE);
        Map<String, Object> res = WorkResults.getResultsMap(resModel);
        ObjectMapper om = new ObjectMapper();
        //om.writerWithDefaultPrettyPrinter().writeValue(System.out, res);
        //om.writerWithDefaultPrettyPrinter().writeValue(System.out, (((Map<String, Object>) res.get("main"))));
        om.writerWithDefaultPrettyPrinter().writeValue(System.out, (((Map<String, Object>) res.get("facets")).get("genres")));
        //om.writerWithDefaultPrettyPrinter().writeValue(System.out, (((Map<String, Object>) res.get("facets")).get("genres")));
    }
}
