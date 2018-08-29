package io.bdrc.auth.model;

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

public class ResourceAccess {
    
    String policy;
    String permission;
    
    public ResourceAccess(Model model,String resourceId) {
        
        Triple t=new Triple(NodeFactory.createURI(resourceId),
                org.apache.jena.graph.Node.ANY,
                org.apache.jena.graph.Node.ANY);
        ExtendedIterator<Triple> ext=model.getGraph().find(t);
        while(ext.hasNext()) {
            Triple tmp=ext.next();
            String value=tmp.getObject().toString().replaceAll("\"", "");
            String prop=tmp.getPredicate().getURI();
            switch (prop) {               
                case RdfConstants.FOR_PERM:
                    permission=value;
                    break;
                case RdfConstants.POLICY:
                    policy=value;
                    break;
            }
        }
    }
    
    public ResourceAccess() {
        this.policy="";
        this.permission="";
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    

}
