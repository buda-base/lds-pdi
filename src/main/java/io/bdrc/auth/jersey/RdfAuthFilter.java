package io.bdrc.auth.jersey;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.Access;
import io.bdrc.auth.AuthProps;
import io.bdrc.auth.TokenValidation;
import io.bdrc.auth.rdf.RdfAuthModel;

@Provider
@PreMatching
public class RdfAuthFilter implements ContainerRequestFilter {
    
    public final static Logger log=LoggerFactory.getLogger(RdfAuthFilter.class.getName());    
 
    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String appId=AuthProps.getPublicProperty("appId");
        String path=ctx.getUriInfo().getPath();
        log.info("IsSecuredEndpoint >> "+path+ " >> "+RdfAuthModel.isSecuredEndpoint(appId,path));
        if(RdfAuthModel.isSecuredEndpoint(appId,path)) {
            String token=getToken(ctx.getHeaderString("Authorization"));            
            if(token==null) {
                abort(ctx);
            }else {        
                TokenValidation validation=new TokenValidation(token,AuthProps.getProperty("lds-pdiClientID"));
                Access access=new Access(validation.getUser(),RdfAuthModel.getEndpoint(path));
                System.out.println("FILTER Access matchGroup >> "+access.matchGroup());
                System.out.println("FILTER Access matchRole >> "+access.matchRole());
                System.out.println("FILTER Access matchPerm >> "+access.matchPermissions());
                if(!access.hasAccess()) {
                    abort(ctx);
                }
                System.out.println("FILTER endpoint >> "+RdfAuthModel.getEndpoint(path));
                System.out.println("FILTER user >> "+validation.getUser());
                
                ctx.setProperty("endpoint", RdfAuthModel.getEndpoint(path));
                ctx.setProperty("user", validation.getUser());
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