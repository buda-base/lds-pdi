package io.bdrc.auth.jersey;

import org.glassfish.jersey.server.ResourceConfig;

public class AuthApplication extends ResourceConfig {
    
    public AuthApplication() {
        register(AuthFilter.class);
    }

}
