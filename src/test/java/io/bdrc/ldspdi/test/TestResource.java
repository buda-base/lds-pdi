package io.bdrc.ldspdi.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Variant;

import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.formatters.TTLRDFWriter;
import io.bdrc.ldspdi.utils.MediaTypeUtils;

@Path("/")
public class TestResource {

    public final static Logger log = LoggerFactory.getLogger(TestResource.class.getName());
    public final String PUBLIC_GROUP = "public";
    public final String ADMIN_GROUP = "admin";
    public final String STAFF_GROUP = "staff";
    public final String READ_PUBLIC_ROLE = "readpublic";
    public final String READ_ONLY_PERM = "readonly";
    public final String READ_PRIVATE_PERM = "readprivate";

    @GET
    @Path("auth/public")
    public Response authPublicGroupTest(@Context ContainerRequestContext crc) {
        return Response.status(200).entity("test auth public done").header("Content-Type", "text/html").build();
    }

    @GET
    @Path("auth/rdf/admin")
    public Response authPrivateResourceAccessTest(@Context ContainerRequestContext crc) {
        System.out.println("auth/ref/admin >>>>>>>>> " + crc.getProperty("access"));
        return Response.status(200).entity("test auth public done").header("Content-Type", "text/html").build();
    }

    @GET
    @Path("ontology/{ont}")
    public Response getOntology(@HeaderParam("Accept") String format, @Context Request request, @PathParam("ont") String ont) {
        System.out.println("{ont} >>>>>>>>> " + ont + " Accept : " + format);
        InputStream str = OntServiceTest.class.getClassLoader().getResourceAsStream("ttl/" + ont + ".ttl");
        System.out.println("STREAM >>>>>>>>> " + ont + " Accept : " + format);
        Model tmp = ModelFactory.createDefaultModel();
        OntDocumentManager dm = OntModelSpec.OWL_MEM.getDocumentManager();
        OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, tmp);
        om.read(str, "", "TURTLE");
        dm.loadImports(om);
        ResponseBuilder builder = null;
        if (format != null) {
            Variant variant = request.selectVariant(MediaTypeUtils.resVariants);
            if (variant == null) {
                // return Response.status(406).build();
                return null;
            }
            MediaType mediaType = variant.getMediaType();
            // browser request : serving html page
            if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
                // builder = Response.ok(new Viewable("/ontologyHome.jsp", mod));
            } else {
                final String JenaLangStr = MediaTypeUtils.getJenaFromExtension(MediaTypeUtils.getExtFromMime(mediaType));
                final StreamingOutput stream = new StreamingOutput() {
                    @Override
                    public void write(OutputStream os) throws IOException, WebApplicationException {
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
                builder = Response.ok(stream, MediaTypeUtils.getMimeFromExtension(MediaTypeUtils.getExtFromMime(mediaType)));
            }
        }

        Iterator<String> it = dm.listDocuments();
        while (it.hasNext()) {
            System.out.println("Document >>" + it.next());
        }
        return builder.build();
        // return tmp;
    }

}
