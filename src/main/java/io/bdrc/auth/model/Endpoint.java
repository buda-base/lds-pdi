package io.bdrc.auth.model;

import java.util.ArrayList;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.iterator.ExtendedIterator;

public class Endpoint {
    
    public final static String APPID="http://purl.bdrc.io/ontology/ext/auth/appId";
    public final static String PATH="http://purl.bdrc.io/ontology/ext/auth/path";
    public final static String ROLE="http://purl.bdrc.io/ontology/ext/auth/forRole";
    public final static String GROUP="http://purl.bdrc.io/ontology/ext/auth/forGroup";
    public final static String PERM="http://purl.bdrc.io/ontology/ext/auth/forPermission";
    
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
                case APPID:
                    appId=value;
                    break;
                case PATH:
                    path=value;
                    break;
                case ROLE:
                    roles.add(value);
                    break;
                case GROUP:
                    groups.add(value);
                    break;
                case PERM:
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
