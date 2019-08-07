package io.bdrc.taxonomy;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.LdsQuery;
import io.bdrc.ldspdi.sparql.LdsQueryService;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.restapi.exceptions.RestException;

public class TaxModel {

    static Model model = null;

    public static void fetchModel() throws RestException {
        final LdsQuery qfp = LdsQueryService.get(ServiceConfig.getProperty("taxtreeArqFile"), "library");
        final Map<String, String> map = new HashMap<>();
        map.put("R_RES", ServiceConfig.getProperty("taxonomyRoot"));
        final String query = qfp.getParametizedQuery(map, false);
        model = QueryProcessor.getGraph(query);
    }

    public static Model getModel() {
        return model;
    }

}
