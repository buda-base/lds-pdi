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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class CorsFilter implements Filter {

    public static final int ACCESS_CONTROL_MAX_AGE_IN_SECONDS = 24 * 60 * 60;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        ((HttpServletResponse) response).addHeader("Access-Control-Allow-Origin", "*");
        ((HttpServletResponse) response).addHeader("Access-Control-Allow-Credentials", "true");
        ((HttpServletResponse) response).addHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
        ((HttpServletResponse) response).addHeader("Access-Control-Allow-Headers",

                "Origin, Accept, X-Requested-With, Content-Type, " + "Access-Control-Request-Method, Access-Control-Request-Headers, "
                        + "Authorization, Keep-Alive, User-Agent, If-Modified-Since, If-None-Match, Cache-Control");
        ((HttpServletResponse) response).addHeader("Access-Control-Expose-Headers",
                "ETag, Last-Modified, Content-Type, Cache-Control, Vary, Access-Control-Max-Age");
        // ((HttpServletResponse) response).addHeader("Access-Control-Max-Age",
        // "private," + Integer.toString(ACCESS_CONTROL_MAX_AGE_IN_SECONDS));
        chain.doFilter(request, response);
    }
}
