<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="io.bdrc.ldspdi.sparql.QueryConstants"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<script type="text/javascript"> 
function showHide() {    
	var x = document.getElementById("query");    
	if (x.style.display === "none") {        
		x.style.display = "block";    
	} else {
        x.style.display = "none";    
    }
}   
</script>
<style>
table {
    border-collapse: collapse;
    border-spacing: 0;
    margin-left: 20px;
    width: 80%;
    border: 1px solid #ddd;
}

th, td {
    text-align: left;
    padding: 16px;
}

tr:nth-child(even) {
    background-color: #f2f2f2
}
input[type=text], select {
    width: 100%;
    padding: 12px 20px;
    margin: 8px 0;
    display: inline-block;
    border: 1px solid #ccc;
    border-radius: 4px;
    box-sizing: border-box;
}

input[type=submit] {
    width: 80%;
    background-color: #4CAF50;
    color: white;
    padding: 14px 20px;
    margin: 8px 0;
    border: none;
    border-radius: 4px;
    cursor: pointer;
}

input[type=submit]:hover {
    background-color: #45a049;
}

</style>
<title>BDRC Public data results</title>
</head>
</head>
<body>
<br><span><b> Returned ${model.numResults} results in ${model.getExecTime()} ms</b></span><br>
<span><b> Page number : ${model.getPageNumber()}</b></span><br>
<span><b> Total pages : ${model.getNumberOfPages()}</b></span><br>
<span><b> ResultSet Hash=${model.getHash()}</b></span><br>
<c:if test="${!model.isUrlQuery()}">
<span><b> Template name=${model.getId()}  </b>
<a href="javascript:showHide()">(view/hide query)</a>
</span>
<div id="query" style="display:none">
<br>
<table>
<tr>
	<td>${model.getQuery().trim()}</td>
	<td>
	<c:if test="${model.getParamList().size()>0}">
		<form action="/resource/templates">
			<c:forEach items="${model.getParamList()}" var="p">
			    ${p}<br>					    
	            <input type="text" id="${p}" name="${p}" placeholder="..."><br>
			</c:forEach>
			<br>
			
		    <input type="hidden" id="searchType" name="searchType" value="${model.getId()}">		    		  
		    <input type="submit" value="Submit">
		</form>
	</c:if>	
	</td>
</tr>
</table>
</div>
</c:if>
<br><br>
<c:if test="${!model.isFirstPage()}"><a href="${model.getpLinks().getPrevGet()}">Prev</a></c:if>
<c:if test="${!model.isLastPage()}"><a href="${model.getpLinks().getNextGet()}">Next</a></c:if>
<br><br>
<table>
<tr>
<c:forEach items="${model.headers}" var="h">
<th>${h}</th>
</c:forEach>
</tr>
<c:forEach items="${model.rows}" var="qsi">
<tr>
	<c:forEach items="${model.headers}" var="h">
	<td>${qsi.getValue(h)}</td>
	</c:forEach>
</tr>
</c:forEach>
</table>
<br>
<c:if test="${!model.isFirstPage()}"><a href="${model.getpLinks().getPrevGet()}">Prev</a></c:if>
<c:if test="${!model.isLastPage()}"><a href="${model.getpLinks().getNextGet()}">Next</a></c:if>
</body>
</html>