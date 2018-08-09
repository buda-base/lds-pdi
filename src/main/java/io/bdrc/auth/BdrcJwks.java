package io.bdrc.auth;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Properties;

import com.auth0.jwt.impl.PublicClaims;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BdrcJwks {
    
    static Properties props=new Properties();
    static Properties authProp = new Properties();
    static JsonNode node ;
    
    public static final String ALG=PublicClaims.ALGORITHM;
    public static final String KID=PublicClaims.KEY_ID;
    public static final String KTY="kty";
    public static final String USE="use";
    public static final String X5C="x5c";
    public static final String N="n";
    public static final String E="e";
    public static final String X5T="x5t";
    
    static {
        InputStream input = BdrcJwks.class.getClassLoader().getResourceAsStream("auth.properties");
        try {
            props.load(input);
            input.close();        
            InputStream authInput = new FileInputStream(props.getProperty("propertyFile")); 
            authProp.load(authInput);        
            authInput.close();
            ObjectMapper mapper = new ObjectMapper();
            URL url = new URL(authProp.getProperty("jwksUrl"));
            node=mapper.readTree(url);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }        
    }
    
   public static String getValue(String key) {
        if(key.equals(X5C)) {
            return  node.findValue(X5C).get(0).asText();
        }
        return node.findValue(key).asText();        
   }
   
   public static String getProp(String prop) {
       return authProp.getProperty(prop);
   }
    
   public static RSAPublicKey getPublicKey() throws CertificateException, IOException, InvalidKeySpecException, NoSuchAlgorithmException {
       BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(getValue(N)));
       BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(getValue(E)));
       return (RSAPublicKey)KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
   }
}
