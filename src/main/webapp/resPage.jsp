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
    border: 1px solid #ddd;
}

td {
    text-align: left;
    vertical-align:top;
    padding: 16px;
}

th {
    padding-top: 12px;
    padding-bottom: 12px;
    text-align: center;
    background-color: #4e7F50;
    color: white;
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
<table style="width: 90%;">
<tr>
	<td style="width: 60%;">${model.getQuery().trim()}<br>
	
	<table style="width: 100%;">
	<tr><td></td><td style="text-align: center;"><b>PARAMS</b></td></tr>
	   <c:forEach items="${model.getParams()}" var="par">	       
	       <tr><td><b>${par.name} </b></td><td>${par}<br></td></tr>
	   </c:forEach>
	   <tr><td></td><td style="text-align: center;"><b>OUTPUT</b></td></tr>
	   <c:forEach items="${model.getOutputs()}" var="otp">
           <tr><td><b>${otp.name} </b></td><td>${otp}<br></td></tr>
       </c:forEach>
    </table>
	</td>
	<td>
	<c:if test="${model.getParamList().size()>0}">
		<form action="/query/${model.getId()}">
			<c:forEach items="${model.getParamList()}" var="p">
			    <c:set var="val" value="${p}"/>
                <c:if test="${!p.equals(\"NONE\")}">
                ${p}<br>				    
	            <input type="text" id="${p}" name="${p}" value='${model.getParamValue(val)}'><br>
	            </c:if>
			</c:forEach>
			<br>		    		    		  
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
<c:forEach items="${model.headrows}" var="h">
<th>${h}</th>
</c:forEach>
</tr>
<c:forEach items="${model.mvc_rows}" var="qsi">
<tr>
	<c:forEach items="${model.headrows}" var="h">
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