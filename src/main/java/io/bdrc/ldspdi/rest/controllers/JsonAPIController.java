package io.bdrc.ldspdi.rest.controllers;

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

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.bdrc.ldspdi.exceptions.LdsError;
import io.bdrc.ldspdi.exceptions.RestException;
import io.bdrc.ldspdi.objects.json.QueryListItem;
import io.bdrc.ldspdi.objects.json.QueryTemplate;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.LdsQuery;
import io.bdrc.ldspdi.sparql.LdsQueryService;
import io.bdrc.ldspdi.utils.Helpers;
import io.bdrc.libraries.StreamingHelpers;

@RestController
@RequestMapping("/")
public class JsonAPIController {

    public final static Logger log = LoggerFactory.getLogger(JsonAPIController.class);
    private List<String> fileList;

    public JsonAPIController() throws RestException {
        fileList = getQueryTemplates();
    }

    @GetMapping(value = "/queries", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> queriesListGet(HttpServletResponse response) throws RestException {
        log.info("Call to queriesListGet()");
        Helpers.setCacheControl(response, "public");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(StreamingHelpers.getJsonObjectStream(getQueryListItems(fileList)));
    }

    @PostMapping(value = "/queries", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> queriesListPost(HttpServletResponse response) throws RestException {
        log.info("Call to queriesListPost()");
        Helpers.setCacheControl(response, "public");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(StreamingHelpers.getJsonObjectStream(getQueryListItems(fileList)));
    }

    @GetMapping(value = "/queries/{template}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> queryDescGet(HttpServletResponse response, @PathVariable("template") String name) throws RestException {
        log.info("Call to queryDescGet()");
        Helpers.setCacheControl(response, "public");
        final LdsQuery qfp = LdsQueryService.get(name + ".arq");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(StreamingHelpers.getJsonObjectStream(qfp.getTemplate()));
    }

    @PostMapping(value = "/queries/{template}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> queryDescPost(HttpServletResponse response, @PathVariable("template") String name) throws RestException {
        log.info("Call to queryDescPost()");
        Helpers.setCacheControl(response, "public");
        final LdsQuery qfp = LdsQueryService.get(name + ".arq");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(StreamingHelpers.getJsonObjectStream(qfp.getTemplate()));
    }

    private ArrayList<QueryListItem> getQueryListItems(List<String> filesList) throws RestException {
        ArrayList<QueryListItem> items = new ArrayList<>();
        for (String file : filesList) {
            String tmp = file.substring(file.lastIndexOf('/') + 1);
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
