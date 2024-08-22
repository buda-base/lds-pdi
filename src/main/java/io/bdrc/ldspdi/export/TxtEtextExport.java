package io.bdrc.ldspdi.export;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.bdrc.auth.AccessInfo;
import io.bdrc.auth.AccessInfoAuthImpl;
import io.bdrc.ewtsconverter.EwtsConverter;
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
    
    public static final EwtsConverter ewtsConverter = new EwtsConverter();
    public final static Logger log = LoggerFactory.getLogger(TxtEtextExport.class);

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
        public int compare(final QuerySolution a, final QuerySolution b) {
            // there is one solution that contains the access, etc. we put it first
            if (!a.contains("chunkstart")) return -1;
            if (!b.contains("chunkstart")) return 1;
            final Integer ai = a.getLiteral("chunkstart").getInt();
            final Integer bi = b.getLiteral("chunkstart").getInt();
            return ai.compareTo(bi);
        }
    }

    static final StartCharComparator startCharComparatorInstance = new StartCharComparator();

    public static String getStringForTxt(final ResultSetWrapper res, final Integer startChar, final Integer endChar, final Map<String,String> ltagConversionMap) {
        List<QuerySolution> sols = res.getQuerySolutions();
        Collections.sort(sols, startCharComparatorInstance);
        final StringBuilder sb = new StringBuilder();
        for (QuerySolution qs : sols) {
            if (!qs.contains("chunkstart"))
                continue;
            final int qsStartChar = qs.getLiteral("chunkstart").getInt();
            final int qsEndChar = qs.getLiteral("chunkend").getInt();
            final Literal qsContent = qs.getLiteral("chunkcontent").asLiteral();
            final String qsContentS = qsContent.getString();
            final String qsContentSToAdd;
            if (qsStartChar < startChar && qsEndChar > endChar) {
                qsContentSToAdd = qsContentS.substring(startChar - qsStartChar, endChar - qsStartChar);
            } else if (qsStartChar < startChar) {
                qsContentSToAdd = qsContentS.substring(startChar - qsStartChar);
            } else if (qsEndChar > endChar) {
                qsContentSToAdd = qsContentS.substring(0, endChar - qsStartChar);
            } else {
                qsContentSToAdd = qsContentS;
            }
            if (ltagConversionMap.containsKey(qsContent.getLanguage())) {
                // here we just assume that we're converting to ewts since it's the only thing that
                // can happen in the code
                sb.append(ewtsConverter.toWylie(qsContentSToAdd));
            } else {
                sb.append(qsContentSToAdd);
            }
        }
        return sb.toString();
    }
    
    public static final Map<String,String> getLtagConversionMap(final List<Locale> llist) {
        final Map<String,String> res = new HashMap<>();
        for (final Locale l : llist) {
            final String ltag = l.toLanguageTag();
            if (ltag.equals("bo-x-ewts")) {
                res.put("bo", "bo-x-ewts");
            }
        }
        return res;
    }
    
    public static void addToLtagConversionMap(final String prefLangs, Map<String,String> ltagConversionMap) {
        if (prefLangs == null || prefLangs.isEmpty())
            return;
        for (final String l : prefLangs.split(",")) {
            if (l.equals("bo-x-ewts")) {
                ltagConversionMap.put("bo", "bo-x-ewts");
            }
        }
    }
    
    public static ResponseEntity<StreamingResponseBody> getResponse(final HttpServletRequest request, final String resUri, final Integer startChar, final Integer endChar, final String resName, final String prefLangs) throws RestException {
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
        AccessInfo acc = (AccessInfo) request.getAttribute("access");
        if (acc == null)
            acc = new AccessInfoAuthImpl();
        final String accessShortName = qs.get("access").asResource().getLocalName();
        final String statusShortName = qs.get("status").asResource().getLocalName();
        final AccessInfoAuthImpl.AccessLevel al = acc.hasResourceAccess(accessShortName, statusShortName, qs.get("einst").asResource().getURI());
        if (al != AccessInfoAuthImpl.AccessLevel.OPEN) {
            return ResponseEntity.status(acc.isLogged() ? 403 : 401).cacheControl(CacheControl.noCache())
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
        final List<Locale> locales = Collections.list(request.getLocales());
        final Map<String,String> ltagConversionMap = getLtagConversionMap(locales);
        addToLtagConversionMap(prefLangs, ltagConversionMap);
        final String resStr = getStringForTxt(res, startChar, endChar, ltagConversionMap);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).header("Allow", "GET, OPTIONS, HEAD")
                .header("Vary", "Negotiate, Accept")
                .header("Content-Disposition", "attachment; filename=\""+fName+"\"")
                .cacheControl(cc)
                .body(StreamingHelpers.getStream(resStr));
    }

}
