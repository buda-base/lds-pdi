package io.bdrc.auth.rdf;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.model.AuthModel;

public class RdfAuthModel implements Runnable{
    
    static Model authMod; 
    static AuthModel auth;
    
    public final static Logger log=LoggerFactory.getLogger(RdfAuthModel.class.getName());
        
    public static void init() {
        
        try {
            log.info("URL >> "+AuthProps.getPublicProperty("policiesUrl"));
            HttpURLConnection connection = (HttpURLConnection) new URL(AuthProps.getPublicProperty("policiesUrl")).openConnection();
            InputStream stream=connection.getInputStream();
            //InputStream stream=RdfAuthModel.class.getClassLoader().getResourceAsStream("fullModel.ttl");    
            authMod = ModelFactory.createDefaultModel();
            //InputStream stream=RdfAuthModel.class.getClassLoader().getResourceAsStream("fullModel.ttl");           
            authMod.read(stream, "", "TURTLE");
            stream.close();
            auth=new AuthModel(authMod);
            authMod.add(auth.getModel());
            log.info("Done updating rdfAuth Model"); 
        } catch (IOException io) {
            log.error("Error initializing OntModel", io);            
        }
    }
    
    public static AuthModel getAuthModel() {
        return auth;
    }
    
    public static Model getFullModel() {
        return authMod;
    }
        
    public static void main(String[] args) {
        RdfAuthModel.init();
        RdfAuthModel.authMod.write(System.out,"TURTLE");
    }

    @Override
    public void run() {
        init();        
    }
    

}
