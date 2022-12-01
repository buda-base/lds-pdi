- [LDS-PDI API](#lds-pdi-api)
  * [Summary Table of endpoints](#summary-table-of-endpoints)
    + [Public Data endpoints](#public-data-endpoints)
    + [Ontology endpoints](#ontology-endpoints)
    + [Administrative Endpoints](#administrative-endpoints)
    + [SPARQL Query templates](#sparql-query-templates)
  * [Details](#details)
  * [Resource endpoints](#resource-endpoints)
      - [`/resource/{res}`](#--resource--res--)
      - [`/resource/{res}.{ext}`](#--resource--res--ext--)
      - [Admindata and grand endpoint](#admindata-and-grand-endpoint)
    + [Supported RDF mime types and extensions](#supported-rdf-mime-types-and-extensions)
  * [Query templates](#query-templates)
    + [List of available query templates](#list-of-available-query-templates)
  * [Query templates description](#query-templates-description)
    + [Table query results](#table-query-results)
      - [Controling the output](#controling-the-output)
      - [Using JSON POST with curl](#using-json-post-with-curl)
    + [Graph query results](#graph-query-results)
# LDS-PDI API

## Summary Table of endpoints

### Public Data endpoints

See [Resource Endpoints](#resource-endpoints) section for a full description.

Endpoint |  output |
---|---|
`/resource/{res}` | displays RDF data about the resource, serialized according to the HTTP `Accept` header (see [Supported mime types](#supported-rdf-mime-types-and-extensions)) - with a html header leading to a redirection to a configurable html page |
`/resource/{res}.{ext}`| similar endpoint where the RDF serialization is determined by the extension |
`/admindata/{res}`| idem |
`/admindata/{res}.{ext}`| idem |
`/graph/{res}`| idem |
`/graph/{res}.{ext}`| idem |
`/prefixes`| a turtle serialization of all the prefixes used by the ldspdi server [example in bdrc.io](http://purl.bdrc.io/prefixes) |


### Ontology endpoints

Endpoint |  output |
---|---|
`/{base : .*}/{other}`| generic endpoint using wildcards in order to serve the various URIs of the ontology. When open in a web browser, serves an html representation. Ex: [/ontology/core/](http://purl.bdrc.io/ontology/core/), [bdo:Person](http://purl.bdrc.io/ontology/core/Person)|
`/{base : .*}/{other}.{ext}`| return serialization of the requested ontology URI according to the extension (see [Supported mime types](#supported-rdf-mime-types-and-extensions)). Ex: [/ontology/core.ttl](http://purl.bdrc.io/ontology/core.ttl) |
`/ontology/data/{ext}`| get the complete ontology as a single model, serialized according to the given extension |
`/context.jsonld`| the JSON-LD context |

### Administrative Endpoints

Endpoint |  output |
---|---|
`/robots.txt`   | `User-agent: * Disallow: /` (configurable) |
`/cache`  |  Cache and memory monitoring |
`/callbacks/github/owl-schema`| (`POST` only) a webhook for updating the ontologies models from the owl-schema git repo and updating fuseki dataset ontology schemas |
`/callbacks/github/lds-queries`| (`POST` only) a webhook for updating the SPARQL queries templates |
`/clearcache`| (`POST` only) empties all caches of the ldspdi server|

### SPARQL Query templates

See the section [Query templates](#query-templates) for a full description.

Endpoint |  output |
---|---|
`/queries`| provides a list of all the available public templates in json ([example on bdrc.io](http://purl.bdrc.io/queries)) |
`/queries/{template}`| provides the json representation of the template ([example on bdrc.io](http://purl.bdrc.io/queries/Etexts_count))|
`/query/table/{template}`| the result of the query associated with a `SELECT` SPARQL template |
`/query/graph/{file}`| the result of a `CONSTRICT` SPARQL template |

## Details

## Resource endpoints

#### `/resource/{res}`

Returns a serialization of the data about the resource using the SPARQL query `library/ResInfo.arq`.

The returned data corresponds to the `Accept:` header of the request (see below for supported values). In case of no `Accept:` header, the endpoint returns a `303` http code with a list of possible serializations.

If the `Accept:` header is `html` (when the url is open through a web browser for instance), then the response is a `302` redirect to a html page that can be configured.

#### `/resource/{res}.{ext}`

This endpoint is similar to the previous one, except that the serialization of data depends on the extension (`{ext}`).

#### Admindata and grand endpoint

The endpoints at

- `/admindata/{res}`
- `/admindata/{res}.{ext}`

behave in the same way. The endpoints at

- `/graph/{res}`
- `/graph/{res}.{ext}`

are intended for RDF graphs and behave in a slightly different way: they use the `library/Resgraph.arq` SPARQL query, and they do not redirect to html pages.


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

When called in `POST`, the endpoint only accepts JSON and produces `JSON or XML` (*when the format parameter is set to xml in the Request JSON body*).

#### Controling the output

You can pass query parameters to control the output format:

| Parameter name | Possible values | Default |
| ----- | ----- | ----- |
| `format` | `csv` (simple), `csv_f` (full), `json` or `xml` | `json` |
| `pageSize` | integer | 50 |
| `pageNumber` | integer | 1 |

**Note:** page information does not apply to `xml` format: the whole resultSet is returned in such a case.

Example:

```
/query/table/volumesForWork?R_RES=bdr:W23703&format=csv&pageSize=50&pageNumber=1
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

The returned value is a model, serialized according to the value of the HTTP `Accept:` header, with JSON-LD being the default.

## Lexicography endpoints

### /lexicography/entriesForChunk?chunk={chunk}&lang={lang}&cursor_start={cursor_start}&cursor_end={cursor_end}

The endpoint returns dictionary entries corresponding to a range of characters in a context. It takes 4 arguments :
- `{chunk}` is a piece of text surrounding the selected text. There is no restriction on its size, but we advise at least 40 characters on both sides of the selected text. There is no restriction on the boundaries (they can be in the middle of a word or syllable).
- `{lang}` is the BCP47 tag of the language of the chunk
- `{cursor_start}` and `cursor_end` represent the character coordinates of the selected text within the chunk, with the first character of the chunk having the coordinate 0.

The result of the call is an unordered JSON array of "entries", where an entry has the following fields:
- `uri` (string): the uri of the entry
- `word` (literal): the key of the entry in the dictionary
- `def` (literal): the definition in the dictionary
- `nb_tokens` (int): the number of tokens in the word (to help sorting the entries in the display)
- `chunk_offset_start` and `chunk_offset_end` represent the character range of the entry in the original chunk
- `cursor_in_entry_start` and `cursor_in_entry_end` represent the (approximate) character range of the selected text within the entry

where the type `literal` is a dictionary with two keys: `value` and `lang`.
