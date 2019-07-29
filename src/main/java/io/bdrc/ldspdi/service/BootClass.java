package io.bdrc.ldspdi.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.ldspdi.ontology.service.core.OntData;
import io.bdrc.ldspdi.results.ResultsCache;
import io.bdrc.restapi.exceptions.RestException;
import io.bdrc.taxonomy.TaxModel;

public class BootClass implements ServletContextListener {

    public final static Logger log = LoggerFactory.getLogger(BootClass.class.getName());

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        // Do nothing;
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        try {

            final String configPath = System.getProperty("ldspdi.configpath");
            ServiceConfig.init();
            ResultsCache.init();
            GitService.update(ServiceConfig.getProperty("queryPath"));
            OntData.init();
            TaxModel.fetchModel();
            InputStream is = new FileInputStream(configPath + "ldspdi-private.properties");
            Properties private_props = new Properties();
            private_props.load(is);
            private_props.putAll(ServiceConfig.getProperties());
            AuthProps.init(private_props);
            if (ServiceConfig.useAuth()) {
                // RdfAuthModel.updateAuthData(fuseki);
                // For applications
                RdfAuthModel.readAuthModel();
            }
            log.info("BootClass has been properly initialized");
        } catch (IllegalArgumentException | RestException | IOException e) {
            log.error("BootClass init error", e);
        }
    }

}
