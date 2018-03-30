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
    padding-left: 12px;
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
<p style="text-align:center;font-size:16px;">
<a href="#prefixes">Prefixes / </a>
<a href="#root">Root classes / </a>
<a href="#all">Classes / </a>
<a href="#allprops">Properties / </a>
<a href="#indiv">Individuals</a></p>

<!-- PREFIXES -->
<a name="prefixes"></a> 
<p style="text-align:center;font-size:20px;"><b>${OntData.getNumPrefixes()} Prefixes</b></p>
<table style="width:55%;margin:auto">
<tr><th>prefix</th><th>name space</th></tr>
<c:forEach items="${OntData.getPrefixMap().keySet()}" var="k">
    <c:set var="val" value="${k}"/>
    <tr><td>${k}:</td><td>${OntData.getPrefixMap().get(val)}</td></tr>
</c:forEach> 
</table> 

<!-- ROOT CLASSES -->
<a name="root"></a>
<p style="text-align:center;font-size:20px;"><b>${OntData.getNumRootClasses()} simple root classes</b></p>
<table style="width:55%;margin:auto">
<tr><th>local name</th><th>full URI</th></tr>
<c:forEach items="${OntData.getOntRootClasses()}" var="root">    
    <tr><td>${root.getId()}</td><td><a href="${root.getUri()}">${root.getUri()}</a></td></tr>
</c:forEach> 
</table> 

<!-- ALL CLASSES -->
<a name="all"></a>
<p style="text-align:center;font-size:20px;"><b>${OntData.getAllClasses().size()} classes</b></p>
<table style="width:55%;margin:auto">
<tr><th>local name</th><th>full URI</th></tr>
<c:forEach items="${OntData.getAllClasses()}" var="cls">    
    <tr><td>${cls.getLocalName()}</td><td><a href="${cls.getURI()}">${cls.getURI()}</a></td></tr>
</c:forEach> 
</table>

<!-- ALL PROPERTIES -->
<a name="allprops"></a>
<p style="text-align:center;font-size:20px;"><b>${OntData.getAllProps().size()} properties</b></p>
<table style="width:55%;margin:auto">
<tr><th>local name</th><th>full URI</th><th>property type</th></tr>
<c:forEach items="${OntData.getAllProps()}" var="prop">    
    <tr><td>${prop.getLocalName()}</td><td><a href="${prop.getURI()}">${prop.getURI()}</a></td><td>${prop.getRDFType().getLocalName()}</td></tr>
</c:forEach> 
</table>

<!-- ALL INDIVIDUALS -->
<a name="indiv"></a>
<p style="text-align:center;font-size:20px;"><b>${OntData.getAllIndividuals().size()} individuals</b></p>
<table style="width:55%;margin:auto">
<tr><th>Local name</th><th>Full URI</th><th> Class</th></tr>
<c:forEach items="${OntData.getAllIndividuals()}" var="ind">    
    <tr><td>${ind.getLocalName()}</td><td><a href="${ind.getURI()}">${ind.getURI()}</a></td><td><a href="${ind.getRDFType()}">${ind.getRDFType().getLocalName()}</a></td></tr>
</c:forEach> 
</table>
</body>
</html>