package io.bdrc.ldspdi.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class BdrcAuthResource {

    public final static Logger log=LoggerFactory.getLogger(BdrcAuthResource.class.getName());

    @GET
    @Path("/auth/users")
    public Response getUsers() {
        log.info("Call getUsers()");
        return Response.ok(new Viewable("/users.jsp")).build();
    }

}
