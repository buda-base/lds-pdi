<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="io.bdrc.ontology.service.core.*"%>
<%@page import="java.util.*"%>
<%
HashMap t=(HashMap)request.getAttribute("model");
OntClassModel model = (OntClassModel)t.get("model"); 
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Ontology Class - <%= model.getId()%></title>
</head>
<body>
<h2>Ontology Class - <a href="ontology?classUri=<%= model.getUri()%>"><%= model.getId()%></a></h2>
<% if (model.isPresent()) {
	if (model.hasParent()){%>
		<h4>Parent class: <a href="ontology?classUri=<%=model.getParent().getUri()%>"><%=model.getParent().getId()%></a></h4>
	    <% } 
	    List<String> labels = model.getLabels() ;	    
	    if(labels.size()!=0){%>
		    <h4>Labels: </h4>
		    <p><%for(String label:labels){%> <%=label %><br/>
		    <%} %>
	    <%}else{ %><p>No labels found.</p>
	    <%}%>
	    <h4>Comments:</h4>
	    <%List<String> comments = model.getComments() ; 
	    if(comments.size()!=0){
	      for(String comment:comments){%>
		<div style="white-space: pre-wrap;"><%=comment%></div><hr/>
		<%}}else { %><p>No comments found.</p>
		<%} %>
		<% List<StmtModel> stmts =model.getOtherProperties(); %>
	            <h4>Other Properties:</h4>
	            <% if(stmts.size()!=0){
	            	for(StmtModel stmt:stmts){ %><p><%=stmt.getPropertyId()%> 	            
					<% if(stmt.objectHasUri()) {%>					
						<a href="ontology?classUri=<%=stmt.getObjectUri()%>"><%=stmt.getObject() %></a>
					<%}else{ %>
						<%=stmt.getObject() %></p>
					<%}
					}
	              }else{ %>
	          <p>No properties found.</p>
	        <%} %>
	        <% List<OntClassModel> subclasses =model.getSubclasses(); %>
	        <%if(subclasses.size()!=0) {%>
	    		<h4>Subclasses:</h4>
	    	
	    		<%for(OntClassModel sub:subclasses){%>
	        	<ul><li><a href="ontology?classUri=<%=sub.getUri()%>"><%=sub.getId() %></a></li></ul>
	        <%}}%>	    		
<% } else {%>
<p>This class is defined external to this ontology.</p>
<%} %>
</body>
</html>