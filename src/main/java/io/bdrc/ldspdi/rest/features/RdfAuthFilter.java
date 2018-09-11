package io.bdrc.ldspdi.rest.features;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.Access;
import io.bdrc.auth.AuthProps;
import io.bdrc.auth.TokenValidation;
import io.bdrc.auth.UserProfile;
import io.bdrc.auth.model.Endpoint;
import io.bdrc.auth.rdf.RdfAuthModel;

//@Provider
//@PreMatching


public class RdfAuthFilter implements ContainerRequestFilter {

    public final static Logger log=LoggerFactory.getLogger(RdfAuthFilter.class.getName());
    @Context private HttpServletRequest httpRequest;

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        ctx.setProperty("access", new Access());
        String token=getToken(ctx.getHeaderString("Authorization"));
        System.out.println("FILTER TOKEN >> "+token);
        TokenValidation validation=null;
        String appId=AuthProps.getProperty("appId");
        String path=ctx.getUriInfo().getPath();
        Endpoint end=RdfAuthModel.getEndpoint(path);
        UserProfile prof=null;
        if(end==null) {
            //endpoint is not secured - Using default (empty endpoint)
            //for Access Object
            end=new Endpoint();
        }
        if(token !=null) {
            //User is logged on
            //Getting his profile
            validation=new TokenValidation(token);
            prof=validation.getUser();
        }
        log.info("FILTER IsSecuredEndpoint >> "+path+ " >> "+RdfAuthModel.isSecuredEndpoint(appId,path));
        if(RdfAuthModel.isSecuredEndpoint(appId,path)) {
            //Endpoint is secure
            if(validation==null) {
                //no token --> access forbidden
                abort(ctx);
            }else {
                Access access=new Access(prof,end);
                System.out.println("FILTER Access matchGroup >> "+access.matchGroup());
                System.out.println("FILTER Access matchRole >> "+access.matchRole());
                System.out.println("FILTER Access matchPerm >> "+access.matchPermissions());
                if(!access.hasEndpointAccess()) {
                    abort(ctx);
                }
                System.out.println("FILTER endpoint >> "+end);
                System.out.println("FILTER user >> "+prof);
                ctx.setProperty("access", access);
            }
        }
        else {
            //end point not secured
            if(validation!=null) {
                //token present since validation is not null
                Access acc=new Access(prof,end);
                ctx.setProperty("access", acc);
                System.out.println("FILTER put access >> "+acc);
            }
        }
    }

    void abort(ContainerRequestContext ctx) {
        ctx.abortWith(Response.status(Response.Status.FORBIDDEN)
                .entity("access to this resource is restricted")
                .build());
    }

    String getToken(String header) {
        try {
            if(header!=null) {
                return header.split(" ")[1];
            }
        }
        catch(Exception ex) {
            log.error(ex.getMessage());
            return null;
        }
        return null;
    }
}