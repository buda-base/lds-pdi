package io.bdrc.ldspdi.service;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.jsp.JspMvcFeature;

//import io.bdrc.auth.jersey.RdfAuthFilter;
import io.bdrc.ldspdi.rest.features.CacheControlFilterFactory;
import io.bdrc.ldspdi.rest.features.CharsetResponseFilter;
import io.bdrc.ldspdi.rest.features.CorsFilter;
import io.bdrc.ldspdi.rest.features.GZIPWriterInterceptor;

@ApplicationPath("/")
public class LdspdiApplication extends ResourceConfig {

    public LdspdiApplication() {
        register(LoggingFeature.class);
        register(CorsFilter.class);
        register(GZIPWriterInterceptor.class);
        property(JspMvcFeature.TEMPLATE_BASE_PATH, "").register(JspMvcFeature.class);
        register(CacheControlFilterFactory.class);
        register(CharsetResponseFilter.class);
        //register(RdfAuthFilter.class);
    }
}
