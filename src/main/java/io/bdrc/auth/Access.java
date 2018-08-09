package io.bdrc.auth;

import io.bdrc.auth.model.Endpoint;

public class Access {
    
    UserProfile user;
    Endpoint endpoint;    
        
    public Access(UserProfile user, Endpoint endpoint) {
        super();
        this.user = user;
        this.endpoint = endpoint;
    }
    
    public boolean hasAccess() {
        return matchGroup() || matchRole() || matchPermissions();        
    }
    
    public boolean matchGroup() {
        boolean match=false;         
        for(String gp:user.getGroups()) {
            if(endpoint.getGroups().contains(gp)) {
                return true;
            }
        }
        return match;
    }
    
    public boolean matchRole() {
        boolean match=false;         
        for(String r:user.getRoles()) {
            if(endpoint.getRoles().contains(r)) {
                return true;
            }
        }
        return match;
    }
    
    public boolean matchPermissions() {
        boolean match=false;         
        for(String pm:user.getPermissions()) {
            if(endpoint.getPermissions().contains(pm)) {
                return true;
            }
        }
        return match;
    }

}
