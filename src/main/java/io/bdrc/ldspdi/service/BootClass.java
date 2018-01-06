package io.bdrc.ldspdi.service;

import javax.servlet.ServletContextEvent;

import io.bdrc.ontology.service.core.OntAccess;

public class BootClass implements javax.servlet.ServletContextListener{
	
	public void contextDestroyed(ServletContextEvent arg0) {
        //Do nothing;
    }
 
    public void contextInitialized(ServletContextEvent arg0) {
        try {
             
            OntAccess.init();
             
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

}
