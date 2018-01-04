# LDS-PDI

Contains a framework for executing external queries files through a rest API.

Moreover, each query file contains its own description taht is used to dynamically generate a html file containing the Sparql Public Data Interface specifications  

# Compiling and deploying

```
mvn clean package
```
# Config

fuseki server name url is set in ldsrest.properties:

```
#Fuseki server
fuseki=http://localhost:13180/fuseki/bdrcrw/query
```

# Running

```
mvn jetty:run then go to http://localhost:8080/lds-pdi/index.jsp
```

# Usage

This framework will automatically add new sparql query templates based on files published to the « src/main/resources/queries » directory.

New query files must have the .arq extension and are formatted as follows :

```
#QueryScope=Place
#QueryReturnType=Table
#QueryResults=A table containing the Id and the name of the place whose name contains the NAME param value
#QueryParams=NAME
#QueryUrl=/lds-pdi/query?searchType=pdi_pl_name&NAME=dgon gsar

select ?Place_ID ?Place_Name
where {
  ?Place_ID a :Place;
   skos:prefLabel ?Place_Name .
  Filter(contains(?Place_Name, "${NAME}"))
}
```
Note : the parameter placeholder of the query must match the value of QueryParams.

# Copyright and License

All the code and API are Copyright (C) 2017 Buddhist Digital Resource Center and are under the Apache 2.0 Public License.
