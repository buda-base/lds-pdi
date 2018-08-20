package io.bdrc.auth.jersey;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.UserProfile;
import io.bdrc.auth.rdf.RdfAuthModel;

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
    @Path("/auth/public")       
    public Response authPublicGroupTest(@Context ContainerRequestContext crc) {
        log.info("auth/public ROLES >> "+crc.getProperty("user"));
        return Response.status(200).entity("test auth public done").header("Content-Type", "text/html").build();
    }
    
    @GET 
    @Path("/auth/private")       
    public Response authPrivateTest(@Context ContainerRequestContext crc) {
        log.info("auth/private ROLES >> "+crc.getProperty("user"));
        if(((UserProfile)crc.getProperty("user")).hasPermission(READ_PRIVATE_PERM)) {
            return Response.status(200).entity("test auth public done").header("Content-Type", "text/html").build();
        }
        return Response.status(403).entity("There was an authorization issue").header("Content-Type", "text/html").build();  
    }
    
    @GET 
    @Path("/auth/staff")       
    public Response authStaffTest(@Context ContainerRequestContext crc) {        
        log.info("auth/staff ROLES >> "+crc.getProperty("user")); 
        if(((UserProfile)crc.getProperty("user")).isInGroup(STAFF_GROUP)) {
            return Response.status(200).entity("test auth public done").header("Content-Type", "text/html").build();
        }
        return Response.status(403).entity("There was an authorization issue").header("Content-Type", "text/html").build();  
    }
    
    @GET 
    @Path("/auth/admin")       
    public Response authAdminTest(@Context ContainerRequestContext crc) {        
        log.info("auth/admin ROLES >> "+crc.getProperty("user")); 
        if(((UserProfile)crc.getProperty("user")).isInGroup(ADMIN_GROUP)) {
            return Response.status(200).entity("test auth public done").header("Content-Type", "text/html").build();
        }
        return Response.status(403).entity("There was an authorization issue").header("Content-Type", "text/html").build(); 
    }
    
    @GET 
    @Path("/auth/rdf/public")       
    public Response authRdfPublicTest(@Context ContainerRequestContext crc) {
        log.info("auth/rdf/public ENDPOINT PATH>> "+crc.getUriInfo().getPath());
        //log.info("auth/rdf/public ENDPOINT >> "+RdfAuthModel.getAuthModel().getEndpoint(crc.getUriInfo().getPath()));
        return Response.status(200).entity("test auth rdf public done").header("Content-Type", "text/html").build();
    }
    
    @GET 
    @Path("/auth/rdf/private")       
    public Response authRdfPrivateTest(@Context ContainerRequestContext crc) {
        //log.info("auth/rdf/private ENDPOINTS >> "+RdfAuthModel.getAuthModel().getEndpoints());
        log.info("auth/rdf/private ENDPOINT PATH>> "+crc.getUriInfo().getPath());
        //log.info("auth/rdf/private ENDPOINT >> "+RdfAuthModel.getAuthModel().getEndpoint(crc.getUriInfo().getPath()));       
        return Response.status(200).entity("test auth rdf private done").header("Content-Type", "text/html").build();         
    }
    
    @GET 
    @Path("/auth/rdf/staff")       
    public Response authRdfStaffTest(@Context ContainerRequestContext crc) {        
        log.info("auth/rdf/staff ENDPOINT >> "+crc.getProperty("endpoint"));        
        return Response.status(200).entity("test auth rdf staff done").header("Content-Type", "text/html").build();  
    }
    
    @GET 
    @Path("/auth/rdf/admin")       
    public Response authRdfAdminTest(@Context ContainerRequestContext crc) {        
        log.info("auth/rdf/admin ENDPOINT >> "+crc.getProperty("endpoint"));        
        return Response.status(200).entity("test auth rdf admin done").header("Content-Type", "text/html").build(); 
    }

}
