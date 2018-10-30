package io.bdrc.ldspdi.rest.features;

import java.lang.reflect.Method;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;

@Provider
public class CacheControlFilterFactory implements DynamicFeature {

    private final static String MAX_AGE = "max-age=";
    private final static String NO_CACHE = "no-cache";

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        Method resourceMethod = resourceInfo.getResourceMethod();
        if (resourceMethod.isAnnotationPresent(JerseyCacheControl.class) ) {
            JerseyCacheControl ccontrol = resourceMethod.getAnnotation(JerseyCacheControl.class);
            if(ccontrol.noCache()) {
                context.register(new CacheResponseFilter(NO_CACHE));
            }else {
                long seconds = ccontrol.maxAge();
                context.register(new CacheResponseFilter(MAX_AGE + seconds));
            }
        }
    }

    private static class CacheResponseFilter implements ContainerResponseFilter {
        private final String headerValue;

        CacheResponseFilter(String headerValue) {
            this.headerValue = headerValue;
        }

        @Override
        public void filter(ContainerRequestContext ctx, ContainerResponseContext responseContext) {
            String appId=AuthProps.getProperty("appId");
            String path=ctx.getUriInfo().getPath();
            System.out.println("IS Secured ? >> "+RdfAuthModel.isSecuredEndpoint(appId,path));
            if(!RdfAuthModel.isSecuredEndpoint(appId,path)) {
                responseContext.getHeaders().putSingle(HttpHeaders.CACHE_CONTROL, "public,"+headerValue);
            }else {
                responseContext.getHeaders().putSingle(HttpHeaders.CACHE_CONTROL, "private");
            }
        }
    }
}