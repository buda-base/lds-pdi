<%@page import="io.bdrc.ldspdi.Utils.DocFileBuilder"%>
<%@page import="io.bdrc.ldspdi.service.GitService"%>
<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<% 
String serverName = request.getServerName();
int portNumber = request.getServerPort();
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
<title>BDRC Public data Interface</title>
</head>
<body>
<h1>TBRC Public Data Interface</h1>
        <p>This resource provides direct data access to the TBRC Library</p>
        <p>The data is provided under a Creative Commons CC-BY license.</p>
        <p>This work is licensed under the Creative Commons Attribution 3.0 Unported License.<br> 
    To view a copy of this license, visit http://creativecommons.org/licenses/by/3.0/ or send a letter to 
    Creative Commons, 444 Castro Street, Suite 900, Mountain View, California, 94041, USA.
    </p>
    
<h2>Navigate through BDRC Ontology</h2>
<p>You can use this service to access the current BDRC ontology and discover the data model:</p>
<p><a href="ontOverview.jsp">Ontology service</a></p>

<h2>Instructions</h2>
<p>Public queries are run via urls whose specifications are given below. However, you can get any resource turtle representation 
using this general url format:</p>
<p><a href="http://<%=serverName+":"+portNumber%>/resource/P1583">http://<%=serverName+":"+portNumber%>/resource/P1583</a></p>
<p>where P1583 is a BDRC resource ID</p>
<div align="center"><h2>Url specifications by query types</h2></div>
<div align="center"><%= content %></div>
<br><br><hr><br>
</body>
</html>
