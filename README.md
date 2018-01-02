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
mvn jetty:run serves the app locally
```


# Copyright and License

All the code and API are Copyright (C) 2017 Buddhist Digital Resource Center and are under the Apache 2.0 Public License.
