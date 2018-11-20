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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.objects.json.QueryListItem;
import io.bdrc.ldspdi.objects.json.QueryTemplate;
import io.bdrc.ldspdi.rest.features.CorsFilter;
import io.bdrc.ldspdi.rest.features.GZIPWriterInterceptor;
import io.bdrc.ldspdi.rest.features.JerseyCacheControl;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.LdsQuery;
import io.bdrc.ldspdi.sparql.LdsQueryService;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ldspdi.utils.ResponseOutputStream;
import io.bdrc.restapi.exceptions.RestException;


@Path("/")
public class JsonAPIResource {

    public final static Logger log=LoggerFactory.getLogger(JsonAPIResource.class.getName());
    private ArrayList<String> fileList;

    public JsonAPIResource() {
        super();
        ResourceConfig config=new ResourceConfig( JsonAPIResource.class);
        config.register(CorsFilter.class);
        config.register(GZIPWriterInterceptor.class);
        fileList=getQueryTemplates();
    }

    @GET
    @Path("/queries")
    @JerseyCacheControl()
    public Response queriesListGet() throws RestException{
        log.info("Call to queriesListGet()");
        return Response.ok(
                ResponseOutputStream.getJsonResponseStream(getQueryListItems(fileList))).build();
    }

    @POST
    @Path("/queries")
    @JerseyCacheControl()
    @Produces(MediaType.APPLICATION_JSON)
    public ArrayList<QueryListItem> queriesListPost() throws RestException{
        log.info("Call to queriesListPost()");
        return getQueryListItems(fileList);
    }

    @GET
    @Path("/queries/{template}")
    @JerseyCacheControl()
    public Response queryDescGet(@PathParam("template") String name) throws RestException {
        log.info("Call to queryDescGet()");
        final LdsQuery qfp = LdsQueryService.get(name+".arq");
        return Response.ok(
                ResponseOutputStream.getJsonResponseStream(qfp.getTemplate())).build();
    }

    @POST
    @Path("/queries/{template}")
    @JerseyCacheControl()
    @Produces(MediaType.APPLICATION_JSON)
    public QueryTemplate queryDescPost(@PathParam("template") String name) throws RestException{
        log.info("Call to queryDescPost()");
        final LdsQuery qfp = LdsQueryService.get(name+".arq");
        return qfp.getTemplate();
    }

    private ArrayList<QueryListItem> getQueryListItems(ArrayList<String> filesList) throws RestException{
        ArrayList<QueryListItem> items=new ArrayList<>();
        for(String file:filesList) {
            final LdsQuery qfp = LdsQueryService.get(file+".arq");
            QueryTemplate qt=qfp.getTemplate();
            items.add(new QueryListItem(qt.getId(),"/queries/"+qt.getId(),qt.getQueryResults()));
        }
        return items;
    }

    private ArrayList<String> getQueryTemplates() {
        ArrayList<String> files=new ArrayList<>();
        java.nio.file.Path dpath = Paths.get(ServiceConfig.getProperty(QueryConstants.QUERY_PATH)+"public");
        if (Files.isDirectory(dpath)) {
            try (DirectoryStream<java.nio.file.Path> stream = Files.newDirectoryStream(dpath)) {
                for (java.nio.file.Path path : stream) {
                    String tmp=path.toString();
                    if(tmp.endsWith(".arq")) {
                        files.add(tmp.substring(tmp.lastIndexOf("/")+1));
                    }
                }
            } catch (IOException e) {
                log.error("Error while getting query templates", e);
                e.printStackTrace();
            }
        }
        return files;
    }

}
