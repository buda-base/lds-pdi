package io.bdrc.auth.jersey;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.TokenValidation;
import io.bdrc.auth.model.AuthModel;
import io.bdrc.auth.rdf.RdfAuthModel;


@Provider
@PreMatching
public class AuthFilter implements ContainerRequestFilter {
    
    public final static Logger log=LoggerFactory.getLogger(AuthFilter.class.getName());
    //AuthModel auth=RdfAuthModel.getAuthModel();
 
    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        /*
        String token=getToken(ctx.getHeaderString("Authorization"));
        if(token==null) {
            ctx.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("access to this resource is restricted")
                    .build());
        }else {        
            TokenValidation validation=new TokenValidation(token,AuthProps.getProperty("lds-pdiClientID"));
            //log.info("TOKEN in authFilter >> "+token);
            //log.info("Setting ROLES (in authFilter) to >> "+validation.getUser());
            ctx.setProperty("user", validation.getUser());
        }*/
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
