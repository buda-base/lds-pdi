# LDS-PDI API

## Summary Table of endpoints

### Data endpoints

|Method| Endpoint |  output |
|------|---|---|
|GET|  `/robots.txt`   | `User-agent: * Disallow: /` |
|GET|   `/cache`  |  Cache and memory monitoring | 
|GET|`/context.jsonld`| the jsonld context used for jsonld serialization|  
|GET|`/admindata/{res}`| displays some admindata about the resource res (example: /admindata/P1583)|
|GET|`/graph/{res}`|returns a serialization of the model of the given resource based on the value of the Accept header - trig is the default serialization (example: /graph/P1583)|
|GET|`/admindata/{res}.{ext}`|displays a serialization of the model of the admindata for the given resource, according to the extension (see Supported mime types and file extensions section in API.md)|
|GET|`/graph/{res}.{ext}`|displays a serialization of the graph of the given resource, according to the extension (see Supported mime types and file extensions section in API.md)|
|GET|`/prefixes`| a turtle serialization of all the prefixes used by the ldspdi server|
|GET|`/resource/{res}`|displays resource metadata according to the value of the Accept header - text/html leads to a pretty view of this data (example: /resource/P1583)|
|GET|`/resource/{res}.{ext}`|displays a serialization of the metadata of the given resource, according to the extension (see Supported mime types and file extensions section in API.md)|
|GET|`/{base : .*}/{other}`|generic endpointusing wildcards in order to serve the ontology (as a whole or as individuals resources of any ontology) (example: [http://purl.bdrc.io/ontology/core/](http://purl.bdrc.io/ontology/core/) serves a pretty html view of the core ontology while [http://purl.bdrc.io/ontology/core/Person](http://purl.bdrc.io/ontology/core/Person) serves a pretty html view of the Person class of the core ontology|
|GET|`/{base : .*}/{other}.{ext}`| return serialization of the requested ontology according to the extension (see Supported mime types and file extensions section in API.md). Example: [http://purl.bdrc.io/ontology/core.ttl](http://purl.bdrc.io/ontology/core.ttl) |
|GET|`/ontology/data/{ext}`| get all ontologies in a single model, serialized according to the given extension (see Supported mime types and file extensions section in API.md) See [http://purl.bdrc.io/ontology/data/ttl](http://purl.bdrc.io/ontology/data/ttl) |
|POST|`/callbacks/github/owl-schema`| a webhook for updating the ontologies models from the owl-schema git repo and updating fuseki dataset ontology schemas|

### SPARQL Query templates

|Method| Endpoint |  output |
|------|---|---|
|GET|`/query/table/{file}`| the outcome of a template returning a jena result set where {file} is the name of the arq template (without its extension). When the templates requires parameters (as in most cases) a queryString must be provided; example: [http://purl.bdrc.io/query/table/Work_ImgList?R_RES=bdr:W29329](http://purl.bdrc.io/query/table/Work_ImgList?R_RES=bdr:W29329). By default the results are provided as an html table. However adding a "format" parameter to the query string can leads to an json or csv output; example: [http://purl.bdrc.io/query/table/Work_ImgList?R_RES=bdr:W29329&format=json](http://purl.bdrc.io/query/table/Work_ImgList?R_RES=bdr:W29329&format=json) or [http://purl.bdrc.io/query/table/Work_ImgList?R_RES=bdr:W29329&format=csv](http://purl.bdrc.io/query/table/Work_ImgList?R_RES=bdr:W29329&format=csv). In the case we want csv, we can specify a profile param to get a light version of the csv, as follows: [http://purl.bdrc.io/query/table/Work_ImgList?R_RES=bdr:W29329&format=csv&profile=simple] (http://purl.bdrc.io/query/table/Work_ImgList?R_RES=bdr:W29329&format=csv&profile=simple) NOTE: You can also specify the page size and page number, as follows, when requesting html output [http://purl.bdrc.io/query/table/volumesForWork?R_RES=bdr:W23703&pageSize=120&pageNumber=1] (http://purl.bdrc.io/query/table/volumesForWork?R_RES=bdr:W23703&pageSize=120&pageNumber=1)|
|POST|`/query/table/{file}`|Same as the GET version except that query params are sent as a json object and that the output is always a json representation of the result set. Example: *curl -v -H "Content-Type: application/json" -d '{"R_RES":"bdr:W23703"}' http://purl.bdrc.io/query/table/volumesForWork*|
|GET|`/query/graph/{file}`|the outcome of a template returning a graph where {file} is the name of the arq template (without its extension). When the templates requires parameters (as in most cases) a queryString must be provided; example: [http://purl.bdrc.io/query/graph/IIIFPres_volumeOutline?R_RES=bdr:V22084_I0886](http://purl.bdrc.io/query/graph/IIIFPres_volumeOutline?R_RES=bdr:V22084_I0886). the serialization of the graph is based on the value of the Accept header. Default serialization is jsonld, alternatives links are provided in Alternates header when no Accept header is provided|
|POST|`/query/graph/{file}`| same as the GET version except that it only consumes json data. Example: *curl -v -H "Content-Type: application/json" -H "Accept: text/turtle" -d '{"R_RES":"bdr:V22084_I0886"}' http://purl.bdrc.io/query/graph/IIIFPres_volumeOutline*|
|POST|`/callbacks/github/lds-queries`|a webhook for updating the local repository of templates. It is trigerred by [github ldsqueries repo](https://github.com/buda-base/lds-queries)|
|POST|`/clearcache`| empties all caches of the ldspdi server|
|GET|`/queries`| provides a list of all the available public templates for that server in the json format (see [http://purl.bdrc.io/queries](http://purl.bdrc.io/queries))|
|POST|`/queries`|same as above - no parameters|
|GET|`/queries/{template}`|provides the json representation of the template specified by its name as the path variable {template} example: [http://purl.bdrc.io/queries/Etexts_count](http://purl.bdrc.io/queries/Etexts_count)|
|POST|`/queries/{template}`|same as above|

## Details

## Resource endpoints

#### `/resource/{res}`

Returns a serialization of the data about the resource using the SPARQL query `library/ResInfo.arq`.

The returned data corresponds to the `Accept:` header of the request (see below for supported values). In case of no `Accept:` header, the endpoint returns a `303` http code with a list of possible serializations.

If the `Accept:` header is `html` (when the url is open through a web browser for instance), then the response is a `302` redirect to a html page that can be configured.

#### `/resource/{res}.{ext}`

This endpoint is similar to the previous one, except that the serialization of data depends on the extension (`{ext}`).

### Supported RDF mime types and extensions

| Mime type | Ext | note |
|------|---|---|
| `text/turtle` | `ttl` | processed by `STTLWriter` (see [lib](https://github.com/buda-base/jena-stable-turtle/)) |
| `application/n-triples` | `nt` | |
| `application/n-quads` | `nq` | |
| `text/trig` | `trig` | processed by `STrigWriter` (see [lib](https://github.com/buda-base/jena-stable-turtle/)) |
| `application/rdf+xml` | `rdf` | |
| `application/owl+xml` | `owl` | |
| `application/ld+json` | `jsonld` | processed by `JSONLDFormatter` |
| `application/rdf+thrift` | `rt` | |
| `application/rdf+thrift` | `trdf` | |
| `application/json` | `rj` | |
| `application/json` | `json` | This is different from Json-ld and is probably not what you want |
| `application/trix+xml` | `trix` | |

## Query templates

See [lds-queries](https://github.com/buda-base/lds-queries) for an explanation of the templates and their format and see the configuration of lds-pdi on how to configure them.

### List of available query templates

```
/queries
```

returns the list of all the queries available in the form of a JSON list like:

```json
[
  {
    "id": "Etexts_contents",
    "href": "/queries/Etexts_contents",
    "description": "A table containing the Resource ID, etext contents and score for the given query and language tag with the given limit"
  },
  ...
]
```

Where `href` is a link to the query template description.

## Query templates description

```
/queries/{template_name}
```

returns JSON data about the query template, in a format like:

```json
{
  "id": "Res_byName",
  "domain": "public",
  "queryScope": "General",
  "queryResults": "A table containing the Id and matching literal for the given query and language tag with the given limit",
  "queryReturn": "Table",
  "queryParams": "L_NAME,LG_NAME,I_LIM",
  "params": [
    {
      "type": "string",
      "name": "L_NAME",
      "langTag": "LG_NAME",
      "isLuceneParam": true,
      "example": "(\"'od zer\" OR \"ye shes\")"
    },
    {
      "type": "int",
      "name": "I_LIM",
      "description": "the maximum number of results"
    }
  ],
  "outputs": [
    {
      "name": "?s",
      "type": "URI",
      "description": "the resource URI"
    },
    {
      "name": "?lit",
      "type": "string",
      "description": "the label/pref. label of the resource"
    }
  ],
  "template": " select distinct ?s ?lit WHERE { { (?s ?sc ?lit) text:query ( skos:prefLabel ?L_NAME ) . } union { (?b ?sc ?lit) text:query ( rdfs:label ?L_NAME ) . ?s ?t ?b . } } limit ?I_LIM",
  "demoLink": "/query/Res_byName?L_NAME=(%22mkhan+chen%22+AND+(%22%27od+zer%22+OR+%22ye+shes%22))&LG_NAME=bo-x-ewts&I_LIM=100"
}
```

which is mostly a JSON version of the parameters given in the original query file.

### Table query results

For queries returning a table, `/query/table/{template_id}` returns the results using the following JSON format:

```json
{
  "pageNumber" : 1,
  "numberOfPages" : 2,
  "pageSize" : 50,
  "numResults" : 81,
  "execTime" : 517,
  "hash" : 1287965507,
  "isLastPage" : false,
  "isFirstPage" : true,
  "pLinks" : {
    "prevGet" : null,
    "nextGet" : null,
    "currJsonParams" : "{\"L_NAME\":\"(\\\"mkhan chen\\\" AND (\\\"'od zer\\\" OR \\\"ye shes\\\"))\",\"searchType\":\"Res_byName\",\"pageSize\":\"50\",\"I_LIM\":\"100\",\"hash\":\"1287965507\",\"LG_NAME\":\"bo-x-ewts\"}",
    "prevJsonParams" : null,
    "nextJsonParams" : "{\"L_NAME\":\"(\\\"mkhan chen\\\" AND (\\\"'od zer\\\" OR \\\"ye shes\\\"))\",\"pageNumber\":\"2\",\"searchType\":\"Res_byName\",\"pageSize\":\"50\",\"I_LIM\":\"100\",\"hash\":\"1287965507\",\"LG_NAME\":\"bo-x-ewts\"}"
  },
  "headers" : [ "s", "lit" ],
  "rows" : [ {
    "dataRow" : {
      "s" : "http://purl.bdrc.io/resource/P0RK26",
      "lit" : "mkhan chen 'od zer dpal/@bo-x-ewts"
    }
  },
 .... more datRows
  {
    "dataRow" : {
      "s" : "804c3b056a079b77deaa6a5e91905f6f",
      "lit" : "yongs kyi bshes gnyen chen po dbon stod pa mkhan chen thams cad mkhyen pa mkhyen rab chos kyi nyi ma 'phrin las mtha' yas pa'i 'od zer gyi rnam par thar pa cung tsam brjod pa dag pa'i snang ba/@bo-x-ewts"
    }
  } 
  ]
}
```

where `pLinks` is an object giving previous and next pages URLs for GET request (`prevGet` & `nextGet`) along with post json request params of the current, next and previous pages (For request posting json).

For instance:
```
/query/table/Res_byName?L_NAME=("mkhan chen" AND ("'od zer" OR "ye shes"))&L_LANG=@bo-x-ewts&I_LIM=100
```

or using curl:

```
curl --data "L_NAME=(\"mkhan chen\" AND (\"'od zer\" OR \"ye shes\"))&LG_NAME=bo-x-ewts&I_LIM=100" http://purl.bdrc.io/query/table/Res_byName
```

#### Controling the output

You can pass query parameters to control the output format:
| Parameter name | Possible values | Default |
| ----- | ----- | ----- |
| `format` | `csv` or `json` | `json` |
| `pageSize` | integer | ? |
| `pageNumber` | integer | 1 |
| `profile` | `simple` or ? | ? |

Example:

```
/query/table/volumesForWork?R_RES=bdr:W23703&format=csv&pageSize=50&pageNumber=1&profile=simple
```

#### Using JSON POST with curl

First create a file test.json :

```json
{
  "L_NAME": "(\"mkhan chen\" AND (\"'od zer\" OR \"ye shes\"))",
  "LG_NAME": "bo-x-ewts",
  "I_LIM":"100"
}
```

Then run:

```sh
curl -H "Content-Type: application/json" -X POST -d @test.json http://localhost:8080/query/{template id}
```

### Graph query results

For queries returning a graph, the endpoint is `/query/graph/{template_id}`. Example:

```
/query/graph/graphTest?R_RES=bdr:W22084
```

The returned value is a model, serialized according to the value `Accept:` header.
