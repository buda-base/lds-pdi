package io.bdrc.auth.model;

import java.util.ArrayList;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.iterator.ExtendedIterator;

import io.bdrc.auth.rdf.RdfConstants;

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

public class Endpoint {
        
    String path;
    String appId;
    ArrayList<String> groups;
    ArrayList<String> roles;
    ArrayList<String> permissions;
    
    public Endpoint(Model model,String resourceId) {
        groups=new ArrayList<>();
        roles=new ArrayList<>();
        permissions=new ArrayList<>();
        Triple t=new Triple(NodeFactory.createURI(resourceId),
                org.apache.jena.graph.Node.ANY,
                org.apache.jena.graph.Node.ANY);
        ExtendedIterator<Triple> ext=model.getGraph().find(t);
        while(ext.hasNext()) {
            Triple tmp=ext.next();
            String value=tmp.getObject().toString().replaceAll("\"", "");
            String prop=tmp.getPredicate().getURI();
            switch (prop) {               
                case RdfConstants.APPID:
                    appId=value;
                    break;
                case RdfConstants.PATH:
                    path=value;
                    break;
                case RdfConstants.FOR_ROLE:
                    roles.add(value);
                    break;
                case RdfConstants.FOR_GROUP:
                    groups.add(value);
                    break;
                case RdfConstants.FOR_PERM:
                    permissions.add(value);
                    break;
            }
        }
    }

    public Endpoint() {
        groups=new ArrayList<>();
        roles=new ArrayList<>();
        permissions=new ArrayList<>();
        appId="";
        path="";
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setAppId(String app) {
        this.appId = app;
    }

    public void setGroups(ArrayList<String> groups) {
        this.groups = groups;
    }

    public void setRoles(ArrayList<String> roles) {
        this.roles = roles;
    }

    public void setPermissions(ArrayList<String> permissions) {
        this.permissions = permissions;
    }

    public String getPath() {
        return path;
    }


    public String getAppId() {
        return appId;
    }


    public ArrayList<String> getGroups() {
        return groups;
    }


    public ArrayList<String> getRoles() {
        return roles;
    }


    public ArrayList<String> getPermissions() {
        return permissions;
    }

    @Override
    public String toString() {
        return "Endpoint [path=" + path + ", appId=" + appId + ", groups=" + groups
                + ", roles=" + roles + ", permissions=" + permissions + "]";
    }

}
