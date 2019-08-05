# LDS-PDI 

(Linked Data Server - Public Data Interface)

Contains a framework for executing external queries files through a rest API. Integrates lds-search and lds-rest projects and extends ontology-service project.

Moreover, each query file contains its own description that is used to dynamically generate a html file containing the Sparql Public Data Interface specifications  

# Compiling and deploying

```
mvn clean package
```
# Configuration

In order to work properly, ldspdi requires a **fuseki dataset**, a **query template** git repository and an **ontologies** git repository.

Moreover, webhooks must be setup in these two git repositories so the fuseki dataset can be updated when a change occur in an ontology or when adding/editing any query or graph template.

### Configuration file

A location (/my/configfile/dir/) for the ldspdi.properties must be setup. 

This location is given at startup on the command line as a system parameter named "ldspdi.configpath". 
Example (using jetty):

`mvn jetty:run -Dldspdi.configpath=/my/configfile/dir/`

### Properties file (main settings):

All props are defined in ldspdi.properties file. An example is shown [here](https://github.com/buda-base/lds-pdi/blob/master/ldspdi.properties.templates)

To configure fuseki dataset: 

`fusekiUrl=http://your.server:port/fuseki/bdrcrw/query`

To synchronize local and remote template dir

`queryPath=/Users/marc/dev/lds-queries/`

`git_remote_url=https://github.com/buda-base/lds-queries.git`

To synchronize and load ontologies from remote

`ontPoliciesUrl=https://raw.githubusercontent.com/buda-base/owl-schema/master/ont-policy.rdf`

# Running

```
mvn jetty:run -Dldspdi.configpath=/my/configfile/dir/
```
# Home demo page
```
http://localhost:8080/index.jsp
```

