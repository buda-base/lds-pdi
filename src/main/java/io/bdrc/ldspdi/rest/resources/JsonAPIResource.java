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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.springframework.stereotype.Component;

import io.bdrc.ldspdi.objects.json.QueryListItem;
import io.bdrc.ldspdi.objects.json.QueryTemplate;
import io.bdrc.ldspdi.rest.features.CorsFilter;
import io.bdrc.ldspdi.rest.features.SpringCacheControl;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.LdsQuery;
import io.bdrc.ldspdi.sparql.LdsQueryService;
import io.bdrc.ldspdi.utils.ResponseOutputStream;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

@Component
@Path("/")
public class JsonAPIResource {

	public final static Logger log = LoggerFactory.getLogger(JsonAPIResource.class.getName());
	private List<String> fileList;

	public JsonAPIResource() throws RestException {
		super();
		ResourceConfig config = new ResourceConfig(JsonAPIResource.class);
		config.register(CorsFilter.class);
		fileList = getQueryTemplates();
	}

	@GET
	@Path("/queries")
	@SpringCacheControl()
	@Produces(MediaType.APPLICATION_JSON)
	public Response queriesListGet() throws RestException {
		log.info("Call to queriesListGet()");
		return Response.ok(ResponseOutputStream.getJsonResponseStream(getQueryListItems(fileList)), MediaType.APPLICATION_JSON_TYPE).build();
	}

	@POST
	@Path("/queries")
	@SpringCacheControl()
	@Produces(MediaType.APPLICATION_JSON)
	public Response queriesListPost() throws RestException {
		log.info("Call to queriesListPost()");
		return Response.ok(ResponseOutputStream.getJsonResponseStream(getQueryListItems(fileList)), MediaType.APPLICATION_JSON_TYPE).build();
	}

	@GET
	@Path("/queries/{template}")
	@SpringCacheControl()
	@Produces(MediaType.APPLICATION_JSON)
	public Response queryDescGet(@PathParam("template") String name) throws RestException {
		log.info("Call to queryDescGet()");
		final LdsQuery qfp = LdsQueryService.get(name + ".arq");
		return Response.ok(ResponseOutputStream.getJsonResponseStream(qfp.getTemplate()), MediaType.APPLICATION_JSON_TYPE).build();
	}

	@POST
	@Path("/queries/{template}")
	@SpringCacheControl()
	@Produces(MediaType.APPLICATION_JSON)
	public Response queryDescPost(@PathParam("template") String name) throws RestException {
		log.info("Call to queryDescPost()");
		final LdsQuery qfp = LdsQueryService.get(name + ".arq");
		return Response.ok(ResponseOutputStream.getJsonResponseStream(qfp.getTemplate()), MediaType.APPLICATION_JSON_TYPE).build();
	}

	private ArrayList<QueryListItem> getQueryListItems(List<String> filesList) throws RestException {
		ArrayList<QueryListItem> items = new ArrayList<>();
		for (String file : filesList) {
			String tmp = file.substring(file.lastIndexOf('/') + 1);
			// final LdsQuery qfp = LdsQueryService.get(file + ".arq");
			final LdsQuery qfp = LdsQueryService.get(tmp);
			QueryTemplate qt = qfp.getTemplate();
			items.add(new QueryListItem(qt.getId(), "/queries/" + qt.getId(), qt.getQueryResults()));
		}
		return items;
	}

	private static List<String> getQueryTemplates() throws RestException {
		List<String> files = new ArrayList<>();
		java.nio.file.Path dpath = Paths.get(ServiceConfig.LOCAL_QUERIES_DIR + "public");
		Stream<java.nio.file.Path> walk;
		try {
			walk = Files.walk(dpath);
			files = walk.map(x -> x.toString()).filter(f -> f.endsWith(".arq")).collect(Collectors.toList());
		} catch (IOException e1) {
			log.error("Error while getting query templates", e1);
			e1.printStackTrace();
			throw new RestException(500, new LdsError(LdsError.MISSING_RES_ERR).setContext(ServiceConfig.LOCAL_QUERIES_DIR + "public in DocFileModel.getQueryTemplates()"));
		}
		walk.close();
		return files;
	}

}
