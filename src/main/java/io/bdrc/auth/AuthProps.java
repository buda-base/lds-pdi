package io.bdrc.auth;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import io.bdrc.ldspdi.service.ServiceConfig;

/*******************************************************************************
 * Copyright (c) 2018 Buddhist Digital Resource Center (BDRC)
 * 
 * If this file is a derivation of another work the license header will appear below; 
 * otherwise, this work is licensed under the Apache License, Version 2.0 
 * (the "License"); you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

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
