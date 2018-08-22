package io.bdrc.auth;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.impl.PublicClaims;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

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

public class TokenValidation {
    
    public final static Logger log=LoggerFactory.getLogger(TokenValidation.class.getName());
    DecodedJWT decodedJwt;
    List<String> scopes;
    UserProfile user;    
    String token;
    String audience;
    boolean valid;
        
    public TokenValidation(String token, String audience) {
        super();
        this.token = token;
        this.audience=audience;
        try {
            valid = checkTokenValidity()
                    & checkTokenSignature(audience)
                    & validateTokenKeyId();
            setScopes();
            user=new UserProfile(decodedJwt);
        } catch (IllegalArgumentException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException
                | IOException e) {
            // TODO Auto-generated catch block
            log.error(e.getMessage());
        }        
    }
    
    void setScopes() {
        Claim cl=decodedJwt.getClaims().get("scope");
        if(cl!=null) {        
            scopes=Arrays.asList(cl.asString().split(" "));
        }else {
            scopes=Arrays.asList("".split(" "));
        }
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public boolean isValidScope(String scope) {
        return scopes.contains(scope);
    }

    public boolean checkTokenValidity() {
        try {
            decodedJwt = JWT.decode(token);
            return true;
        }
        catch(JWTDecodeException e) {
            return false;
        }
    }
    
    public List<String> getScopes(){
        return scopes;
    }
    
    public UserProfile getUser(){
        return user;
    }
    
    public String getKeyId() {
        return decodedJwt.getKeyId();
    }
    
    public String getSubject() {
        return decodedJwt.getSubject();
    }
    
    public String getAlgorithm() {
        return decodedJwt.getAlgorithm();
    }
    
    public String getSignature() {
        return decodedJwt.getSignature();
    }
    
    public List<String> getAudience() {
        return decodedJwt.getAudience();
    }
    
    public boolean checkTokenSignature(String audience) throws IllegalArgumentException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        try {
            Algorithm algo = Algorithm.RSA256(BdrcJwks.getPublicKey(), null);
            JWTVerifier verifier = JWT.require(algo)
                    .withIssuer(BdrcJwks.getProp("issuer"))
                    .withClaim(PublicClaims.NOT_BEFORE,Calendar.getInstance().getTime().getTime())
                    .build(); 
            decodedJwt = verifier.verify(token);
            return (true && validateTokenKeyId());
        }
        catch(JWTVerificationException ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
    public boolean validateTokenExpiration() {        
        Calendar cal=Calendar.getInstance();        
        return decodedJwt.getExpiresAt().after(cal.getTime());        
    }
    
    public boolean validateTokenIssuer() {
        return decodedJwt.getIssuer().equals("https://bdrc-io.auth0.com/");                
    }
    
    public boolean validateTokenAudience() {
        return decodedJwt.getAudience().contains(audience);                
    }
    
    public boolean validateTokenAudience(String other_audience) {
        return decodedJwt.getAudience().contains(other_audience);                
    }
    
    public boolean validateTokenKeyId() {
        return decodedJwt.getKeyId().equals(BdrcJwks.getValue(BdrcJwks.KID));                
    }
    
    public DecodedJWT getVerifiedJwt() {
        return decodedJwt;
    }

}
