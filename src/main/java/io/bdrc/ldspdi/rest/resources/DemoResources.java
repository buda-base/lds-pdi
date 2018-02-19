package io.bdrc.ldspdi.rest.resources;

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
import java.util.HashMap;
import java.util.Map;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;


import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.Template;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.jsp.JspMvcFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.Utils.DocFileModel;
import io.bdrc.ontology.service.core.OntClassModel;
import io.bdrc.restapi.exceptions.RestException;

@Path("/")
@Template
public class DemoResources {
    
    public static Logger log=LoggerFactory.getLogger(DemoResources.class.getName());
    
    public DemoResources() {
        super();        
        ResourceConfig config=new ResourceConfig(DemoResources.class);
        config.register(LoggingFeature.class);
        config.register(CorsFilter.class);
        config.property(JspMvcFeature.TEMPLATE_BASE_PATH, "").register(JspMvcFeature.class);
    } 
    
    @GET 
    @Produces(MediaType.TEXT_HTML)    
    public Viewable getHomePage() throws RestException{
        log.info("Call to getHomePage()");         
        return new Viewable("/index.jsp",new DocFileModel()); 
    } 
       
    @GET 
    @Path("/demo/{file}")
    @Produces(MediaType.TEXT_HTML)
    public Viewable templateRelative(@PathParam("file") String file) throws RestException{
        log.info("Call to templateRelative()");
        return new Viewable("/"+file);         
    }
    
    @GET
    @Path("demo/ontology")
    @Produces("text/html")
    public Viewable getOntologyClassView(@QueryParam("classUri") String uri) throws RestException{
        log.info("Call to getOntologyClassView()");          
        Map<String, Object> map = new HashMap<>();
        map.put("model", new OntClassModel(uri)); 
        return new Viewable("/ontClass.jsp", map);        
    }
    

}
