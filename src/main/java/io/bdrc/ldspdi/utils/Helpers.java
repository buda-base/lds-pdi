package io.bdrc.ldspdi.utils;

/*******************************************************************************
 * Copyright (c) 2017 Buddhist Digital Resource Center (BDRC)
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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.formatters.JSONLDFormatter;
import io.bdrc.formatters.JSONLDFormatter.DocType;
import io.bdrc.formatters.TTLRDFWriter;

public class Helpers {

	public final static Logger log = LoggerFactory.getLogger(Helpers.class.getName());

	public static StringBuffer multiChoiceTpl = getTemplateStr("multiChoice.tpl");

	public static InputStream getResourceOrFile(final String baseName) {
		InputStream stream = null;
		stream = Helpers.class.getClassLoader().getResourceAsStream("/" + baseName);
		if (stream != null) {
			return stream;
		}
		stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("/" + baseName);
		if (stream != null) {
			return stream;
		}
		final String fileBaseName = "src/main/resources/" + baseName;
		try {
			stream = new FileInputStream(fileBaseName);
			return stream;
		} catch (FileNotFoundException e) {
			log.debug("FileNotFound: " + baseName);
			return null;
		}
	}

	public static String bdrcEncode(String url) {
		String encoded = url.replace("\"", "%22");
		encoded = encoded.replace(' ', '+');
		encoded = encoded.replace("\'", "%27");
		return encoded;
	}

	public static boolean isValidURI(String uri) {
		String[] schemes = { "http", "https" };
		UrlValidator urlValidator = new UrlValidator(schemes);
		return urlValidator.isValid(uri);
	}

	public static HashMap<String, String> convertMulti(MultivaluedMap<String, String> map) {
		HashMap<String, String> copy = new HashMap<>();
		Set<String> set = map.keySet();
		for (String key : set) {
			copy.put(key, map.getFirst(key));
		}
		return copy;
	}

	public static StringBuffer getTemplateStr(String tlpPath) {
		final InputStream stream = getResourceOrFile(tlpPath);
		final BufferedReader buffer = new BufferedReader(new InputStreamReader(stream));
		final StringBuffer sb = new StringBuffer();
		try {
			String line = buffer.readLine();
			while (line != null) {
				sb.append(line + System.lineSeparator());
				line = buffer.readLine();

			}
		} catch (IOException e) {
			log.error("Unable to parse the html multi Choices template in Helpers.getMultiChoicesHtml()");
		}
		return sb;
	}

	public static String getMultiChoicesHtml(final String path, final boolean resource) {
		final StringBuilder sb = new StringBuilder();
		for (final Entry<String, MediaType> e : MediaTypeUtils.getResExtensionMimeMap().entrySet()) {
			final String ext = e.getKey();
			final String mimeDesc = e.getValue().toString();
			if (resource) {
				sb.append("<tr><td><a href=\"" + path + "." + ext + "\">" + path + "." + ext + "</a><td>" + mimeDesc + "</td></tr>\n");
			} else {
				sb.append("<tr><td><a href=\"" + path + "&format=" + ext + "\">" + path + "." + ext + "</a><td>" + mimeDesc + "</td></tr>\n");
			}
		}
		final HashMap<String, String> map = new HashMap<>();
		map.put("path", path);
		map.put("rows", sb.toString());
		StringSubstitutor s = new StringSubstitutor(map);
		return s.replace(multiChoiceTpl);
	}

	public static StreamingResponseBody getStream(String obj) {
		final StreamingResponseBody stream = new StreamingResponseBody() {
			@Override
			public void writeTo(final OutputStream os) throws IOException {
				os.write(obj.getBytes());
			}
		};
		return stream;
	}

	public static StreamingResponseBody getJsonObjectStream(Object obj) {
		ObjectMapper om = new ObjectMapper();
		final StreamingResponseBody stream = new StreamingResponseBody() {
			@Override
			public void writeTo(final OutputStream os) throws IOException {
				om.writerWithDefaultPrettyPrinter().writeValue(os, obj);
			}
		};
		return stream;
	}

	public static StreamingResponseBody getModelStream(final Model model, final String format, final String res, DocType docType) {
		final StreamingResponseBody stream = new StreamingResponseBody() {
			@Override
			public void writeTo(OutputStream os) {
				if (format.equals("jsonld")) {
					final Object json = JSONLDFormatter.modelToJsonObject(model, res, docType);
					JSONLDFormatter.jsonObjectToOutputStream(json, os);
					return;
				}
				final String JenaFormat = MediaTypeUtils.getJenaFromExtension(format);
				if (JenaFormat == null || JenaFormat.equals("STTL") || JenaFormat.contentEquals(RDFLanguages.strLangTriG)) {
					final RDFWriter writer = TTLRDFWriter.getSTTLRDFWriter(model, "");
					writer.output(os);
					return;
				}
				model.write(os, JenaFormat);
			}
		};
		return stream;
	}

}