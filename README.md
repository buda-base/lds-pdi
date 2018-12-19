# LDS-PDI 

(Linked Data Server - Public Data Interface)

Contains a framework for executing external queries files through a rest API. Integrates lds-search and lds-rest projects and extends ontology-service project.

Moreover, each query file contains its own description that is used to dynamically generate a html file containing the Sparql Public Data Interface specifications  

# Compiling and deploying

```
mvn clean package
```
# Configuration

### Deployment on a tomcat server :
in tomcat context.xml file, add the following lines:


```
<Parameter name="fuseki" value="http://<server:port>/fuseki/bdrcrw/query"
         override="false"/>
<Parameter name="queryPath" value="/local/path/to/"
         override="false"/>

```
### Running it in tomcat from Eclipse IDE:

update context.xml as above in workspaceDir/Servers/tomcatXXX/

### Running it using Maven Jetty plugin:

in src/main/webapp/WEB-INF/webdefault.xml set your local values in


```
<context-param>
    <param-name>fuseki</param-name>
    <param-value>http://<server;port>/fuseki/bdrcrw/query</param-value>;
</context-param>
<context-param>
    <param-name>queryPath</param-name>
    <param-value>/local/path/to/</param-value>
</context-param>

```
# Query file repository

All query templates files are automatically fetched from lds-queries github repository (https://github.com/BuddhistDigitalResourceCenter/lds-queries).
This repository is being cloned locally to the location specified by the queryPath parameter mentionned above.

Any authorized user for the github lds-queries repo can therefore create, TEST thoroughly (10 times better than one), and share queries by pushing them to the github repository.

Changes will appear after refreshing
```
http://localhost:8080/index.jsp
```

# Running

```
mvn jetty:run
```
# Home demo page
```
http://localhost:8080/index.jsp
```

# Ontology browsing service
```
http://localhost:8080/demo/ontOverview.jsp
```

OR (by classes)

```
http://localhost:8080/ontology/admin/Product

http://localhost:8080/ontology/core/Work
```

#### View the ontology file 

You can view/download the ontology file at the following url:

```
http://localhost:8080/ontology.{ext}
```

where {ext} can be any of the supported mime types and file extensions shown below, "jsonld" excepted.

# Json Context of lds-pdi
```
GET or POST: http://localhost:8080/context.jsonld
```
# Resources

### GET && POST
/resource/{res} 

(returns turtle format by default)
```
Ex GET: http://localhost:8080/resource/P1583
Ex POST: curl -X POST http://localhost:8080/resource/P1583
```
/resource/{res}.{ext}

(returns format according to extensions {ext} - see below for supported formats/ext mapping)
```
Ex GET: http://localhost:8080/resource/P1583.jsonld
Ex POST : curl -X POST http://localhost:8080/resource/P634.rdf
Ex POST JSON : curl -H "Content-Type: application/json" -X POST -d '{"res":"P1583","ext":"jsonld"}' http://localhost:8080/resource
```

##### Supported mime types and file extensions

text/turtle=ttl (default : processed by BDRC STTLWriter)

application/n-triples=nt

application/n-quads=nq

text/trig=trig

application/rdf+xml=rdf

application/owl+xml=owl

application/ld+json=jsonld (processed by BDRC JSONLDFormatter)

application/rdf+thrift=rt

application/rdf+thrift=trdf

application/json=rj

application/json=json

application/trix+xml=trix

# Query templates

### GET && POST

lds-pdi serves paginated results based on several parameters values.

/query/{template id} : POST requests return the following JSON format :
```
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
Ex GET: http://localhost:8080/query/Res_byName?L_NAME=("mkhan chen" AND ("'od zer" OR "ye shes"))&L_LANG=@bo-x-ewts&I_LIM=100
Ex POST: curl --data "L_NAME=(\"mkhan chen\" AND (\"'od zer\" OR \"ye shes\"))&LG_NAME=bo-x-ewts&I_LIM=100" http://localhost:8080/query/Res_byName
```

## JSON and CVS output

You can get json or cvs output formats (using GET on the /query endpoints) by adding &format=json or &format=csv to your URL request:

**Ex :**
```
http://purl.bdrc.io/query/volumesForWork?R_RES=bdr:W23703&format=json&pageSize=50&pageNumber=1
```
or (for a csv detailed response - values + datatypes)
```
http://purl.bdrc.io/query/volumesForWork?R_RES=bdr:W23703&format=csv&pageSize=50&pageNumber=1
```
or (for a csv simplified response - values only)

```
http://purl.bdrc.io/query/volumesForWork?R_RES=bdr:W23703&format=csv&pageSize=50&pageNumber=1&profile=simple
```
Ex Testing POST JSON : 


1) Create a file test.json :

```
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

```
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

### Guidelines for creating query templates

ldspdi performs a strict parameter evaluation in order to prevent Sparql injection. It therefore requires parameter types to be specified. Parameter types are as follows :

Literal : each literal parameter name must be prefixed by « L_ » (Ex : L_NAME)

Literal Lang: each literal parameter can be associated with a language using a parameter prefixed by LG_ (Ex : if you want L_FOO to be searched in the ewts language, you must add a LG_FOO=bo-x-ewts to your request and declare it in the #QueryParams section of your template).

Integer : each literal parameter name must be prefixed by « I_ » (Ex : I_LIM)

Resource : each Resource ID parameter must be prefixed by « R_ » (Ex : R_RES)


Additional rule : Filter on variables should be the last ones in a query

ex:
```
FILTER (contains(?root_name, ?NAME ))

FILTER ((contains(?comment_type, "commentary" ))
```

will fail because it is subject to an injection attack while :
```
FILTER ((contains(?comment_type, "commentary" ))

FILTER (contains(?root_name, ?NAME ))
```
will go through without any issue.

### Example :


```
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


```
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
```
#param.L_NAME.type=string
#param.L_NAME.langTag=LG_NAME
#param.L_NAME.isLucene=true
#param.L_NAME.example=("'od zer" OR "ye shes")
```

**param** and **output** have the same syntax : 
```
#{metadataType}.{variable_name}.{data_name}
```

#### param metadata specs

For a Literal param (prefixed by L_): only {type}{langTag}{isLucene}{example} are valid data_name.

For a Integer param (prefixed by I_): only {type}{desc} are valid data_name.

For a Resource param (prefixed by R_): only {type}{subType}{desc} are valid data_name. the subtype indicates whether this param is a resource ID(bdr:P1583) or a resource type (:Work)

#### output metadata specs

For all output metadata, only only {type}{desc} are valid data_name.

#### metadata declaration complete model example
```
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

graph templates are templates producing a graph instead of a value or a table-like set of results. Graph templates use the CONSTRUCT sparql keyword.
They follow the same rules as query templates regarding self description features except for output fields descriptions which don't make any sense in the case of a graphed result.

**The root for the endpoint is /graph (/query being used by query templates)**

Example:
```
/graph/graphTest?R_RES=bdr:W22084
```


# JSON API

## Available query templates

```
GET or POST: http://localhost:8080/queries
```

returns JSON objects of the form
```
{
id: "Etexts_contents",
href: "/queries/Etexts_contents",
description: "A table containing the Resource ID, etext contents and score for the given query and language tag with the given limit"
},
```
Where descLink is a link to the JSON query template representation.

## Query templates description
```
GET or POST: http://localhost:8080/queries/{template_name}
```
returns JSON queryTemplate object :
```
{
id: "Res_byName",
domain: "public",
queryScope: "General",
queryResults: "A table containing the Id and matching literal for the given query and language tag with the given limit",
queryReturn: "Table",
queryParams: "L_NAME,LG_NAME,I_LIM",
params: [
{
type: "string",
name: "L_NAME",
langTag: "LG_NAME",
isLuceneParam: "true",
example: "("'od zer" OR "ye shes")"
},
{
type: "int",
name: "I_LIM",
description: "the maximum number of results"
}
],
outputs: [
{
name: "?s",
type: "URI",
description: "the resource URI"
},
{
name: "?lit",
type: "string",
description: "the label/pref. label of the resource"
}
],
template: " select distinct ?s ?lit WHERE { { (?s ?sc ?lit) text:query ( skos:prefLabel ?L_NAME ) . } union { (?b ?sc ?lit) text:query ( rdfs:label ?L_NAME ) . ?s ?t ?b . } } limit ?I_LIM",
demoLink: "/query/Res_byName?L_NAME=(%22mkhan+chen%22+AND+(%22%27od+zer%22+OR+%22ye+shes%22))&LG_NAME=bo-x-ewts&I_LIM=100"
}
```
# Quick search support

lds-pdi offers a quick cross language "search by name" feature using simple Urls as follows:

exact name search:
```
http://localhost:8080/resource/{type}/exact/{name}
```
fuzzy search (returns only the first 100 results):
```
http://localhost:8080/resource/{type}/{name}
```
{type} must be one of the following : 

work ; person ; place ; role ; lineage ;

#### Examples:
```
http://localhost:8080/resource/person/exact/西岡祖秀

http://localhost:8080/resource/work/stong pa nyid bdun cu
```

# CORS support

lds-pdi offers CORS support. Cors configuration parameters can be specified in the ldspdi.properties file, as follows:

```
#cors settings

Allow-Origin=*
Allow-Headers=origin, content-type, accept, authorization
Allow-Credentials=true
Allow-Methods=GET, POST, PUT, DELETE, OPTIONS, HEAD
```

These parameter apply to the whole application.


# Copyright and License

All the code and API are Copyright (C) 2017 Buddhist Digital Resource Center and are under the Apache 2.0 Public License.
