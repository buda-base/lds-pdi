package io.bdrc.ldspdi.rest.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.validator.routines.ISBNValidator;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Leader;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import io.bdrc.formatters.TTLRDFWriter;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

public class MarcExport {

    public static final MarcFactory factory = MarcFactory.newInstance();

    public static final String BDO = "http://purl.bdrc.io/ontology/core/";
    public static final String ADM = "http://purl.bdrc.io/ontology/admin/";
    public static final String TMP = "http://purl.bdrc.io/ontology/tmp/";
    public static final Property partOf = ResourceFactory.createProperty(BDO+"workPartOf");
    public static final Property hasExpression = ResourceFactory.createProperty(BDO+"workHasExpression");
    public static final Property workIsbn = ResourceFactory.createProperty(BDO+"workIsbn");
    public static final Property workLccn = ResourceFactory.createProperty(BDO+"workLccn");
    public static final Property admAccess = ResourceFactory.createProperty(ADM+"access");
    public static final Property admLicense = ResourceFactory.createProperty(ADM+"license");
    public static final Property tmpPublishedYear = ResourceFactory.createProperty(TMP+"publishedYear");
    //public static final Property tmpCompletedYear = ResourceFactory.createProperty(TMP+"completedYear");
    public static final Property tmpBirthYear = ResourceFactory.createProperty(TMP+"birthYear");
    public static final Property tmpDeathYear = ResourceFactory.createProperty(TMP+"deathYear");
    public static final Property tmpLang = ResourceFactory.createProperty(TMP+"workLanguage");
    public static final Property publisherLocation = ResourceFactory.createProperty(BDO+"publisherLocation");

    // communicated by Columbia, XML leaders don't need addresses
    public static final String baseLeaderStr = "     nam a22    3ia 4500";
    static final Leader leader = factory.newLeader(baseLeaderStr);
    static final ISBNValidator isbnvalidator = ISBNValidator.getInstance(false);
    final static DateTimeFormatter yymmdd = DateTimeFormatter.ofPattern("yyMMdd");

    // initialize static fields:
    static final ControlField f006 = factory.newControlField("006", "m");
    static final ControlField f007 = factory.newControlField("007", "cr");
    static final DataField f040 = factory.newDataField("040", ' ', ' ');
    static final DataField f336 = factory.newDataField("336", ' ', ' ');
    static final DataField f337 = factory.newDataField("337", ' ', ' ');
    static final DataField f338 = factory.newDataField("338", ' ', ' ');
    static final DataField f533 = factory.newDataField("533", ' ', ' ');
    static final DataField f710_2 = factory.newDataField("710", '2', ' ');

    static final DataField f506_restricted = factory.newDataField("506", '1', ' ');
    static final DataField f506_open = factory.newDataField("506", '0', ' ');
    static final DataField f506_fairUse = factory.newDataField("506", '1', ' ');

    static final DataField f542_PD = factory.newDataField("542", '1', ' ');

    static final Map<String,String> localNameToMarcLang = new HashMap<>();

    static final String defaultCountryCode = "   ";
    static final String defaultLang = "und";

    static {
        localNameToMarcLang.put("LangBo", "tib");
        localNameToMarcLang.put("LangSa", "san");
        localNameToMarcLang.put("LangRu", "rus");
        localNameToMarcLang.put("LangZh", "chi");
        localNameToMarcLang.put("LangPi", "pli");
        localNameToMarcLang.put("LangNew", "new");
        localNameToMarcLang.put("LangHi", "hin");
        localNameToMarcLang.put("LangMn", "mon");
        localNameToMarcLang.put("LangEn", "eng");
        localNameToMarcLang.put("LangDz", "dzo");
        f040.addSubfield(factory.newSubfield('a', "NNC"));
        f040.addSubfield(factory.newSubfield('b', "eng"));
        f040.addSubfield(factory.newSubfield('e', "rda"));
        f040.addSubfield(factory.newSubfield('e', "NNC"));
        f336.addSubfield(factory.newSubfield('a', "text"));
        f336.addSubfield(factory.newSubfield('b', "txt"));
        f336.addSubfield(factory.newSubfield('2', "rdacontent"));
        f337.addSubfield(factory.newSubfield('a', "computer"));
        f337.addSubfield(factory.newSubfield('b', "c"));
        f337.addSubfield(factory.newSubfield('2', "rdamedia"));
        f338.addSubfield(factory.newSubfield('a', "online resource"));
        f338.addSubfield(factory.newSubfield('b', "er"));
        f338.addSubfield(factory.newSubfield('2', "rdacarrier"));
        f533.addSubfield(factory.newSubfield('a', "Electronic reproduction"));
        f533.addSubfield(factory.newSubfield('b', "Cambridge, Mass. :"));
        f533.addSubfield(factory.newSubfield('c', "Buddhist Digital Resource Center"));
        f710_2.addSubfield(factory.newSubfield('a', "Buddhist Digital Resource Center"));
        // see https://www.oclc.org/content/dam/oclc/digitalregistry/506F_vocabulary.pdf
        f506_restricted.addSubfield(factory.newSubfield('f', "No online access"));
        f506_restricted.addSubfield(factory.newSubfield('2', "star"));
        //f506_open.addSubfield(factory.newSubfield('u', "http://creativecommons.org/licenses/publicdomain"));
        f506_open.addSubfield(factory.newSubfield('2', "star"));
        f506_open.addSubfield(factory.newSubfield('f', "Unrestricted online access"));
        f506_fairUse.addSubfield(factory.newSubfield('2', "star"));
        f506_fairUse.addSubfield(factory.newSubfield('f', "Preview only"));
        f542_PD.addSubfield(factory.newSubfield('u', "http://creativecommons.org/licenses/publicdomain"));
        f542_PD.addSubfield(factory.newSubfield('l', "Public Domain"));
    }

    public static boolean indent = true;

    public static String getAuthorStr(final Model m) {
        return null;
    }

    public static final Map<String,String> pubLocToCC = getPubLoctoCC();

    public static Map<String,String> getPubLoctoCC() {
        final Map<String,String> res = new HashMap<>();
        final CSVReader reader;
        final CSVParser parser = new CSVParserBuilder().build();
        final ClassLoader classLoader = MarcExport.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("publisherLocationToMARCCountryCode.csv");
        final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        reader = new CSVReaderBuilder(in)
                .withCSVParser(parser)
                .build();
        String[] line = null;
        while (line != null) {
            if (line.length > 1) {
                if (line[1].length() < 3)
                    res.put(line[0], " "+line[1]);
                else
                    res.put(line[0], line[1]);
            }
            try {
                line = reader.readNext();
            } catch (IOException e) {
                e.printStackTrace();
                return res;
            }
        }
        return res;
    }

    public static void addIsbn(final Model m, final Resource main, final Record r) {
        final StmtIterator si = main.listProperties(workIsbn);
        while (si.hasNext()) {
            final Statement s = si.next();
            final String isbn = s.getLiteral().getString();
            final String validIsbn = isbnvalidator.validate(isbn);
            final DataField df = factory.newDataField("020", ' ', ' ');
            if (validIsbn != null) {
                df.addSubfield(factory.newSubfield('a', validIsbn));
            } else {
                df.addSubfield(factory.newSubfield('z', isbn));
            }
            r.addVariableField(df);
        }
    }

    public static void addAccess(final Model m, final Resource main, final Record r) {
        final Resource access = main.getPropertyResourceValue(admAccess);
        if (access == null) {
            return; // maybe there should be a f506_unknown?
        } else {
            switch (access.getLocalName()) {
            case "AccessOpen":
                r.addVariableField(f506_open);
                break;
            case "AccessFairUse":
                r.addVariableField(f506_fairUse);
                break;
            default:
                r.addVariableField(f506_restricted);
                break;
            }
            // TODO: what about restriction in China?
        }
        final Resource license = main.getPropertyResourceValue(admLicense);
        if (license == null) {
            return; // maybe there should be a f506_unknown?
        } else {
            switch (license.getLocalName()) {
            case "LicensePublicDomain":
                r.addVariableField(f542_PD);
                break;
            default:
                break;
            }
        }
    }

    // tmp, for debug
    public static void printModel(final Model m) {
        TTLRDFWriter.getSTTLRDFWriter(m).output(System.out);

    }

    public static void add008(final Model m, final Resource main, final Record r) {
        //printModel(m);
        final StringBuilder sb = new StringBuilder();
        final LocalDate localDate = LocalDate.now();
        sb.append(localDate.format(yymmdd));
        final Statement publishedYearS = main.getProperty(tmpPublishedYear);
        if (publishedYearS == null) {
            sb.append("b    ");
        } else {
            final int publishedYear = publishedYearS.getInt();
            if (publishedYear > 9999 || publishedYear < 0) {
                sb.append("b    ");
            } else {
                final String date = String.format("%04d", publishedYear);
                sb.append('s');
                sb.append(date);
            }
        }
        sb.append("    ");
        final Statement publisherLocationS = main.getProperty(publisherLocation);
        if (publisherLocationS == null) {
            sb.append(defaultCountryCode);
        } else {
            final String pubLocStr = publisherLocationS.getObject().asLiteral().getString();
            final String marcCC = pubLocToCC.getOrDefault(pubLocStr, defaultCountryCode);
            sb.append(marcCC);
        }
        sb.append("     o           ");
        final Statement languageS = main.getProperty(tmpLang);
        if (languageS == null) {
            sb.append(defaultLang);
        } else {
            sb.append(languageS.getString());
        }
        sb.append("od");
        r.addVariableField(factory.newControlField("008", sb.toString()));
    }

    public static Record marcFromModel(final Model m, final Resource main) {
        final Record record = factory.newRecord(leader);
        // these are static and supplied by Columbia
        record.addVariableField(f006);
        record.addVariableField(f007);
        record.addVariableField(f040);
        record.addVariableField(f336);
        record.addVariableField(f337);
        record.addVariableField(f338);
        record.addVariableField(f533);
        record.addVariableField(f710_2);
        // maybe something like that could work?
        //record.addVariableField(factory.newControlField("003", "BDRC"));
        // these depend on the model
        final DataField f588 = factory.newDataField("588", ' ', ' ');
        // TODO: replace [date]
        f588.addSubfield(factory.newSubfield('a', "Description based on online resource viewed on [date]; title from title page."));
        record.addVariableField(f588);
        record.addVariableField(factory.newControlField("001", "(BDRC)"+main.getURI()));
        final DataField f856 = factory.newDataField("856", '4', '0');
        f856.addSubfield(factory.newSubfield('u', main.getURI()));
        f856.addSubfield(factory.newSubfield('z', "Available from BDRC"));
        record.addVariableField(f856);
        add008(m, main, record);
        // lccn
        final StmtIterator si = main.listProperties(workLccn);
        while (si.hasNext()) {
            final String lccn = si.next().getLiteral().getString();
            final DataField f776_08 = factory.newDataField("776", '0', '8');
            f776_08.addSubfield(factory.newSubfield('w', "(DLC)   [LCCN] "+lccn));
            f776_08.addSubfield(factory.newSubfield('i', "Electronic reproduction of (manifestation)"));
            record.addVariableField(f776_08);
        }
        addIsbn(m, main, record);
        addAccess(m, main, record);
        return record;
    }

    public static Model getMarcModel(final String resUri) throws RestException {
        Model model = QueryProcessor.getSimpleResourceGraph(resUri, "resForMarc.arq");
        if (model.size() < 1) {
            throw new RestException(404,new LdsError(LdsError.NO_GRAPH_ERR).setContext(resUri));
        }
        final Resource main = model.getResource(resUri);
        final Resource type = main.getPropertyResourceValue(RDF.type);
        if (!type.getLocalName().equals("Work")) {
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setMsg("Resource is not a Work"));
        }
        if (main.hasProperty(partOf)) {
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setMsg("Resource is part of another Work"));
        }
        if (main.hasProperty(hasExpression)) {
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setMsg("Resource is an abstract Work"));
        }
        return model;
    }

    public static Response getResponse(final MediaType mt, final String resUri) throws RestException {
        final Model m = getMarcModel(resUri);
        final Resource main = m.getResource(resUri);
        final Record r = marcFromModel(m, main);
        final StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(final OutputStream os) throws IOException, WebApplicationException {
                final MarcXmlWriter writer = new MarcXmlWriter(os, indent);
                writer.write(r);
                writer.close();
            }
        };
        final ResponseBuilder builder = Response.ok(stream);
        builder.header("Allow", "GET, OPTIONS, HEAD");
        builder.header("Content-Type", mt);
        builder.header("Vary", "Negotiate, Accept");
        return builder.build();
    }
}
