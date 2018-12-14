package io.bdrc.ldspdi.rest.resources;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.jena.rdf.model.Model;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.MediaTypeUtils;
import io.bdrc.ldspdi.utils.ResponseOutputStream;
import io.bdrc.restapi.exceptions.RestException;

@Path("/")
public class BdrcAuthResource {

    public final static Logger log=LoggerFactory.getLogger(BdrcAuthResource.class.getName());
    public static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";
    public String fusekiUrl=ServiceConfig.getProperty(ServiceConfig.FUSEKI_URL);

    @GET
    @Path("/auth/details")
    public Response getUsers() {
        log.info("Call getUsers()");
        ResponseBuilder builder=Response.ok(new Viewable("/authDetails.jsp"));
        builder=setLastModified(builder);
        return builder.build();
    }

    @GET
    @Path("/auth/location")
    public Response getLocation() {
        log.info("Call getLocation()");
        ResponseBuilder builder=Response.ok(new Viewable("/auth.jsp"));
        builder=setLastModified(builder);
        return builder.build();
    }

    @GET
    @Path("/resource-auth/{res}")
    public Response getAuthResource(@PathParam("res") final String res) throws RestException {
        log.info("Call getAuthResource()");
        String query="describe <http://purl.bdrc.io/resource-auth/"+res+">";
        Model m=QueryProcessor.getGraphFromModel(query, QueryProcessor.getAuthGraph(null, "authDataGraph"));
        ResponseBuilder builder=Response.ok(ResponseOutputStream.getModelStream(m, "ttl"), MediaTypeUtils.getMimeFromExtension("ttl"));
        builder=setLastModified(builder);
        return builder.build();
    }

    @GET
    @Path("/authmodel")
    public Response getAuthModel(@Context Request request) throws RestException {
        log.info("Call to getAuthModel()");
        ResponseBuilder builder=Response.ok(ResponseOutputStream.getModelStream(
                QueryProcessor.getAuthGraph(fusekiUrl,"authDataGraph")),MediaTypeUtils.getMimeFromExtension("ttl"));
        builder=setLastModified(builder);
        return builder.build();
    }

    @GET
    @Path("/authmodel/updated")
    public long getAuthModelUpdated(@Context Request request) {
        //log.info("Call to getAuthModelUpdated()");
        if(ServiceConfig.useAuth()) {
            return RdfAuthModel.getUpdated();
        }else {
            return 999999999;
        }
    }

    @POST
    @Path("/callbacks/github/bdrc-auth")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateAuthModel() throws RestException{
        if(ServiceConfig.useAuth()) {
            log.info("updating Auth data model() >>");
            Thread t=new Thread(new RdfAuthModel());
            t.start();
            return Response.ok("Auth Model was updated").build();
        }
        return Response.ok("Auth usage is disabled").build();
    }

    private ResponseBuilder setLastModified(ResponseBuilder builder) {
        Calendar cal=Calendar.getInstance();
        if(ServiceConfig.useAuth()) {
            cal.setTimeInMillis(RdfAuthModel.getUpdated());
        }
        SimpleDateFormat formatter = new SimpleDateFormat(PATTERN_ASCTIME, Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        builder.header("Last-Modified", formatter.format(cal.getTime()));
        return builder;
    }
}
