package io.bdrc.ldspdi.rest.features;

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

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import io.bdrc.ldspdi.service.ServiceConfig;

@Provider
public class CorsFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext request,
            ContainerResponseContext response) throws IOException {
        response.getHeaders().add("Access-Control-Allow-Origin", ServiceConfig.getProperty("Allow-Origin"));
        response.getHeaders().add("Access-Control-Allow-Headers",ServiceConfig.getProperty("Allow-Headers"));
        response.getHeaders().add("Access-Control-Allow-Credentials", ServiceConfig.getProperty("Allow-Credentials"));
        response.getHeaders().add("Access-Control-Allow-Methods",ServiceConfig.getProperty("Allow-Methods"));
        response.getHeaders().add("Access-Control-Expose-Headers",ServiceConfig.getProperty("Expose-Headers"));
        response.getHeaders().add("Access-Control-Max-Age",ServiceConfig.getProperty("Max-Age"));
    }
}
