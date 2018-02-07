# LDS-PDI

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

/templates : POST requests return the following JSON format :
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
Ex GET: http://localhost:8080/resource/templates?searchType=Res_byName&L_NAME=("mkhan chen" AND ("'od zer" OR "ye shes"))&L_LANG=@bo-x-ewts&I_LIM=100
Ex POST: curl --data "searchType=Res_byName&L_NAME=(\"mkhan chen\" AND (\"'od zer\" OR \"ye shes\"))&LG_NAME=bo-x-ewts&I_LIM=100" http://localhost:8080/resource/templates
```

**NOTE:** You can get this json output format using GET requests by adding &jsonOut or ?jsonOut to your URL request.

Ex Testing POST JSON : 


1) Create a file test.json :

```
{
  "searchType": "Res_byName",
  "L_NAME": "(\"mkhan chen\" AND (\"'od zer\" OR \"ye shes\"))",
  "LG_NAME": "bo-x-ewts",
  "I_LIM":"100"
}
```

2) running curl :
```
curl -H "Content-Type: application/json" -X POST -d @test.json http://localhost:8080/resource/templates
```

# Query templates format specifications

This framework will automatically add new sparql query templates to the index page based on files published to the « /local/dir/queries » directory.

New query files must have the .arq extension and are formatted as follows :

```
#QueryScope=Place
#QueryReturnType=Table
#QueryResults=A table containing the Id and the name of the place whose name contains the L_NAME param value
#QueryParams=L_NAME
#QueryUrl=?searchType=Place_byName&L_NAME=dgon gsar

select ?Place_ID ?Place_Name
where {
  ?Place_ID a :Place;
   skos:prefLabel ?Place_Name .
  Filter(contains(?Place_Name, ?L_NAME))
}
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
#QueryUrl=?searchType=Res_byName&L_NAME=("mkhan chen" AND ("'od zer" OR "ye shes"))&LG_NAME=bo-x-ewts&I_LIM=100

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
#QueryUrl=?searchType=Person_adminDetails&R_RES=P1583


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

# JSON API

## Available query templates

```
GET or POST: http://localhost:8080/queries
```

returns JSON objects of the form
```
{"name":"Lineage_list","descLink":"http://localhost:8080/queries/Lineage_list"}
```
Where descLink is a link to the JSON query template representation.

## Query templates description
```
GET or POST: http://localhost:8080/queries/{template_name}
```
returns JSON queryTemplate object :
```
{ 
  "id" : "PersonNames_byNameLang", 
  "domain" : "public", 
  "queryScope" : "Person", 
  "queryResults" : "A table containing the Id, primary_name, name_type and name_type value for people whose any name matches the NAME with the LANG tag", 
  "queryReturn" : "Table", 
  "queryParams" : "L_NAME,LG_NAME", 
  "template" : " select distinct ?Person ?Primary_Name ?Name_Type ?Name WHERE { (?Person ?sc1 ?Primary_Name) text:query ( ?L_NAME ) . ?Person a :Person . ?Person :personName ?p_name . (?p_name ?sc2 ?Name) text:query (rdfs:label ?L_NAME) . ?p_name rdf:type ?Name_Type . OPTIONAL {?p_name ?title ?Name} . FILTER (?Name_Type != :PersonName) }", 
  "demoLink" : "/resource/templates?searchType=PersonNames_byNameLang&L_NAME=%22%27od+zer%22&LG_NAME=bo-x-ewts" }
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
