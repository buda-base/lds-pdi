<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="io.bdrc.ldspdi.sparql.*"%>
<%@page import="io.bdrc.ontology.service.core.*"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Ontology Class - ${model.getId()}</title>
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
<body style="margin-left:50px;width:80%;">
<h2>Ontology Class - <a href="${model.getUri()}">${model.getId()}</a></h2>
<c:if test = "${model.isPresent()}">
    <c:set var="uri" value="${model.getUri()}"/>
    <!-- PARENT CLASS -->
    <c:if test = "${model.hasParent()}">    
        <h4>Parent class: <a href="${model.getParent().getUri()}">${model.getParent().getId()}</a></h4>
    </c:if>    
       
    <!-- LABELS -->
    <c:if test = "${model.getLabels().size()>0}">
        <h4>Labels: </h4>
        <c:forEach items="${model.getLabels()}" var="label">    
            ${label}<br>
        </c:forEach> 
    </c:if>
    
    <!-- COMMENTS -->
    <c:if test = "${model.getComments().size()>0}">
        <h4>Comments:</h4>
        <c:forEach items="${model.getComments()}" var="comm">    
            <div style="white-space: pre-wrap;">${comm}</div><hr/>
        </c:forEach> 
    </c:if>
    
    <!-- INDIVIDUALS -->
    <c:if test = "${model.getIndividuals().size()>0}">
        <h4>Individuals:</h4>
        <c:forEach items="${model.getIndividuals()}" var="ind">
            <c:set var="val" value="${ind.getNameSpace()}"/>    
            <ul><li><a href="${ind.getURI()}">${Prefixes.getPrefix(val)}${ind.getLocalName()}</a></li></ul>
        </c:forEach> 
    </c:if>
    
    <!-- DOMAIN PROPERTIES -->
    <c:if test = "${OntData.getDomainUsages(uri).size()>0}">
    <table id="specs">
        <tr><th>Root properties applying to the ${model.getId()} domain:</th></tr>
        <tr><td style="font-size:16px;line-height: 1.6;">
        <c:forEach items="${OntData.getDomainUsages(uri)}" var="dom">                
            <a href="${dom.getURI()}">${dom.getLocalName()}</a> /
        </c:forEach> 
        </td></tr>
     </table>
    </c:if>
    <br>
    <!-- DOMAIN SUB PROPERTIES -->
    <c:if test = "${OntData.getAllSubProps(uri).keySet().size()>0}">
    <table id="specs">
        <tr><th>Sub properties applying to the ${model.getId()} domain:</th></tr>
        <tr><td style="font-size:16px;line-height: 1.6;">
        <c:forEach items="${OntData.getAllSubProps(uri).keySet()}" var="key"> 
        <c:set var="k" value="${key}"/>
        <table style="margin-left:50px;">
            <tr><td><i><b>Sub properties of ${key}</b></i></td></tr>               
            <tr><td>
            <c:forEach items="${OntData.getAllSubProps(uri).get(k)}" var="pr">
                <a href="${pr.getURI()}">${pr.getLocalName()}</a> /
            </c:forEach> 
            </td></tr>
        </table>
        </c:forEach> 
        </td></tr>
     </table>
    </c:if>
    <br>
    <!-- INHERITED DOMAIN PROPERTIES -->
    <c:if test = "${model.hasParent()}">
    <c:set var="p_uri" value="${model.getParent().getUri()}"/>
        <c:if test = "${OntData.getDomainUsages(p_uri).size()>0}">
	    <table id="specs">
	        <tr><th>Inherited domain properties from ${model.getParent().getId()}:</th></tr>
	        <tr><td style="font-size:16px;line-height: 1.6;">
	        <c:forEach items="${OntData.getDomainUsages(p_uri)}" var="dom">                
	            <a href="${dom.getURI()}">${dom.getLocalName()}</a> /
	        </c:forEach> 
	        </td></tr>
	     </table>
	    </c:if>
    </c:if>
    <br>
    <!-- RANGE PROPERTIES -->
    <c:if test = "${OntData.getRangeUsages(uri).size()>0}">
        <table id="specs">
        <tr><th>Properties whose range is ${model.getId()}:</th></tr>
        <tr><td style="font-size:16px;line-height: 1.6;">
        <c:forEach items="${OntData.getRangeUsages(uri)}" var="dom">                
            <a href="${dom.getURI()}">${dom.getLocalName()}</a> /
        </c:forEach>
        </td></tr>
     </table> 
    </c:if>
    
    <!-- SUBCLASSES -->
    <c:if test = "${model.getSubclasses().size()>0}">
        <h3>Subclasses:</h3>
        <c:forEach items="${model.getSubclasses()}" var="sub">               
            <ul><li><a href="${sub.getUri()}">${sub.getId()}</a></li></ul>
        </c:forEach> 
    </c:if>
    
    
</c:if>
<br><br>
</body>
</html>