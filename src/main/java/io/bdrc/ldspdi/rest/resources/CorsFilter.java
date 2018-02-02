package io.bdrc.ldspdi.rest.resources;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import io.bdrc.ldspdi.service.ServiceConfig;

@Provider
public class CorsFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext request,
            ContainerResponseContext response) throws IOException {
        response.getHeaders().add("Access-Control-Allow-Origin", ServiceConfig.getProperty("Allow-Origin"));
        response.getHeaders().add("Access-Control-Allow-Headers",ServiceConfig.getProperty("Allow-Headers"));
        response.getHeaders().add("Access-Control-Allow-Credentials", ServiceConfig.getProperty("Allow-Credentials"));
        response.getHeaders().add("Access-Control-Allow-Methods",ServiceConfig.getProperty("Allow-Methods"));
    }
}
