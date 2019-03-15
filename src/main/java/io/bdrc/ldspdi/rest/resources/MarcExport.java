package io.bdrc.ldspdi.rest.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.ISBNValidator;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Leader;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import io.bdrc.ewtsconverter.TransConverter;
import io.bdrc.formatters.TTLRDFWriter;
import io.bdrc.ldspdi.ontology.service.core.OntData;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

public class MarcExport {

    public static final MarcFactory factory = MarcFactory.newInstance();

    public final static Logger log = LoggerFactory.getLogger(MarcExport.class.getName());

    public static final String BDO = "http://purl.bdrc.io/ontology/core/";
    public static final String BDR = "http://purl.bdrc.io/resource/";
    public static final String ADM = "http://purl.bdrc.io/ontology/admin/";
    public static final String TMP = "http://purl.bdrc.io/ontology/tmp/";
    public static final Property partOf = ResourceFactory.createProperty(BDO+"workPartOf");
    public static final Property workHasPart = ResourceFactory.createProperty(BDO+"workHasPart");
    public static final Property workPartTreeIndex = ResourceFactory.createProperty(BDO+"workPartTreeIndex");
    public static final Property hasExpression = ResourceFactory.createProperty(BDO+"workHasExpression");
    public static final Property workEditionStatement = ResourceFactory.createProperty(BDO+"workEditionStatement");
    public static final Property workExtentStatement = ResourceFactory.createProperty(BDO+"workExtentStatement");
    public static final Property workPublisherName = ResourceFactory.createProperty(BDO+"workPublisherName");
    public static final Property workPublisherLocation = ResourceFactory.createProperty(BDO+"workPublisherLocation");
    public static final Property workAuthorshipStatement = ResourceFactory.createProperty(BDO+"workAuthorshipStatement");
    public static final Property workCatalogInfo = ResourceFactory.createProperty(BDO+"workCatalogInfo");
    public static final Property workBiblioNote = ResourceFactory.createProperty(BDO+"workBiblioNote");
    public static final Property creatorTerton = ResourceFactory.createProperty(BDO+"creatorTerton");
    // TODO: update
    public static final Property creatorMainAuthor = ResourceFactory.createProperty(BDO+"creatorGeneralAuthor");
    public static final Property creatorTranslator = ResourceFactory.createProperty(BDO+"creatorTranslator");
    // TODO: update
    public static final Property creatorIndicScholar = ResourceFactory.createProperty(BDO+"creatorPandita");
    public static final Property creatorContributingAuthor = ResourceFactory.createProperty(BDO+"creatorContributingAuthor");
    public static final Property creatorCommentator = ResourceFactory.createProperty(BDO+"creatorCommentator");
    public static final Property creatorCompiler = ResourceFactory.createProperty(BDO+"creatorEditor");
    // TODO: update
    public static final Property creatorReviser = ResourceFactory.createProperty(BDO+"creatorReviserOfTranslation");
    public static final Property creatorScribe = ResourceFactory.createProperty(BDO+"creatorScribe");
    public static final Property creatorCalligrapher = ResourceFactory.createProperty(BDO+"creatorCalligrapher");
    public static final Property creatorArtist = ResourceFactory.createProperty(BDO+"creatorArtist");
    public static final Property workEvent = ResourceFactory.createProperty(BDO+"workEvent");
    public static final Property workTitle = ResourceFactory.createProperty(BDO+"workTitle");
    public static final Property workIsAbout = ResourceFactory.createProperty(BDO+"workIsAbout");
    public static final Property workNumberOf = ResourceFactory.createProperty(BDO+"workNumberOf");
    public static final Property workSeriesName = ResourceFactory.createProperty(BDO+"workSeriesName");
    public static final Property workSeriesNumber = ResourceFactory.createProperty(BDO+"workSeriesNumber");
    public static final Property personEvent = ResourceFactory.createProperty(BDO+"personEvent");
    public static final Property personName = ResourceFactory.createProperty(BDO+"personName");
    public static final Property langScript = ResourceFactory.createProperty(BDO+"langScript");
    public static final Property otherLangScript = ResourceFactory.createProperty(BDO+"otherLangScript");
    public static final Property language = ResourceFactory.createProperty(BDO+"language");
    public static final Property script = ResourceFactory.createProperty(BDO+"script");
    public static final Property onYear = ResourceFactory.createProperty(BDO+"onYear");
    public static final Property notBefore = ResourceFactory.createProperty(BDO+"notBefore");
    public static final Property notAfter = ResourceFactory.createProperty(BDO+"notAfter");
    public static final Property workIsbn = ResourceFactory.createProperty(BDO+"workIsbn");
    public static final Property workLccn = ResourceFactory.createProperty(BDO+"workLccn");
    public static final Property workLcCallNumber = ResourceFactory.createProperty(BDO+"workLcCallNumber");
    public static final Property admAccess = ResourceFactory.createProperty(ADM+"access");
    public static final Property admLicense = ResourceFactory.createProperty(ADM+"license");
    public static final Property tmpPublishedYear = ResourceFactory.createProperty(TMP+"publishedYear");
    //public static final Property tmpCompletedYear = ResourceFactory.createProperty(TMP+"completedYear");
    public static final Property tmpBirthYear = ResourceFactory.createProperty(TMP+"birthYear");
    public static final Property tmpDeathYear = ResourceFactory.createProperty(TMP+"deathYear");
    public static final Property tmpMarcLang = ResourceFactory.createProperty(TMP+"workMarcLanguage");
    public static final Property tmpBcpLang = ResourceFactory.createProperty(TMP+"workBCPLanguage");
    public static final Property tmpOtherLang = ResourceFactory.createProperty(TMP+"workOtherLanguage");
    public static final Property publisherLocation = ResourceFactory.createProperty(BDO+"publisherLocation");

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

    public static final Map<String,MarcInfo> titleLocalNameToMarcInfo = new HashMap<>();

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
    static final DataField f506_restrictedInChina = factory.newDataField("506", '1', ' ');

    static final DataField f542_PD = factory.newDataField("542", '1', ' ');

    static final String defaultCountryCode = "   ";
    static final String defaultLang = "und";

    // for 588 field
    static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);

    static {
        titleLocalNameToMarcInfo.put("WorkTitlePageTitle", new MarcInfo(0, '5', null));
        titleLocalNameToMarcInfo.put("WorkBibliographicalTitle", new MarcInfo(1, '3', "BDRC Bibliographical title:"));
        titleLocalNameToMarcInfo.put("WorkCoverTitle", new MarcInfo(2, '4', null));
        titleLocalNameToMarcInfo.put("WorkFullTitle", new MarcInfo(3, '3', null));
        titleLocalNameToMarcInfo.put("WorkHalfTitle", new MarcInfo(4, '3', null)); // or 3?
        titleLocalNameToMarcInfo.put("WorkColophonTitle", new MarcInfo(5, '3', "Colophon title:"));
        titleLocalNameToMarcInfo.put("WorkCopyrightPageTitle", new MarcInfo(6, '3', null));
        titleLocalNameToMarcInfo.put("WorkDkarChagTitle", new MarcInfo(7, '3', "Table of contents title:"));
        titleLocalNameToMarcInfo.put("WorkOtherTitle", new MarcInfo(8, '3', null));
        titleLocalNameToMarcInfo.put("WorkRunningTitle", new MarcInfo(9, '7', null));
        titleLocalNameToMarcInfo.put("WorkSpineTitle", new MarcInfo(10, '8', null));
        titleLocalNameToMarcInfo.put("WorkTitlePortion", new MarcInfo(11, '0', null));
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
        f338.addSubfield(factory.newSubfield('b', "cr"));
        f338.addSubfield(factory.newSubfield('2', "rdacarrier"));
        f533.addSubfield(factory.newSubfield('a', "Electronic reproduction"));
        f533.addSubfield(factory.newSubfield('b', "Cambridge, Mass. :"));
        f533.addSubfield(factory.newSubfield('c', "Buddhist Digital Resource Center."));
        f710_2.addSubfield(factory.newSubfield('a', "Buddhist Digital Resource Center."));
        // see https://www.oclc.org/content/dam/oclc/digitalregistry/506F_vocabulary.pdf
        f506_restricted.addSubfield(factory.newSubfield('a', "Access restricted."));
        f506_restricted.addSubfield(factory.newSubfield('f', "No online access"));
        f506_restricted.addSubfield(factory.newSubfield('2', "star."));
        f506_open.addSubfield(factory.newSubfield('a', "Open Access."));
        f506_open.addSubfield(factory.newSubfield('f', "Unrestricted online access"));
        f506_open.addSubfield(factory.newSubfield('2', "star."));
        f506_fairUse.addSubfield(factory.newSubfield('a', "Access restricted to a few sample pages."));
        f506_fairUse.addSubfield(factory.newSubfield('f', "Preview only"));
        f506_fairUse.addSubfield(factory.newSubfield('2', "star."));
        f506_restrictedInChina.addSubfield(factory.newSubfield('a', "Open to readers with IP addresses outside China."));
        f542_PD.addSubfield(factory.newSubfield('l', "Public Domain"));
        f542_PD.addSubfield(factory.newSubfield('u', "https://creativecommons.org/publicdomain/mark/1.0/"));
    }

    public static boolean indent = true;

    public static final Map<String,String> pubLocToCC = getPubLoctoCC();

    public static String getLangStr(final Literal l) {
        final String lang = l.getLanguage();
        if (lang == null || !"bo-x-ewts".equals(lang)) {
            return StringUtils.capitalize(l.getString());
        }
        final String alalc = TransConverter.ewtsToAlalc(l.getString(), true);
        return StringUtils.capitalize(alalc.replace('-', ' '));
    }

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
            case "AccessRestrictedInChina":
                r.addVariableField(f506_restrictedInChina);
                break;
            default:
                r.addVariableField(f506_restricted);
                break;
            }
        }
    }

    // tmp, for debug
    public static void printModel(final Model m) {
        TTLRDFWriter.getSTTLRDFWriter(m,"").output(System.out);
    }

    public static void addPubInfo(final Model m, final Resource main, final Record r) {
        final DataField f264 = factory.newDataField("264", ' ', '1');
        StmtIterator si = main.listProperties(workPublisherLocation);
        boolean hasPubLocation = false;
        while (si.hasNext()) {
            final Literal pubLocation = si.next().getLiteral();
            if (pubLocation.getString().contains("s.")) {
                continue;
            }
            f264.addSubfield(factory.newSubfield('a', getLangStr(pubLocation)+" :"));
            hasPubLocation = true;
        }
        if (!hasPubLocation) {
            f264.addSubfield(factory.newSubfield('a', "[Place of publication not identified]"));
        }
        si = main.listProperties(workPublisherName);
        boolean hasPubName = false;
        while (si.hasNext()) {
            final Literal pubName = si.next().getLiteral();
            if (pubName.getString().contains("s.")) {
                continue;
            }
            f264.addSubfield(factory.newSubfield('b', getLangStr(pubName)+","));
            hasPubName = true;
        }
        if (!hasPubName) {
            f264.addSubfield(factory.newSubfield('b', "[publisher not identified]"));
        }
        si = main.listProperties(workEvent);
        boolean hasDate = false;
        while (si.hasNext()) {
            final Resource event = si.next().getResource();
            final Resource eventType = event.getPropertyResourceValue(RDF.type);
            if (eventType == null || !eventType.getLocalName().equals("PublishedEvent")) {
                continue;
            }
            final Statement onYearS = event.getProperty(onYear);
            if (onYearS == null) {
                continue;
            }
            int year = onYearS.getInt();
            f264.addSubfield(factory.newSubfield('c', String.valueOf(year)+"."));
            hasDate = true;
        }
        if (!hasDate) {
            f264.addSubfield(factory.newSubfield('c', "[date of publication not identified]"));
        }
        r.addVariableField(f264);
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
        sb.append("     o     000 ||");
        final Statement languageS = main.getProperty(tmpMarcLang);
        if (languageS == null) {
            sb.append(defaultLang);
        } else {
            sb.append(languageS.getString());
        }
        sb.append("od");
        r.addVariableField(factory.newControlField("008", sb.toString()));
    }

    public static void add300(final Model m, final Resource main, final Record r) {
        final Statement extentStatementS = main.getProperty(workExtentStatement);
        if (extentStatementS == null) {
            return;
        }
        String st = extentStatementS.getString();
        System.out.println(st);
        st = st.replace("pp.", "pages");
        st = st.replace("p.", "pages");
        System.out.println(st);
        st = st.replace("ff.", "folios");
        // the s in volume(s) is a bit annoying
        st = st.replaceAll("^1 v\\.", "1 volume");
        st = st.replaceAll("[^0-9]1 v\\.", "1 volume");
        st = st.replaceAll("v\\.", "volumes");
        final DataField f300 = factory.newDataField("300", ' ', ' ');
        f300.addSubfield(factory.newSubfield('a', st));
        r.addVariableField(f300);
    }

    public static void addCreatorName(final Model m, final Resource name, final List<String> nameList) {
        final Literal l = name.getProperty(RDFS.label).getLiteral();
        final String nameStr = getLangStr(l);
        nameList.add(nameStr);
    }

    public static String getCreatorString(final Model m, final Resource creator) {
        // here we want to keep an order among the various names and titles
        // otherwise the output could be inconsistent among queries
        final List<String> names = new ArrayList<>();
        final List<String> otherNames = new ArrayList<>();
        final List<String> titles = new ArrayList<>();
        final List<String> otherTitles = new ArrayList<>();
        StmtIterator si = creator.listProperties(personName);
        while (si.hasNext()) {
            final Resource name = si.next().getResource();
            final Resource type = name.getPropertyResourceValue(RDF.type);
            final String typeLocalName = (type == null) ? "" : type.getLocalName();
            switch(typeLocalName) {
            case "":
                addCreatorName(m, name, otherNames);
                break;
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
                continue;
            }
        }
        Integer birthYear = null;
        Integer deathYear = null;
        si = creator.listProperties(tmpBirthYear);
        while (si.hasNext()) {
            birthYear = si.next().getInt();
        }
        si = creator.listProperties(tmpDeathYear);
        while (si.hasNext()) {
            deathYear = si.next().getInt();
        }
        final StringBuilder sb = new StringBuilder();
        if (!names.isEmpty()) {
            Collections.sort(names);
            sb.append(names.get(0));
        } else if (!otherNames.isEmpty()) {
            Collections.sort(otherNames);
            sb.append(otherNames.get(0));
        }
        if (!titles.isEmpty()) {
            Collections.sort(titles);
            sb.append(" / ");
            sb.append(titles.get(0));
        } else if (!otherTitles.isEmpty()) {
            Collections.sort(otherTitles);
            sb.append(" / ");
            sb.append(otherTitles.get(0));
        }
        if (birthYear == null) {
            if (deathYear != null) {
                sb.append(", ?-");
                sb.append(deathYear);
            }
        } else {
            sb.append(", ");
            sb.append(birthYear);
            sb.append('-');
            if (deathYear != null) {
                sb.append(deathYear);
            }
        }
        return sb.toString();
    }

    public static void addAuthorRel(final Model m, final Resource main, final Record r, final Property prop, final String rel) {
        StmtIterator si = main.listProperties(prop);
        while (si.hasNext()) {
            final Resource creator = si.next().getResource();
            final String creatorStr = getCreatorString(m, creator);
            final DataField f720_1_ = factory.newDataField("720", '1', ' ');
            f720_1_.addSubfield(factory.newSubfield('a', creatorStr));
            f720_1_.addSubfield(factory.newSubfield('e', rel));
            r.addVariableField(f720_1_);
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
                        final boolean lang1ok = lang1.equals(this.bcpTag) || lang1.startsWith(this.bcpTag+'-');
                        final boolean lang2ok = lang2.equals(this.bcpTag) || lang2.startsWith(this.bcpTag+'-');
                        if (lang1ok && !lang2ok) {
                            return -1;
                        }
                        if (!lang1ok && lang2ok) {
                            return 1;
                        }
                    }
                    res = lang1.compareTo(lang2);
                    if (res != 0) return res;
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

    private final static CompareStringLiterals comp = new CompareStringLiterals(null);

    private static String getbcp47lang(final Resource main) {
        final Statement languageS = main.getProperty(tmpBcpLang);
        if (languageS != null) {
            return languageS.getString();
        }
        return null;
    }

    private static void addTitles(final Model m, final Resource main, final Record record, final String bcp47lang) {
        // again, we keep titles in order for consistency among marc queries
        final Map<String,List<Literal>> titles = new TreeMap<>();
        StmtIterator si = main.listProperties(workTitle);
        String subtitleStr = null;
        Integer highestPrio = 999;
        List<Literal> highestPrioList = null;
        while (si.hasNext()) {
            final Resource title = si.next().getResource();
            final Resource type = title.getPropertyResourceValue(RDF.type);
            final String typeLocalName = type.getLocalName();
            StmtIterator labelSi = title.listProperties(RDFS.label);
            while (labelSi.hasNext()) {
                final Literal titleLit = labelSi.next().getLiteral();
                if (typeLocalName.equals("WorkSubtitle")) {
                    subtitleStr = getLangStr(titleLit);
                    continue;
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
        si = main.listProperties(workAuthorshipStatement);
        String authorshipStatement = null;
        while (si.hasNext()) {
            authorshipStatement = getLangStr(si.next().getLiteral());
        }
        final Literal mainTitleL = highestPrioList.get(0);
        highestPrioList.remove(0);
        final DataField f245 = factory.newDataField("245", '0', '0');
        String mainTitleS;
        if (subtitleStr != null || authorshipStatement != null) {
            mainTitleS = getLangStr(mainTitleL) + " / ";
        } else {
            mainTitleS = getLangStr(mainTitleL) + ".";
        }
        f245.addSubfield(factory.newSubfield('a', mainTitleS));
        if (subtitleStr != null) {
            if (authorshipStatement != null) {
                f245.addSubfield(factory.newSubfield('b', subtitleStr+" / "));
            } else {
                f245.addSubfield(factory.newSubfield('b', subtitleStr+"."));
            }
        }
        if (authorshipStatement != null) {
            f245.addSubfield(factory.newSubfield('c', authorshipStatement+"."));
        }
        record.addVariableField(f245);
        for (Entry<String,List<Literal>> e : titles.entrySet()) {
            final MarcInfo mi = titleLocalNameToMarcInfo.get(e.getKey());
            final List<Literal> list = e.getValue();
            Collections.sort(list, compbcp);
            for (Literal l : list) {
                final DataField f246 = factory.newDataField("246", '1', mi.subindex2);
                if (mi.subfieldi != null) {
                    f246.addSubfield(factory.newSubfield('i', mi.subfieldi));
                }
                f246.addSubfield(factory.newSubfield('a', getLangStr(l)));
                record.addVariableField(f246);
            }
        }
    }

    private static void addTopics(final Model m, final Resource main, final Record record) {
        final StmtIterator si = main.listProperties(workIsAbout);
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
        StmtIterator si = main.listProperties(workNumberOf);
        boolean hasSeries = false;
        final DataField f490 = factory.newDataField("490", '0', ' ');
        while (si.hasNext()) {
            final Resource series = si.next().getResource();
            final Literal l = getPreferredLit(series, bcp47lang);
            if (l == null)
                continue;
            hasSeries = true;
            f490.addSubfield(factory.newSubfield('a', getLangStr(l)));
        }
        si = main.listProperties(workSeriesNumber);
        while (si.hasNext()) {
            final Literal series = si.next().getLiteral();
            hasSeries = true;
            f490.addSubfield(factory.newSubfield('v', getLangStr(series)));
        }
        si = main.listProperties(workSeriesName);
        while (si.hasNext()) {
            final Literal series = si.next().getLiteral();
            hasSeries = true;
            f490.addSubfield(factory.newSubfield('a', getLangStr(series)));
        }
        if (hasSeries)
            record.addVariableField(f490);
    }

    private static Literal getPreferredLit(Resource r, final String bcp47lang) {
        final StmtIterator labelSi = r.listProperties(SKOS.prefLabel);
        if (!labelSi.hasNext())
            return null;
        final Map<String,List<Literal>> labels = new TreeMap<>();
        while (labelSi.hasNext()) {
            final Literal topicLabel = labelSi.next().getLiteral();
            final String lng = topicLabel.getLanguage();
            final List<Literal> litList = labels.computeIfAbsent(lng, x -> new ArrayList<>());
            litList.add(topicLabel);
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
        return interestingLiterals.get(0);
    }

    private static String getLangLabel(final OntModel m, final String uri) {
        final Resource main = m.getResource(uri);
        final Resource lang = main.getPropertyResourceValue(language);
        return lang.getProperty(RDFS.label).getString();
    }

    // returns the script or null if the script is deemed unnecessary
    private static String getScriptLabel(final OntModel m, final String uri) {
        Resource main = m.getResource(uri);
        final Resource lang = main.getPropertyResourceValue(language);
        final String langLoc = lang.getLocalName();
        if (langLoc.equals("LangPi") || langLoc.equals("LangSa")) {
            final Resource scriptR = main.getPropertyResourceValue(script);
            return scriptR.getProperty(RDFS.label).getString();
        }
        return null;
    }

    private static void addLanguages(final Model m, final Resource main, final Record record) {
        StmtIterator lsi = main.listProperties(langScript);
        final OntModel ontMod = OntData.ontMod;
        if (ontMod == null) { // during tests
            return;
        }
        final StringBuilder sb = new StringBuilder();
        int nbLangScripts = 0;
        String firstScriptLabel = null;
        while (lsi.hasNext()) {
            final String langScriptUri = lsi.next().getObject().asResource().getURI();
            final String plainEnglish = getLangLabel(ontMod, langScriptUri);
            nbLangScripts += 1;
            if (nbLangScripts == 1) {
                sb.append("In ");
                firstScriptLabel = getScriptLabel(ontMod, langScriptUri);
            } else {
                sb.append(" and ");
            }
            sb.append(plainEnglish);
        }
        lsi = main.listProperties(otherLangScript);
        while (lsi.hasNext()) {
            final String langScriptUri = lsi.next().getObject().asResource().getURI();
            final String plainEnglish = getLangLabel(ontMod, langScriptUri);
            nbLangScripts += 1;
            if (nbLangScripts == 1) {
                sb.append("In ");
                firstScriptLabel = getScriptLabel(ontMod, langScriptUri);
            } else {
                sb.append(" and ");
            }
            sb.append(plainEnglish);
        }
        if (nbLangScripts == 0)
            return;
        final DataField f546 = factory.newDataField("546", ' ', ' ');
        if (nbLangScripts == 1 && firstScriptLabel != null) {
            f546.addSubfield(factory.newSubfield('a', sb.toString()+';'));
            f546.addSubfield(factory.newSubfield('b', firstScriptLabel+" script."));
        } else {
            sb.append('.');
            f546.addSubfield(factory.newSubfield('a', sb.toString()));
        }
        record.addVariableField(f546);
    }

    private static void addOutline(final Model m, final Resource main, final Record record, final String bcp47lang) {
        final StmtIterator si = main.listProperties(workHasPart);
        final StringBuilder sb = new StringBuilder();
        final Map<String,Literal> parts = new TreeMap<>();
        boolean hasParts = false;
        // we keep the parts in order
        while (si.hasNext()) {
            final Resource part = si.next().getResource();
            final Statement indexTreeS = part.getProperty(workPartTreeIndex);
            if (indexTreeS == null)
                continue;
            final Literal l = getPreferredLit(part, bcp47lang);
            if (l == null)
                continue;
            final String indexTree = indexTreeS.getString();
            parts.put(indexTree, l);
            hasParts = true;
        }
        if (!hasParts)
            return;
        final DataField f505 = factory.newDataField("505", '0', ' ');
        boolean first = true;
        for (Literal l : parts.values()) {
            if (!first)
                sb.append(" -- ");
            sb.append(getLangStr(l));
            first = false;
        }
        sb.append('.');
        f505.addSubfield(factory.newSubfield('a', sb.toString()));
        record.addVariableField(f505);
    }

    public static void addAuthors(final Model m, final Resource main, final Record r) {
        addAuthorRel(m, main, r, creatorTerton, "author.");
        addAuthorRel(m, main, r, creatorMainAuthor, "author.");
        addAuthorRel(m, main, r, creatorTranslator, "translator.");
        addAuthorRel(m, main, r, creatorIndicScholar, "consultant.");
        addAuthorRel(m, main, r, creatorContributingAuthor, "contributor.");
        addAuthorRel(m, main, r, creatorCommentator, "commentator for written text.");
        addAuthorRel(m, main, r, creatorCompiler, "editor.");
        addAuthorRel(m, main, r, creatorReviser, "corrector.");
        addAuthorRel(m, main, r, creatorScribe, "scribe.");
        addAuthorRel(m, main, r, creatorCalligrapher, "calligrapher.");
        addAuthorRel(m, main, r, creatorArtist, "artist.");
    }

    public static Record marcFromModel(final Model m, final Resource main) {
        final Record record = factory.newRecord(leader);
        record.addVariableField(factory.newControlField("001", "(BDRC) "+main.getURI()));
        // maybe something like that could work?
        //record.addVariableField(factory.newControlField("003", "BDRC"));
        record.addVariableField(f006);
        record.addVariableField(f007);
        add008(m, main, record);
        addIsbn(m, main, record); // 020
        record.addVariableField(f040);
        StmtIterator si = main.listProperties(workLcCallNumber);
        while (si.hasNext()) {
            String lccn = si.next().getLiteral().getString();
            // see https://github.com/BuddhistDigitalResourceCenter/xmltoldmigration/issues/55
            lccn = lccn.toUpperCase();
            final int firstSpaceIdx = lccn.indexOf(' ');
            if (firstSpaceIdx == -1)
                continue;
            final String classNumber = lccn.substring(0, firstSpaceIdx);
            final String cutterNumber = lccn.substring(firstSpaceIdx+1);
            final DataField f050__4 = factory.newDataField("050", ' ', '4');
            f050__4.addSubfield(factory.newSubfield('a', classNumber));
            f050__4.addSubfield(factory.newSubfield('b', cutterNumber));
            record.addVariableField(f050__4);
        }
        final String bcp47lang = getbcp47lang(main);
        if (bcp47lang == null) {
            log.error("no bcp47 lang tag returned for "+main.getLocalName());
        }
        addTitles(m, main, record, bcp47lang); // 245
        // edition statement
        si = main.listProperties(workEditionStatement);
        while (si.hasNext()) {
            final Literal editionStatement = si.next().getLiteral();
            final DataField f250 = factory.newDataField("250", ' ', ' ');
            f250.addSubfield(factory.newSubfield('a', getLangStr(editionStatement)+"."));
            record.addVariableField(f250);
        }
        addPubInfo(m, main, record); // 264
        add300(m, main, record);
        record.addVariableField(f336);
        record.addVariableField(f337);
        record.addVariableField(f338);
        addSeries(m, main, record, bcp47lang); // 490
        // Columbia requested that 546 be the first 5xx field
        addLanguages(m, main, record); // 546
        // biblio note
        si = main.listProperties(workBiblioNote);
        while (si.hasNext()) {
            final Literal biblioNote = si.next().getLiteral();
            final DataField f500 = factory.newDataField("500", ' ', ' ');
            String biblioNoteS = getLangStr(biblioNote);
            if (!biblioNoteS.endsWith(".")) {
                biblioNoteS += ".";
            }
            f500.addSubfield(factory.newSubfield('a', biblioNoteS));
            record.addVariableField(f500);
        }
        addOutline(m, main, record, bcp47lang); // 505
        addAccess(m, main, record); // 506
        // catalog info (summary)
        si = main.listProperties(workCatalogInfo);
        while (si.hasNext()) {
            final Literal catalogInfo = si.next().getLiteral();
            final DataField f520 = factory.newDataField("520", ' ', ' ');
            f520.addSubfield(factory.newSubfield('a', getLangStr(catalogInfo)));
            record.addVariableField(f520);
        }
        record.addVariableField(f533);
        final Resource license = main.getPropertyResourceValue(admLicense);
        if (license != null && license.getLocalName().equals("LicensePublicDomain")) {
            record.addVariableField(f542_PD);
        }
        final DataField f588 = factory.newDataField("588", ' ', ' ');
        final Date curDate = new Date();
        String dateStr = dateFormat.format(curDate);
        f588.addSubfield(factory.newSubfield('a', "Description based on online resource (BDRC, viewed "+dateStr+")"));
        record.addVariableField(f588);
        addTopics(m, main, record); // 653
        record.addVariableField(f710_2);
        addAuthors(m, main, record); // 720
        // lccn
        si = main.listProperties(workLccn);
        while (si.hasNext()) {
            final String lccn = si.next().getLiteral().getString();
            final DataField f776_08 = factory.newDataField("776", '0', '8');
            f776_08.addSubfield(factory.newSubfield('w', "(DLC)   [LCCN] "+lccn));
            f776_08.addSubfield(factory.newSubfield('i', "Electronic reproduction of (manifestation)"));
            record.addVariableField(f776_08);
        }
        final DataField f856 = factory.newDataField("856", '4', '0');
        f856.addSubfield(factory.newSubfield('u', main.getURI()));
        f856.addSubfield(factory.newSubfield('z', "Available from BDRC"));
        record.addVariableField(f856);
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
        // should be temporary
        if (main.getLocalName().startsWith("W1FPL")) {
            model.add(main, tmpMarcLang, "pli");
            model.add(main, tmpBcpLang, "pi");
            model.add(main, langScript, model.createResource(BDR+"PiMymr"));
        }
        // this should be correct but breaks W2DB4598 because of poorly encoded series data
        //        if (main.hasProperty(hasExpression)) {
        //            throw new RestException(404, new LdsError(LdsError.NO_GRAPH_ERR).setMsg("Resource is an abstract Work"));
        //        }st
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
