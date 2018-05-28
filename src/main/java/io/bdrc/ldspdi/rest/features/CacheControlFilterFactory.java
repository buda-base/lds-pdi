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



/**
 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9
 * @author ALVN
 */
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
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
            responseContext.getHeaders().putSingle(HttpHeaders.CACHE_CONTROL, headerValue);
        }
    }
}