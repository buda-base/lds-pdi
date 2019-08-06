# LDS-PDI 

A.k.a. *Linked Data Server - Public Data Interface*

## Features

The server provides APIs to:
- serve RDF resources in different serializations (ttl, jsonld, etc.)
- serve an RDF vocabulary, providing convenient html pages to describe the classes and properties
- run pre-defined SPARQL queries, passing arguments in URL
- augment the SPARQL results with some facet information

The API and routes provided by the server are described in a separate document: [API.md](API.md).

## Deployment

### Running locally

You can run the server locally with a maven command:

```
mvn jetty:run -Dldspdi.configpath=/my/configfile/dir/
```

See below for an example for an explanation of the configpath argument). This will make the server accessible on

```
http://localhost:8080/index.jsp
```

### Building a jar

For deployment in a web server environment (Tomcat, Jetty, etc.) you can compile a jar file with:

```
mvn clean package
```

and deploy it in your environment.

## Configuration

### Configuration file

The configuration of the server must be recorded in a property file named `ldspdi.properties` (see next paragraph for its location). A commented template is provided: [ldspdi.properties.template](ldspdi.properties.template), please use it as a documentation and model for the configuration file.

### Configuration directory

The path of the directory containing `ldspdi.properties` must be passed to the server through a system property called `ldspdi.configpath`. You can pass this value when starting your server from the command line, here's an example setting it when running locally:

```
mvn jetty:run -Dldspdi.configpath=/my/configfile/dir/
```

##### In a buda-base environment

Note that the [buda-base](https://github.com/buda-base/buda-base) environment sets the config path to `/etc/buda/ldspdi/`, so the only thing you need to is to copy your configuration file to `/etc/buda/ldspdi/ldspdi.properties`.

### Webhooks (optional)

In order for the SPARQL queries and the ontology synchronize in real time with the git repositories, you can use a webhook mechanism. Typically you can configure github to send a message to `/callbacks/github/owl-schema` and `/callbacks/github/lds-queries`. This is not necessary for the platform to function, and you can call these webhooks manually (in a daily cron for instance) to get updates.


# Table of public ldspdi endpoints

## Public data

|Method| Endpoint |  output |
|------|---|---|
|GET|  /robots.txt   |User-agent: * Disallow: /| 
|GET|   /cache  |  Cache and memory monitoring | 
|GET|/context.jsonld| the jsonld context used for jsonld serialization|  
|GET|/admindata/{res}| displays some admindata about the resource res (example: /admindata/P1583)|
|GET|/graph/{res}|returns a serialization of the model of the given resource based on the value of the Accept header - trig is the default serialization (example: /graph/P1583)|
|GET|/admindata/{res}.{ext}|displays a serialization of the model of the admindata for the given resource, according to the extension (see Supported mime types and file extensions section in API.md)|
|GET|/graph/{res}.{ext}|displays a serialization of the graph of the given resource, according to the extension (see Supported mime types and file extensions section in API.md)|
|GET|/prefixes| a turtle serialization of all the prefixes used by the ldspdi server|
|GET|/resource/{res}|displays resource metadata according to the value of the Accept header - text/html leads to a pretty view of this data (example: /resource/P1583)|
|GET|/resource/{res}.{ext}|displays a serialization of the metadata of the given resource, according to the extension (see Supported mime types and file extensions section in API.md)|
|GET|/{base : .*}/{other}|generic endpointusing wildcards in order to serve the ontology (as a whole or as individuals resources of any ontology) (example: [http://purl.bdrc.io/ontology/core/](http://purl.bdrc.io/ontology/core/) serves a pretty html view of the core ontology while [http://purl.bdrc.io/ontology/core/Person](http://purl.bdrc.io/ontology/core/Person) serves a pretty html view of the Person class of the core ontology|
|GET|/{base : .*}/{other}.{ext}| return serialization of the requested ontology according to the extension (see Supported mime types and file extensions section in API.md). Example: [http://purl.bdrc.io/ontology/core.ttl](http://purl.bdrc.io/ontology/core.ttl) |
|GET|/ontology/data/{ext}| get all ontologies in a single model, serialized according to the given extension (see Supported mime types and file extensions section in API.md) See [http://purl.bdrc.io/ontology/data/ttl](http://purl.bdrc.io/ontology/data/ttl) |
|POST|/callbacks/github/owl-schema| a webhook for updating the ontologies models from the owl-schema git repo and updating fuseki dataset ontology schemas|

## Public templates

|Method| Endpoint |  output |
|------|---|---|
|GET|/query/table/{file}| the outcome of a template returning a jena result set where {file} is the name of the arq template (without its extension). When the templates requires parameters (as in most cases) a queryString must be provided; example: [http://purl.bdrc.io/query/table/Work_ImgList?R_RES=bdr:W29329](http://purl.bdrc.io/query/table/Work_ImgList?R_RES=bdr:W29329). By default the results are provided as an html table. However adding a "format" parameter to the query string can leads to an json or csv output; example: [http://purl.bdrc.io/query/table/Work_ImgList?R_RES=bdr:W29329&format=json](http://purl.bdrc.io/query/table/Work_ImgList?R_RES=bdr:W29329&format=json) or [http://purl.bdrc.io/query/table/Work_ImgList?R_RES=bdr:W29329&format=csv](http://purl.bdrc.io/query/table/Work_ImgList?R_RES=bdr:W29329&format=csv). In the case we want csv, we can specify a profile param to get a light version of the csv, as follows: [http://purl.bdrc.io/query/table/Work_ImgList?R_RES=bdr:W29329&format=csv&profile=simple] (http://purl.bdrc.io/query/table/Work_ImgList?R_RES=bdr:W29329&format=csv&profile=simple) NOTE: You can also specify the page size and page number, as follows, when requesting html output [http://purl.bdrc.io/query/table/volumesForWork?R_RES=bdr:W23703&pageSize=120&pageNumber=1] (http://purl.bdrc.io/query/table/volumesForWork?R_RES=bdr:W23703&pageSize=120&pageNumber=1)|
|POST|/query/table/{file}|Same as the GET version except that query params are sent as a json object and that the output is always a json representation of the result set. Example: *curl -v -H "Content-Type: application/json" -d '{"R_RES":"bdr:W23703"}' http://purl.bdrc.io/query/table/volumesForWork*|
|GET|/query/graph/{file}|the outcome of a template returning a graph where {file} is the name of the arq template (without its extension). When the templates requires parameters (as in most cases) a queryString must be provided; example: [http://purl.bdrc.io/query/graph/IIIFPres_volumeOutline?R_RES=bdr:V22084_I0886](http://purl.bdrc.io/query/graph/IIIFPres_volumeOutline?R_RES=bdr:V22084_I0886). the serialization of the graph is based on the value of the Accept header. Default serialization is jsonld, alternatives links are provided in Alternates header when no Accept header is provided|
|POST|/query/graph/{file}| same as the GET version except that it only consumes json data. Example: *curl -v -H "Content-Type: application/json" -H "Accept: text/turtle" -d '{"R_RES":"bdr:V22084_I0886"}' http://purl.bdrc.io/query/graph/IIIFPres_volumeOutline*|
|POST|/callbacks/github/lds-queries|a webhook for updating the local repository of templates. It is trigerred by [github ldsqueries repo](https://github.com/buda-base/lds-queries)|
|POST|/clearcache| empties all caches of the ldspdi server|
|GET|/queries| provides a list of all the available public templates for that server in the json format (see [http://purl.bdrc.io/queries](http://purl.bdrc.io/queries))|
|POST|/queries|same as above - no parameters|
|GET|/queries/{template}|provides the json representation of the template specified by its name as the path variable {template} example: [http://purl.bdrc.io/queries/Etexts_count](http://purl.bdrc.io/queries/Etexts_count)|
|POST|/queries/{template}|same as above|

## Copyright and License

All the code and API are Copyright (C) 2017 Buddhist Digital Resource Center and are under the Apache 2.0 Public License.
