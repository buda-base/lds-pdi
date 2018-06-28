<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="io.bdrc.ldspdi.ontology.service.core.*"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Ontology Property - ${model.getUri()}</title>
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
<body>
<h2>Ontology Property - <a href="${model.getUri()}">${model.getName()}</a></h2>
<b>Rdf Type:</b> <a href="${model.getRdfType()}">${model.getRdfType()}</a><br>
<b>Label:</b> ${model.getLabel()}<br>
<b>Domain:</b> <a href="${model.getDomain()}">${model.getDomain()}</a><br>
<b>Range:</b> <a href="${model.getRange()}">${model.getRange()}</a><br>
<b>Comment:</b> ${model.getComment()}<br>
<br>
<!-- SUB PROPS -->
    <c:if test = "${model.getAllSubProps().size()>0}">
        <h3>Sub properties: </h3>
        
        <c:forEach items="${model.getAllSubProps()}" var="prop"> 
        <table id="specs" style="width:60%;"> 
        <tr><th></th><th>${prop.getName()}</th></tr>          
            <tr><td><b>Uri :</b></td><td> <a href="${prop.getUri()}">${prop.getUri()}</a></td></tr>            
            <tr><td><b>Rdf Type :</b></td><td> <a href="${prop.getRdfType()}">${prop.getRdfType()}</a></td></tr>
            <tr><td><b>Label :</b></td><td> ${prop.getLabel()}</td></tr>
            <tr><td><b>Domain :</b></td><td> <a href="${prop.getDomain()}">${prop.getDomain()}</a></td></tr>            
            <tr><td><b>Range :</b></td><td> <a href="${prop.getRange()}">${prop.getRange()}</a></td></tr>
            </table><br>          
        </c:forEach>
        
    </c:if> 
    
    <!-- PARENT PROPS -->
    <c:if test = "${model.getParentProps().size()>0}">
        <h3>Parent properties: </h3>
        
        <c:forEach items="${model.getParentProps()}" var="prop"> 
        <table id="specs" style="width:60%;"> 
        <tr><th></th><th>${prop.getName()}</th></tr>          
            <tr><td><b>Uri :</b></td><td> <a href="${prop.getUri()}">${prop.getUri()}</a></td></tr>            
            <tr><td><b>Rdf Type :</b></td><td> <a href="${prop.getRdfType()}">${prop.getRdfType()}</a></td></tr>
            <tr><td><b>Label :</b></td><td> ${prop.getLabel()}</td></tr>
            <tr><td><b>Domain :</b></td><td> <a href="${prop.getDomain()}">${prop.getDomain()}</a></td></tr>            
            <tr><td><b>Range :</b></td><td> <a href="${prop.getRange()}">${prop.getRange()}</a></td></tr>
            </table><br>          
        </c:forEach>
        
    </c:if>     
</body>
</html>