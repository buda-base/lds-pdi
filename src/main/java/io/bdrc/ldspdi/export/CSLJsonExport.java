package io.bdrc.ldspdi.export;

import java.time.LocalDate;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.bdrc.ewtsconverter.EwtsConverter;
import io.bdrc.ewtsconverter.TransConverter;
import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.sparql.QueryProcessor;

/*
 * Here's some knowledge that can be useful when dealing with libraries:
 *
 * https://aurimasv.github.io/z2csl/typeMap.xml
 * 
 * we use two types:
 *   book
 *   bookSection
 */

public class CSLJsonExport {

    public final static Logger log = LoggerFactory.getLogger(CSLJsonExport.class);
    
    public static final EwtsConverter ewtsConverter = new EwtsConverter();
    public static final Property inRootInstance = ResourceFactory.createProperty(MarcExport.BDO + "inRootInstance");
    public static final Property numberOfVolumes = ResourceFactory.createProperty(MarcExport.BDO + "numberOfVolumes");
    public static final Property instanceHasVolume = ResourceFactory.createProperty(MarcExport.BDO + "instanceHasVolume");
    public static final Property instanceReproductionOf = ResourceFactory.createProperty(MarcExport.BDO + "instanceReproductionOf");
    public static final Property partType = ResourceFactory.createProperty(MarcExport.BDO + "partType");
    
    public static final String ewtsToBo(String ewts) {
        if (ewts.startsWith("*"))
            ewts = ewts.substring(1);
        String unicode = ewtsConverter.toUnicode(ewts);
        return unicode;
    }
    
    public static final String ewtsToLatn(String ewts) {
        if (ewts.startsWith("*"))
            ewts = ewts.substring(1);
        String alalc = TransConverter.ewtsToAlalc(ewts, true);
        alalc = alalc.replace("u0fbe", "x");
        return StringUtils.capitalize(alalc.replace('-', ' ').trim());
    }
    
    public static final class FieldInfo {
        public String label_latn = null;
        public String label_bo = null;
        public String label_bo_latn = null;
        public String label_en = null;
        public String label_sa_tibt = null;
        public String label_sa_tibt_latn = null;
        public String label_sa_latn = null;
        public String label_sa_latn_ndia = null;
        public String label_zh = null;
        public String label_zh_pinyin = null;

        public FieldInfo() {}
        
        public void addFromLiteral(final Literal l, final boolean replaceIfPresent) {
            final String lang = l.getLanguage();
            switch(lang) {
            case "bo":
            case "dz":
                if (replaceIfPresent || this.label_bo == null)
                    this.label_bo = l.getString();
                break;
            case "bo-x-ewts":
            case "dz-x-ewts":
                if (replaceIfPresent || this.label_bo_latn == null) {
                    this.label_bo_latn = ewtsToLatn(l.getString());
                }
                if (replaceIfPresent || this.label_bo == null)
                    this.label_bo = ewtsToBo(l.getString());
                break;
            case "sa-x-ewts":
                if (replaceIfPresent || this.label_sa_tibt_latn == null)
                    this.label_sa_tibt_latn = ewtsToLatn(l.getString());
                if (replaceIfPresent || this.label_bo == null)
                    this.label_sa_tibt = ewtsToBo(l.getString());
                break;
            case "bo-alalc97":
                if (replaceIfPresent || this.label_bo_latn == null)
                    this.label_bo_latn = StringUtils.capitalize(l.getString());
                break;
            case "sa-alalc97":
                if (replaceIfPresent || this.label_sa_latn == null)
                    this.label_sa_latn = StringUtils.capitalize(l.getString());
                break;
            case "sa-x-iast":
                if (replaceIfPresent || this.label_sa_latn == null)
                    this.label_sa_latn = StringUtils.capitalize(l.getString());
                break;
            case "sa-x-ndia":
                if (replaceIfPresent || this.label_sa_latn_ndia == null)
                    this.label_sa_latn_ndia = StringUtils.capitalize(l.getString());
                break;
            case "zh-hans":
                if (replaceIfPresent || this.label_zh == null)
                    this.label_zh = StringUtils.capitalize(l.getString());
                break;
            case "zh-latn-pinyin":
                if (replaceIfPresent || this.label_zh_pinyin == null)
                    this.label_zh_pinyin = StringUtils.capitalize(l.getString());
                break;
            case "en":
            case "en-x-mixed":
                if (replaceIfPresent || this.label_en == null)
                    this.label_en = StringUtils.capitalize(l.getString());
                break;
            default:
                log.debug("ignoring lang tag "+lang);
            }
        }
        
        public void fill_missing() {
            // the three fields we're going to use are:
            // - bo
            // - bo_latn
            // - en
            // - zh
            // let's fill bo first:
            fill_missing_bo();
            fill_missing_latn();
            fill_missing_en();
            fill_missing_zh();
        }
        
        public void fill_missing_bo() {
            if (this.label_bo != null)
                return;
            if (this.label_sa_tibt != null) {
                this.label_bo = this.label_sa_tibt;
                return;
            }
            if (this.label_zh != null) {
                this.label_bo = this.label_sa_tibt;
                return;
            }
            if (this.label_sa_latn != null) {
                this.label_bo = this.label_sa_latn;
                return;
            }
            if (this.label_sa_latn_ndia != null) {
                this.label_bo = this.label_sa_latn_ndia;
                return;
            }
            if (this.label_en != null) {
                this.label_bo = this.label_en;
                return;
            }
        }
        
        public void fill_missing_latn() {
            if (this.label_latn != null)
                return;
            if (this.label_sa_latn != null) {
                this.label_latn = this.label_sa_latn;
                return;
            }
            if (this.label_sa_latn_ndia != null) {
                this.label_latn = this.label_sa_latn_ndia;
                return;
            }
            if (this.label_bo_latn != null) {
                this.label_latn = this.label_bo_latn;
                return;
            }
            if (this.label_sa_tibt_latn != null) {
                this.label_latn = this.label_sa_tibt_latn;
                return;
            }
            if (this.label_zh_pinyin != null) {
                this.label_latn = this.label_zh_pinyin;
                return;
            }
            if (this.label_en != null) {
                this.label_latn = this.label_en;
                return;
            }
            if (this.label_zh != null) {
                this.label_latn = this.label_zh;
                return;
            }
        }

        public void fill_missing_en() {
            if (this.label_en != null)
                return;
            this.label_en = this.label_latn;
        }
        
        public void fill_missing_zh() {
            if (this.label_zh != null)
                return;
            if (this.label_zh_pinyin != null) {
                this.label_zh = this.label_zh_pinyin;
                return;
            }
            if (this.label_sa_latn != null) {
                this.label_zh = this.label_sa_latn;
                return;
            }
            if (this.label_bo != null) {
                this.label_zh = this.label_bo;
                return;
            }
            if (this.label_sa_tibt != null) {
                this.label_zh = this.label_sa_tibt;
                return;
            }
            if (this.label_en != null) {
                this.label_zh = this.label_en;
                return;
            }
            if (this.label_latn != null) {
                this.label_zh = this.label_latn;
                return;
            }
        }
        
    }
    
    public static final FieldInfo getEntityLabelField(Model m, Resource r, final boolean followSameAs, final boolean lookAtAltLabels) {
        FieldInfo fi = new FieldInfo();
        addEntityLabels(m, r, fi, lookAtAltLabels);
        if (followSameAs) {
            NodeIterator si = m.listObjectsOfProperty(r, OWL.sameAs);
            while (si.hasNext()) {
                Resource sa = si.next().asResource();
                addEntityLabels(m, sa, fi, lookAtAltLabels);
            }
        }
        fi.fill_missing();
        return fi;
    }
    
    public static final void addEntityLabels(Model m, Resource r, final FieldInfo fi, final boolean lookAtAltLabels) {
        NodeIterator si = m.listObjectsOfProperty(r, SKOS.prefLabel);
        while (si.hasNext()) {
            Literal l = si.next().asLiteral();
            fi.addFromLiteral(l, false);
        }
        if (!lookAtAltLabels)
            return;
        si = m.listObjectsOfProperty(r, SKOS.altLabel);
        while (si.hasNext()) {
            Literal l = si.next().asLiteral();
            fi.addFromLiteral(l, false);
        }
    }
    
    public static final FieldInfo getLabelFieldForLiteral(Literal l) {
        FieldInfo fi = new FieldInfo();
        fi.addFromLiteral(l, false);
        fi.fill_missing();
        return fi;
    }

    public static Model getModelForCSL(final String resUri) throws RestException {
        Model model = QueryProcessor.getSimpleResourceGraph(resUri, "resForCSL.arq");
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
        return model;
    }
    
    public static class CSLResObj {
        @JsonIgnore
        public ObjectMapper mapper = new ObjectMapper();
        public ObjectNode bo;
        public ObjectNode latn;
        public ObjectNode en;
        public ObjectNode zh;
        
        CSLResObj() {
            this.bo = mapper.createObjectNode();
            this.latn = mapper.createObjectNode();
            this.en = mapper.createObjectNode();
            this.zh = mapper.createObjectNode();
            final LocalDate now = LocalDate.now();
            final ArrayNode components = mapper.createArrayNode();
            components.add(now.getYear());
            components.add(now.getMonthValue());
            components.add(now.getDayOfMonth());
            ObjectNode acc = this.bo.putObject("accessed");
            ArrayNode dateparts = acc.putArray("date-parts");
            dateparts.add(components);
            acc = this.zh.putObject("accessed");
            dateparts = acc.putArray("date-parts");
            dateparts.add(components);
            acc = this.en.putObject("accessed");
            dateparts = acc.putArray("date-parts");
            dateparts.add(components);
            acc = this.latn.putObject("accessed");
            dateparts = acc.putArray("date-parts");
            dateparts.add(components);
        }
        
        public void addCommonField(final String fieldName, final String value) {
            this.bo.put(fieldName, value);
            this.zh.put(fieldName, value);
            this.en.put(fieldName, value);
            this.latn.put(fieldName, value);
        }
        
        public void addSimpleFieldInfo(final String fieldName, final FieldInfo fi) {
            this.bo.put(fieldName, fi.label_bo);
            this.zh.put(fieldName, fi.label_zh);
            this.en.put(fieldName, fi.label_en);
            this.latn.put(fieldName, fi.label_latn);
        }
    }
    
    public static void addDirectLangField(final CSLResObj res, final String fieldName, final Model m, final Resource r, final Property p) {
        final NodeIterator si = m.listObjectsOfProperty(r, p);
        final FieldInfo fi = new FieldInfo();
        while (si.hasNext()) {
            fi.addFromLiteral(si.next().asLiteral(), false);
        }
        fi.fill_missing();
        if (fi.label_bo == null)
            return;
        res.addSimpleFieldInfo(fieldName, fi);
    }
    
    public static Resource getImageReproduction(final Model m, final Resource root) {
        final StmtIterator si = root.listProperties(instanceHasVolume);
        while (si.hasNext()) {
            Resource potentialIinstance = si.next().getResource();
            final StmtIterator typeIt = potentialIinstance.listProperties(RDF.type);
            while (typeIt.hasNext()) {
                Resource type = si.next().getResource();
                if (type.getURI().equals(MarcExport.BDO+"ImageInstance"))
                    return potentialIinstance;
            }
        }
        return null;
    }
    
    public static void addSection(final CSLResObj res, final Model m, final Resource r) {
        Resource parent = r.getPropertyResourceValue(MarcExport.partOf);
        while (parent != null) {
            Resource partTypeR = parent.getPropertyResourceValue(partType);
            if (partTypeR != null && partTypeR.getLocalName().equals("PartTypeSection")) {
                FieldInfo fi = getEntityLabelField(m, parent, false, true);
                res.addSimpleFieldInfo("section", fi);
                break;
            }
            parent = parent.getPropertyResourceValue(MarcExport.partOf);
        }
    }
    
    public static FieldInfo fiBDRC = new FieldInfo();
    static {
        fiBDRC.label_bo = "ནང་བསྟན་དཔེ་ཚོགས་ལྟེ་གནས།（BDRC）";
        fiBDRC.label_en = "Buddhist Digital Resource Center (BDRC)";
        fiBDRC.label_latn = "Buddhist Digital Resource Center (BDRC)";
        fiBDRC.label_zh = "佛教数字资源中心（BDRC）";
    }
    
    public static CSLResObj getObject(final Model m, final Resource r) {
        CSLResObj res = new CSLResObj();
        Resource root = r.getPropertyResourceValue(inRootInstance);
        
        res.addCommonField("url", r.getURI());
        res.addCommonField("id", "bdr:"+r.getLocalName());
        res.addSimpleFieldInfo("source", fiBDRC);
        FieldInfo fi = getEntityLabelField(m, r, false, true);
        res.addSimpleFieldInfo("title", fi);
        if (root == null) {
            Resource repOf = r.getPropertyResourceValue(instanceReproductionOf);
            if (repOf != null)
                root = repOf;
            res.addCommonField("type", "book");
            // getting title:
            
        } else {
            res.addCommonField("type", "chapter");
            addSection(res, m, r);
            fi = getEntityLabelField(m, root, false, true);
            res.addSimpleFieldInfo("container-title", fi);
        }

        int volnum = 0;
        Statement nbvolLitSt = root.getProperty(numberOfVolumes);
        if (nbvolLitSt != null) {
            volnum = nbvolLitSt.getInt();
        }
        if (volnum == 0) {
            Resource iinstance = getImageReproduction(m, root);
            StmtIterator ni = iinstance.listProperties(instanceHasVolume);
            for ( ; ni.hasNext() ; ++volnum ) ni.next();
        }
        if (volnum != 0) {
            res.addCommonField("number-of-volumes", String.valueOf(volnum));
        }
            
        // publisher name
        addDirectLangField(res, "publisher", m, root, MarcExport.publisherName);
        addDirectLangField(res, "publisher-place", m, root, MarcExport.publisherLocation);
        addDirectLangField(res, "edition", m, root, MarcExport.editionStatement);
        
        return res;
    }
    
    public static ResponseEntity<CSLResObj> getResponse(final String resUri) throws RestException, JsonProcessingException {
        // I really don't like that but making that better would mean either:
        // - a very weird and probably slower SPARQL query
        // - two queries
        // The idea is that we're sending MARC records for items to Columbia,
        // as they only want "electronic resources" records, and electronic
        // resources are imageinstances (scans) in our system, not regular instances.
        boolean scansMode = resUri.startsWith(MarcExport.ScanUriPrefix);
        final Model m;
        final Resource main;

        m = getModelForCSL(resUri);
        main = m.getResource(resUri);
        
        CSLResObj res = getObject(m, main);
        
        // add stuff
        
        return ResponseEntity.ok().header("Allow", "GET, OPTIONS, HEAD").header("Vary", "Negotiate, Accept").contentType(MediaType.APPLICATION_JSON).body(res);

    }
    
}
