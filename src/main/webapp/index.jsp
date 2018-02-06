<%@page import="io.bdrc.ldspdi.Utils.DocFileBuilder"%>
<%@page import="io.bdrc.ldspdi.service.GitService"%>
<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<% 
GitService.update(request.getServletContext().getInitParameter("queryPath"));
String content=DocFileBuilder.getContent(new String("/resource/templates"));

%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
#specs {
    font-family: "Trebuchet MS", Arial, Helvetica, sans-serif;
    border-collapse: collapse;
    width: 90%;
}

#specs td, #customers th {
    border: 1px solid #ddd;
    padding: 8px;
}

#specs tr:nth-child(even){background-color: #f2f2f2;}

#specs tr:hover {background-color: #ddd;}

#specs th {
    padding-top: 12px;
    padding-bottom: 12px;
    text-align: left;
    background-color: #4e7F50;
    color: white;
}
</style>
<script type="text/javascript">
function onto(){
	var x = "ontology."+document.getElementById("format").value;
	window.location.assign(x);
}
</script>
<title>BDRC Public data Interface</title>
</head>
<body>
<h1>BDRC Public Data Interface</h1>
        <p>This resource provides direct data access to the BDRC Library</p>        
    
<h2>Navigate through BDRC Ontology</h2>
<p>You can use this service to access the current BDRC ontology and discover the data model:</p>
<p><a href="/demo/ontOverview.jsp">Ontology service</a></p>
<div> View/download the ontology file: <select id="format">
  <option value="ttl">text/turtle=ttl</option>
  <option value="rdf">application/rdf+xml=rdf</option>
  <option value="owl">application/owl+xml=owl</option>
  <option value="json">application/json=json</option>
  <option value="nt">application/n-triples=nt</option>
  <option value="trix">application/trix+xml=trix</option>
</select> <button onclick="javascript:onto();" type="button"> View </button>
</div>

<h2>Instructions</h2>
<p>Public queries are run via urls whose specifications are given below. However, you can get any resource turtle representation 
using this general url format:</p>
<p><a href="/resource/P1583">http://serverName:portNumber/resource/P1583</a></p>
<p>where P1583 is a BDRC resource ID</p>
<div align="center"><h2>Url specifications by query types</h2></div>
<div align="center"><%= content %></div>
<br><br><hr><br>
</body>
</html>
