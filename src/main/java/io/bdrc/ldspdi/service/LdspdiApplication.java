package io.bdrc.ldspdi.service;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.jsp.JspMvcFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import io.bdrc.auth.jersey.RdfAuthFilter;
import io.bdrc.ldspdi.rest.features.CacheControlFilterFactory;
import io.bdrc.ldspdi.rest.features.CharsetResponseFilter;
import io.bdrc.ldspdi.rest.features.CorsFilter;
import io.bdrc.ldspdi.rest.features.GZIPWriterInterceptor;
import io.bdrc.ldspdi.rest.features.RdfAuthFilter;

@ApplicationPath("/")
public class LdspdiApplication extends ResourceConfig {

    public final static Logger log=LoggerFactory.getLogger(LdspdiApplication.class.getName());

    public LdspdiApplication() {
        register(LoggingFeature.class);
        register(CorsFilter.class);
        register(GZIPWriterInterceptor.class);
        property(JspMvcFeature.TEMPLATE_BASE_PATH, "").register(JspMvcFeature.class);
        register(CacheControlFilterFactory.class);
        register(CharsetResponseFilter.class);
        if(ServiceConfig.useAuth()) {
            register(RdfAuthFilter.class);
        }
        log.info("LdspdiApplication features have been properly registered");
    }
}
