package io.bdrc.ldspdi.export;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.ISBNValidator;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.marc4j.MarcException;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Leader;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import io.bdrc.ewtsconverter.EwtsConverter;
import io.bdrc.ewtsconverter.TransConverter;
import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.libraries.BudaMediaTypes;
import io.bdrc.libraries.formatters.TTLRDFWriter;

/*
 * Here's some knowledge that can be useful when dealing with libraries:
 *
 * There's a bug in the MARC Perl library that triggers warnings such as:
 *
 * utf8 "\xCA" does not map to Unicode at /usr/lib/x86_64-linux-gnu/perl/5.26/Encode.pm line 212
 *
 * This is due to https://rt.cpan.org/Public/Bug/Display.html?id=32332 (patch included!) and is not a bug in the records.
 *
 * Also, when serializing to .mrc, some library systems will only accept strict sizes:
 *  - length of a field must be at most 9999 bytes
 *  - length of a record must be at most 99999 bytes
 *
 * This means splitting (or just removing) the 505 field, which is the easiest leverage.
 * 
 * Possible additions:
 * https://www.loc.gov/marc/authority/ad375.html ?
 * https://www.loc.gov/marc/bibliographic/ecbdcntf.html indicating the ISO script in subfield 6
 * https://www.loc.gov/marc/mac/2019/2019-02.html
 * 
 */

public class MarcExport {

    public static final MarcFactory factory = MarcFactory.newInstance();

    public final static Logger log = LoggerFactory.getLogger(MarcExport.class);

    public static final String BDO = "http://purl.bdrc.io/ontology/core/";
    public static final String BDR = "http://purl.bdrc.io/resource/";
    public static final String ADM = "http://purl.bdrc.io/ontology/admin/";
    public static final String TMP = "http://purl.bdrc.io/ontology/tmp/";
    public static final String BF = "http://id.loc.gov/ontologies/bibframe/";
    public static final Property partOf = ResourceFactory.createProperty(BDO + "partOf");
    public static final Property hasPart = ResourceFactory.createProperty(BDO + "hasPart");
    public static final Property partTreeIndex = ResourceFactory.createProperty(BDO + "partTreeIndex");
    public static final Property hasInstance = ResourceFactory.createProperty(BDO + "workHasInstance");
    public static final Property editionStatement = ResourceFactory.createProperty(BDO + "editionStatement");
    public static final Property extentStatement = ResourceFactory.createProperty(BDO + "extentStatement");
    public static final Property publisherName = ResourceFactory.createProperty(BDO + "publisherName");
    public static final Property publisherLocation = ResourceFactory.createProperty(BDO + "publisherLocation");
    public static final Property authorshipStatement = ResourceFactory.createProperty(BDO + "authorshipStatement");
    public static final Property catalogInfo = ResourceFactory.createProperty(BDO + "catalogInfo");
    public static final Property workBiblioNote = ResourceFactory.createProperty(BDO + "biblioNote");
    public static final Property creator = ResourceFactory.createProperty(BDO + "creator");
    public static final Property role = ResourceFactory.createProperty(BDO + "role");
    public static final Property agent = ResourceFactory.createProperty(BDO + "agent");
    public static final Property instanceEvent = ResourceFactory.createProperty(BDO + "instanceEvent");
    public static final Property workEvent = ResourceFactory.createProperty(BDO + "workEvent");
    public static final Property hasTitle = ResourceFactory.createProperty(BDO + "hasTitle");
    public static final Property workIsAbout = ResourceFactory.createProperty(BDO + "workIsAbout");
    public static final Property workGenre = ResourceFactory.createProperty(BDO + "workGenre");
    public static final Property serialInstanceOf = ResourceFactory.createProperty(BDO + "serialInstanceOf");
    public static final Property seriesNumber = ResourceFactory.createProperty(BDO + "seriesNumber");
    public static final Property personEvent = ResourceFactory.createProperty(BDO + "personEvent");
    public static final Property personName = ResourceFactory.createProperty(BDO + "personName");
    public static final Property language = ResourceFactory.createProperty(BDO + "language");
    public static final Property script = ResourceFactory.createProperty(BDO + "script");
    public static final Property onYear = ResourceFactory.createProperty(BDO + "onYear");
    public static final Property notBefore = ResourceFactory.createProperty(BDO + "notBefore");
    public static final Property notAfter = ResourceFactory.createProperty(BDO + "notAfter");
    public static final Property workLcCallNumber = ResourceFactory.createProperty(BDO + "workLcCallNumber");
    public static final Property tmpAccess = ResourceFactory.createProperty(TMP + "access");
    public static final Property tmpLicense = ResourceFactory.createProperty(TMP + "license");
    public static final Property tmpStatus = ResourceFactory.createProperty(TMP + "status");
    public static final Property tmpPublishedYear = ResourceFactory.createProperty(TMP + "publishedYear");
    public static final Property langBCP47Lang = ResourceFactory.createProperty(BDO + "langBCP47Lang");
    public static final Property langMARCCode = ResourceFactory.createProperty(BDO + "langMARCCode");
    public static final Property restrictedInChina = ResourceFactory.createProperty(TMP + "restrictedInChina");
    
    public static final class MarcInfo {
        public final Integer prio;
        public final Character subindex2;
        public final String subfieldi;

        public MarcInfo(final Integer prio, final Character subindex2, final String subfieldi) {
            this.prio = prio;
            this.subindex2 = subindex2;
            this.subfieldi = subfieldi;
        }
    }

    public static final Map<String, MarcInfo> titleLocalNameToMarcInfo = new HashMap<>();

    public static final EwtsConverter ewtsConverter = new EwtsConverter();

    // communicated by Columbia, XML leaders don't need addresses
    public static final String baseLeaderStr = "     nam a22000003ia4500";
    static final Leader leader = factory.newLeader(baseLeaderStr);
    static final ISBNValidator isbnvalidator = ISBNValidator.getInstance(false);
    final static DateTimeFormatter yymmdd = DateTimeFormatter.ofPattern("yyMMdd");
    final static DateTimeFormatter f005_f = DateTimeFormatter.ofPattern("yyyyMMddHHmmss.S");

    // initialize static fields:
    static final ControlField f006 = factory.newControlField("006", "m     o  d        ");
    static final ControlField f007 = factory.newControlField("007", "cr |||||||||||");
    static final DataField f040 = factory.newDataField("040", ' ', ' ');
    static final DataField f336 = factory.newDataField("336", ' ', ' ');
    static final DataField f337 = factory.newDataField("337", ' ', ' ');
    static final DataField f338 = factory.newDataField("338", ' ', ' ');
    static final DataField f533 = factory.newDataField("533", ' ', ' ');
    static final DataField f710_2 = factory.newDataField("710", '2', ' ');
    // this is the identifier used by Harvard for BDRC, it hasn't been used by Columbia
    // see https://github.com/buda-base/lds-pdi/issues/227
    static final ControlField f003 = factory.newControlField("003", "MaCbBDRC");

    static final DataField f506_restricted = factory.newDataField("506", '1', ' ');
    static final DataField f506_open = factory.newDataField("506", '0', ' ');
    static final DataField f506_fairUse = factory.newDataField("506", '1', ' ');
    static final DataField f506_open_ric = factory.newDataField("506", '1', ' ');
    static final DataField f506_fairUse_ric = factory.newDataField("506", '1', ' ');

    static final DataField f542_PD = factory.newDataField("542", '1', ' ');

    static final String defaultCountryCode = "xx ";
    static final String defaultLang = "und";

    // for 588 field
    static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);

    static {
        // from Columbia:
        // If you use first indicator 1 and subfield $i, then there is no second
        // indicator. It needs to be blank.
        titleLocalNameToMarcInfo.put("TitlePageTitle", new MarcInfo(0, '5', null));
        titleLocalNameToMarcInfo.put("BibliographicalTitle", new MarcInfo(1, ' ', "BDRC Bibliographical title:"));
        titleLocalNameToMarcInfo.put("CoverTitle", new MarcInfo(2, '4', null));
        titleLocalNameToMarcInfo.put("FullTitle", new MarcInfo(3, '3', null));
        titleLocalNameToMarcInfo.put("HalfTitle", new MarcInfo(4, '3', null)); // or 3?
        titleLocalNameToMarcInfo.put("ColophonTitle", new MarcInfo(5, ' ', "Colophon title:"));
        titleLocalNameToMarcInfo.put("CopyrightPageTitle", new MarcInfo(6, '3', null));
        titleLocalNameToMarcInfo.put("DkarChagTitle", new MarcInfo(7, ' ', "Table of contents title:"));
        titleLocalNameToMarcInfo.put("OtherTitle", new MarcInfo(8, '3', null));
        titleLocalNameToMarcInfo.put("RunningTitle", new MarcInfo(9, '7', null));
        titleLocalNameToMarcInfo.put("SpineTitle", new MarcInfo(10, '8', null));
        titleLocalNameToMarcInfo.put("TitlePortion", new MarcInfo(11, '0', null));
        titleLocalNameToMarcInfo.put("Title", new MarcInfo(99, '3', null));
        f040.addSubfield(factory.newSubfield('a', "NNC"));
        f040.addSubfield(factory.newSubfield('b', "eng"));
        // Columbia doesn't want RDA here
        // f040.addSubfield(factory.newSubfield('e', "rda"));
        f040.addSubfield(factory.newSubfield('c', "NNC"));
        f336.addSubfield(factory.newSubfield('a', "text"));
        f336.addSubfield(factory.newSubfield('b', "txt"));
        f336.addSubfield(factory.newSubfield('2', "rdacontent"));
        f337.addSubfield(factory.newSubfield('a', "computer"));
        f337.addSubfield(factory.newSubfield('b', "c"));
        f337.addSubfield(factory.newSubfield('2', "rdamedia"));
        f338.addSubfield(factory.newSubfield('a', "online resource"));
        f338.addSubfield(factory.newSubfield('b', "cr"));
        f338.addSubfield(factory.newSubfield('2', "rdacarrier"));
        f533.addSubfield(factory.newSubfield('a', "Electronic reproduction."));
        f533.addSubfield(factory.newSubfield('b', "Cambridge, Mass. :"));
        f533.addSubfield(factory.newSubfield('c', "Buddhist Digital Resource Center."));
        f533.addSubfield(factory.newSubfield('n', "Available via World Wide Web."));
        f710_2.addSubfield(factory.newSubfield('a', "Buddhist Digital Resource Center."));
        // see https://www.oclc.org/content/dam/oclc/digitalregistry/506F_vocabulary.pdf
        f506_restricted.addSubfield(factory.newSubfield('a', "Access restricted."));
        f506_restricted.addSubfield(factory.newSubfield('f', "No online access"));
        f506_restricted.addSubfield(factory.newSubfield('2', "star."));
        f506_open.addSubfield(factory.newSubfield('a', "Open Access."));
        f506_open.addSubfield(factory.newSubfield('f', "Unrestricted online access"));
        f506_open.addSubfield(factory.newSubfield('2', "star"));
        f506_fairUse.addSubfield(factory.newSubfield('a', "Access restricted to a few sample pages."));
        f506_fairUse.addSubfield(factory.newSubfield('f', "Preview only"));
        f506_fairUse.addSubfield(factory.newSubfield('2', "star"));
        f506_open_ric.addSubfield(factory.newSubfield('a', "Access restricted in some countries."));
        f506_fairUse_ric.addSubfield(factory.newSubfield('a', "Access restricted to a few sample pages, access restricted in some countries."));
        f542_PD.addSubfield(factory.newSubfield('l', "Public domain"));
        f542_PD.addSubfield(factory.newSubfield('u', "http://creativecommons.org/publicdomain/mark/1.0/"));
    }

    public static boolean indent = true;

    public static final Map<String, String> pubLocToCC = getPubLoctoCC();

    public static String getLangStr(final Literal l) {
        final String lang = l.getLanguage();
        if (lang == null || !"bo-x-ewts".equals(lang)) {
            return getLangStrNoConv(l);
        }
        String alalc = TransConverter.ewtsToAlalc(l.getString(), true);
        alalc = alalc.replace("u0fbe", "x");
        return StringUtils.capitalize(alalc.replace('-', ' ').trim());
    }

    public static String getLangStrNoConv(final Literal l) {
        String st = l.getString();
        if (st.startsWith("[")) {
            st = "[" + StringUtils.capitalize(st.substring(1));
        } else {
            st = StringUtils.capitalize(st);
        }
        return st.replaceAll("[/|;]", "").trim();
    }

    public static Map<String, String> getPubLoctoCC() {
        final Map<String, String> res = new HashMap<>();
        final CSVReader reader;
        final CSVParser parser = new CSVParserBuilder().build();
        final ClassLoader classLoader = MarcExport.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("publisherLocationToMARCCountryCode.csv");
        final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        reader = new CSVReaderBuilder(in).withCSVParser(parser).build();
        try {
            String[] line = reader.readNext();
            ;
            while (line != null) {
                if (line.length > 1) {
                    if (line[1].length() < 3)
                        res.put(line[0], line[1] + " ");
                    else
                        res.put(line[0], line[1]);
                }
                line = reader.readNext();
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
            return res;
        }
        return res;
    }
    
    public static List<String> getId(final Model m, final Resource main, final Resource type) {
        final ResIterator si = m.listResourcesWithProperty(RDF.type, type);
        final List<String> res = new ArrayList<>();
        while (si.hasNext()) {
            final Resource id = si.next();
            final Statement value = id.getProperty(RDF.value);
            if (value == null) continue;
            res.add(value.getString());
        }
        return res;
    }

    // possible types: bdr:HollisId, bf:Isbn, bf:Lccn, bf:ShelfMarkLcc 
    public static void addIsbn(final Model m, final Resource main, final Record r, final boolean itemMode) {
        final List<String> isbnList = getId(m, main, m.createResource(BF+"Isbn"));
        for (final String isbn : isbnList) {
            final String validIsbn = isbnvalidator.validate(isbn);
            if (!itemMode) {
                final DataField df = factory.newDataField("020", ' ', ' ');
                if (validIsbn != null) {
                    df.addSubfield(factory.newSubfield('a', validIsbn));
                } else {
                    df.addSubfield(factory.newSubfield('z', isbn));
                }
                r.addVariableField(df);
            } else {
                // changed by Harvard, Columbia made us put the ISBN in 020$z in that case
                final DataField df = factory.newDataField("760", '0', ' ');
                df.addSubfield(factory.newSubfield('c', "Original"));
                df.addSubfield(factory.newSubfield('z', isbn));
                r.addVariableField(df);
            }
        }
    }

    public static void addAccess(final Model m, final Resource main, final Record r) {
        final Resource access = main.getPropertyResourceValue(tmpAccess);
        if (access == null) {
            return; // maybe there should be a f506_unknown?
        }
        boolean ric = false;
        final Statement ricS = main.getProperty(restrictedInChina);
        if (ricS != null) {
            ric = ricS.getBoolean();
        }
        switch (access.getLocalName()) {
        case "AccessOpen":
            r.addVariableField(ric ? f506_open_ric : f506_open);
            break;
        case "AccessFairUse":
            r.addVariableField(ric ? f506_fairUse_ric : f506_fairUse);
            break;
        default:
            r.addVariableField(f506_restricted);
            break;
        }
    }

    public static void addPubInfo(final Model m, final Resource main, final Record r) {
        final DataField f264 = factory.newDataField("264", ' ', '1');
        StmtIterator si = main.listProperties(publisherLocation);
        boolean hasPubLocation = false;
        while (si.hasNext()) {
            final Literal pubLocation = si.next().getLiteral();
            if (pubLocation.getString().contains("s.")) {
                continue;
            }
            f264.addSubfield(factory.newSubfield('a', getLangStr(pubLocation) + " :"));
            hasPubLocation = true;
            break;
        }
        if (!hasPubLocation) {
            f264.addSubfield(factory.newSubfield('a', "[Place of publication not identified] :"));
        }
        si = main.listProperties(publisherName);
        boolean hasPubName = false;
        while (si.hasNext()) {
            final Literal pubName = si.next().getLiteral();
            if (pubName.getString().contains("s.")) {
                continue;
            }
            f264.addSubfield(factory.newSubfield('b', getLangStr(pubName) + ","));
            hasPubName = true;
        }
        if (!hasPubName) {
            f264.addSubfield(factory.newSubfield('b', "[publisher not identified],"));
        }
        final Statement publishedYearS = main.getProperty(tmpPublishedYear);
        if (publishedYearS == null) {
            f264.addSubfield(factory.newSubfield('c', "[date of publication not identified]"));
        } else {
            final String publishedYearStr = publishedYearS.getLiteral().getLexicalForm();
            try {
                int publishedYear = Integer.parseInt(publishedYearStr);
                if (publishedYear > 9999 || publishedYear < 0) {
                    f264.addSubfield(factory.newSubfield('c', "[date of publication not identified]"));
                } else {
                    f264.addSubfield(factory.newSubfield('c', String.valueOf(publishedYear) + "."));
                } 
            } catch (NumberFormatException e) {  }
        }
        r.addVariableField(f264);
    }

    public static void add008(final Model m, final Resource main, final Record r, final String marcLang, final LocalDateTime now) {
        final StringBuilder sb = new StringBuilder();
        sb.append(now.format(yymmdd));
        final Statement publishedYearS = main.getProperty(tmpPublishedYear);
        if (publishedYearS == null) {
            sb.append("nuuuuuuuu");
        } else {
            final String publishedYearStr = publishedYearS.getLiteral().getLexicalForm();
            try {
                int publishedYear = Integer.parseInt(publishedYearStr);
                if (publishedYear > 9999 || publishedYear < 0) {
                    sb.append("nuuuuuuuu");
                } else {
                    final String date = String.format("%04d", publishedYear);
                    sb.append('s');
                    sb.append(date);
                    sb.append("    ");
                } 
            } catch (NumberFormatException e) {  }
        }
        final Statement publisherLocationS = main.getProperty(publisherLocation);
        if (publisherLocationS == null) {
            sb.append(defaultCountryCode);
        } else {
            String pubLocStr = publisherLocationS.getObject().asLiteral().getString().toLowerCase().trim();
            final String marcCC = pubLocToCC.getOrDefault(pubLocStr, defaultCountryCode);
            sb.append(marcCC);
        }
        sb.append("|||||||||| 000 ||");
        if (marcLang == null) {
            sb.append(defaultLang);
        } else {
            sb.append(marcLang);
        }
        sb.append("od");
        r.addVariableField(factory.newControlField("008", sb.toString()));
    }

    public static void add300(final Model m, final Resource main, final Record r) {
        final Statement extentStatementS = main.getProperty(extentStatement);
        if (extentStatementS == null) {
            return;
        }
        String st = extentStatementS.getString();
        st = st.replace("pp.", "pages");
        st = st.replace("p.", "pages");
        st = st.replace("ff.", "folios");
        // the s in volume(s) is a bit annoying
        st = st.replaceAll("(\\d,) +(\\d)", "$1$2");
        st = st.replaceAll("^1 ?v\\.", "1 volume");
        st = st.replaceAll("[^0-9]1 ?v\\.", "1 volume");
        st = st.replaceAll(" ?v\\.", " volumes");
        final DataField f300 = factory.newDataField("300", ' ', ' ');
        f300.addSubfield(factory.newSubfield('a', "1 online resource (" + st + ")"));
        r.addVariableField(f300);
    }

    public static void addCreatorName(final Model m, final Resource name, final List<Literal> nameList) {
        final Literal l = name.getProperty(RDFS.label).getLiteral();
        nameList.add(l);
    }

    public static final Map<String, String> roleToName = new HashMap<>();
    static {
        roleToName.put("R0ER0025", "author."); // terton
        roleToName.put("R0ER0019", "author."); // creatorMainAuthor
        roleToName.put("R0ER0020", "translator."); // creatorTranslator
        roleToName.put("R0ER0018", "consultant."); // creatorIndicScholar
        roleToName.put("R0ER0016", "contributor."); // creatorContributingAuthor
        roleToName.put("R0ER0014", "commentator for written text."); // creatorCommentator
        roleToName.put("R0ER0014", "editor."); // creatorCompiler
        roleToName.put("R0ER0023", "corrector."); // creatorReviser
        roleToName.put("R0ER0024", "scribe."); // creatorScribe
        roleToName.put("R0ER0013", "calligrapher."); // creatorCalligrapher
        roleToName.put("R0ER0010", "artist."); // creatorArtist
    }

    public static void addAuthors(final Model m, final Resource main, final Record r, final Index880 i880, final List<DataField> list880, final List<DataField> list720) {
        StmtIterator si = main.listProperties(creator);
        while (si.hasNext()) {
            final Resource agentAsRole = si.next().getResource();
            final Resource roleR = agentAsRole.getPropertyResourceValue(role);
            final Resource agentR = agentAsRole.getPropertyResourceValue(agent);
            if (roleR == null || agentR == null) {
                return;
            }
            String rel = roleToName.get(roleR.getLocalName());
            if (rel == null) {
                rel = "author.";
            }
            // here we want to keep an order among the various names and titles
            // otherwise the output could be inconsistent among queries
            final List<Literal> names = new ArrayList<>();
            final List<Literal> otherNames = new ArrayList<>();
            final List<Literal> titles = new ArrayList<>();
            final List<Literal> otherTitles = new ArrayList<>();
            StmtIterator nsi = agentR.listProperties(personName);
            while (nsi.hasNext()) {
                final Resource name = nsi.next().getResource();
                final Resource type = name.getPropertyResourceValue(RDF.type);
                final String typeLocalName = (type == null) ? "" : type.getLocalName();
                switch (typeLocalName) {
                case "PersonPrimaryName":
                    addCreatorName(m, name, names);
                    break;
                case "PersonPrimaryTitle":
                    addCreatorName(m, name, titles);
                    break;
                case "PersonTitle":
                    addCreatorName(m, name, otherTitles);
                    break;
                default:
                    addCreatorName(m, name, otherNames);
                    break;
                }
            }
            Integer birthYear = null;
            Integer deathYear = null;
            si = agentR.listProperties(personEvent);
            while (si.hasNext()) {
                final Resource event = si.next().getResource();
                final Resource eventType = event.getPropertyResourceValue(RDF.type);
                if (eventType != null && eventType.getLocalName().equals("PersonBirth")) {
                    if (event.hasProperty(onYear)) {
                        final String birthYearStr = event.getProperty(onYear).getLiteral().getLexicalForm();
                        try {
                            birthYear = Integer.parseInt(birthYearStr);
                        } catch (NumberFormatException e) {  }
                    }
                }
                if (eventType != null && eventType.getLocalName().equals("PersonDeath")) {
                    if (event.hasProperty(onYear)) {
                        final String deathYearStr = event.getProperty(onYear).getLiteral().getLexicalForm();
                        try {
                            deathYear = Integer.parseInt(deathYearStr);
                        } catch (NumberFormatException e) {  }
                    }
                }
            }
            final StringBuilder sb = new StringBuilder();
            final StringBuilder sb880 = new StringBuilder();
            boolean has880 = false;
            if (!names.isEmpty()) {
                Collections.sort(names, baseComp);
                sb.append(getLangStr(names.get(0)));
                final String name880 = get880String(names.get(0));
                if (name880 != null) {
                    sb880.append(name880);
                    i880.addScript("Tibt");
                    has880 = true;
                }
            } else if (!otherNames.isEmpty()) {
                Collections.sort(otherNames, baseComp);
                sb.append(getLangStr(otherNames.get(0)));
                final String name880 = get880String(otherNames.get(0));
                if (name880 != null) {
                    sb880.append(name880);
                    i880.addScript("Tibt");
                    has880 = true;
                }
            }
            if (!titles.isEmpty()) {
                Collections.sort(titles, baseComp);
                sb.append(" / ");
                final String title = getLangStr(titles.get(0));
                sb.append(title);
                if (has880) {
                    final String title880 = get880String(titles.get(0));
                    if (title880 != null) {
                        sb880.append(" / ");
                        sb880.append(title880);
                        i880.addScript("Tibt");
                    } else {
                        sb880.append(title);
                    }
                }
            } else if (!otherTitles.isEmpty()) {
                Collections.sort(otherTitles, baseComp);
                sb.append(" / ");
                final String title = getLangStr(otherTitles.get(0));
                sb.append(title);
                if (has880) {
                    final String title880 = get880String(otherTitles.get(0));
                    if (title880 != null) {
                        sb880.append(" / ");
                        sb880.append(title880);
                    } else {
                        sb880.append(title);
                    }
                }
            }
            // There should be a coma at the end, except when the date ends with a hyphen.
            if (birthYear == null) {
                if (deathYear != null) {
                    sb.append(", ?-");
                    sb.append(deathYear + ",");
                    if (has880) {
                        sb880.append(", ?-");
                        sb880.append(deathYear + ",");
                    }
                } else {
                    sb.append(',');
                }
            } else {
                sb.append(", ");
                sb.append(birthYear);
                sb.append('-');
                if (has880) {
                    sb880.append(", ");
                    sb880.append(birthYear);
                    sb880.append('-');
                }
                if (deathYear != null) {
                    sb.append(deathYear + ",");
                    if (has880) {
                        sb880.append(deathYear + ",");
                    }
                }
            }
            final DataField f720_1_ = factory.newDataField("720", '1', ' ');
            if (has880) {
                String curi880 = i880.getNext();
                final DataField f880 = factory.newDataField("880", '1', ' ');
                list880.add(f880);
                f720_1_.addSubfield(factory.newSubfield('6', "880-" + curi880));
                f880.addSubfield(factory.newSubfield('6', "720-" + curi880));
                f880.addSubfield(factory.newSubfield('a', sb880.toString()));
                f880.addSubfield(factory.newSubfield('e', rel));
            }
            f720_1_.addSubfield(factory.newSubfield('a', sb.toString()));
            f720_1_.addSubfield(factory.newSubfield('e', rel));
            list720.add(f720_1_);
        }
    }

    public final static class CompareStringLiterals implements Comparator<Literal> {

        private static final Collator collator = Collator.getInstance(); // root locale

        private String bcpTag = null;

        public CompareStringLiterals(final String bcpTag) {
            this.bcpTag = bcpTag;
        }

        @Override
        public int compare(Literal l1, Literal l2) {
            final String lang1 = l1.getLanguage();
            final String lang2 = l2.getLanguage();
            int res;
            if (!lang1.isEmpty()) {
                if (!lang2.isEmpty()) {
                    // we want those with a corresponding bcpTag at the beginning of the list
                    if (this.bcpTag != null) {
                        final boolean lang1ok = lang1.equals(this.bcpTag) || lang1.startsWith(this.bcpTag + '-');
                        final boolean lang2ok = lang2.equals(this.bcpTag) || lang2.startsWith(this.bcpTag + '-');
                        if (lang1ok && !lang2ok) {
                            return -1;
                        }
                        if (!lang1ok && lang2ok) {
                            return 1;
                        }
                    }
                    res = lang1.compareTo(lang2);
                    if (res != 0)
                        return res;
                    return collator.compare(l1.getString(), l2.getString());
                } else {
                    return -1;
                }
            } else if (!lang2.isEmpty()) {
                return 1;
            }
            return collator.compare(l1.getString(), l2.getString());
        }
    }

    private static String get880String(final Literal l) {
        final String lang = l.getLanguage();
        if ("bo-x-ewts".equals(lang)) {
            final String orig = l.getString();
            if (orig.indexOf('x') > -1) {
                return null;
            }
            final List<String> warns = new ArrayList<>();
            final String u = ewtsConverter.toUnicode(orig, warns, true);
            if (warns.isEmpty()) {
                // only convert literals with no errors in the ewts
                return u;
            }
        }
        return null;
    }

    private static void addTitles(final Model m, final Resource main, final Record record, final String bcp47lang, final Index880 i880, final List<DataField> list880, final List<DataField> list245246) {
        // again, we keep titles in order for consistency among marc queries
        final Map<String, List<Literal>> titles = new TreeMap<>();
        String subtitleStr = null;
        String subtitleStr880 = null;
        Integer highestPrio = 999;
        int nbChineseHanzi = 0;
        int nbChinesePinyin = 0;
        Literal firstChinesePinyin = null;
        Literal firstChineseHanzi = null;
        List<Literal> highestPrioList = null;
        StmtIterator si = main.listProperties(hasTitle);
        while (si.hasNext()) {
            final Resource title = si.next().getResource();
            // get type, if bdo:Title only consider it if it's the only type, otherwise discard it:
            StmtIterator typeSi = title.listProperties(RDF.type);
            String typeLocalName = null;
            while (typeSi.hasNext()) {
                Statement s = typeSi.next();
                if (s.getObject().asResource().getLocalName().equals("Title") && typeSi.hasNext())
                    continue;
                typeLocalName = s.getObject().asResource().getLocalName();
                break;
            }
            StmtIterator labelSi = title.listProperties(RDFS.label);
            while (labelSi.hasNext()) {
                final Literal titleLit = labelSi.next().getLiteral();
                if (typeLocalName.equals("Subtitle")) {
                    subtitleStr = getLangStr(titleLit);
                    subtitleStr880 = get880String(titleLit);
                    continue;
                }
                final String lang = titleLit.getLanguage().toLowerCase();
                if (lang.startsWith("zh-han")) {
                    firstChineseHanzi = titleLit;
                    nbChineseHanzi += 1;
                } else if (lang.startsWith("zh-latn-pinyin")) {
                    firstChinesePinyin = titleLit;
                    nbChinesePinyin += 1;
                }
                final MarcInfo mi = titleLocalNameToMarcInfo.get(typeLocalName);
                if (mi == null) {
                    log.error("missing MarcInfo for title type {}", typeLocalName);
                    continue;
                }
                if (!titles.containsKey(typeLocalName)) {
                    titles.put(typeLocalName, new ArrayList<>());
                }
                final List<Literal> titleList = titles.get(typeLocalName);
                titleList.add(titleLit);
                final Integer prio = mi.prio;
                if (prio < highestPrio) {
                    highestPrio = prio;
                    highestPrioList = titleList;
                }
            }
        }
        if (nbChineseHanzi != 1 || nbChinesePinyin != 1) {
            firstChineseHanzi = null;
            firstChinesePinyin = null;
        }
        boolean firstChineseDone = false;
        if (highestPrioList == null) {
            // no title...
            return;
        }
        final CompareStringLiterals compbcp = new CompareStringLiterals(bcp47lang);
        if (highestPrioList.size() > 1) {
            // sorting highestPrioList by language
            Collections.sort(highestPrioList, compbcp); // works for null
        }
        // authorship statement
        si = main.listProperties(authorshipStatement);
        String authorshipStatement = null;
        String authorshipStatement880 = null;
        while (si.hasNext()) {
            final Literal authorshipStatementL = si.next().getLiteral();
            authorshipStatement = getLangStr(authorshipStatementL);
            authorshipStatement880 = get880String(authorshipStatementL);
            // not sure how to handle multiple authorship statements...
            break;
        }
        Literal mainTitleL = highestPrioList.get(0);
        highestPrioList.remove(0);
        final DataField f245 = factory.newDataField("245", '0', '0');
        String mainTitleS880;
        if (mainTitleL == firstChinesePinyin || mainTitleL == firstChineseHanzi) {
            mainTitleL = firstChinesePinyin;
            firstChineseDone = true;
            mainTitleS880 = firstChineseHanzi.getString();
            i880.addScript("$1");
        } else {
            mainTitleS880 = get880String(mainTitleL);
        }
        String curi880;
        final DataField f880_main = factory.newDataField("880", '0', '0');
        if (mainTitleS880 != null) {
            curi880 = i880.getNext();
            i880.addScript("Tibt");
            list880.add(f880_main);
            f245.addSubfield(factory.newSubfield('6', "880-" + curi880));
            f880_main.addSubfield(factory.newSubfield('6', "245-" + curi880));
        }
        String mainTitleS;
        // ma che buoni questi spaghetti!
        if (subtitleStr != null) {
            mainTitleS = getLangStr(mainTitleL) + " :";
            if (mainTitleS880 != null) {
                mainTitleS880 += " : ";
            }
        } else if (authorshipStatement != null) {
            mainTitleS = getLangStr(mainTitleL) + " /";
            if (mainTitleS880 != null) {
                mainTitleS880 += " / ";
            }
        } else {
            mainTitleS = getLangStr(mainTitleL) + ".";
            if (mainTitleS880 != null) {
                mainTitleS880 += ".";
            }
        }
        f245.addSubfield(factory.newSubfield('a', mainTitleS));
        if (mainTitleS880 != null) {
            f880_main.addSubfield(factory.newSubfield('a', mainTitleS880));
        }
        if (subtitleStr != null) {
            if (authorshipStatement != null) {
                f245.addSubfield(factory.newSubfield('b', subtitleStr + " /"));
            } else {
                f245.addSubfield(factory.newSubfield('b', subtitleStr + "."));
            }
        }
        if (mainTitleS880 != null && subtitleStr880 != null) {
            if (authorshipStatement != null) {
                f880_main.addSubfield(factory.newSubfield('b', subtitleStr880 + " /"));
            } else {
                f880_main.addSubfield(factory.newSubfield('b', subtitleStr880 + "."));
            }
        }
        if (authorshipStatement != null) {
            f245.addSubfield(factory.newSubfield('c', authorshipStatement + "."));
            if (mainTitleS880 != null) {
                if (authorshipStatement880 != null) {
                    f880_main.addSubfield(factory.newSubfield('c', authorshipStatement880 + "."));
                } else {
                    f880_main.addSubfield(factory.newSubfield('c', authorshipStatement + "."));
                }

            }
        }
        list245246.add(f245);
        for (Entry<String, List<Literal>> e : titles.entrySet()) {
            final MarcInfo mi = titleLocalNameToMarcInfo.get(e.getKey());
            final List<Literal> list = e.getValue();
            Collections.sort(list, compbcp);
            for (Literal l : list) {
                final DataField f246 = factory.newDataField("246", '1', mi.subindex2);
                final String titleS880;
                if (l == firstChinesePinyin || l == firstChineseHanzi) {
                    if (!firstChineseDone) {
                        l = firstChinesePinyin;
                        firstChineseDone = true;
                        titleS880 = firstChineseHanzi.getString();
                        i880.addScript("$1");
                    } else {
                        continue;
                    }
                } else {
                    titleS880 = get880String(l);
                }
                if (titleS880 != null) {
                    i880.addScript("Tibt");
                    curi880 = i880.getNext();
                    final DataField f880 = factory.newDataField("880", '1', mi.subindex2);
                    f246.addSubfield(factory.newSubfield('6', "880-" + curi880));
                    f880.addSubfield(factory.newSubfield('6', "246-" + curi880));
                    if (mi.subfieldi != null) {
                        f880.addSubfield(factory.newSubfield('i', mi.subfieldi));
                    }
                    f880.addSubfield(factory.newSubfield('a', titleS880));
                    list880.add(f880);
                }
                if (mi.subfieldi != null) {
                    f246.addSubfield(factory.newSubfield('i', mi.subfieldi));
                }
                f246.addSubfield(factory.newSubfield('a', getLangStr(l)));
                list245246.add(f246);
            }
        }
    }

    private static void addTopics(final Model m, final Resource main, final Record record) {
        final StmtIterator si = main.listProperties(workIsAbout); // TODO: also workGenre?
        final DataField f653 = factory.newDataField("653", ' ', ' ');
        boolean hasTopic = false;
        while (si.hasNext()) {
            final Resource topic = si.next().getResource();
            final Literal l = getPreferredLit(topic, null);
            if (l == null)
                continue;
            hasTopic = true;
            f653.addSubfield(factory.newSubfield('a', getLangStr(l)));
        }
        if (hasTopic) {
            record.addVariableField(f653);
        }
    }

    private static void addSeries(final Model m, final Resource main, final Record record, final String bcp47lang) {
        StmtIterator si = main.listProperties(serialInstanceOf);
        boolean hasSeries = false;
        final DataField f490 = factory.newDataField("490", '0', ' ');
        while (si.hasNext()) {
            final Resource series = si.next().getResource();
            final Literal l = getPreferredLit(series, bcp47lang);
            if (l == null)
                continue;
            hasSeries = true;
            f490.addSubfield(factory.newSubfield('a', getLangStr(l) + " ;"));
        }
        si = main.listProperties(seriesNumber);
        while (si.hasNext()) {
            final Literal series = si.next().getLiteral();
            hasSeries = true;
            f490.addSubfield(factory.newSubfield('v', getLangStr(series)));
        }
        if (hasSeries)
            record.addVariableField(f490);
    }

    private static Literal getPreferredLit(Resource r, final String bcp47lang) {
        StmtIterator labelSi = r.listProperties(SKOS.prefLabel);
        if (!labelSi.hasNext()) {
            labelSi = r.listProperties(RDFS.label);
            if (!labelSi.hasNext())
                return null;
        }
        final Map<String, List<Literal>> labels = new TreeMap<>();
        while (labelSi.hasNext()) {
            final Literal label = labelSi.next().getLiteral();
            final String lng = label.getLanguage();
            final List<Literal> litList = labels.computeIfAbsent(lng, x -> new ArrayList<>());
            litList.add(label);
        }
        List<Literal> interestingLiterals = null;
        if (labels.containsKey("en")) {
            interestingLiterals = labels.get("en");
        } else if (labels.containsKey("bo-x-ewts")) {
            interestingLiterals = labels.get("bo-x-ewts");
        } else {
            // other than that we just take the first one
            for (final List<Literal> l : labels.values()) {
                interestingLiterals = l;
                break;
            }
        }
        Collections.sort(interestingLiterals, new CompareStringLiterals(bcp47lang));
        if (interestingLiterals.size() == 0)
            return null;
        return interestingLiterals.get(0);
    }

    private static CompareStringLiterals baseComp = new CompareStringLiterals(null);

    private static List<Resource> getScriptLeaves(final Model m, final Resource main, final Property p) {
        StmtIterator lsi = main.listProperties(p);
        final Map<Resource, Boolean> resHasSubClass = new HashMap<>();
        while (lsi.hasNext()) {
            final Resource script = lsi.next().getObject().asResource();
            Resource superR = script.getPropertyResourceValue(SKOS.broader);
            if (superR != null) {
                resHasSubClass.put(superR, true);
            }
            resHasSubClass.putIfAbsent(script, false);
        }
        final List<Resource> res = new ArrayList<>();
        for (Entry<Resource, Boolean> e : resHasSubClass.entrySet()) {
            if (e.getValue() == false) {
                res.add(e.getKey());
            }
        }
        return res;
    }

    private static final Pattern noScript = Pattern.compile(" script$", Pattern.CASE_INSENSITIVE);
    
    private static void addLanguages(final Model m, final Resource main, final Record record) {
        final StringBuilder sb = new StringBuilder();
        int nbLangs = 0;
        String firstScriptLabel = null;
        List<Resource> sLeaves = getScriptLeaves(m, main, script);
        if (sLeaves.size() > 0) {
            // just take the first script, not ideal but a reasonable approximation
            Literal firstScriptLabelL = getPreferredLit(sLeaves.get(0), null);
            if (firstScriptLabelL == null)
                firstScriptLabel = sLeaves.get(0).getLocalName();
            else
                firstScriptLabel = firstScriptLabelL.getString();
        }
        StmtIterator lsi = main.listProperties(language);
        while (lsi.hasNext()) {
            final Resource lang = lsi.next().getObject().asResource();
            final Literal langL = getPreferredLit(lang, null);
            final String plainEnglish;
            if (langL == null)
                plainEnglish = lang.getLocalName();
            else
                plainEnglish = langL.getString();
            nbLangs += 1;
            if (nbLangs == 1) {
                sb.append("In ");
            } else {
                sb.append(" and ");
            }
            sb.append(plainEnglish);
        }
        if (nbLangs == 0)
            return;
        final DataField f546 = factory.newDataField("546", ' ', ' ');
        if (nbLangs == 1 && firstScriptLabel != null) {
            f546.addSubfield(factory.newSubfield('a', sb.toString() + ';'));
            firstScriptLabel = noScript.matcher(firstScriptLabel).replaceAll("");
            f546.addSubfield(factory.newSubfield('b', firstScriptLabel + " script."));
        } else {
            sb.append('.');
            f546.addSubfield(factory.newSubfield('a', sb.toString()));
        }
        record.addVariableField(f546);
    }

    public static String getLangStr505(final Literal l) {
        final String lang = l.getLanguage();
        if (lang == null || !"bo-x-ewts".equals(lang)) {
            return getLangStrNoConv(l);
        }
        String[] parts = l.getString().split("(/ ?)+");
        final StringBuilder res = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            final String part = parts[i].trim();
            String alalc = TransConverter.ewtsToAlalc(part, true);
            alalc = StringUtils.capitalize(alalc.replace("u0fbe", "x").replace('-', ' ').trim());
            res.append(alalc);
            if (i < parts.length - 1) {
                res.append(". ");
            }
        }
        return res.toString();
    }

    private static void addOutline(final Model m, final Resource main, final Record record, final String bcp47lang, final boolean limitSize) {
        final Selector selector = new SimpleSelector(null, hasPart, (RDFNode) null);
        final StmtIterator si = m.listStatements(selector);
        StringBuilder sb = new StringBuilder();
        final Map<String, Literal> parts = new TreeMap<>();
        int nbParts = 0;
        // we keep the parts in order
        while (si.hasNext()) {
            final Resource part = si.next().getResource();
            final Statement indexTreeS = part.getProperty(partTreeIndex);
            if (indexTreeS == null)
                continue;
            final Literal l = getPreferredLit(part, bcp47lang);
            if (l == null)
                continue;
            final String indexTree = indexTreeS.getString();
            parts.put(indexTree, l);
            nbParts += 1;
        }
        // some works have just one first level (this is kind of weird), eg W1GS61415
        if (nbParts < 2)
            return;
        DataField f505 = factory.newDataField("505", '0', ' ');
        boolean first = true;
        int totalBytes = 10; // just to be sure
        for (Literal l : parts.values()) {
            final String s505 = getLangStr505(l);
            if (limitSize) {
                int thisTotalBytes;
                try {
                    thisTotalBytes = s505.getBytes("UTF-8").length;
                } catch (UnsupportedEncodingException e) {
                    log.error("error in UTF8 encoding", e); // very unlikely
                    return;
                }
                if (thisTotalBytes > 9990) {
                    log.info("skipped 505 field as it was too long to fit in a mrc field");
                    continue;
                }
                if (thisTotalBytes + totalBytes > 9990) {
                    // we must split the 505 field so that each part is less than 9999 bytes:
                    sb.append('.');
                    f505.addSubfield(factory.newSubfield('a', sb.toString()));
                    record.addVariableField(f505);
                    f505 = factory.newDataField("505", '0', ' ');
                    sb = new StringBuilder();
                    first = true;
                    totalBytes = thisTotalBytes;
                } else {
                    totalBytes += thisTotalBytes;
                }
            }
            if (!first) {
                sb.append(" -- ");
                totalBytes += 4;
            }
            sb.append(s505);
            first = false;
        }
        sb.append('.');
        f505.addSubfield(factory.newSubfield('a', sb.toString()));
        record.addVariableField(f505);
    }

    public static final class Index880 {
        // https://www.oclc.org/bibformats/en/controlsubfields.html#subfield6
        private int nextIndex = 1;
        public List<String> scripts = new ArrayList<>();

        public Index880() {
        }

        public String getNext() {
            nextIndex += 1;
            return String.format("%02d", nextIndex - 1);
        }

        public void addScript(String script) {
            if (!scripts.contains(script))
                scripts.add(script);
        }
    }

    // tmp, for debug
    public static void printModel(final Model m) {
        TTLRDFWriter.getSTTLRDFWriter(m, "").output(System.out);
    }

    public static List<String> getLanguages(final Model m, final Resource main) {
        final List<String> res = new ArrayList<>();
        final StmtIterator lsi = main.listProperties(language);
        while (lsi.hasNext()) {
            final String lurl = lsi.next().getResource().getURI();
            res.add(lurl);
        }
        return res;
    }

    public static boolean isJournal(final Model m, final Resource workR) {
        boolean res = false;
        StmtIterator si = workR.listProperties(workGenre);
        while (si.hasNext()) {
            final String topic = si.next().getResource().getLocalName();
            if (topic.equals("T297"))
                res = true;
        }
        si = workR.listProperties(workIsAbout);
        while (si.hasNext()) {
            final String topic = si.next().getResource().getLocalName();
            if (topic.equals("T297"))
                res = true;
        }
        return res;
    }

    public static void add066(final Record r, final Index880 i880) {
        if (!i880.scripts.isEmpty()) {
            final DataField f066 = factory.newDataField("066", ' ', ' ');
            if (i880.scripts.contains("Tibt"))
                f066.addSubfield(factory.newSubfield('c', "Tibt"));
            if (i880.scripts.contains("$1"))
                f066.addSubfield(factory.newSubfield('c', "$1"));
            r.addVariableField(f066);
        }
    }

    public static void add041(final Model m, final Record r, final List<String> langUrls, final String mainLangMarcCode) {
        if (langUrls.size() < 2)
            return;
        // 0 means it's not a translation... maybe 1 should be indicated when it is?
        final DataField f041 = factory.newDataField("041", '0', ' ');
        final List<String> marcCodes = new ArrayList<String>();
        for (final String langUrl : langUrls) {
            final Resource langR = m.getResource(langUrl);
            final Statement marcLangS = langR.getProperty(langMARCCode);
            if (marcLangS != null) {
                marcCodes.add(marcLangS.getString());
            }
        }
        // mainLangMarcCode is what appears in 008, when it is "mul", it should appear
        // in the list
        // (which is a bit counter-intuitive)
        if (mainLangMarcCode.equals("mul")) {
            marcCodes.add("mul");
        }
        // codes should be sorted alphabetically
        Collections.sort(marcCodes);
        for (final String marcCode : marcCodes) {
            f041.addSubfield(factory.newSubfield('a', marcCode));
        }
        r.addVariableField(f041);
    }

    public static final String langTibetan = BDR + "LangBo";

    public static Record marcFromModel(final Model m, final Resource workR, final Resource originalR, final boolean scansMode, final boolean limitSize) {
        final Index880 i880 = new Index880();
        final Record record = factory.newRecord(leader);
        // Columbia originally asked us to prefix the ID with "(BDRC)", but Harvard specifically asked
        // us not to do that, especially since they introduced the 003 control field
        record.addVariableField(factory.newControlField("001", "bdr:" + originalR.getLocalName()));
        record.addVariableField(f003);
        final LocalDateTime now = LocalDateTime.now();
        // record.addVariableField(factory.newControlField("005", now.format(f005_f)));
        record.addVariableField(f006);
        record.addVariableField(f007);
        final List<String> langUrls = getLanguages(m, workR);
        String bcp47lang = null;
        String langMarcCode = null;
        // request from Columbia, when we have multiple languages recorded, we should
        // indicate Tibetan as the main language (since we cannot check every occurence
        // of multiple languages).
        // we first reorganize langUrls so that it contains Tibetan first in this case:
        String mainLangUrl = null;
        if (langUrls.size() == 1) {
            mainLangUrl = langUrls.get(0);
            final Resource langR = m.getResource(mainLangUrl);
            final Statement marcLangS = langR.getProperty(langMARCCode);
            if (marcLangS != null) {
                langMarcCode = marcLangS.getString();
            }
        } else if (langUrls.size() > 1) {
            langMarcCode = "mul";
            final int idxTibt = langUrls.indexOf(langTibetan);
            if (idxTibt != -1) {
                mainLangUrl = langTibetan;
                // when the work has some Tibetan language, we mark it as Tibetan
                langMarcCode = "tib";
            } else {
                mainLangUrl = langUrls.get(0);
            }
        }
        if (mainLangUrl != null) {
            final Resource langR = m.getResource(mainLangUrl);
            final Statement bcpLangS = langR.getProperty(langBCP47Lang);
            if (bcpLangS != null) {
                bcp47lang = bcpLangS.getString();
            }
        }
        add008(m, workR, record, langMarcCode, now);
        addIsbn(m, workR, record, scansMode); // 020
        // Colubia asked us to add a 035 field, but Harvard asked us to remove it
        // since it can be derived from 001 + 003
//        final DataField f035 = factory.newDataField("035", ' ', ' ');
//        f035.addSubfield(factory.newSubfield('a', "(BDRC)bdr:" + originalR.getLocalName()));
//        record.addVariableField(f035);
//        if (scansMode) {
//            DataField f035_2 = factory.newDataField("035", ' ', ' ');
//            f035_2.addSubfield(factory.newSubfield('a', "(BDRC)bdr:" + workR.getLocalName()));
//            record.addVariableField(f035_2);
//        }
        record.addVariableField(f040);
        add041(m, record, langUrls, langMarcCode);
        List<String> lcCallNumberList = getId(m, workR, m.getResource(BF+"ShelfMarkLcc"));
        for (String lcCallNumber : lcCallNumberList) {
            // https://github.com/BuddhistDigitalResourceCenter/xmltoldmigration/issues/55
            lcCallNumber = lcCallNumber.toUpperCase();
            final int firstSpaceIdx = lcCallNumber.indexOf(' ');
            if (firstSpaceIdx == -1)
                continue;
            final String classNumber = lcCallNumber.substring(0, firstSpaceIdx);
            final String cutterNumber = lcCallNumber.substring(firstSpaceIdx + 1);
            final DataField f050__4 = factory.newDataField("050", ' ', '4');
            f050__4.addSubfield(factory.newSubfield('a', classNumber));
            f050__4.addSubfield(factory.newSubfield('b', cutterNumber));
            record.addVariableField(f050__4);
        }
        final List<DataField> list880 = new ArrayList<>();
        final List<DataField> list245246 = new ArrayList<>();
        // this records 066, 245, 246 and 880 that we will use later
        addTitles(m, workR, record, bcp47lang, i880, list880, list245246);
        // same principle, finishing the 880 and 066 record, and lists the 720 records
        final List<DataField> list720 = new ArrayList<>();
        addAuthors(m, workR, record, i880, list880, list720);
        add066(record, i880);
        for (DataField df245246 : list245246) {
            record.addVariableField(df245246);
        }
        // edition statement
        StmtIterator si = workR.listProperties(editionStatement);
        while (si.hasNext()) {
            final Literal editionStatement = si.next().getLiteral();
            final DataField f250 = factory.newDataField("250", ' ', ' ');
            String f250str = getLangStrNoConv(editionStatement);
            if (!f250str.endsWith(".")) {
                f250str += ".";
            }
            f250.addSubfield(factory.newSubfield('a', f250str));
            record.addVariableField(f250);
        }
        addPubInfo(m, workR, record); // 264
        add300(m, workR, record);
        record.addVariableField(f336);
        record.addVariableField(f337);
        record.addVariableField(f338);
        addSeries(m, workR, record, bcp47lang); // 490
        // Columbia requested that 546 be the first 5xx field
        addLanguages(m, workR, record); // 546
        // biblio note
        si = workR.listProperties(workBiblioNote);
        while (si.hasNext()) {
            final Literal biblioNote = si.next().getLiteral();
            final DataField f500 = factory.newDataField("500", ' ', ' ');
            String biblioNoteS = getLangStrNoConv(biblioNote);
            if (!biblioNoteS.endsWith(".")) {
                biblioNoteS += ".";
            }
            f500.addSubfield(factory.newSubfield('a', biblioNoteS));
            record.addVariableField(f500);
        }
        if (!isJournal(m, workR)) {
            addOutline(m, workR, record, bcp47lang, limitSize); // 505
        }
        if (scansMode)
            addAccess(m, workR, record); // 506
        // catalog info (summary)
        si = workR.listProperties(catalogInfo);
        while (si.hasNext()) {
            final Literal catalogInfo = si.next().getLiteral();
            final DataField f520 = factory.newDataField("520", ' ', ' ');
            f520.addSubfield(factory.newSubfield('a', getLangStrNoConv(catalogInfo)));
            record.addVariableField(f520);
        }
        if (scansMode)
            record.addVariableField(f533);
        final Resource license = workR.getPropertyResourceValue(tmpLicense);
        if (license != null && license.getLocalName().equals("LicensePublicDomain")) {
            record.addVariableField(f542_PD);
        }
        final DataField f588 = factory.newDataField("588", ' ', ' ');
        final Date curDate = new Date();
        String dateStr = dateFormat.format(curDate);
        f588.addSubfield(factory.newSubfield('a', "Description based on online resource (BDRC, viewed " + dateStr + ")"));
        record.addVariableField(f588);
        addTopics(m, workR, record); // 653
        record.addVariableField(f710_2);
        for (DataField df720 : list720) {
            record.addVariableField(df720);
        }
        // lccn
        List<String> lccnList = getId(m, workR, m.getResource(BF+"Lccn"));
        for (String lccn : lccnList) {
            // from Columbia: spaces should be added at the end of the lccn string so that
            // it spans
            // 12 characters exactly, counting the 3 first spaces (so 9 for our lccn string)
            for (int i = lccn.length(); i < 9; i++) {
                lccn += ' ';
            }
            final DataField f776_08 = factory.newDataField("776", '0', '8');
            f776_08.addSubfield(factory.newSubfield('i', "Electronic reproduction of (manifestation)"));
            f776_08.addSubfield(factory.newSubfield('w', "(DLC)   " + lccn));
            if (scansMode)
                record.addVariableField(f776_08);
            // TODO: in normal mode, I'm not sure what this should be
        }
        if (scansMode) {
            final DataField f856 = factory.newDataField("856", '4', '0');
            f856.addSubfield(factory.newSubfield('u', originalR.getURI()));
            f856.addSubfield(factory.newSubfield('z', "Available from BDRC"));
            record.addVariableField(f856);
        }
        for (DataField df880 : list880) {
            record.addVariableField(df880);
        }
        // only for big errors, not that useful:
        // List<MarcError> errors = record.getErrors();
        // if (errors != null) { // annoyingly nullable...
        // for (MarcError me : record.getErrors()) {
        // System.out.println("test");
        // }
        // }
        return record;
    }

    public static Model getModelForMarc(final String resUri) throws RestException {
        Model model = QueryProcessor.getSimpleResourceGraph(resUri, "resForMarc.arq");
        if (model.size() < 1) {
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setContext(resUri));
        }
        final Resource main = model.getResource(resUri);
        final StmtIterator stmti = main.listProperties(RDF.type);
        boolean isInstance = false;
        while (stmti.hasNext()) {
            final Resource type = stmti.next().getObject().asResource();
            if (type.getLocalName().contains("Instance")) {
                isInstance = true;
                break;
            }
        }
        if (!isInstance) {
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setMsg("Resource is not an Instance"));
        }
        if (main.hasProperty(partOf)) {
            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setMsg("Resource is part of another Instance"));
        }
        // this should be correct but breaks W2DB4598 because of poorly encoded series
        // data
        // if (main.hasProperty(hasExpression)) {
        // throw new RestException(404, new
        // LdsError(LdsError.NO_GRAPH_ERR).setMsg("Resource is an abstract Work"));
        // }
        return model;
    }

    public static void remove505(Record r) {
        // removing all 505 fields
        // this code is very weird but it's apparently the canonical way to do that
        List<DataField> fields = r.getDataFields();
        Iterator<DataField> i = fields.iterator();
        while (i.hasNext()) {
            DataField field = i.next();
            if (field.getTag().equals("505")) {
                i.remove();
            }
        }
    }

    public static final String ScanUriPrefix = BDR + "W";
    public static final String InstanceUriPrefix = BDR + "MW";

    public static ResponseEntity<StreamingResponseBody> getResponse(final MediaType mt, final String resUri) throws RestException {
        // I really don't like that but making that better would mean either:
        // - a very weird and probably slower SPARQL query
        // - two queries
        // The idea is that we're sending MARC records for items to Columbia,
        // as they only want "electronic resources" records, and electronic
        // resources are imageinstances (scans) in our system, not regular instances.
        boolean scansMode = resUri.startsWith(ScanUriPrefix);
        final Model m;
        final Resource main;
        // we could also imagine rendering mrc with longer fields / records
        // but currently this doesn't play well with Columbia system
        final boolean limitSize = mt.equals(BudaMediaTypes.MT_MRC);
        m = getModelForMarc(resUri);
        main = m.getResource(resUri);
        
        final Resource origMain = m.getResource(resUri);
        final Record r = marcFromModel(m, main, origMain, scansMode, limitSize);
        final StreamingResponseBody stream = new StreamingResponseBody() {
            @Override
            public void writeTo(final OutputStream os) throws IOException {
                final MarcWriter writer;
                if (mt.equals(BudaMediaTypes.MT_MRC)) {
                    writer = new MarcStreamWriter(os, "UTF-8", !limitSize);
                } else {
                    writer = new MarcXmlWriter(os, indent);
                }
                try {
                    writer.write(r);
                } catch (MarcException e) {
                    // we suppose that the problem is that the record is too long:
                    log.info("removing the outline so that the record fits in 99999 bytes");
                    remove505(r);
                    writer.write(r);
                }
                writer.close();
            }
        };
        return ResponseEntity.ok().header("Allow", "GET, OPTIONS, HEAD").header("Vary", "Negotiate, Accept").contentType(mt).body(stream);

    }
}
