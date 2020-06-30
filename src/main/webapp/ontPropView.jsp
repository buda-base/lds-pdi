<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="io.bdrc.ldspdi.ontology.service.core.*"%>
<%@page import="io.bdrc.ldspdi.utils.Helpers"%>
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
.lang {
    position: relative;
    margin: 0;
    vertical-align: -5px;
    padding: 3px;
    line-height: 0;
    font-size: 12px;
    color: #778899;
}
</style>
</head>
<body>
<h2>Ontology Property - <a href="${model.getUri()}">${model.getName()}</a></h2>
<b>Type:</b> <a href="${model.getRdfTypeUri()}">${model.getRdfType()}</a><br>
<b>Label:</b> ${model.getLabel()}<span class="lang">${model.getLabelLang()}</span><br>
<c:choose>
<c:when test="${model.isDomainInherited()}">
<b>Domain:</b> Inherited<br>
</c:when>
<c:otherwise>
<b>Domain:</b> 
<c:forEach items="${model.getDomain()}" var="dom">
<a href="${dom}">${OntData.ontAllMod.shortForm(dom)}</a>&nbsp; 
</c:forEach>
<br>
</c:otherwise>
</c:choose>
<c:choose>
<c:when test="${model.isRangeInherited()}">
<b>Range:</b> Inherited<br>
</c:when>
<c:otherwise>
<b>Range:</b> 
<c:forEach items="${model.getRange()}" var="range">
<a href="${range}">${OntData.ontAllMod.shortForm(range)}</a>&nbsp; 
</c:forEach>
<br>
</c:otherwise>
</c:choose>
<!-- COMMENTS -->
<c:if test = "${model.getComments().size()>0}">
        <h4>Comments:</h4>
        <c:forEach items="${model.getCommentsLang()}" var="comm">    
            <div style="white-space: pre-wrap;">${comm[0]}<span class="lang">${comm[1]}</span></div><hr/>
        </c:forEach> 
</c:if>

<!-- ADMIN ANNOTS -->
    <c:if test = "${model.getAdminAnnotProps().size()>0}">
        <h4>Annotations:</h4>
        <c:forEach items="${model.getAdminAnnotProps()}" var="prop">
            <div style="white-space: pre-wrap;"><b>adm:${prop.getLocalName()}</b><span>&nbsp;:&nbsp;${model.getPropertyValue(prop).asLiteral().getString()}</span><span class="lang">${model.getPropertyValue(prop).asLiteral().getLanguage()}</span></div><hr/>
        </c:forEach>
    </c:if>
    
    
<c:if test = "${OntData.getOwlCharacteristics().getOwlProps(model.getUri()).size()>0}">
<b>Characteristics: </b>
<c:forEach items="${OntData.getOwlCharacteristics().getOwlProps(model.getUri())}" var="owlprop"> 
 <a href="${owlprop}">${OntData.getOwlCharacteristics().getPrefixed(owlprop)}</a> 
</c:forEach>
</c:if>
<c:if test = "${OntData.getOwlCharacteristics().isInverseOfProp(model.getUri())}">
<b>Is Inverse of: </b><a href="${OntData.getOwlCharacteristics().getInverseOfProp(model.getUri())}">
${OntData.getOwlCharacteristics().getShortInverse(OntData.getOwlCharacteristics().getInverseOfProp(model.getUri()))}</a>
</c:if>

<!-- SUB PROPS -->
    <c:if test = "${model.getAllSubProps(true).size()>0}">
        <h3>Sub properties: </h3>
        
        <c:forEach items="${model.getAllSubProps(true)}" var="prop"> 
        <table id="specs" style="width:60%;"> 
        <tr><th></th><th>${prop.getName()}</th></tr>          
            <tr><td><b>Uri:</b></td><td> <a href="${prop.getUri()}">${prop.getUri()}</a></td></tr>            
            <tr><td><b>Type:</b></td><td> <a href="${prop.getRdfTypeUri()}">${prop.getRdfType()}</a></td></tr>
            <tr><td><b>Label:</b></td><td>${prop.getLabel()}<span class="lang">${prop.getLabelLang()}</span></td></tr>
            <c:choose>
                <c:when test="${prop.isDomainInherited()}">
                <tr><td><b>Domain:</b></td><td>Inherited</td></tr>
                </c:when>
                <c:otherwise>
                <tr><td><b>Domain:</b></td><td>
                <c:forEach items="${prop.getDomain()}" var="dom">
                    <a href="${dom}">${OntData.ontAllMod.shortForm(dom)}</a>&nbsp; 
                </c:forEach>
                </td></tr>
                </c:otherwise>
            </c:choose> 
            <c:choose>
                <c:when test="${prop.isRangeInherited()}">
                <tr><td><b>Range:</b></td><td>Inherited</td></tr>
                </c:when>
                <c:otherwise>           
                <tr><td><b>Range:</b></td><td> 
                <c:forEach items="${prop.getRange()}" var="range">
                <a href="${range}">${OntData.ontAllMod.shortForm(range)}</a>&nbsp; 
                </c:forEach>
                </td></tr>
                </c:otherwise>
            </c:choose>
            </table><br>          
        </c:forEach>
        
    </c:if> 
    <!-- PARENT PROPS -->
    <c:if test = "${model.getParentProps().size()>0}">
        <h3>Parent properties: </h3>
        
        <c:forEach items="${model.getParentProps()}" var="prop">         
        <table id="specs" style="width:60%;"> 
        <tr><th></th><th>${prop.getName()}</th></tr>          
            <tr><td><b>Uri:</b></td><td> <a href="${prop.getUri()}">${prop.getUri()}</a></td></tr>            
            <tr><td><b>Type:</b></td><td> <a href="${prop.getRdfTypeUri()}">${prop.getRdfType()}</a></td></tr>
            <tr><td><b>Label:</b></td><td> ${prop.getLabel()}<span class="lang">${prop.getLabelLang()}</span></td></tr>
            <c:choose>
                <c:when test="${prop.isDomainInherited()}">
                <tr><td><b>Domain:</b></td><td>Inherited</td></tr>
                </c:when>
                <c:otherwise>
                <tr><td><b>Domain:</b></td><td> 
                <c:forEach items="${prop.getDomain()}" var="dom">
                    <a href="${dom}">${OntData.ontAllMod.shortForm(dom)}</a>&nbsp; 
                </c:forEach>
                </td></tr>
                </c:otherwise>
            </c:choose>            
            <c:choose>
                <c:when test="${prop.isRangeInherited()}">
                <tr><td><b>Range:</b></td><td>Inherited</td></tr>
                </c:when>
                <c:otherwise>           
                <tr><td><b>Range:</b></td><td> 
                <c:forEach items="${prop.getRange()}" var="range">
                <a href="${range}">${OntData.ontAllMod.shortForm(range)}</a>&nbsp; 
                </c:forEach>
                </td></tr>
                </c:otherwise>
            </c:choose>
            </table><br>          
        </c:forEach>
        
    </c:if>     
    
</body>
</html>