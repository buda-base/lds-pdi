package io.bdrc.ldspdi.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.jena.rdf.model.Model;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.ResponseOutputStream;
import io.bdrc.restapi.exceptions.RestException;

@Path("/")
public class BdrcAuthResource {

    public final static Logger log=LoggerFactory.getLogger(BdrcAuthResource.class.getName());

    @GET
    @Path("/auth/details")
    public Response getUsers() {
        log.info("Call getUsers()");
        return Response.ok(new Viewable("/authDetails.jsp")).build();
    }

    @GET
    @Path("/auth/location")
    public Response getLocation() {
        log.info("Call getLocation()");
        return Response.ok(new Viewable("/auth.jsp")).build();
    }

    @GET
    @Path("/resource-auth/{res}")
    public Response getAuthResource(@PathParam("res") final String res) throws RestException {
        log.info("Call getAuthResource()");
        String query="describe adr:"+res;
        Model m=QueryProcessor.getGraph(query, null, null);
        return Response.ok(ResponseOutputStream.getModelStream(m, "ttl"), MediaType.APPLICATION_JSON).build();
    }

}
