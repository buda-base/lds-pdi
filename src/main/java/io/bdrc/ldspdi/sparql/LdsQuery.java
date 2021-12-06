package io.bdrc.ldspdi.sparql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.impl.XSDDateTimeType;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.XSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.objects.json.BooleanParam;
import io.bdrc.ldspdi.objects.json.DateTimeParam;
import io.bdrc.ldspdi.objects.json.GYearParam;
import io.bdrc.ldspdi.objects.json.IntParam;
import io.bdrc.ldspdi.objects.json.Output;
import io.bdrc.ldspdi.objects.json.Param;
import io.bdrc.ldspdi.objects.json.QueryTemplate;
import io.bdrc.ldspdi.objects.json.ResParam;
import io.bdrc.ldspdi.objects.json.StringParam;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.utils.Helpers;

public class LdsQuery {

    public final static Logger log = LoggerFactory.getLogger(LdsQuery.class);

    private final String PARAM = "#param";
    private final String OUTPUT = "#output";
    private final String QUERY = "#Query";

    private QueryTemplate template;
    private String queryName;
    private String query;
    private String queryHtml;
    private HashMap<String, String> paramsData = new HashMap<>();
    private HashMap<String, String> outputsData = new HashMap<>();
    private HashMap<String, String> queryData = new HashMap<>();
    private List<String> requiredParams;
    private List<String> optionalParams;
    private ArrayList<String> outputsNames;
    private HashMap<String, String> litLangParams = new HashMap<>();
    private long limit_max = Long.parseLong(ServiceConfig.getProperty(QueryConstants.LIMIT));

    public LdsQuery(String filePath) throws RestException {
        final File f = new File(filePath);
        final String fileBaseName = f.getName();
        queryName = fileBaseName.substring(0, fileBaseName.lastIndexOf("."));
        readTemplateData(f);
        template = new QueryTemplate(getQueryName(), QueryConstants.QUERY_PUBLIC_DOMAIN,
                queryData.get(QueryConstants.QUERY_URL), queryData.get(QueryConstants.QUERY_SCOPE),
                queryData.get(QueryConstants.QUERY_RESULTS), queryData.get(QueryConstants.QUERY_RETURN_TYPE),
                queryData.get(QueryConstants.QUERY_PARAMS), queryData.get(QueryConstants.QUERY_OPT_PARAMS),
                buildParams(), buildOutputs(), getQuery());
    }

    private void readTemplateData(File file) throws RestException {
        BufferedReader brd = null;
        String readLine = "";
        query = "";
        queryHtml = "";
        outputsNames = new ArrayList<String>();
        try {
            brd = new BufferedReader(new FileReader(file));
            while ((readLine = brd.readLine()) != null) {
                readLine = readLine.trim();
                if (readLine.startsWith("#") && readLine.contains("=")) {
                    String[] tmp = new String[2];
                    tmp[0] = readLine.substring(0, readLine.indexOf("="));
                    tmp[1] = readLine.substring(readLine.indexOf("=") + 1);
                    if (readLine.startsWith(PARAM)) {
                        paramsData.put(tmp[0].substring(1, tmp[0].length()), tmp[1].trim());
                    }
                    if (readLine.startsWith(OUTPUT)) {
                        String s = readLine.substring(readLine.indexOf(".") + 1);
                        s = s.substring(0, s.indexOf("."));
                        if (!outputsNames.contains(s)) {
                            outputsNames.add(s);
                        }
                        outputsData.put(tmp[0].substring(1, tmp[0].length()), tmp[1].trim());
                    }
                    if (readLine.startsWith(QUERY)) {
                        queryData.put(tmp[0].substring(1, tmp[0].length()), tmp[1].trim());
                    }
                } else {
                    if (!readLine.startsWith("#")) {
                        query = query + " " + readLine;
                        queryHtml = queryHtml + " " + readLine + "<br>";
                    }
                }
            }
            brd.close();
            queryHtml = queryHtml.substring(15);
            //log.error(queryData.toString());
            String customLimit = queryData.get(QueryConstants.QUERY_LIMIT);
            if (customLimit != null) {
                this.limit_max = Long.parseLong(customLimit);
            }
        } catch (Exception e) {
            log.error("QueryFile parsing error", e);
            throw new RestException(500, new LdsError(LdsError.PARSE_ERR)
                    .setContext("Query template parsing failed for: " + file.getName()));
        } finally {
            try {
                brd.close();
            } catch (Exception e) {
            }
        }
    }
    
    public String qNameToURI(final String param) throws RestException {
        if (param.contains(":")) {
            if (Helpers.isValidURI(param)) {
                return param;
            } else {
                String[] parts = param.split(Pattern.compile(":").toString());
                if (parts[0] == null) {
                    parts[0] = "";
                }
                // may be done automatically by
                // parametrizedSparqlString, to be checked
                final String fullUri = ServiceConfig.PREFIX.getFullIRI(parts[0]);
                if (fullUri != null) {
                    return fullUri + parts[1];
                } else {
                    throw new RestException(500,
                            new LdsError(LdsError.PARSE_ERR).setContext(
                                    " in QueryFileParser.getParametizedQuery() ParameterException :" + param
                                            + " Unknown prefix"));
                }
            }
        } else {
            throw new RestException(500, new LdsError(LdsError.PARSE_ERR)
                    .setContext(" in QueryFileParser.getParametizedQuery() ParameterException :" + param
                            + " This parameter must be of the form prefix:resource or spaceNameUri/resource"));
        }
    }
    
    public final List<Resource> nulllist = Arrays.asList(ResourceFactory.createResource("http://null.null/none"));

    public String getParametizedQuery(Map<String, String> converted, boolean limit) throws RestException {
        String error = checkQueryArgsSyntax(converted);
        if (!error.trim().equals("")) {
            throw new RestException(500,
                    new LdsError(LdsError.PARSE_ERR).setContext(" in File->" + getQueryName() + "; ERROR: " + error));
        }
        HashMap<String, String> litParams = getLitLangParams();
        if (converted == null) {
            converted = new HashMap<>();
        }
        ParameterizedSparqlString queryStr = new ParameterizedSparqlString(query,
                ServiceConfig.PREFIX.getPrefixMapping());
        for (final String opt : getOptionalParams()) {
            queryStr.setLiteral(opt+QueryConstants.BOUND_ARGS_PARAMSUFFIX, converted.get(opt) != null);
        }
        for (final String st : getAllParams()) {
            final boolean listMode = st.endsWith(QueryConstants.LIST_ARGS_PARAMSUFFIX);
            if (converted.get(st) == null) {
                if (listMode)
                    queryStr.setValues(st, nulllist);
                continue;
            }
            if (st.startsWith(QueryConstants.INT_ARGS_PARAMPREFIX)) {
                if (listMode) {
                    List<Literal> l = new ArrayList<>();
                    for (final String v : converted.get(st).split(",")) {
                        l.add(ResourceFactory.createTypedLiteral(v, XSDDatatype.XSDinteger));
                    }
                    queryStr.setValues(st, l);
                } else {
                    queryStr.setLiteral(st, converted.get(st), XSDDatatype.XSDinteger);
                }
                continue;
            }
            if (st.startsWith(QueryConstants.BOOLEAN_ARGS_PARAMPREFIX)) {
                if (listMode) {
                    List<Literal> l = new ArrayList<>();
                    for (final String v : converted.get(st).split(",")) {
                        l.add(ResourceFactory.createTypedLiteral(Boolean.parseBoolean(v)));
                    }
                    queryStr.setValues(st, l);
                } else {
                    queryStr.setLiteral(st, Boolean.parseBoolean(converted.get(st)));
                }
                continue;
            }
            if (st.startsWith(QueryConstants.DATE_ARGS_PARAMPREFIX)) {
                queryStr.setLiteral(st, converted.get(st), XSDDateTimeType.XSDdateTime);
                continue;
            }
            if (st.startsWith(QueryConstants.GY_ARGS_PARAMPREFIX)) {
                queryStr.setLiteral(st, converted.get(st), XSDDateTimeType.XSDgYear);
                continue;
            }
            if (st.startsWith(QueryConstants.RES_ARGS_PARAMPREFIX)) {
                if (listMode) {
                    List<Resource> l = new ArrayList<>();
                    for (final String v : converted.get(st).split(",")) {
                        l.add(ResourceFactory.createResource(qNameToURI(v)));
                    }
                    queryStr.setValues(st, l);
                } else {
                    queryStr.setIri(st, qNameToURI(converted.get(st)));
                }
                continue;
            }
            if (st.startsWith(QueryConstants.LITERAL_ARGS_PARAMPREFIX)) {
                String lim = converted.get(QueryConstants.LITERAL_LIMITPREFIX + st.substring(st.indexOf('_') + 1));
                if (lim == null) {
                    lim = ServiceConfig.getProperty("text_query_limit");
                }
                if (litParams.keySet().contains(st)) {
                    if (converted.get(litParams.get(st)) != null) {
                        String lang = converted.get(litParams.get(st)).toLowerCase();
                        try {
                            new Locale.Builder().setLanguageTag(lang).build();
                        } catch (IllformedLocaleException ex) {
                            return "ERROR --> language param :" + lang + " is not a valid BCP 47 language tag"
                                    + ex.getMessage();
                        }
                        if (limit) {
                            queryStr.setLiteral(st, converted.get(st), lang + " " + lim);
                        } else {
                            queryStr.setLiteral(st, converted.get(st), lang);
                        }
                    }
                } else {
                    if (converted.get(st) != null) {
                        // Some literals do not have a lang associated with them
                        if (limit) {
                            queryStr.setLiteral(st, converted.get(st));
                        } else {
                            queryStr.setLiteral(st, converted.get(st));
                        }
                    }
                }
            }
        }
        Query q;
        try {
            q = queryStr.asQuery();
        } catch (Exception e) {
            throw new RestException(500,
                    new LdsError(LdsError.PARSE_ERR).setContext(" in LdsQuery.getParametizedQuery() template ->"
                            + getQueryName() + ".arq" + "; ERROR: " + e.getMessage() + "; query : "+queryStr.toString()));
        }
        if (!queryData.get(QueryConstants.QUERY_RETURN_TYPE).equals(QueryConstants.GRAPH)) {
            if (q.hasLimit()) {
                if (q.getLimit() > limit_max) {
                    q.setLimit(limit_max);
                }
            } else {
                q.setLimit(limit_max);
            }
        }
        final String res = q.toString();
        if (res.startsWith(QueryConstants.QUERY_ERROR)) {
            throw new RestException(500, new LdsError(LdsError.PARSE_ERR).setContext(
                    " in LdsQuery.getParametizedQuery() template ->" + getQueryName() + ".arq" + "; ERROR: " + query));
        }
        return res;
    }

    public String checkQueryArgsSyntax(Map<String, String> map) {
        String check = "";
        System.out.println("PARAMS>> " + getRequiredParams() + " MAp >>" + map);
        for (String required : getRequiredParams()) {
            if (map.get(required) == null || "null".contentEquals(map.get(required))) {
                check = "The request mandatory parameter " + required + " is missing : the query cannot be built";
                return check;
            }
        }
        for (String arg : getAllParams()) {
            if (!arg.equals(QueryConstants.QUERY_NO_ARGS) && !arg.startsWith(QueryConstants.LITERAL_LIMITPREFIX)) {
                // Param is not a Lang param --> if it's not present in the
                // query --> Send error
                if (!arg.startsWith(QueryConstants.LITERAL_LG_ARGS_PARAMPREFIX) && query.indexOf("?" + arg) == -1) {
                    check = "Arg syntax is incorrect : query does not have a ?" + arg + " variable";
                    return check;
                }
                // Param is a Lang param --> check if the corresponding lit
                // param is present
                if (arg.startsWith(QueryConstants.LITERAL_LG_ARGS_PARAMPREFIX)) {
                    String expectedLiteralParam = QueryConstants.LITERAL_ARGS_PARAMPREFIX
                            + arg.substring(arg.indexOf("_") + 1);
                    if (!getAllParams().contains(expectedLiteralParam)) {
                        check = "Arg syntax is incorrect : query does not have a literal variable "
                                + expectedLiteralParam + " corresponding to lang " + arg + " variable";
                        return check;
                    }
                    litLangParams.put(expectedLiteralParam, arg);
                }
            }
        }
        return "";
    }

    public List<String> getRequiredParams() {
        if (requiredParams == null) {
            String s = queryData.get("QueryParams");
            if (s != null && !s.equalsIgnoreCase("NONE")) {
                requiredParams = (Arrays.asList(s.split(",")));
            } else {
                requiredParams = new ArrayList<>();
            }
        }
        return requiredParams;
    }

    private List<String> getOptionalParams() {
        if (optionalParams == null) {
            if (queryData.get("QueryOptParams") != null) {
                optionalParams = (Arrays.asList(queryData.get("QueryOptParams").split(",")));
            } else {
                optionalParams = new ArrayList<>();
            }
        }
        return optionalParams;
    }

    private ArrayList<String> getAllParams() {
        ArrayList<String> all = new ArrayList<>();
        for (String p : getRequiredParams()) {
            all.add(p);
        }
        for (String p : getOptionalParams()) {
            all.add(p);
        }
        return all;
    }

    public HashMap<String, String> getLitLangParams() {
        return litLangParams;
    }

    private ArrayList<Param> buildParams() throws RestException {
        ArrayList<Param> p = new ArrayList<>();
        ArrayList<String> all = getAllParams();
        for (String s : all) {
            // first get the type of the param
            String type = paramsData.get("param." + s + "." + QueryConstants.PARAM_TYPE);
            if (type != null) {
                String prefix = "param.";
                switch (type) {
                    case QueryConstants.STRING_PARAM :
                        StringParam stp = new StringParam(s);
                        stp.setLangTag(paramsData.get(prefix + s + "." + QueryConstants.PARAM_LANGTAG));
                        stp.setIsLuceneParam(paramsData.get(prefix + s + "." + QueryConstants.PARAM_LUCENE));
                        stp.setExample(paramsData.get(prefix + s + "." + QueryConstants.PARAM_EXAMPLE));
                        p.add(stp);
                        break;
                    case QueryConstants.INT_PARAM :
                        IntParam intp = new IntParam(s);
                        intp.setDescription(paramsData.get(prefix + s + "." + QueryConstants.PARAM_DESC));
                        p.add(intp);
                        break;
                    case QueryConstants.BOOLEAN_PARAM :
                        BooleanParam boolp = new BooleanParam(s);
                        boolp.setDescription(paramsData.get(prefix + s + "." + QueryConstants.PARAM_DESC));
                        p.add(boolp);
                        break;
                    case QueryConstants.RES_PARAM :
                        ResParam rtp = new ResParam(s, paramsData.get(prefix + s + "." + QueryConstants.PARAM_SUBTYPE));
                        rtp.setDescription(paramsData.get(prefix + s + "." + QueryConstants.PARAM_DESC));
                        p.add(rtp);
                        break;
                    case QueryConstants.RES_PARAM_URI :
                        ResParam rtp1 = new ResParam(s,
                                paramsData.get(prefix + s + "." + QueryConstants.PARAM_SUBTYPE));
                        rtp1.setDescription(paramsData.get(prefix + s + "." + QueryConstants.PARAM_DESC));
                        p.add(rtp1);
                        break;
                    case QueryConstants.DATETIME_PARAM :
                        DateTimeParam datetimep = new DateTimeParam(s);
                        datetimep.setDescription(paramsData.get(prefix + s + "." + QueryConstants.PARAM_DESC));
                        p.add(datetimep);
                        break;
                    case QueryConstants.GY_PARAM :
                        GYearParam gp = new GYearParam(s);
                        gp.setDescription(paramsData.get(prefix + s + "." + QueryConstants.PARAM_DESC));
                        p.add(gp);
                        break;
                    default :
                        log.error("unknown type: " + type);
                }
            }
        }
        return p;
    }

    private ArrayList<Output> buildOutputs() throws RestException {
        ArrayList<Output> o = new ArrayList<>();
        for (String s : outputsNames) {
            String prefix = "output.";
            Output output = new Output(s, outputsData.get(prefix + s + "." + QueryConstants.OUTPUT_TYPE),
                    outputsData.get(prefix + s + "." + QueryConstants.OUTPUT_DESC));
            o.add(output);
        }
        return o;
    }

    public String getQueryName() {
        return queryName;
    }

    public String getQuery() {
        return query;
    }

    public String getQueryHtml() {
        return queryHtml;
    }

    public HashMap<String, String> getParamsData() {
        return paramsData;
    }

    public HashMap<String, String> getOutputsData() {
        return outputsData;
    }

    public HashMap<String, String> getQueryData() {
        return queryData;
    }

    public QueryTemplate getTemplate() {
        return template;
    }

    public long getLimit_max() {
        return limit_max;
    }

    public static void main(String[] args) {
        try {
            ServiceConfig.init();
            HashMap<String, String> map = new HashMap<>();
            LdsQuery lds = new LdsQuery("src/test/resources/arq/public/optional_complex.arq");
            // LdsQueryNew lds = new
            // LdsQueryNew("lds-queries/public/Etexts_count.arq");
            // LdsQuery lds = new LdsQuery("lds-queries/public/Etext_base.arq");
            // map.put("R_RES", "bdr:UT4CZ5369_I1KG9127_0000");
            // map.put("L_LNG", "en");
            map.put("R_COLLECTION", "bdr:PR1");
            map.put("B_RIC", "true");
            map.put("B_COMPLETE", "false");
            map.put("R_LANG_LIST", "bdr:l1,bdr:l2");
            // map.put("D_TIME1", "2012-01-31 23:59:59");
            // map.put("L_l", "\"མིག་གི་ཡུལ\"");
            // map.put("LG_l", "bo");
            // map.put("R_g", "http://purl.bdrc.io/resource/GenderMale");
            // LdsQuery lds = new
            // LdsQuery("lds-queries/public/Work_ImgList.arq");
            // map.put("R_RES", "bdr:W29329");
            System.out.println("Required Params >>" + lds.getRequiredParams());
            System.out.println("Optional Params >>" + lds.getOptionalParams());
            System.out.println("CHECK >>" + lds.checkQueryArgsSyntax(map));
            System.out.println("Query >>" + lds.getQuery());
            System.out.println("Parametized Query >>" + lds.getParametizedQuery(map, false));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
