package io.bdrc.ldspdi.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.bdrc.formatters.TTLRDFWriter;
import io.bdrc.ldspdi.utils.BudaMediaTypes;
import io.bdrc.ldspdi.utils.MediaTypeUtils;

@RestController
@RequestMapping("/")
public class TestResource {

    public final static Logger log = LoggerFactory.getLogger(TestResource.class.getName());
    public final String PUBLIC_GROUP = "public";
    public final String ADMIN_GROUP = "admin";
    public final String STAFF_GROUP = "staff";
    public final String READ_PUBLIC_ROLE = "readpublic";
    public final String READ_ONLY_PERM = "readonly";
    public final String READ_PRIVATE_PERM = "readprivate";

    @GetMapping("auth/public")
    public ResponseEntity<String> authPublicGroupTest() {
        System.out.println("Call to auth/public >>>>>>>>>");
        return ResponseEntity.status(200).header("Content-Type", "text/html").body("test auth public done");
    }

    @GetMapping("auth/rdf/admin")
    public ResponseEntity<String> authPrivateResourceAccessTest() {
        System.out.println("auth/ref/admin >>>>>>>>> ");
        return ResponseEntity.status(200).header("Content-Type", "text/html").body("test auth public done");
    }

    @GetMapping("ontology/{ont}")
    public ResponseEntity<StreamingResponseBody> getOntology(@RequestHeader(value = "Accept", required = false) String format, @PathVariable("ont") String ont) {
        System.out.println("{ont} >>>>>>>>> " + ont + " Accept : " + format);
        InputStream str = OntServiceTest.class.getClassLoader().getResourceAsStream("ttl/" + ont + ".ttl");
        System.out.println("STREAM >>>>>>>>> " + ont + " Accept : " + format);
        Model tmp = ModelFactory.createDefaultModel();
        OntDocumentManager dm = OntModelSpec.OWL_MEM.getDocumentManager();
        OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, tmp);
        om.read(str, "", "TURTLE");
        dm.loadImports(om);
        MediaType mediaType = null;
        StreamingResponseBody stream = null;
        ResponseBuilder builder = null;
        if (format != null) {
            mediaType = BudaMediaTypes.selectVariant(format, BudaMediaTypes.resVariants);
            if (mediaType == null) {
                // return Response.status(406).build();
                return null;
            }
            // browser request : serving html page
            if (mediaType.equals(MediaType.TEXT_HTML)) {
                // builder = Response.ok(new Viewable("/ontologyHome.jsp", mod));
            } else {
                final String JenaLangStr = MediaTypeUtils.getJenaFromExtension(BudaMediaTypes.getExtFromMime(mediaType));
                stream = new StreamingResponseBody() {
                    @Override
                    public void writeTo(OutputStream os) throws IOException, WebApplicationException {
                        if (JenaLangStr == "STTL") {
                            final RDFWriter writer = TTLRDFWriter.getSTTLRDFWriter(om, "");
                            writer.output(os);
                        } else {
                            org.apache.jena.rdf.model.RDFWriter wr = om.getWriter(JenaLangStr);
                            if (JenaLangStr.equals(RDFLanguages.strLangRDFXML)) {
                                wr.setProperty("xmlbase", "");
                            }
                            // here using the absolute path as baseUri since it has been recognized
                            // as the base uri of a declared ontology (in ontologies.yml file)
                            wr.write(om, os, "");
                        }
                    }
                };

            }
        }
        return ResponseEntity.ok().contentType(mediaType).body(stream);
    }

}
