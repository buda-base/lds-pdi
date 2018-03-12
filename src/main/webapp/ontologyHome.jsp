<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="io.bdrc.ontology.service.core.*"%>
<%@page import="java.util.List"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>BDRC Ontology home page</title>
<style>
#specs {
    font-family: "Trebuchet MS", Arial, Helvetica, sans-serif;
    border-collapse: collapse;
    width: 95%;
}
#specs td, #customers th {
    border: 0px solid #ddd;
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
</head>
<body id="specs">
<table style="margin:auto;width:60%;background-color:#f2f2f2;border:0px">
<tr>
<td><img src="https://www.tbrc.org/browser/images/webcontent/Layout2017/BDRC.svg" style="margin-bottom:10px;margin-top:10px;vertical-align:middle;width:100px;"/></td>
<td style="vertical-align:middle;text-align:center;font-size:28px">Buddhist Digital Resource Center - Ontology homepage<br></td>
</tr>
</table>
<p style="text-align:center;font-size:16px;">Retrieved from <a href="${OntAccess.getOwlURL()}">${OntAccess.getOwlURL()}.html</a></p>

<!-- PREFIXES -->
<p style="text-align:center;font-size:20px;"><b>${OntAccess.getNumPrefixes()} Prefixes</b></p>
<table style="width:55%;margin:auto">
<tr><th>prefix</th><th>name space</th></tr>
<c:forEach items="${OntAccess.getPrefixMap().keySet()}" var="k">
    <c:set var="val" value="${k}"/>
    <tr><td>${k}:</td><td>${OntAccess.getPrefixMap().get(val)}</td></tr>
</c:forEach> 
</table> 

<!-- ROOT CLASSES -->
<p style="text-align:center;font-size:20px;"><b>${OntAccess.getNumRootClasses()} simple root classes</b></p>
<table style="width:55%;margin:auto">
<tr><th>local name</th><th>full URI</th></tr>
<c:forEach items="${OntAccess.getOntRootClasses()}" var="root">    
    <tr><td>${root.getId()}</td><td><a href="${root.getUri()}">${root.getUri()}</a></td></tr>
</c:forEach> 
</table> 

<!-- ALL CLASSES -->
<p style="text-align:center;font-size:20px;"><b>${OntAccess.getAllClasses().size()} classes</b></p>
<table style="width:55%;margin:auto">
<tr><th>local name</th><th>full URI</th></tr>
<c:forEach items="${OntAccess.getAllClasses()}" var="cls">    
    <tr><td>${cls.getLocalName()}</td><td><a href="${cls.getURI()}">${cls.getURI()}</a></td></tr>
</c:forEach> 
</table>

<!-- ALL PROPERTIES -->
<p style="text-align:center;font-size:20px;"><b>${OntAccess.getAllProps().size()} properties</b></p>
<table style="width:55%;margin:auto">
<tr><th>local name</th><th>full URI</th></tr>
<c:forEach items="${OntAccess.getAllProps()}" var="prop">    
    <tr><td>${prop.getLocalName()}</td><td><a href="${prop.getURI()}">${prop.getURI()}</a></td></tr>
</c:forEach> 
</table>
</body>
</html>