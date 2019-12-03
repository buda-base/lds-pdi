package io.bdrc.ldspdi.sparql;

/*******************************************************************************
 * Copyright (c) 2018 Buddhist Digital Resource Center (BDRC)
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.objects.json.IntParam;
import io.bdrc.ldspdi.objects.json.Output;
import io.bdrc.ldspdi.objects.json.Param;
import io.bdrc.ldspdi.objects.json.QueryTemplate;
import io.bdrc.ldspdi.objects.json.ResParam;
import io.bdrc.ldspdi.objects.json.StringParam;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.libraries.Prefixes;

public class LdsQuery {

    private HashMap<String, String> metaInf;
    private String query;
    private String queryHtml;
    private String queryName;
    private HashMap<String, String> litLangParams = new HashMap<>();
    private QueryTemplate template;
    private ArrayList<Param> params;
    private ArrayList<Output> outputs;
    private String prefixedQuery;
    private long limit_max = Long.parseLong(ServiceConfig.getProperty(QueryConstants.LIMIT));

    public final static Logger log = LoggerFactory.getLogger(LdsQuery.class);

    public LdsQuery(String filePath) throws RestException {
        metaInf = new HashMap<>();
        final File f = new File(filePath);
        final String fileBaseName = f.getName();
        queryName = fileBaseName.substring(0, fileBaseName.lastIndexOf("."));
        parseTemplate(f);
        template = new QueryTemplate(getTemplateName(), QueryConstants.QUERY_PUBLIC_DOMAIN, metaInf.get(QueryConstants.QUERY_URL), metaInf.get(QueryConstants.QUERY_SCOPE), metaInf.get(QueryConstants.QUERY_RESULTS),
                metaInf.get(QueryConstants.QUERY_RETURN_TYPE), metaInf.get(QueryConstants.QUERY_PARAMS), params, outputs, getQuery());
    }

    public String getTemplateName() {
        return this.queryName;
    }

    private void parseTemplate(File file) throws RestException {
        // log.info("parse template at {}", file.getAbsolutePath());
        BufferedReader brd = null;
        HashMap<String, HashMap<String, String>> p_map = new HashMap<>();
        HashMap<String, HashMap<String, String>> o_map = new HashMap<>();
        String readLine = "";
        query = "";
        queryHtml = "";
        try {
            brd = new BufferedReader(new FileReader(file));
            while ((readLine = brd.readLine()) != null) {
                readLine = readLine.trim();
                boolean processed = false;
                if (readLine.startsWith("#")) {
                    readLine = readLine.substring(1);

                    int index = readLine.indexOf("=");
                    if (index != -1) {
                        String info0 = readLine.substring(0, index);
                        String info1 = readLine.substring(index + 1).trim();
                        if (info0.startsWith(QueryConstants.PARAM)) {
                            List<String> parsed = Arrays.asList(info0.split(Pattern.compile("\\.").toString()));
                            if (parsed.size() == 3 && QueryConstants.isValidInfoType(parsed.get(2))) {
                                HashMap<String, String> mp = p_map.get(parsed.get(1));
                                if (mp == null) {
                                    mp = new HashMap<>();
                                }
                                mp.put(parsed.get(2), info1);
                                p_map.put(parsed.get(1), mp);
                            } else {
                                throw new RestException(500, new LdsError(LdsError.PARSE_ERR).setContext("Query template parsing failed, invalid param declaration :" + info0));
                            }
                            processed = true;
                        }
                        if (info0.startsWith(QueryConstants.OUTPUT)) {
                            List<String> parsed = Arrays.asList(info0.split(Pattern.compile("\\.").toString()));
                            if (parsed.size() == 3 && QueryConstants.isValidOutput(parsed.get(2))) {
                                HashMap<String, String> op = o_map.get(parsed.get(1));
                                if (op == null) {
                                    op = new HashMap<>();
                                }
                                op.put(parsed.get(2), info1);
                                o_map.put(parsed.get(1), op);
                            }
                            processed = true;
                        }
                        if (!processed) {
                            metaInf.put(info0, info1);
                        }
                    }
                } else {
                    query = query + " " + readLine;
                    queryHtml = queryHtml + " " + readLine + "<br>";
                }
            }
            brd.close();
            // Check the validity of the return type
            checkReturnType();
            queryHtml = queryHtml.substring(15);
            params = buildParams(p_map);
            outputs = buildOutputs(o_map);
            prefixedQuery = Prefixes.getPrefixesString() + " " + query;
            String customLimit = getMetaInf().get(QueryConstants.QUERY_LIMIT);
            if (customLimit != null) {
                this.limit_max = Long.parseLong(customLimit);
            }
        } catch (Exception ex) {
            log.error("QueryFile parsing error", ex);
            throw new RestException(500, new LdsError(LdsError.PARSE_ERR).setContext("Query template parsing failed for: " + file.getName()));
        } finally {
            try {
                brd.close();
            } catch (Exception e) {
            }
        }
    }

    public long getLimit_max() {
        return limit_max;
    }

    public HashMap<String, String> getMetaInf() {
        return metaInf;
    }

    public void setParams(ArrayList<Param> params) {
        this.params = params;
    }

    public String getPrefixedQuery() {
        return prefixedQuery;
    }

    private void checkReturnType() throws RestException {
        if (!QueryConstants.isValidReturnType(metaInf.get(QueryConstants.QUERY_RETURN_TYPE))) {
            throw new RestException(500, new LdsError(LdsError.PARSE_ERR).setContext("Query template parsing failed :" + metaInf.get(QueryConstants.QUERY_RETURN_TYPE) + " is not a valid query return type"));
        }
    }

    private ArrayList<Param> buildParams(HashMap<String, HashMap<String, String>> p_map) throws RestException {
        ArrayList<Param> p = new ArrayList<>();
        Set<String> names = p_map.keySet();
        for (String name : names) {
            HashMap<String, String> mp = p_map.get(name);
            switch (mp.get(QueryConstants.PARAM_TYPE)) {
            case QueryConstants.STRING_PARAM:
                StringParam stp = new StringParam(name);
                stp.setLangTag(mp.get(QueryConstants.PARAM_LANGTAG));
                stp.setIsLuceneParam(mp.get(QueryConstants.PARAM_LUCENE));
                stp.setExample(mp.get(QueryConstants.PARAM_EXAMPLE));
                p.add(stp);
                break;
            case QueryConstants.INT_PARAM:
                IntParam intp = new IntParam(name);
                intp.setDescription(mp.get(QueryConstants.PARAM_DESC));
                p.add(intp);
                break;
            case QueryConstants.RES_PARAM:
                ResParam rtp = new ResParam(name, mp.get(QueryConstants.PARAM_SUBTYPE));
                rtp.setDescription(mp.get(QueryConstants.PARAM_DESC));
                p.add(rtp);
                break;
            }
        }
        return p;
    }

    private ArrayList<Output> buildOutputs(HashMap<String, HashMap<String, String>> o_map) throws RestException {
        ArrayList<Output> o = new ArrayList<>();
        Set<String> names = o_map.keySet();
        for (String name : names) {
            HashMap<String, String> op = o_map.get(name);
            Output output = new Output(name, op.get(QueryConstants.OUTPUT_TYPE), op.get(QueryConstants.OUTPUT_DESC));
            o.add(output);
        }
        return o;
    }

    public String checkQueryArgsSyntax() {
        String check = "";
        String q_params = metaInf.get(QueryConstants.QUERY_PARAMS);
        if (q_params == null) {
            // no params no need to parse
            return "";
        }
        String[] args = q_params.split(Pattern.compile(",").toString());
        List<String> params = Arrays.asList(args);
        for (String arg : args) {
            if (!arg.equals(QueryConstants.QUERY_NO_ARGS)) {
                // Param is not a Lang param --> if it's not present in the query --> Send error
                if (!arg.startsWith(QueryConstants.LITERAL_LG_ARGS_PARAMPREFIX) && query.indexOf("?" + arg) == -1) {
                    check = "Arg syntax is incorrect : query does not have a ?" + arg + " variable";
                    return check;
                }
                // Param is a Lang param --> check if the corresponding lit param is present
                if (arg.startsWith(QueryConstants.LITERAL_LG_ARGS_PARAMPREFIX)) {
                    String expectedLiteralParam = QueryConstants.LITERAL_ARGS_PARAMPREFIX + arg.substring(arg.indexOf("_") + 1);
                    if (!params.contains(expectedLiteralParam)) {
                        check = "Arg syntax is incorrect : query does not have a literal variable " + expectedLiteralParam + " corresponding to lang " + arg + " variable";
                        return check;
                    }
                    litLangParams.put(expectedLiteralParam, arg);
                }
            }
        }
        return "";
    }

    public String getQuery() {
        return query;
    }

    public String getParametizedQuery(Map<String, String> converted) throws RestException {

        if (!checkQueryArgsSyntax().trim().equals("")) {
            throw new RestException(500, new LdsError(LdsError.PARSE_ERR).setContext(" in File->" + getTemplateName() + "; ERROR: " + checkQueryArgsSyntax()));
        }
        List<String> params = getQueryParams();
        HashMap<String, String> litParams = getLitLangParams();
        if (converted == null) {
            converted = new HashMap<>();
        }
        if (!hasValidParams(converted.keySet(), params)) {
            throw new RestException(500, new LdsError(LdsError.MISSING_PARAM_ERR).setContext(" in LdsQuery.getParametizedQuery() " + converted));
        }
        ParameterizedSparqlString queryStr = new ParameterizedSparqlString(prefixedQuery, Prefixes.getPrefixMapping());
        for (String st : converted.keySet()) {
            if (st.startsWith(QueryConstants.INT_ARGS_PARAMPREFIX)) {
                queryStr.setLiteral(st, Integer.parseInt(converted.get(st)));
            }
            if (st.startsWith(QueryConstants.RES_ARGS_PARAMPREFIX)) {
                String param = converted.get(st);
                if (param.contains(":")) {
                    if (Helpers.isValidURI(param)) {
                        queryStr.setIri(st, param);
                    } else {
                        String[] parts = param.split(Pattern.compile(":").toString());
                        if (parts[0] == null) {
                            parts[0] = "";
                        }
                        // may be done automatically by parametrizedSparqlString, to be checked
                        final String fullUri = Prefixes.getFullIRI(parts[0]);
                        if (fullUri != null) {
                            queryStr.setIri(st, fullUri + parts[1]);
                        } else {
                            throw new RestException(500, new LdsError(LdsError.PARSE_ERR).setContext(" in QueryFileParser.getParametizedQuery() ParameterException :" + param + " Unknown prefix"));
                        }
                    }
                } else {
                    throw new RestException(500, new LdsError(LdsError.PARSE_ERR).setContext(" in QueryFileParser.getParametizedQuery() ParameterException :" + param + " This parameter must be of the form prefix:resource or spaceNameUri/resource"));
                }

            }
            if (st.startsWith(QueryConstants.LITERAL_ARGS_PARAMPREFIX)) {
                String lim = converted.get(QueryConstants.LITERAL_LIMITPREFIX + st.substring(st.indexOf('_') + 1));
                if (lim == null) {
                    lim = ServiceConfig.getProperty("text_query_limit");
                }
                if (litParams.keySet().contains(st)) {
                    String lang = converted.get(litParams.get(st)).toLowerCase();
                    try {
                        new Locale.Builder().setLanguageTag(lang).build();
                    } catch (IllformedLocaleException ex) {
                        return "ERROR --> language param :" + lang + " is not a valid BCP 47 language tag" + ex.getMessage();
                    }
                    queryStr.setLiteral(st, converted.get(st), lang + " " + lim);

                } else {
                    // Some literals do not have a lang associated with them
                    queryStr.setLiteral(st, converted.get(st) + " " + lim);
                }
            }
        }
        Query q = queryStr.asQuery();
        if (!metaInf.get(QueryConstants.QUERY_RETURN_TYPE).equals(QueryConstants.GRAPH)) {
            if (q.hasLimit()) {
                if (q.getLimit() > limit_max) {
                    q.setLimit(limit_max);
                }
            } else {
                q.setLimit(limit_max);
            }
        }

        if (q.toString().startsWith(QueryConstants.QUERY_ERROR)) {
            throw new RestException(500, new LdsError(LdsError.PARSE_ERR).setContext(" in LdsQuery.getParametizedQuery() template ->" + getTemplateName() + ".arq" + "; ERROR: " + query));
        }
        return q.toString();
    }

    public HashMap<String, String> getLitLangParams() {
        return litLangParams;
    }

    public String getQueryHtml() {
        return queryHtml;
    }

    public QueryTemplate getTemplate() {
        return template;
    }

    public List<String> getQueryParams() {
        if (template.getQueryParams() == null) {
            return new ArrayList<String>();
        }
        return Arrays.asList(template.getQueryParams().split(","));
    }

    private static boolean hasValidParams(Set<String> reqParams, List<String> params) {
        for (String pr : params) {
            if (!reqParams.contains(pr.trim()) && !pr.equals("NONE")) {
                return false;
            }
        }
        return true;
    }

}
