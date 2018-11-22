package io.bdrc.ldspdi.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Path("/")
public class AuthTestResource {

    public final static Logger log=LoggerFactory.getLogger(AuthTestResource.class.getName());
    public final String PUBLIC_GROUP="public";
    public final String ADMIN_GROUP="admin";
    public final String STAFF_GROUP="staff";
    public final String READ_PUBLIC_ROLE="readpublic";
    public final String READ_ONLY_PERM="readonly";
    public final String READ_PRIVATE_PERM="readprivate";

    @GET
    @Path("auth/public")
    public Response authPublicGroupTest(@Context ContainerRequestContext crc) {
        return Response.status(200).entity("test auth public done").header("Content-Type", "text/html").build();
    }

    @GET
    @Path("auth/rdf/admin")
    public Response authPrivateResourceAccessTest(@Context ContainerRequestContext crc) {
        System.out.println("auth/ref/admin >>>>>>>>> "+crc.getProperty("access"));
        return Response.status(200).entity("test auth public done").header("Content-Type", "text/html").build();
    }
}
