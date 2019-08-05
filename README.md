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


## Copyright and License

All the code and API are Copyright (C) 2017 Buddhist Digital Resource Center and are under the Apache 2.0 Public License.
