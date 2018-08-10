package io.bdrc.auth;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import io.bdrc.ldspdi.service.ServiceConfig;

public class AuthProps {    
   
    static Properties authProp = new Properties();
    static Properties props = new Properties();
    
    static {
        InputStream input = AuthProps.class.getClassLoader().getResourceAsStream("auth.properties");
        try {
            props=new Properties();
            props.load(input);
            input.close();            
            String propFile=ServiceConfig.getProperty("propertyPath")+props.getProperty("testPropFile");
            InputStream authInput = new FileInputStream(propFile);
            authProp.load(authInput);
            authInput.close();
        }
        catch(IOException ex) {
            
        }
    }
    
    public static String getProperty(String prop) {
        
        return authProp.getProperty(prop);
    }
    
    public static String getPublicProperty(String prop) {
        return props.getProperty(prop);
    }

}
