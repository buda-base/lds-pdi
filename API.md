# LDS-PDI API

## Table of public ldspdi endpoints

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

### GET && POST

lds-pdi serves paginated results based on several parameters values.

`/query/{template id}` : POST requests return the following JSON format :
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
pLinks is an object giving prev and next pages for GET request (prevGet & nextGet) along with post json request params of the current, next and previous pages (For request posting json)

```
Ex GET: http://localhost:8080/query/table/Res_byName?L_NAME=("mkhan chen" AND ("'od zer" OR "ye shes"))&L_LANG=@bo-x-ewts&I_LIM=100
Ex POST: curl --data "L_NAME=(\"mkhan chen\" AND (\"'od zer\" OR \"ye shes\"))&LG_NAME=bo-x-ewts&I_LIM=100" http://localhost:8080/query/table/Res_byName
```

## JSON and CVS output

You can get json or cvs output formats (using the `/query/table` endpoints) by adding &format=json or &format=csv to your URL request:

**Ex :**
```
http://purl.bdrc.io/query/table/volumesForWork?R_RES=bdr:W23703&format=json&pageSize=50&pageNumber=1
```
or (for a csv detailed response - values + datatypes)
```
http://purl.bdrc.io/query/table/volumesForWork?R_RES=bdr:W23703&format=csv&pageSize=50&pageNumber=1
```
or (for a csv simplified response - values only)

```
http://purl.bdrc.io/query/table/volumesForWork?R_RES=bdr:W23703&format=csv&pageSize=50&pageNumber=1&profile=simple
```
Ex Testing POST JSON : 


1) Create a file test.json :

```json
{
  "L_NAME": "(\"mkhan chen\" AND (\"'od zer\" OR \"ye shes\"))",
  "LG_NAME": "bo-x-ewts",
  "I_LIM":"100"
}
```

2) running curl :
```
curl -H "Content-Type: application/json" -X POST -d @test.json http://localhost:8080/query/{template id}
```

# Query templates format specifications

This framework will automatically add new sparql query templates to the index page based on files published to the « /local/dir/queries » directory.

New query files must have the .arq extension and are formatted as follows :

```sparql
#QueryScope=General
#QueryReturnType=Table
#QueryResults=A table containing the Id and matching literal for the given query and language tag with the given limit
#QueryParams=L_NAME,LG_NAME,I_LIM
#QueryUrl=/Res_withFacet?L_NAME=("mkhan chen" AND ("'od zer" OR "ye shes"))&LG_NAME=bo-x-ewts&I_LIM=100

#param.L_NAME.type=string
#param.L_NAME.langTag=LG_NAME
#param.L_NAME.isLucene=true
#param.L_NAME.example=("'od zer" OR "ye shes")
#param.I_LIM.type=int
#param.I_LIM.desc=the maximum number of results

#output.?s.type=URI
#output.?s.desc=the resource URI
#output.?f.type=URI
#output.?f.desc=the resourceType URI of the resource
#output.?lit.type=string
#output.?lit.desc=the label/pref. label of the resource

select distinct ?s ?f ?lit
WHERE {
  { (?s ?sc ?lit) text:query ( skos:prefLabel ?L_NAME ) . }
  union
  { (?s ?sc ?lit) text:query ( skos:altLabel ?L_NAME ) . }
  union
  { (?t ?sc ?lit) text:query ( rdfs:label ?L_NAME ) . ?s ?p ?t } .
  ?s a ?f  .
}
limit ?I_LIM
```
Note : the parameter placeholder of the query must match the value of QueryParams.

## Guidelines for creating query templates

ldspdi performs a strict parameter evaluation in order to prevent Sparql injection. It therefore requires parameter types to be specified. Parameter types are as follows :

- *Literal*: each literal parameter name must be prefixed by `L_` (ex : `L_NAME`)
- *Literal lang tag*: each literal parameter can be associated with a lang tag using a parameter prefixed by `LG_` (Ex : if you want `L_FOO` to be associated with the `bar` lang tag, you can add `LG_FOO=bar` to your request and declare it in the `QueryParams` section of your template).
- *Integer*: each literal parameter name must be prefixed by `I_` (ex : `I_LIM`)
- *Resource*: each URI parameter must be prefixed `R_` (ex : `R_RES`)

Additional rule : Filter on variables should be the last ones in a query

ex:

```sparql
FILTER (contains(?root_name, ?NAME ))

FILTER ((contains(?comment_type, "commentary" ))
```

will fail because it is subject to an injection attack while :

```sparql
FILTER ((contains(?comment_type, "commentary" ))

FILTER (contains(?root_name, ?NAME ))
```

will be considered safe.

### Example :


```sparql
#QueryScope=General
#QueryReturnType=Table
#QueryResults=A table containing the Id and matching literal for the given query and language tag with the given limit
#QueryParams=L_NAME,LG_NAME,I_LIM
#QueryUrl=/Res_byName?L_NAME=("mkhan chen" AND ("'od zer" OR "ye shes"))&LG_NAME=bo-x-ewts&I_LIM=100

select distinct ?s ?lit
WHERE {
  { (?s ?sc ?lit) text:query ( skos:prefLabel ?L_NAME ) . }
  union
  { (?s ?sc ?lit) text:query ( rdfs:label ?L_NAME ) . }
} limit ?I_LIM

```


```sparql
#QueryScope=Person
#QueryReturnType=Table
#QueryResults=All the detailed admin info (notes, status, log entries) about the person data
#QueryParams=R_RES
#QueryUrl=/Person_adminDetails?R_RES=P1583


select distinct
?ID
?preferredName
?y ?noteRef ?note_value ?admin_prop ?admin_ref ?log_value ?git ?status
where {
    {
      ?ID skos:prefLabel ?preferredName ;
          adm:gitRevision ?git;
        adm:status ?status .
      Filter(?ID=?R_RES)
  }
    UNION {
      OPTIONAL{ ?ID  :note ?noteRef }.
      ?noteRef ?y ?note_value .
      Filter(?ID=?R_RES)
  }
    UNION {
      OPTIONAL{ ?ID  adm:logEntry ?admin_ref }.
      ?admin_ref ?admin_prop ?log_value .
      Filter(?ID=?R_RES)
  }
}
```
#### Query templates metadata

Templates metadata is available as json at the following url:
```
http:/localhost:8080/queries/{template name} (without .arq extension)
```
In addition to general metadata (QueryScope, Id, domain, etc...), there two types of metadata : **param** and **output**

an example of param metadata is as follows:

```spaql
#param.L_NAME.type=string
#param.L_NAME.langTag=LG_NAME
#param.L_NAME.isLucene=true
#param.L_NAME.example=("'od zer" OR "ye shes")
```


**param** and **output** have the same syntax : 

```sparql
#{metadataType}.{variable_name}.{data_name}
```

#### param metadata specs

For a Literal param (prefixed by `L_`): only `{type}`, `{langTag}`, `{isLucene}`, `{example}` are valid data_name.

For a Integer param (prefixed by `I_`): only `{type}`, `{desc}` are valid data_name.

For a Resource param (prefixed by `R_`): only `{type}`, `{subType}`, `{desc}` are valid data_name. the subtype indicates whether this param is a resource URI (ex: `bdr:P1583`) or a resource type (:Work)

#### output metadata specs

For all output metadata, only only {type}{desc} are valid data_name.

#### metadata declaration complete model example
```sparql
#param.L_NAME.type=
#param.L_NAME.langTag=
#param.L_NAME.isLucene=
#param.L_NAME.example=

#param.I_LIM.type=
#param.I_LIM.desc=

#param.R_RES.type=
#param.R_RES.subtype=
#param.R_RES.desc=

#output.?s.type=
#output.?s.desc=
```

# Graph templates

graph templates are templates producing a graph instead of a table set of results. Graph templates use the `CONSTRUCT` SPARQL keyword.
They follow the same rules as query templates regarding self description features except for output fields descriptions which don't make any sense in the case of a graphed result.

The root for the endpoint is `/query/graph`

Example:

```
/query/graph/graphTest?R_RES=bdr:W22084
```


# JSON API

## Available query templates

```
GET or POST: http://localhost:8080/queries
```

returns JSON objects of the form
```json
{
  "id": "Etexts_contents",
  "href": "/queries/Etexts_contents",
  "description": "A table containing the Resource ID, etext contents and score for the given query and language tag with the given limit"
}
```

Where descLink is a link to the JSON query template representation.

## Query templates description
```
GET or POST: http://localhost:8080/queries/{template_name}
```
returns JSON queryTemplate object :

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
