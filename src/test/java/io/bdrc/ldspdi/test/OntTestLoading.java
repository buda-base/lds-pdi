package io.bdrc.ldspdi.test;

import java.io.FileWriter;
import java.util.Iterator;

import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OntTestLoading {

    public static Logger logger = LoggerFactory.getLogger(OntTestLoading.class);

    private static final String ONT_POLICY = "https://raw.githubusercontent.com/buda-base/editor-templates/master/ont-policy.rdf";
    private static final String OUT = System.getProperty("user.dir") + "/";
    private static final String DS = "http://buda1.bdrc.io:13180/fuseki/newcorerw/";

    private static RDFConnection fuConn;

    private static final String BDG = "http://purl.bdrc.io/graph/";
    private static final String SHAPE_ONT = "http://purl.bdrc.io/shapes/core/PersonShapes/";

    private static void doTest(OntModelSpec oms, OntDocumentManager odm, String ontUri, boolean processImports, String graphLocalName,
            boolean loadImports) {
        logger.info("processing {} imports {}", graphLocalName, processImports);
        logger.info("OUT DIR {} ", OUT);

        odm.setProcessImports(processImports);
        /****************************************************************************************************/

        Iterator<String> it = odm.listDocuments();
        OntModel om = null;
        while (it.hasNext()) {
            String uri = it.next();
            if (uri.equals(SHAPE_ONT)) {
                om = odm.getOntology(uri, oms);
            } else {
                odm.getOntology(uri, oms);
            }
        }

        try {
            logger.info("WRITING 1 to {} ", OUT + graphLocalName + "_ONT_MOD_GET_ONT.ttl");
            om.write(new FileWriter(OUT + graphLocalName + "_ONT_MOD_GET_ONT.ttl"), "TTL");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        String graphName = BDG + graphLocalName;
        fuConn.put(graphName, om);

        Model mFmFu = fuConn.fetch(graphName);

        try {
            logger.info("WRITING 2 to {} ", OUT + graphLocalName + "_ONT_MOD_GET_ONT.ttl");
            mFmFu.write(new FileWriter(OUT + graphLocalName + "_MODEL_FM_FUSEKI.ttl"), "TTL");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        /****************************************************************************************************/
        OntModel omFmFu = ModelFactory.createOntologyModel();
        omFmFu.add(mFmFu);
        try {
            logger.info("WRITING 3 to {} ", OUT + graphLocalName + "_ONT_MOD_GET_ONT.ttl");
            omFmFu.write(new FileWriter(OUT + graphLocalName + "_ONT_MODEL_FM_FUSEKI.ttl"), "TTL");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (loadImports) {
            om.loadImports();
            try {
                logger.info("WRITING 4 to {} ", OUT + graphLocalName + "_ONT_MOD_GET_ONT.ttl");
                om.write(new FileWriter(OUT + graphLocalName + "_ONT_MOD_LOAD_IMP.ttl"), "TTL");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {

        fuConn = RDFConnectionFuseki.create().destination(DS).build();

        OntModelSpec oms = new OntModelSpec(OntModelSpec.OWL_MEM);
        OntDocumentManager odm = new OntDocumentManager(ONT_POLICY);
        oms.setDocumentManager(odm);

        //
        // PROCESS IMPORTS FALSE
        //
        doTest(oms, odm, SHAPE_ONT, false, "PersonShapes_NI8", true);

        //
        // PROCESS IMPORTS TRUE
        //

        // It appears that the oms and odm need to be recreated otherwise the imports
        // are not loaded
        // when the OntModel is put to Fuseki
        oms = new OntModelSpec(OntModelSpec.OWL_MEM);
        odm = new OntDocumentManager(ONT_POLICY);
        oms.setDocumentManager(odm);

        doTest(oms, odm, SHAPE_ONT, true, "PersonShapes_WI8", true);
    }
}
