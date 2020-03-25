package io.bdrc.ldspdi.rest.features;

import java.io.IOException;
import java.util.ArrayList;

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
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.ldspdi.service.ServiceConfig;

@Component
@Order(4)
public class RdfAuthFilter implements Filter {

    public final static Logger log = LoggerFactory.getLogger(RdfAuthFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String method = ((HttpServletRequest) request).getMethod();
        if (ServiceConfig.useAuth() && !method.equalsIgnoreCase("OPTIONS")) {
            HttpServletRequest req = (HttpServletRequest) request;
            boolean isSecuredEndpoint = true;
            request.setAttribute("access", new Access());
            String token = getToken(req.getHeader("Authorization"));
            TokenValidation validation = null;
            String path = req.getServletPath();
            log.debug("PATH in Auth filter is {} for HTTP method: {}", path, method);
            Endpoint end = null;
            try {
                ArrayList<Endpoint> endpoints = RdfAuthModel.getEndpoints();
                for (Endpoint e : endpoints) {
                    if (path.startsWith(e.getPath())) {
                        end = e;
                        break;
                    }
                }
                log.debug("ALL ENDPOINTS >> {}", endpoints);
                log.debug("for path {} ENDPOINT IN FILTER id {} ", path, end);
            } catch (Exception e) {
                end = null;
            }
            UserProfile prof = null;
            if (end == null) {
                isSecuredEndpoint = false;
                // endpoint is not secured - Using default (empty endpoint)
                // for Access Object end=new Endpoint();
            } else {
                isSecuredEndpoint = end.isSecured(method);
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
                    log.debug("FILTER Access matchGroup >> {}", access.matchGroup());
                    log.debug("FILTER Access matchRole >> {}", access.matchRole());
                    log.debug("FILTER Access matchPerm >> {}", access.matchPermissions());
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
            return null;
        }
        return header.substring(7);
    }

}