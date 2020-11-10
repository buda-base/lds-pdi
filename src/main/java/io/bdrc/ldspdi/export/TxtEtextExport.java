package io.bdrc.ldspdi.export;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.apache.jena.query.QuerySolution;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.bdrc.auth.Access;
import io.bdrc.auth.Access.AccessLevel;
import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.rest.controllers.PublicDataController;
import io.bdrc.ldspdi.rest.features.CorsFilter;
import io.bdrc.ldspdi.results.ResultSetWrapper;
import io.bdrc.ldspdi.sparql.LdsQuery;
import io.bdrc.ldspdi.sparql.LdsQueryService;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.GeoLocation;
import io.bdrc.libraries.StreamingHelpers;

public class TxtEtextExport {

    public static ResultSetWrapper getResults(final String resUri, final Integer startChar, final Integer endChar) throws RestException {
        Map<String, String> args = new HashMap<>();
        args.put("R_RES", resUri);
        args.put("I_START", startChar.toString());
        args.put("I_END", endChar.toString());
        // process
        final LdsQuery qfp = LdsQueryService.get("ChunksByRange.arq", "library");
        final String query = qfp.getParametizedQuery(args, true);
        ResultSetWrapper res = QueryProcessor.getResults(query, null, null, "100000");
        return res;
    }

    static class StartCharComparator implements Comparator<QuerySolution> {
        @Override
        public int compare(QuerySolution a, QuerySolution b) {
            // there is one solution that contains the access, etc. we put it first
            if (!a.contains("chunkstart")) return -1;
            if (!b.contains("chunkstart")) return 1;
            return a.getLiteral("chunkstart").getString().compareTo(b.getLiteral("chunkstart").getString());
        }
    }

    static final StartCharComparator startCharComparatorInstance = new StartCharComparator();

    public static String getStringForTxt(final ResultSetWrapper res, final Integer startChar, final Integer endChar) {
        List<QuerySolution> sols = res.getQuerySolutions();
        Collections.sort(sols, startCharComparatorInstance);
        final StringBuilder sb = new StringBuilder();
        for (QuerySolution qs : sols) {
            if (!qs.contains("chunkstart"))
                continue;
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
    
    public static ResponseEntity<StreamingResponseBody> getResponse(final HttpServletRequest request, final String resUri, final Integer startChar, final Integer endChar, final String resName) throws RestException {
        final ResultSetWrapper res = getResults(resUri, startChar, endChar);
        if (res.numResults < 2) {
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setMsg("Resource does not exist or no character in range"));
        }
        List<QuerySolution> sols = res.getQuerySolutions();
        Collections.sort(sols, startCharComparatorInstance);
        QuerySolution qs = sols.get(0);
        if (!qs.contains("ric")) {
            throw new RestException(500, LdsError.UNKNOWN_ERR, "cannot get information from Fuseki about access of "+resUri);
        }
        boolean restrictedInChina = qs.get("ric").asLiteral().getBoolean();
        if (restrictedInChina && GeoLocation.isFromChina(request)) {
            return ResponseEntity.status(451).contentType(MediaType.TEXT_PLAIN).body(StreamingHelpers.getStream("Etext not available in your geographical area"));
        }
        Access acc = (Access) request.getAttribute("access");
        if (acc == null)
            acc = new Access();
        final String accessShortName = qs.get("access").asResource().getLocalName();
        final String statusShortName = qs.get("status").asResource().getLocalName();
        final AccessLevel al = acc.hasResourceAccess(accessShortName, statusShortName, qs.get("einst").asResource().getURI());
        if (al != AccessLevel.OPEN) {
            return ResponseEntity.status(acc.isUserLoggedIn() ? 403 : 401).cacheControl(CacheControl.noCache())
                    .body(StreamingHelpers.getStream("Insufficient rights"));
        }
        CacheControl cc = CacheControl.maxAge(CorsFilter.ACCESS_CONTROL_MAX_AGE_IN_SECONDS, TimeUnit.SECONDS);
        if (!accessShortName.equals("AccessOpen") || restrictedInChina) {
            cc = cc.cachePrivate();
        } else {
            cc = cc.cachePublic();
        }
        String fName =  resName;
        if ((!startChar.equals(0)) || !endChar.equals(PublicDataController.defaultMaxValI)) {
            fName += "-" + startChar.toString() + "-";
            if (!endChar.equals(PublicDataController.defaultMaxValI)) {
                fName += endChar.toString();
            } else {
                fName += "end";
            }
        }
        fName += ".txt";
        final String resStr = getStringForTxt(res, startChar, endChar);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).header("Allow", "GET, OPTIONS, HEAD")
                .header("Vary", "Negotiate, Accept")
                .header("Content-Disposition", "attachment; filename=\""+fName+"\"")
                .cacheControl(cc)
                .body(StreamingHelpers.getStream(resStr));
    }

}
