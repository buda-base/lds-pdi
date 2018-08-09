package io.bdrc.auth.model;

import java.util.ArrayList;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.iterator.ExtendedIterator;

public class Endpoint {
    
    public final static String APP="http://purl.bdrc.io/auth/app";
    public final static String APPID="http://purl.bdrc.io/auth/appId";
    public final static String PATH="http://purl.bdrc.io/auth/path";
    public final static String ROLE="http://purl.bdrc.io/auth/forRole";
    public final static String GROUP="http://purl.bdrc.io/auth/forGroup";
    public final static String PERM="http://purl.bdrc.io/auth/forPerm";
    
    String applicationId;
    String path;
    String app;
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
                case APP:
                    app=value;
                    break;
                case APPID:
                    applicationId=value;
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

    public String getApplicationId() {
        return applicationId;
    }


    public String getPath() {
        return path;
    }


    public String getApp() {
        return app;
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
        return "Endpoint [applicationId=" + applicationId + ", path=" + path + ", app=" + app + ", groups=" + groups
                + ", roles=" + roles + ", permissions=" + permissions + "]";
    }

}
