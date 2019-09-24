package io.bdrc.ldspdi.rest.resources;

import java.io.ByteArrayInputStream;

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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import org.apache.commons.io.IOUtils;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFWriter;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.bdrc.formatters.TTLRDFWriter;
import io.bdrc.ldspdi.export.MarcExport;
import io.bdrc.ldspdi.export.TxtEtextExport;
import io.bdrc.ldspdi.ontology.service.core.OntClassModel;
import io.bdrc.ldspdi.ontology.service.core.OntData;
import io.bdrc.ldspdi.ontology.service.core.OntPolicy;
import io.bdrc.ldspdi.ontology.service.core.OntPropModel;
import io.bdrc.ldspdi.rest.features.JerseyCacheControl;
import io.bdrc.ldspdi.results.CacheAccessModel;
import io.bdrc.ldspdi.results.ResultsCache;
import io.bdrc.ldspdi.service.OntPolicies;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.Prefixes;
import io.bdrc.ldspdi.sparql.QueryProcessor;
import io.bdrc.ldspdi.utils.DocFileModel;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.ldspdi.utils.MediaTypeUtils;
import io.bdrc.ldspdi.utils.ResponseOutputStream;
import io.bdrc.restapi.exceptions.ErrorMessage;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;

@Component
@Path("/")

public class PublicDataResource {

	public final static Logger log = LoggerFactory.getLogger(PublicDataResource.class.getName());

	public static final String RES_PREFIX_SHORT = ServiceConfig.getProperty("endpoints.resource.shortprefix");
	public static final String RES_PREFIX = ServiceConfig.getProperty("endpoints.resource.fullprefix");
	public static final String ADM_PREFIX_SHORT = ServiceConfig.getProperty("endpoints.admindata.shortprefix");
	public static final String GRAPH_PREFIX_SHORT = ServiceConfig.getProperty("endpoints.graph.shortprefix");

	@GET
	@Path("/")
	public void getHomePage(@Context HttpServletResponse response) throws RestException, IOException {
		log.info("Call to getHomePage()");
		response.sendRedirect("/index");
	}

	@GET
	@JerseyCacheControl()
	@Path("index")
	@Produces(MediaType.TEXT_XML)
	public Response getIndexPage() throws RestException, IOException {
		log.info("Call to getIndexPage()");
		DocFileModel dfm = new DocFileModel();
		return Response.ok(new Viewable("/index.jsp", dfm)).encoding("UTF-8").build();
	}

	@GET
	@Path("/robots.txt")
	public Response getRobots() {
		log.info("Call getRobots()");
		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {
				os.write(ServiceConfig.getRobots().getBytes());
			}
		};
		return Response.ok(stream, MediaType.TEXT_PLAIN_TYPE).encoding("UTF-8").build();
	}

	@GET
	@Path("cache")
	@Produces(MediaType.TEXT_HTML)
	public Response getCacheInfo() {
		log.info("Call to getCacheInfo()");
		return Response.ok(new Viewable("/cache.jsp", new CacheAccessModel())).encoding("UTF-8").build();
	}

	@GET
	@Path("choice")
	@Produces(MediaType.TEXT_HTML)
	public Response getMultiChoice(@QueryParam("path") String it, @Context UriInfo info) {
		log.info("Call to getMultiChoice() with path={}", it);
		return Response.ok(new Viewable("/multiChoice.jsp", info.getBaseUri() + it)).encoding("UTF-8").build();
	}

	@GET
	@Path("/context.jsonld")
	public Response getJsonContext(@Context Request request) throws RestException {
		log.info("Call to getJsonContext()");
		EntityTag tag = OntData.getEntityTag();
		ResponseBuilder builder = request.evaluatePreconditions(tag);
		if (builder == null) {
			builder = Response.ok(OntData.JSONLD_CONTEXT, MediaTypeUtils.MT_JSONLD);
			builder.header("Last-Modified", OntData.getLastUpdated()).tag(tag);
		}
		return builder.encoding("UTF-8").build();
	}

	@GET
	@Path("/admindata/{res}")
	@JerseyCacheControl()
	public Response getAdResourceGraph(@PathParam("res") final String res, @HeaderParam("fusekiUrl") final String fusekiUrl, @HeaderParam("Accept") String format, @Context UriInfo info, @Context Request request) throws RestException {
		final String prefixedRes = ADM_PREFIX_SHORT + res;
		final Variant variant = request.selectVariant(MediaTypeUtils.resVariants);
		log.info("Call to getAdResourceGraph with format: {} variant is {}", format, variant);
		if (format == null) {
			String html = Helpers.getMultiChoicesHtml(info.getPath(), true);
			ResponseBuilder rb = Response.status(300).encoding("UTF-8").entity(html).header("Content-Type", "text/html").header("Content-Location", info.getBaseUri() + "choice?path=" + info.getPath());
			return setHeaders(rb, getResourceHeaders(info.getPath(), null, "List", null)).build();
		}
		if (variant == null) {
			String html = Helpers.getMultiChoicesHtml(info.getPath(), true);
			ResponseBuilder rb = Response.status(406).encoding("UTF-8").entity(html).header("Content-Type", "text/html").header("Content-Location", info.getBaseUri() + "choice?path=" + info.getPath());
			return setHeaders(rb, getResourceHeaders(info.getPath(), null, "List", null)).build();
		}
		MediaType mediaType = variant.getMediaType();
		if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
			try {
				ResponseBuilder builder = Response.seeOther(new URI(ServiceConfig.getProperty("showUrl") + prefixedRes));
				return setHeaders(builder, getResourceHeaders(info.getPath(), null, "Choice", null)).build();
			} catch (URISyntaxException e) {
				throw new RestException(500, new LdsError(LdsError.URI_SYNTAX_ERR).setContext("getResourceGraphGet()", e));
			}
		}
		Model model = QueryProcessor.getDescribeModel(prefixedRes, fusekiUrl, null);
		if (model.size() == 0) {
			LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes);
			return Response.status(404).encoding("UTF-8").entity(ResponseOutputStream.getExceptionStream(ErrorMessage.getErrorMessage(404, lds))).type(MediaType.APPLICATION_JSON).build();
		}
		String ext = MediaTypeUtils.getExtFromMime(mediaType);
		ResponseBuilder builder = Response.ok(ResponseOutputStream.getModelStream(model, ext, RES_PREFIX + res, null), mediaType).encoding("UTF-8");
		return setHeaders(builder, getResourceHeaders(info.getPath(), ext, "Choice", getEtag(model, res))).build();
	}

	@GET
	@Path("/admindata/{res}.{ext}")
	@JerseyCacheControl()
	public Response getAdResourceGraphExt(@PathParam("res") final String res, @PathParam("ext") final String ext, @HeaderParam("fusekiUrl") final String fusekiUrl, @HeaderParam("Accept") String format, @Context UriInfo info, @Context Request request)
			throws RestException {
		final String prefixedRes = ADM_PREFIX_SHORT + res;
		final String graphType = "describe";
		final MediaType media = MediaTypeUtils.getMimeFromExtension(ext);
		if (media == null) {
			final String html = Helpers.getMultiChoicesHtml("/resource/" + res, true);
			final ResponseBuilder rb = Response.status(300).encoding("UTF-8").entity(html).header("Content-Type", "text/html").header("Content-Location", info.getBaseUri() + "choice?path=" + info.getPath());
			return rb.build();
		}
		if (media.equals(MediaType.TEXT_HTML_TYPE)) {
			throw new RestException(406, new LdsError(LdsError.GENERIC_ERR).setContext(prefixedRes));
		}
		final Model model = QueryProcessor.getCoreResourceGraph(prefixedRes, fusekiUrl, null, graphType);
		if (model.size() == 0) {
			LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes);
			return Response.status(404).entity(ResponseOutputStream.getExceptionStream(ErrorMessage.getErrorMessage(404, lds))).type(MediaType.APPLICATION_JSON).build();
		}
		final ResponseBuilder builder = Response.ok(ResponseOutputStream.getModelStream(model, ext, prefixedRes, null), media).encoding("UTF-8");
		return setHeaders(builder, getResourceHeaders(info.getPath(), ext, null, getEtag(model, res))).build();
	}

	@GET
	@Path("/graph/{res}.{ext}")
	@JerseyCacheControl()
	public Response getGrResourceGraphExt(@PathParam("res") final String res, @PathParam("ext") final String ext, @HeaderParam("fusekiUrl") final String fusekiUrl, @HeaderParam("Accept") String format, @Context UriInfo info, @Context Request request)
			throws RestException {
		final String prefixedRes = GRAPH_PREFIX_SHORT + res;
		final String graphType = "graph";
		final MediaType media = MediaTypeUtils.getMimeFromExtension(ext);
		if (media == null) {
			final String html = Helpers.getMultiChoicesHtml("/resource/" + res, true);
			final ResponseBuilder rb = Response.status(300).entity(html).header("Content-Type", "text/html").header("Content-Location", info.getBaseUri() + "choice?path=" + info.getPath());
			return rb.encoding("UTF-8").build();
		}
		if (media.equals(MediaType.TEXT_HTML_TYPE)) {
			throw new RestException(406, new LdsError(LdsError.GENERIC_ERR).setContext(prefixedRes));
		}
		final Model model = QueryProcessor.getCoreResourceGraph(prefixedRes, fusekiUrl, null, graphType);
		if (model.size() == 0) {
			LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes);
			return Response.status(404).encoding("UTF-8").entity(ResponseOutputStream.getExceptionStream(ErrorMessage.getErrorMessage(404, lds))).type(MediaType.APPLICATION_JSON).build();
		}
		final ResponseBuilder builder = Response.ok(ResponseOutputStream.getModelStream(model, ext, prefixedRes, null), media).encoding("UTF-8");
		return setHeaders(builder, getResourceHeaders(info.getPath(), ext, null, getEtag(model, res))).build();
	}

	@GET
	@Path("/graph/{res}")
	@JerseyCacheControl()
	public Response getGrResourceGraph(@PathParam("res") final String res, @HeaderParam("fusekiUrl") final String fusekiUrl, @HeaderParam("Accept") String format, @Context UriInfo info, @Context Request request) throws RestException {
		final String prefixedRes = GRAPH_PREFIX_SHORT + res;
		final String graphType = "graph";
		final Variant variant = request.selectVariant(MediaTypeUtils.resVariants);
		if (format == null) {
			String html = Helpers.getMultiChoicesHtml(info.getPath(), true);
			ResponseBuilder rb = Response.status(300).entity(html).header("Content-Type", "text/html").header("Content-Location", info.getBaseUri() + "choice?path=" + info.getPath());
			return setHeaders(rb, getResourceHeaders(info.getPath(), null, "List", null)).build();
		}
		if (variant == null) {
			String html = Helpers.getMultiChoicesHtml(info.getPath(), true);
			ResponseBuilder rb = Response.status(406).entity(html).header("Content-Type", "text/html").header("Content-Location", info.getBaseUri() + "choice?path=" + info.getPath());
			return setHeaders(rb, getResourceHeaders(info.getPath(), null, "List", null)).build();
		}
		MediaType mediaType = variant.getMediaType();
		if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
			try {
				ResponseBuilder builder = Response.seeOther(new URI(info.getAbsolutePath() + ".trig")).status(Status.FOUND).encoding("UTF-8");
				return setHeaders(builder, getResourceHeaders(info.getPath(), null, "Choice", null)).build();
			} catch (URISyntaxException e) {
				throw new RestException(500, new LdsError(LdsError.URI_SYNTAX_ERR).setContext("getResourceGraphGet()", e));
			}
		}
		Model model = QueryProcessor.getCoreResourceGraph(prefixedRes, fusekiUrl, null, graphType);
		if (model.size() == 0) {
			LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes);
			return Response.status(404).entity(ResponseOutputStream.getExceptionStream(ErrorMessage.getErrorMessage(404, lds))).type(MediaType.APPLICATION_JSON).build();
		}
		String ext = MediaTypeUtils.getExtFromMime(mediaType);
		ResponseBuilder builder = Response.ok(ResponseOutputStream.getModelStream(model, ext, RES_PREFIX + res, null), mediaType).encoding("UTF-8");
		return setHeaders(builder, getResourceHeaders(info.getPath(), ext, "Choice", getEtag(model, res))).build();
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("/prefixes")
	@JerseyCacheControl()
	public Response getPrefixes(@Context UriInfo info, @Context Request request) throws RestException {
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefixes(Prefixes.getMap());
		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {
				model.write(os, "TURTLE");
			}
		};
		ResponseBuilder builder = Response.ok(stream, MediaTypeUtils.getMimeFromExtension("ttl"));
		return builder.encoding("UTF-8").build();
	}

	@GET
	@Path("/resource/{res}")
	@JerseyCacheControl()
	public Response getResourceGraph(@PathParam("res") final String res, @HeaderParam("fusekiUrl") final String fusekiUrl, @HeaderParam("Accept") String format, @Context UriInfo info, @Context Request request) throws RestException {
		final String prefixedRes = RES_PREFIX_SHORT + res;
		log.info("Call to getResourceGraphGET() with URL: {}, accept: {}", info.getPath(), format);
		log.info("Call to getResourceGraphGET() " + info.getQueryParameters().keySet().contains("graph"));
		final Variant variant = request.selectVariant(MediaTypeUtils.resVariants);
		if (format == null) {
			final String html = Helpers.getMultiChoicesHtml(info.getPath(), true);
			final ResponseBuilder rb = Response.status(300).entity(html).header("Content-Type", "text/html").header("Content-Location", info.getBaseUri() + "choice?path=" + info.getPath());
			return setHeaders(rb, getResourceHeaders(info.getPath(), null, "List", null)).build();
		}
		if (variant == null) {
			final String html = Helpers.getMultiChoicesHtml(info.getPath(), true);
			final ResponseBuilder rb = Response.status(406).entity(html).header("Content-Type", "text/html").header("Content-Location", info.getBaseUri() + "choice?path=" + info.getPath());
			return setHeaders(rb, getResourceHeaders(info.getPath(), null, "List", null)).build();
		}
		final MediaType mediaType = variant.getMediaType();
		if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
			try {
				// ResponseBuilder builder = Response.seeOther(new
				// URI(ServiceConfig.getProperty("showUrl") + prefixedRes));
				String type = getDilaResourceType(res);
				if (!type.equals("")) {
					type = type + "/?fromInner=";
				} else {
					type = RES_PREFIX_SHORT;
				}
				ResponseBuilder builder = Response.seeOther(new URI(ServiceConfig.getProperty("showUrl") + type + res));
				return setHeaders(builder, getResourceHeaders(info.getPath(), null, "Choice", null)).build();
			} catch (URISyntaxException e) {
				throw new RestException(500, new LdsError(LdsError.URI_SYNTAX_ERR).setContext("getResourceGraphGet()", e));
			}
		}
		final Model model = QueryProcessor.getCoreResourceGraph(prefixedRes, fusekiUrl, null, computeGraphType(info));
		if (model.size() == 0) {
			LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes);
			return Response.status(404).entity(ResponseOutputStream.getExceptionStream(ErrorMessage.getErrorMessage(404, lds))).type(MediaType.APPLICATION_JSON).build();
		}
		final String ext = MediaTypeUtils.getExtFromMime(mediaType);
		final ResponseBuilder builder = Response.ok(ResponseOutputStream.getModelStream(model, ext, RES_PREFIX + res, null), mediaType).encoding("UTF-8");
		return setHeaders(builder, getResourceHeaders(info.getPath(), ext, "Choice", getEtag(model, res))).build();
	}

	@GET
	@Path("/resource/{res}.{ext}")
	@JerseyCacheControl()
	public Response getFormattedResourceGraph(@PathParam("res") final String res, @PathParam("ext") final String ext, @DefaultValue("0") @QueryParam("startChar") Integer startChar, @DefaultValue("999999999") @QueryParam("endChar") Integer endChar,
			@HeaderParam("fusekiUrl") String fusekiUrl, @Context final UriInfo info) throws RestException {
		log.info("Call to getFormattedResourceGraph()");
		final String prefixedRes = RES_PREFIX_SHORT + res;
		final MediaType media = MediaTypeUtils.getMimeFromExtension(ext);
		if (media == null) {
			final String html = Helpers.getMultiChoicesHtml("/resource/" + res, true);
			final ResponseBuilder rb = Response.status(300).entity(html).header("Content-Type", "text/html").header("Content-Location", info.getBaseUri() + "choice?path=" + info.getPath());
			return rb.build();
		}
		if (media.equals(MediaType.TEXT_HTML_TYPE)) {
			try {
				String type = getDilaResourceType(res);
				if (!type.equals("")) {
					type = type + "/?fromInner=";
				} else {
					type = RES_PREFIX_SHORT;
				}
				ResponseBuilder builder = Response.seeOther(new URI(ServiceConfig.getProperty("showUrl") + type + res)).encoding("UTF-8");
				return setHeaders(builder, getResourceHeaders(info.getPath(), null, null, null)).build();
			} catch (URISyntaxException e) {
				throw new RestException(500, new LdsError(LdsError.URI_SYNTAX_ERR).setContext("getResourceGraphPost()", e));
			}
		}
		if (ext.startsWith("mrc")) {
			return MarcExport.getResponse(media, RES_PREFIX + res);
		}
		if (ext.equals("txt")) {
			return TxtEtextExport.getResponse(RES_PREFIX + res, startChar, endChar);
		}
		final Model model = QueryProcessor.getCoreResourceGraph(prefixedRes, fusekiUrl, null, computeGraphType(info));
		if (model.size() == 0) {
			LdsError lds = new LdsError(LdsError.NO_GRAPH_ERR).setContext(prefixedRes);
			return Response.status(404).entity(ResponseOutputStream.getExceptionStream(ErrorMessage.getErrorMessage(404, lds))).type(MediaType.APPLICATION_JSON).build();
		}
		final ResponseBuilder builder = Response.ok(ResponseOutputStream.getModelStream(model, ext, RES_PREFIX + res, null), media).encoding("UTF-8");
		return setHeaders(builder, getResourceHeaders(info.getPath(), ext, null, getEtag(model, res))).build();
	}

	@GET
	@Path("/{base : .*}/{other}")
	@JerseyCacheControl()
	public Response getExtOntologyHomePage(@Context final UriInfo info, @Context Request request, @HeaderParam("Accept") String format, @PathParam("base") String base, @PathParam("other") String other) throws RestException, IOException {
		ResponseBuilder builder = null;
		String path = info.getBaseUri().toString();
		log.info("getExtOntologyHomePage WAS CALLED WITH >> {}/{} and format: {}", base, other, format);
		boolean isBase = false;
		String baseUri = "";
		String tmp = info.getAbsolutePath().toString().replace("https", "http");
		log.info("getExtOntologyHomePage tmp is >> {}", tmp);
		if (OntPolicies.isBaseUri(tmp)) {
			baseUri = parseBaseUri(tmp);
			isBase = true;
		}
		log.info("getExtOntologyHomePage absolute path >> {}{}", info.getAbsolutePath(), other);
		if (OntPolicies.isBaseUri(parseBaseUri(tmp + other))) {
			baseUri = parseBaseUri(tmp + other);
			isBase = true;
		}
		log.info("getExtOntologyHomePage baseUri is >> {}", baseUri);
		// Is the full request uri a baseuri?
		if (isBase) {
			OntPolicy pr = OntPolicies.getOntologyByBase(baseUri);
			// if accept header is present
			if (format != null) {
				Variant variant = request.selectVariant(MediaTypeUtils.resVariants);
				if (variant == null) {
					return Response.status(406).build();
				}
				String url = OntPolicies.getOntologyByBase(baseUri).getFileUri();
				// using cache if available
				byte[] byteArr = (byte[]) ResultsCache.getObjectFromCache(url.hashCode());
				if (byteArr == null) {
					HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
					InputStream input = connection.getInputStream();
					byteArr = IOUtils.toByteArray(input);
					input.close();
					ResultsCache.addToCache(byteArr, url.hashCode());
				}
				OntModelSpec oms = new OntModelSpec(OntModelSpec.OWL_MEM);
				OntDocumentManager odm = new OntDocumentManager();
				odm.setProcessImports(false);
				oms.setDocumentManager(odm);
				OntModel om = OntData.getOntModelByBase(baseUri);
				OntData.setOntModel(om);
				om.read(new ByteArrayInputStream(byteArr), baseUri, "TURTLE");
				MediaType mediaType = variant.getMediaType();
				// browser request : serving html page
				if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
					builder = Response.ok(new Viewable("/ontologyHome.jsp", path)).header("ContentType", "text/html;charset=utf-8");
				} else {
					final String JenaLangStr = MediaTypeUtils.getJenaFromExtension(MediaTypeUtils.getExtFromMime(mediaType));
					final StreamingOutput stream = new StreamingOutput() {
						@Override
						public void write(OutputStream os) throws IOException, WebApplicationException {
							if (JenaLangStr == "STTL") {
								final RDFWriter writer = TTLRDFWriter.getSTTLRDFWriter(om, pr.getBaseUri());
								writer.output(os);
							} else {
								org.apache.jena.rdf.model.RDFWriter wr = om.getWriter(JenaLangStr);
								if (JenaLangStr.equals(RDFLanguages.strLangRDFXML)) {
									wr.setProperty("xmlbase", pr.getBaseUri());
								}
								wr.write(om, os, pr.getBaseUri());
							}
						}
					};
					builder = Response.ok(stream, MediaTypeUtils.getMimeFromExtension(MediaTypeUtils.getExtFromMime(mediaType)));
				}
			}
		} else {
			if (OntData.ontAllMod.getOntResource(tmp) == null) {
				LdsError lds = new LdsError(LdsError.ONT_URI_ERR).setContext("Ont resource is null for " + tmp);
				return Response.status(404).entity(ResponseOutputStream.getExceptionStream(ErrorMessage.getErrorMessage(404, lds))).type(MediaType.APPLICATION_JSON).build();
			}
			if (builder == null) {
				if (OntData.isClass(tmp, true)) {
					log.info("CLASS>>" + tmp);
					builder = Response.ok(new Viewable("/ontClassView.jsp", new OntClassModel(tmp, true))).header("ContentType", "text/html;charset=utf-8");
				} else {
					log.info("PROP>>" + tmp);
					builder = Response.ok(new Viewable("/ontPropView.jsp", new OntPropModel(tmp, true))).header("ContentType", "text/html;charset=utf-8");
				}
			}
		}
		return builder.encoding("UTF-8").build();
	}

	@GET
	@Path("/{base : .*}/{other}.{ext}")
	@Produces("text/html")
	@JerseyCacheControl()
	public Response getOntologyResourceAsFile(@Context final UriInfo info, @Context Request request, @PathParam("base") String base, @PathParam("other") String other, @PathParam("ext") String ext) throws RestException {
		String res = info.getAbsolutePath().toString().replace("https", "http");
		res = res.substring(0, res.lastIndexOf('.')) + "/";
		log.info("In getOntologyResourceAsFile(), RES = {}", res);
		ResponseBuilder builder = null;
		final String JenaLangStr = MediaTypeUtils.getJenaFromExtension(ext);
		if (JenaLangStr == null) {
			LdsError lds = new LdsError(LdsError.URI_SYNTAX_ERR).setContext(info.getAbsolutePath().toString());
			return Response.status(404).entity(ResponseOutputStream.getExceptionStream(ErrorMessage.getErrorMessage(404, lds))).type(MediaType.APPLICATION_JSON).build();
		}
		if (OntPolicies.isBaseUri(res)) {
			OntPolicy params = OntPolicies.getOntologyByBase(parseBaseUri(res));
			OntModel model = OntData.getOntModelByBase(params.getBaseUri());
			final StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream os) throws IOException, WebApplicationException {
					if (JenaLangStr == "STTL") {
						model.write(os, "TURTLE");
					} else {
						org.apache.jena.rdf.model.RDFWriter wr = model.getWriter(JenaLangStr);
						if (JenaLangStr.equals(RDFLanguages.strLangRDFXML)) {
							wr.setProperty("xmlbase", params.getBaseUri());
						}
						model.write(os, JenaLangStr);
					}
				}
			};
			builder = Response.ok(stream, MediaTypeUtils.getMimeFromExtension(ext));
		} else {
			LdsError lds = new LdsError(LdsError.ONT_URI_ERR).setContext(info.getAbsolutePath().toString());
			return Response.status(404).entity(ResponseOutputStream.getExceptionStream(ErrorMessage.getErrorMessage(404, lds))).type(MediaType.APPLICATION_JSON).build();
		}
		return builder.encoding("UTF-8").build();
	}

	@GET
	@Path("/ontology/data/{ext}")
	@Produces("text/html")
	@JerseyCacheControl()
	public Response getAllOntologyData(@Context final UriInfo info, @Context Request request, @PathParam("ext") String ext) throws RestException {
		ResponseBuilder builder = null;
		final String JenaLangStr = MediaTypeUtils.getJenaFromExtension(ext);
		if (JenaLangStr == null) {
			LdsError lds = new LdsError(LdsError.URI_SYNTAX_ERR).setContext(info.getAbsolutePath().toString());
			return Response.status(404).entity(ResponseOutputStream.getExceptionStream(ErrorMessage.getErrorMessage(404, lds))).type(MediaType.APPLICATION_JSON).build();
		}
		OntModel model = OntData.ontAllMod;
		final StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {
				if (JenaLangStr == "STTL") {
					model.write(os, "TURTLE");
				} else {
					model.write(os, JenaLangStr);
				}
			}
		};
		builder = Response.ok(stream, MediaTypeUtils.getMimeFromExtension(ext));
		return builder.encoding("UTF-8").build();
	}

	@POST
	@Path("/callbacks/github/owl-schema")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateOntology() throws RestException {
		log.info("updating Ontology models() >>");
		Thread t = new Thread(new OntData());
		t.start();
		return Response.ok("Ontologies were updated").build();
	}

	private static HashMap<String, String> getResourceHeaders(String url, String ext, String tcn, String eTag) {
		HashMap<String, MediaType> map = MediaTypeUtils.getResExtensionMimeMap();
		HashMap<String, String> headers = new HashMap<>();
		if (ext != null) {
			if (url.indexOf(".") < 0) {
				headers.put("Content-Location", url + "." + ext);
			} else {
				url = url.substring(0, url.lastIndexOf("."));
			}
		}
		StringBuilder sb = new StringBuilder("");
		for (Entry<String, MediaType> e : map.entrySet()) {
			sb.append("{\"" + url + "." + e.getKey() + "\" 1.000 {type " + e.getValue().toString() + "}},");
		}
		headers.put("Alternates", sb.toString().substring(0, sb.toString().length() - 1));
		if (tcn != null)
			headers.put("TCN", tcn);
		headers.put("Vary", "Negotiate, Accept");
		if (eTag != null) {
			headers.put("ETag", eTag);
		}
		return headers;
	}

	private static ResponseBuilder setHeaders(ResponseBuilder builder, HashMap<String, String> headers) {
		for (String key : headers.keySet()) {
			builder.header(key, headers.get(key));
		}
		return builder;
	}

	private static String getEtag(Model model, String res) {
		Statement smt = model.getProperty(ResourceFactory.createResource(RES_PREFIX + res), ResourceFactory.createProperty("http://purl.bdrc.io/ontology/admin/gitRevision"));
		if (smt != null) {
			return smt.getObject().toString();
		}
		return null;
	}

	private String parseBaseUri(String s) {
		if (s.endsWith("/") || s.endsWith("#")) {
			s = s.substring(0, s.length() - 1);
		}
		return s;
	}

	private String computeGraphType(UriInfo info) {
		String type = "";
		if (info.getQueryParameters().containsKey("graph")) {
			type = "graph";
		}
		if (info.getQueryParameters().containsKey("describe")) {
			type = "describe";
		}
		return type;
	}

	private String getDilaResourceType(String res) {
		String type = "";
		boolean buda = Boolean.parseBoolean(ServiceConfig.getProperty("isBUDA"));
		if (buda) {
			return "";
		}
		if (res.startsWith("A")) {
			return "person";
		}
		if (res.startsWith("P")) {
			return "place";
		}
		return type;
	}

}