package io.bdrc.ldspdi.export;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.QuerySolution;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.bdrc.ldspdi.results.ResultSetWrapper;
import io.bdrc.ldspdi.sparql.LdsQuery;
import io.bdrc.ldspdi.sparql.LdsQueryService;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

public class TxtEtextExport {

    public static ResultSetWrapper getResults(final String resUri, final Integer startChar, final Integer endChar) throws RestException {
        Map<String, String> args = new HashMap<>();
        args.put("R_RES", resUri);
        args.put("I_START", startChar.toString());
        args.put("I_END", endChar.toString());
        // process
        final LdsQuery qfp = LdsQueryService.get("ChunksByRange.arq", "library");
        final String query = qfp.getParametizedQuery(args, false);
        ResultSetWrapper res = QueryProcessor.getResults(query, null, null, "100000");
        return res;
    }

    static class StartCharComparator implements Comparator<QuerySolution> {
        @Override
        public int compare(QuerySolution a, QuerySolution b) {
            return a.getLiteral("chunkstart").getString().compareTo(b.getLiteral("chunkstart").getString());
        }
    }

    static final StartCharComparator startCharComparatorInstance = new StartCharComparator();

    public static String getStringForTxt(final ResultSetWrapper res, final Integer startChar, final Integer endChar) {
        List<QuerySolution> sols = res.getQuerySolutions();
        Collections.sort(sols, startCharComparatorInstance);
        final StringBuilder sb = new StringBuilder();
        for (QuerySolution qs : sols) {
            final int qsStartChar = qs.getLiteral("chunkstart").getInt();
            final int qsEndChar = qs.getLiteral("chunkend").getInt();
            final String qsContent = qs.getLiteral("chunkcontent").getString();
            if (qsStartChar < startChar && qsEndChar > endChar) {
                sb.append(qsContent.substring(startChar - qsStartChar, endChar - qsStartChar));
            } else if (qsStartChar < startChar) {
                sb.append(qsContent.substring(startChar - qsStartChar));
            } else if (qsEndChar > endChar) {
                sb.append(qsContent.substring(0, endChar - qsStartChar));
            } else {
                sb.append(qsContent);
            }
        }
        return sb.toString();
    }

    public static ResponseEntity<StreamingResponseBody> getResponse(final String resUri, final Integer startChar, final Integer endChar) throws RestException {
        final ResultSetWrapper res = getResults(resUri, startChar, endChar);
        if (res.numResults == 0) {
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setMsg("Resource does not exist or no character in range"));
        }
        final String resStr = getStringForTxt(res, startChar, endChar);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).header("Allow", "GET, OPTIONS, HEAD").header("Vary", "Negotiate, Accept").body(Helpers.getStream(resStr));
    }

}
