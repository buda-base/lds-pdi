<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="io.bdrc.ldspdi.rest.resources.*"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Multiple choices</title>
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
<h1>Possible choices</h1>
<p>The document name you requested (${it.toString()}) without specifying a content type could not be found on this server. However, we found documents similar to the one you requested having various content type.</p>
<br>

<table id="specs" style="width:60%;">
<tr><th>Link</th><th>Mime Type</th></tr> 
<c:forEach items="${MediaTypeUtils.getExtensionMimeMap().keySet()}" var="k">
<c:set var="val" value="${k}"/>
<tr><td><a href="${it}.${k}">${it}.${k}</a><td>${MediaTypeUtils.getExtensionMimeMap().get(val)}</td></tr>  
</c:forEach>
</table>

</body>
</html>