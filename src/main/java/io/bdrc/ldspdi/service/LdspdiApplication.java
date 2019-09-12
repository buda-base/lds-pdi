package io.bdrc.ldspdi.service;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.jsp.JspMvcFeature;
import org.glassfish.jersey.servlet.ServletProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.ldspdi.rest.features.CacheControlFilterFactory;
import io.bdrc.ldspdi.rest.features.CharsetResponseFilter;
import io.bdrc.ldspdi.rest.features.CorsFilter;
import io.bdrc.ldspdi.rest.features.GZIPWriterInterceptor;
import io.bdrc.ldspdi.rest.features.RdfAuthFilter;
import io.bdrc.ldspdi.rest.resources.BdrcAuthResource;
import io.bdrc.ldspdi.rest.resources.JsonAPIResource;
import io.bdrc.ldspdi.rest.resources.LibrarySearchResource;
import io.bdrc.ldspdi.rest.resources.PublicDataResource;
import io.bdrc.ldspdi.rest.resources.PublicTemplatesResource;

@ApplicationPath("/")
@Configuration
public class LdspdiApplication extends ResourceConfig {

	public final static Logger log = LoggerFactory.getLogger(LdspdiApplication.class.getName());

	public LdspdiApplication() {
		register(LoggingFeature.class);
		register(CorsFilter.class);
		register(GZIPWriterInterceptor.class);
		property(JspMvcFeature.TEMPLATE_BASE_PATH, "").register(JspMvcFeature.class);
		register(CacheControlFilterFactory.class);
		register(CharsetResponseFilter.class);
		register(PublicDataResource.class);
		register(PublicTemplatesResource.class);
		register(LibrarySearchResource.class);
		register(JsonAPIResource.class);
		register(BdrcAuthResource.class);
		property(ServletProperties.FILTER_FORWARD_ON_404, true);
		if (ServiceConfig.useAuth()) {
			Thread t = new Thread(new RdfAuthModel());
			t.start();
			register(RdfAuthFilter.class);
		}
		log.info("LdspdiApplication features have been properly registered");
	}
}
