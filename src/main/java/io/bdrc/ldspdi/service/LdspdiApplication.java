package io.bdrc.ldspdi.service;

import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.rdf.RdfAuthModel;

//@ApplicationPath("/")
//@Configuration
public class LdspdiApplication extends ResourceConfig {

	public final static Logger log = LoggerFactory.getLogger(LdspdiApplication.class.getName());

	public LdspdiApplication() {
		// register(LoggingFeature.class);
		// register(CorsFilter.class);
		// register(GZIPWriterInterceptor.class);
		// property(JspMvcFeature.TEMPLATE_BASE_PATH, "").register(JspMvcFeature.class);
		// register(CacheControlFilterFactory.class);
		// register(CharsetResponseFilter.class);
		// register(PublicDataResource.class);
		// register(PublicTemplatesResource.class);
		// register(LibrarySearchResource.class);
		// register(JsonAPIResource.class);
		// register(BdrcAuthResource.class);
		// property(ServletProperties.FILTER_FORWARD_ON_404, true);
		if (ServiceConfig.useAuth()) {
			Thread t = new Thread(new RdfAuthModel());
			t.start();
			// register(RdfAuthFilter.class);
		}
		log.info("LdspdiApplication features have been properly registered");
	}
}
