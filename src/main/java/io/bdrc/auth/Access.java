package io.bdrc.auth;

import io.bdrc.auth.model.Endpoint;
import io.bdrc.auth.model.ResourceAccess;
import io.bdrc.auth.rdf.RdfAuthModel;

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

public class Access {
    
    UserProfile user;
    Endpoint endpoint; 
        
    public Access(UserProfile user, Endpoint endpoint) {
        super();
        this.user = user;
        this.endpoint = endpoint;
    }
    
    public boolean hasEndpointAccess() {
        return matchGroup() || matchRole() || matchPermissions();        
    }
    
    public boolean hasResourceAccess(String accessType) {
        return matchResourcePermissions(accessType);        
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
    
    public boolean matchResourcePermissions(String accessType) {
        boolean match=false;
        ResourceAccess access=RdfAuthModel.getResourceAccess(accessType);
        if(access!=null) {
            for(String pm:user.getPermissions()) {
                if(access.getPermission().equals(pm)) {
                    return true;
                }
            }
        }
        return match;
    }

}
