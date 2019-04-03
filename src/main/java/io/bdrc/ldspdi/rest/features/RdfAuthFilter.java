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
import io.bdrc.auth.TokenValidation;
import io.bdrc.auth.UserProfile;
import io.bdrc.auth.model.Endpoint;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.ldspdi.service.ServiceConfig;

//@Provider
//@PreMatching

public class RdfAuthFilter implements ContainerRequestFilter {

    public final static Logger log = LoggerFactory.getLogger(RdfAuthFilter.class.getName());
    @Context
    private HttpServletRequest httpRequest;

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        if (ServiceConfig.useAuth()) {
            boolean isSecuredEndpoint = true;
            ctx.setProperty("access", new Access());
            String token = getToken(ctx.getHeaderString("Authorization"));
            System.out.println("TOKEN Filter >> " + token);
            TokenValidation validation = null;
            String path = ctx.getUriInfo().getPath();
            System.out.println("PATH Filter >> " + path);
            Endpoint end = RdfAuthModel.getEndpoint(path);
            System.out.println("ENDPOINT >> " + end);
            UserProfile prof = null;
            if (end == null) {
                isSecuredEndpoint = false;
                // endpoint is not secured - Using default (empty endpoint)
                // for Access Object end=new Endpoint();
            }
            if (token != null) {
                // User is logged on
                // Getting his profile
                validation = new TokenValidation(token);
                prof = validation.getUser();
            }
            if (isSecuredEndpoint) {
                // Endpoint is secure
                if (validation == null) {
                    // no token --> access forbidden
                    abort(ctx);
                } else {
                    Access access = new Access(prof, end);
                    // System.out.println("FILTER Access matchGroup >> "+access.matchGroup());
                    // System.out.println("FILTER Access matchRole >> "+access.matchRole());
                    // System.out.println("FILTER Access matchPerm >> "+access.matchPermissions());
                    if (!access.hasEndpointAccess()) {
                        abort(ctx);
                    }
                    ctx.setProperty("access", access);
                }
            } else {
                // end point not secured
                if (validation != null) {
                    // token present since validation is not null
                    Access acc = new Access(prof, end);
                    ctx.setProperty("access", acc);
                }
            }
        }
    }

    void abort(ContainerRequestContext ctx) {
        ctx.abortWith(Response.status(Response.Status.FORBIDDEN).entity("access to this resource is restricted").build());
    }

    String getToken(String header) {
        try {
            if (header != null) {
                return header.split(" ")[1];
            }
        } catch (Exception ex) {
            log.error(ex.getMessage());
            return null;
        }
        return null;
    }
}