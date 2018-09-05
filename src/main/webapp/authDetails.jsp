<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@page import="io.bdrc.auth.rdf.*"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Bdrc users</title>
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
<p style="text-align:center;font-size:20px;"><b>Users</b></p>
<table style="width:95%;margin:auto">
<tr><th>name</th><th>id</th><th>email</th><th>groups</th><th>roles</th></tr>
<c:forEach items="${RdfAuthModel.getUsers().keySet()}" var="k">
    <c:set var="val" value="${k}"/>
    <tr>
	    <td>${RdfAuthModel.getUser(val).getName()}</td>
	    <td><a href="/resource-auth/${RdfAuthModel.getUser(val).getId().trim()}">${RdfAuthModel.getUser(val).getId()}</a></td>	    
	    <td>${RdfAuthModel.getUser(val).getEmail()}</td>
	    <td>
        <c:if test = "${RdfAuthModel.getUser(val).getGroups()!=null}">
        <c:forEach items="${RdfAuthModel.getUser(val).getGroups()}" var="kg">
            <c:set var="valg" value="${kg}"/>
            <a href="/resource-auth/${kg}">${kg}</a> / (${RdfAuthModel.getGroups().get(valg).getName()})<br> 
        </c:forEach> 
        </c:if>
        </td>
	    <td>
	    <c:if test = "${RdfAuthModel.getUser(val).getRoles()!=null}">
	    <c:forEach items="${RdfAuthModel.getUser(val).getRoles()}" var="kr">
            <c:set var="valr" value="${kr}"/>
            <a href="/resource-auth/${kr}">${kr} </a> / (${RdfAuthModel.getRoles().get(valr).getName()})<br> 
        </c:forEach> 
        </c:if>
	    </td>
    </tr>
</c:forEach> 
</table>
<p style="text-align:center;font-size:20px;"><b>Groups</b></p>
<table style="width:95%;margin:auto">
<tr><th>name</th><th>id</th><th>members</th><th>roles</th></tr>
<c:forEach items="${RdfAuthModel.getGroups().keySet()}" var="k">
    <c:set var="val" value="${k}"/>
    <tr>
        <td>${RdfAuthModel.getGroups().get(val).getName()}</td> 
        <td><a href="/resource-auth/${RdfAuthModel.getGroups().get(val).getId()}">${RdfAuthModel.getGroups().get(val).getId()}</a></td>
        <td>
        <c:if test = "${RdfAuthModel.getGroups().get(val).getMembers()!=null}">
        <c:forEach items="${RdfAuthModel.getGroups().get(val).getMembers()}" var="km">
            <c:set var="valm" value="${km}"/>
            <a href="/resource-auth/${km}">${km}</a> / ${RdfAuthModel.getUser(valm).getName()}<br> 
        </c:forEach> 
        </c:if>
        </td>
        <td>
        <c:if test = "${RdfAuthModel.getGroups().get(val).getRoles()!=null}">
        <c:forEach items="${RdfAuthModel.getGroups().get(val).getRoles()}" var="kmr">
            <c:set var="valmr" value="${kmr}"/>
            <a href="/resource-auth/${kmr}">${kmr}</a> / ${RdfAuthModel.getRoles().get(valmr).getName()}<br> 
        </c:forEach> 
        </c:if>
        </td>     
    </tr>
</c:forEach> 
</table>
<p style="text-align:center;font-size:20px;"><b>Roles</b></p>
<table style="width:95%;margin:auto">
<tr><th>name</th><th>desc</th><th>id</th><th>appId</th><th>appType</th><th>permissions</th></tr>
<c:forEach items="${RdfAuthModel.getRoles().keySet()}" var="k">
    <c:set var="val" value="${k}"/>
    <tr>
       <td>${RdfAuthModel.getRoles().get(val).getName()}</td>
       <td>${RdfAuthModel.getRoles().get(val).getDesc()}</td>
       <td><a href="/resource-auth/${RdfAuthModel.getRoles().get(val).getId()}">${RdfAuthModel.getRoles().get(val).getId()}</a></td>
       <td><a href="/resource-auth/${RdfAuthModel.getRoles().get(val).getAppId()}">${RdfAuthModel.getRoles().get(val).getAppId()}</a></td>
       <td>${RdfAuthModel.getRoles().get(val).getAppType()}</td>
       <td>
        <c:if test = "${RdfAuthModel.getRoles().get(val).getPermissions()!=null}">
        <c:forEach items="${RdfAuthModel.getRoles().get(val).getPermissions()}" var="krp">
            <c:set var="valrp" value="${krp}"/>
            <a href="/resource-auth/${krp}">${krp}</a><br> 
        </c:forEach> 
        </c:if>
        </td>    
    </tr>
</c:forEach> 
</table>
<p style="text-align:center;font-size:20px;"><b>Permissions</b></p>
<table style="width:95%;margin:auto">
<tr><th>name</th><th>desc</th><th>appId</th></tr>
<c:forEach items="${RdfAuthModel.getPermissions()}" var="k">
    <c:set var="val" value="${k}"/>
    <tr>
       <td>${val.getName()}</td>
       <td>${val.getDesc()}</td>       
       <td><a href="/resource-auth/${val.getAppId()}">${val.getAppId()}</a></td>
       
    </tr>
</c:forEach> 
</table>
<p style="text-align:center;font-size:20px;"><b>Applications</b></p>
<table style="width:95%;margin:auto">
<tr><th>name</th><th>desc</th><th>appId</th><th>appType</th></tr>
<c:forEach items="${RdfAuthModel.getApplications()}" var="k">
    <c:set var="val" value="${k}"/>
    <tr>
       <td>${val.getName()}</td>
       <td>${val.getDesc()}</td>       
       <td><a href="/resource-auth/${val.getAppId()}">${val.getAppId()}</a></td>
       <td>${val.getAppType()}</td>
    </tr>
</c:forEach> 
</table>
<p style="text-align:center;font-size:20px;"><b>Endpoints</b></p>
<table style="width:95%;margin:auto">
<tr><th>path</th><th>appId</th><th>groups</th><th>roles</th><th>permissions</th></tr>
<c:forEach items="${RdfAuthModel.getEndpoints()}" var="k">
    <c:set var="val" value="${k}"/>
    <tr>
       <td>${val.getPath()}</td>
       <td><a href="/resource-auth/${val.getAppId()}">${val.getAppId()}</a></td>       
       <td>
        <c:if test = "${val.getGroups()!=null}">
        <c:forEach items="${val.getGroups()}" var="keg">
            <c:set var="valeg" value="${keg}"/>
            <a href="/resource-auth/${keg}">${keg}</a> / (${RdfAuthModel.getGroups().get(valeg).getName()})<br> 
        </c:forEach> 
        </c:if>
       </td> 
       <td>
        <c:if test = "${val.getRoles()!=null}">
        <c:forEach items="${val.getRoles()}" var="ker">
            <c:set var="valer" value="${ker}"/>
            <a href="/resource-auth/${ker}">${ker}</a>  / ${RdfAuthModel.getRoles().get(valer).getName()}<br> 
        </c:forEach> 
        </c:if>
       </td>
       <td>
        <c:if test = "${val.getPermissions()!=null}">
        <c:forEach items="${val.getPermissions()}" var="kep">
            <c:set var="valep" value="${kep}"/>
            <a href="/resource-auth/${kep}">${kep}</a><br> 
        </c:forEach> 
        </c:if>
        </td>  
    </tr>
</c:forEach> 
</table>
<p style="text-align:center;font-size:20px;"><b>Resources Access</b></p>
<table style="width:95%;margin:auto">
<tr><th>policy</th><th>permissions</th></tr>
<c:forEach items="${RdfAuthModel.getResourceAccess()}" var="k">
    <c:set var="val" value="${k}"/>
    <tr>
       <td>${val.getPolicy()}</td>       
       <td><a href="/resource-auth/${val.getPermission()}">${val.getPermission()}</a></td>
    </tr>
</c:forEach> 
</table>
</body>
</html>