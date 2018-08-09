package io.bdrc.auth;

import java.util.ArrayList;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

public class UserProfile {
    
    ArrayList<String> groups;
    ArrayList<String> roles;
    ArrayList<String> permissions; 
    String name;
    
    public UserProfile(DecodedJWT decodedJwt) {
        super();
        this.groups = getGroups(decodedJwt);
        this.roles = getRoles(decodedJwt);
        this.permissions = getPermissions(decodedJwt);
        this.name=getName(decodedJwt);
    }
    
    @SuppressWarnings("unchecked")
    ArrayList<String> getGroups(DecodedJWT decodedJwt) {
        Claim claim=decodedJwt.getClaims().get("https://purl.bdrc.io/user_authorization");
        
        if(claim!=null) {
            return (ArrayList<String>)claim.asMap().get("groups");
        }
        return new ArrayList<String>();       
    }

    public ArrayList<String> getGroups() {
        return groups;
    }
    
    @SuppressWarnings("unchecked")
    ArrayList<String> getRoles(DecodedJWT decodedJwt) {
        Claim claim=decodedJwt.getClaims().get("https://purl.bdrc.io/user_authorization");
        if(claim!=null) {
            return (ArrayList<String>)claim.asMap().get("roles");
        }
        return new ArrayList<String>();       
    }
    public ArrayList<String> getRoles() {
        return roles;
    }
    
    @SuppressWarnings("unchecked")
    ArrayList<String> getPermissions(DecodedJWT decodedJwt) {
        Claim claim=decodedJwt.getClaims().get("https://purl.bdrc.io/user_authorization");
        if(claim!=null) {
            return (ArrayList<String>)claim.asMap().get("permissions");
        }
        return new ArrayList<String>();       
    }    
    
    String getName(DecodedJWT decodedJwt) {
        Claim claim=decodedJwt.getClaims().get("name");
        if(claim!=null) {
            return claim.asString();
        }
        return null;       
    }
    
    public ArrayList<String> getPermissions() {
        return permissions;
    }
    
    public boolean isInGroup(String group) {
        return groups.contains(group);
    }
    
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
    
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    @Override
    public String toString() {
        return "User [groups=" + groups + ", roles=" + roles + ", permissions=" + permissions + ", name=" + name + "]";
    }
}
