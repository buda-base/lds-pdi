package io.bdrc.ldspdi.test;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import io.bdrc.auth.Access;
import io.bdrc.auth.UserProfile;
import io.bdrc.auth.model.Endpoint;
import io.bdrc.auth.rdf.RdfAuthModel;

public class RdfAuthTestFilter implements ContainerRequestFilter {

    public final static Logger log=LoggerFactory.getLogger(RdfAuthTestFilter.class.getName());
    @Context private HttpServletRequest httpRequest;

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        boolean isSecuredEndpoint=true;
        ctx.setProperty("access", new Access());
        String token=getToken(ctx.getHeaderString("Authorization"));
        String path=ctx.getUriInfo().getPath();
        Endpoint end=RdfAuthModel.getEndpoint(path);

        UserProfile prof=null;
        if(end==null) {
            isSecuredEndpoint=false;
            //endpoint is not secured - Using default (empty endpoint)
            //for Access Object
            end=new Endpoint();
        }
        if(token !=null) {
            //User is logged on
            //Getting his profile
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256("secret")).build();
            DecodedJWT jwt=verifier.verify(token);
            prof=new UserProfile(jwt);
        }
        if(isSecuredEndpoint) {
            //Endpoint is secure
            if(token==null) {
                //no token --> access forbidden
                abort(ctx);
            }else {
                Access access=new Access(prof,end);
                if(!access.hasEndpointAccess()) {
                    abort(ctx);
                }
                ctx.setProperty("access", access);
            }
        }
        else {
            //end point not secured
            if(token!=null) {
                //token present since validation is not null
                Access acc=new Access(prof,end);
                ctx.setProperty("access", acc);
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