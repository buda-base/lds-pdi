package io.bdrc.ldspdi.utils;

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
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFWriter;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.formatters.JSONLDFormatter;
import io.bdrc.formatters.JSONLDFormatter.DocType;
import io.bdrc.formatters.TTLRDFWriter;
import io.bdrc.restapi.exceptions.ErrorMessage;

public class ResponseOutputStream {

	public static final ObjectMapper om = new ObjectMapper();
	public static boolean prettyPrint = false;

	public static StreamingOutput getJsonResponseStream(Object toJson) {
		return new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {
				if (prettyPrint)
					om.writerWithDefaultPrettyPrinter().writeValue(os, toJson);
				else
					om.writeValue(os, toJson);
			}
		};
	}

	public static StreamingOutput getModelStream(final Model model, final String format, final String res, DocType docType) {
		return new StreamingOutput() {
			@Override
			public void write(OutputStream os) {
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
	}

	public static StreamingOutput getModelStream(final Model model, final String format) {
		return new StreamingOutput() {
			@Override
			public void write(OutputStream os) {
				if (format.equals("jsonld")) {
					JSONLDFormatter.writeModelAsCompact(model, os);
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
	}

	public static StreamingOutput getModelStream(final Model model) {
		return new StreamingOutput() {
			@Override
			public void write(OutputStream os) {
				final RDFWriter writer = TTLRDFWriter.getSTTLRDFWriter(model, "");
				writer.output(os);
			}
		};
	}

	public static StreamingOutput getExceptionStream(ErrorMessage msg) {
		return new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws JsonGenerationException, JsonMappingException, IOException {
				new ObjectMapper().writer().writeValue(os, msg);
			}
		};
	}
}
