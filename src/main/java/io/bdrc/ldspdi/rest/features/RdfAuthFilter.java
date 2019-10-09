package io.bdrc.ldspdi.rest.features;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.bdrc.auth.Access;
import io.bdrc.auth.TokenValidation;
import io.bdrc.auth.UserProfile;
import io.bdrc.auth.model.Endpoint;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.ldspdi.service.ServiceConfig;

@Component
@Order(4)
public class RdfAuthFilter implements Filter {

    public final static Logger log = LoggerFactory.getLogger(RdfAuthFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (ServiceConfig.useAuth()) {
            HttpServletRequest req = (HttpServletRequest) request;
            boolean isSecuredEndpoint = true;
            request.setAttribute("access", new Access());
            String token = getToken(req.getHeader("Authorization"));
            TokenValidation validation = null;
            String path = req.getServletPath();
            Endpoint end;
            try {
                end = RdfAuthModel.getEndpoint(path);
            } catch (Exception e) {
                e.printStackTrace();
                end = null;
            }
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
                    ((HttpServletResponse) response).setStatus(403);
                    return;
                } else {
                    Access access = new Access(prof, end);
                    // System.out.println("FILTER Access matchGroup >> "+access.matchGroup());
                    // System.out.println("FILTER Access matchRole >> "+access.matchRole());
                    // System.out.println("FILTER Access matchPerm >> "+access.matchPermissions());
                    if (!access.hasEndpointAccess()) {
                        ((HttpServletResponse) response).setStatus(403);
                        return;
                    }
                    request.setAttribute("access", access);
                }
            } else {
                // end point not secured
                if (validation != null) {
                    // token present since validation is not null
                    Access acc = new Access(prof, end);
                    request.setAttribute("access", acc);
                }
            }
        }
        chain.doFilter(request, response);
    }

    public static String getToken(final String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            log.info("invalid Authorization header: {}", header);
            return null;
        }
        return header.substring(7);
    }

}