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

Any authorized user for the github lds-queries repo can therefore create, TEST thoroughly (10 times better than one), and share your queries by pushing them to the github repository.

Changes will appear after refreshing
```
http://localhost:8080/lds-pdi/index.jsp
```

# Running

```
mvn jetty:run
```
# Home page
```
http://localhost:8080/index.jsp
```

# Ontology browsing service page
```
http://localhost:8080/ontOverview.jsp
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
# Query templates

### GET && POST
/templates (POST requests return JSON format - Jena raw response format - see https://www.w3.org/TR/rdf-sparql-json-res/ )
```
Ex GET: http://localhost:8080/resource/templates?searchType=pdi_p_luceneName&L_NAME=klu+sgrub
Ex POST: curl --data "searchType=pdi_w_bibli&L_NAME=rgyud+bla+ma" http://localhost:8080/resource/templates
Ex POST JSON : curl -H "Content-Type: application/json" -X POST -d '{"searchType":"pdi_w_bibli","L_NAME":"chos dbyings bstod pa"}' http://localhost:8080/resource/templates
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

# Query templates format specifications

This framework will automatically add new sparql query templates to the index page based on files published to the « /local/dir/queries » directory.

New query files must have the .arq extension and are formatted as follows :

```
#QueryScope=Place
#QueryReturnType=Table
#QueryResults=A table containing the Id and the name of the place whose name contains the L_NAME param value
#QueryParams=L_NAME
#QueryUrl=/lds-pdi/query?searchType=pdi_pl_name&L_NAME=dgon gsar

select ?Place_ID ?Place_Name
where {
  ?Place_ID a :Place;
   skos:prefLabel ?Place_Name .
  Filter(contains(?Place_Name, ?NAME))
}
```
Note : the parameter placeholder of the query must match the value of QueryParams.

### Guidelines for creating query templates

ldspdi performs a strict parameter evaluation in order to prevent Sparql injection. It therefore requires parameter types to be specified. Parameter types are as follows :

Literal : each literal parameter name must be prefixed by « L_ » (Ex : L_NAME)

Integer : each literal parameter name must be prefixed by « I_ » (Ex : I_LIM)

Resource : each literal parameter name must be prefixed by « R_ » (Ex : R_RES)


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
#QueryResults=A table containing the Id and matching skos:prefLabel for the given query and language tag with the given limit
#QueryParams=L_NAME,L_LANG,I_LIM
#QueryUrl=/lds-pdi/query?searchType=pdi_any_luceneLabel&L_NAME=("mkhan chen" AND ("'od zer" OR "ye shes"))&L_LANG=@bo-x-ewts&I_LIM=100


select distinct ?s ?lit
WHERE
{
  (?s ?sc ?lit) text:query ( ?L_NAME?L_LANG ) .
} limit ?I_LIM

```
Note : the @ is now part of the L_LANG literal parameter and is not part of the query skeleton anymore.

```
#QueryScope=Person
#QueryReturnType=Table
#QueryResults=All the detailed admin info (notes, status, log entries) about the person data
#QueryParams=R_RES
#QueryUrl=/lds-pdi/query?searchType=pdi_p_adminDetails&R_RES=P1583


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

# Copyright and License

All the code and API are Copyright (C) 2017 Buddhist Digital Resource Center and are under the Apache 2.0 Public License.
