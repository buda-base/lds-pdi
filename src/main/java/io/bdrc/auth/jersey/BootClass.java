package io.bdrc.auth.jersey;

import javax.servlet.ServletContextEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.restapi.exceptions.RestException;

public class BootClass implements javax.servlet.ServletContextListener{

public final static Logger log=LoggerFactory.getLogger(BootClass.class.getName());
    
    public void contextDestroyed(ServletContextEvent arg0) {
        //Do nothing;
    }
 
    public void contextInitialized(ServletContextEvent arg0) {
        try {
            
           RdfAuthModel.init();
        } 
        catch (IllegalArgumentException | RestException e) {
            log.error("BootClass init error", e);
            e.printStackTrace();
        } 
    }
    
}
