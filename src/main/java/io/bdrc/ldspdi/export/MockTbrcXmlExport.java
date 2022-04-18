package io.bdrc.ldspdi.export;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import io.bdrc.ldspdi.exceptions.RestException;

public class MockTbrcXmlExport {
    
    public static final String workNs = "http://www.tbrc.org/models/work#";
    public final static Logger log = LoggerFactory.getLogger(MockTbrcXmlExport.class);
    public static final Property digitalLendingPossible = ResourceFactory.createProperty(MarcExport.BDO + "digitalLendingPossible");

    public static class InfoForXML {
        public String status = "editing";
        public String access = "restrictedByTbrc";
        public String license = "copyright";
        public String cdl = "ok";
        public String copyrightStatus = "PublicDomain";
    }
    
    public static final Map<String,String> bdoAccessToTbrcAccess = new HashMap<>();
    public static final Map<String,String> bdoStatusToTbrcStatus = new HashMap<>();
    public static final Map<String,String> bdoAccessToTbrcLicense = new HashMap<>();
    public static final Map<String,String> bdoCsToStr = new HashMap<>();
    
    static {
        bdoAccessToTbrcAccess.put("AccessFairUse", "fairUse");
        bdoAccessToTbrcAccess.put("AccessOpen", "openAccess");
        bdoAccessToTbrcAccess.put("AccessRestrictedByTbrc", "restrictedByTbrc");
        bdoAccessToTbrcAccess.put("AccessRestrictedSealed", "restrictedSealed");
        bdoAccessToTbrcAccess.put("AccessRestrictedTemporarily", "temporarilyRestricted");
        bdoAccessToTbrcLicense.put("AccessFairUse", "copyright");
        bdoAccessToTbrcLicense.put("AccessOpen", "ccby");
        bdoAccessToTbrcLicense.put("AccessRestrictedByTbrc", "copyright");
        bdoAccessToTbrcLicense.put("AccessRestrictedSealed", "copyright");
        bdoAccessToTbrcLicense.put("AccessRestrictedTemporarily", "copyright");
        bdoStatusToTbrcStatus.put("StatusEditing", "editing");
        bdoStatusToTbrcStatus.put("StatusOnHold", "onHold");
        bdoStatusToTbrcStatus.put("StatusProvisional", "provisional");
        bdoStatusToTbrcStatus.put("StatusReleased", "released");
        bdoStatusToTbrcStatus.put("StatusWithdrawn", "withdrawn");
        bdoCsToStr.put("CopyrightClaimed", "incopyright");
        bdoCsToStr.put("CopyrightInCopyright", "incopyright");
        bdoCsToStr.put("CopyrightPublicDomain", "publicdomain");
        bdoCsToStr.put("CopyrightUndetermined", "undetermined");
    }
    
    public static InfoForXML infoForXML(final Model m, final Resource main) {
        InfoForXML res = new InfoForXML();
        final Resource accessVal = main.getPropertyResourceValue(MarcExport.access);
        if (accessVal != null) {
            res.access = bdoAccessToTbrcAccess.getOrDefault(accessVal.getLocalName(), "restrictedByTbrc");
            res.license = bdoAccessToTbrcLicense.getOrDefault(accessVal.getLocalName(), "copyright");
        }
        final Resource statusVal = main.getPropertyResourceValue(MarcExport.tmpStatus);
        if (statusVal != null)
            res.status = bdoStatusToTbrcStatus.getOrDefault(statusVal.getLocalName(), "onHold");
        // IA's script doesn't look at the status, so the idea is to mark the unreleased items as restricted
        if (!res.status.equals("released"))
            res.access = "restrictedByTbrc";
        final Statement ricS = main.getProperty(MarcExport.restrictedInChina);
        if (ricS != null && ricS.getBoolean() == true) 
            res.access = "restrictedInChina";
        final Resource cs = main.getPropertyResourceValue(MarcExport.copyrightStatus);
        if (cs != null) {
            res.copyrightStatus = bdoCsToStr.getOrDefault(cs.getLocalName(), "incopyright");
            // if a book has been determined as being in copyright but is put in open access on our website
            // we still want to restrict it on IA
            if (res.copyrightStatus.equals("incopyright")) {
                if (res.access.equals("openAccess"))
                    res.access = "fairUse";
                if (res.license.equals("ccby"))
                    res.license = "copyright";
            }
        } else {
            res.copyrightStatus = "publicdomain";
        }
        // introducing a new access value (fairUse-nocdl) and a new license value (copyright-nocdl)
        final Statement dlpS = main.getProperty(digitalLendingPossible);
        if (dlpS != null && dlpS.getBoolean() == false)
            res.cdl = "nok";
        return res;
    }
    
    public static ResponseEntity<StreamingResponseBody> getResponse(final String resUri) throws RestException {
        if (!resUri.startsWith(MarcExport.ScanUriPrefix))
            return ((BodyBuilder) ResponseEntity.notFound().header("Allow", "GET, OPTIONS, HEAD").header("Vary", "Negotiate, Accept")).body(null);
        final Model m = MarcExport.getModelForMarc(resUri);
        final Resource main = m.getResource(resUri);
        
        final InfoForXML info = infoForXML(m, main);
        
        final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        final DocumentBuilder docBuilder;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            throw new RestException(500, 500, "can't configure xml builder: "+e1.getLocalizedMessage());
        }

        // root elements
        final Document doc = docBuilder.newDocument();
        final Element rootElement = doc.createElementNS(workNs, "work:work");
        rootElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:work", workNs);
        rootElement.setAttribute("RID", main.getLocalName());
        //rootElement.setAttribute("status", info.status);
        doc.appendChild(rootElement);

        final Element archiveInfoElt = doc.createElementNS(workNs, "work:archiveInfo");
        rootElement.appendChild(archiveInfoElt);
        archiveInfoElt.setAttribute("access", info.access);
        archiveInfoElt.setAttribute("license", info.license);
        archiveInfoElt.setAttribute("cdl", info.cdl);
        //archiveInfoElt.setAttribute("copyrightStatus", info.copyrightStatus);
        
        final StreamingResponseBody stream = new StreamingResponseBody() {
            @Override
            public void writeTo(final OutputStream os) throws IOException {
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                try {
                    Transformer transformer = transformerFactory.newTransformer();
                    DOMSource source = new DOMSource(doc);
                    StreamResult result = new StreamResult(os);
                    transformer.transform(source, result);
                } catch (TransformerException e) {
                    log.error("could not serialize xml", e);
                }
            }
        };
        return ResponseEntity.ok().header("Allow", "GET, OPTIONS, HEAD").header("Vary", "Negotiate, Accept").contentType(MediaType.APPLICATION_XML).body(stream);
    }
    
}
