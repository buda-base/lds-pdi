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
import io.bdrc.taxonomy.TaxModel;
import io.bdrc.taxonomy.Taxonomy;

public class TaxResultTest {
    @BeforeClass
    public static void init() throws JsonParseException, JsonMappingException, IOException {
        final Model taxModel = Utils.getModelFromFileName(Utils.TESTDIR + "O9TAXTBRC201605" + ".ttl", Lang.TURTLE);
        TaxModel.initWithModel(taxModel);
        Taxonomy.init("http://purl.bdrc.io/resource/O9TAXTBRC201605");
    }
    
    @Test
    public void mainTest() throws RestException, JsonGenerationException, JsonMappingException, IOException {
        final Model resModel = Utils.getModelFromFileName(Utils.TESTDIR + "taxtest-resmodel" + ".ttl", Lang.TURTLE);
        Map<String, Object> res = WorkResults.getResultsMap(resModel);
        ObjectMapper om = new ObjectMapper();
        om.writerWithDefaultPrettyPrinter().writeValue(System.out, res);
    }
}
