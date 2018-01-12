<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="io.bdrc.ontology.service.core.*"%>
<%@page import="io.bdrc.ldspdi.composer.*"%>
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
		<%if (!OntAccess.isRootClass(model.getUri())) {%>
		<h4>&#9658;Object properties:</h4>
	    <%ArrayList<ClassProperty> object_Props= OntAccess.listObjectProps(model.getUri()) ; 
	    if(object_Props.size()!=0){
	      for(ClassProperty object_prop :object_Props){%>
		<div style="white-space: pre-wrap;"><b><%=object_prop.getName()%>:</b>  &#9675; Label: <i><%=object_prop.getLabel()%></i>  &#9675; Range: <i><%=object_prop.getRange()%></i></div>
		<%}}else { %><p>No object properties found.</p>
		<%} %>
		<h4>&#9658;Datatype properties:</h4>
	    <%ArrayList<ClassProperty> datatype_Props= OntAccess.listDataTypeProps(model.getUri()) ; 
	    if(datatype_Props.size()!=0){
	      for(ClassProperty datatype_prop :datatype_Props){%>
		<div style="white-space: pre-wrap;"><b><%=datatype_prop.getName()%>:</b>  &#9675; Label: <i><%=datatype_prop.getLabel()%></i>  &#9675; Range: <i><%=datatype_prop.getRange()%></i></div>
		<%}}else { %><p>No datatype properties found.</p>
		<%} %>
		<h4>&#9658;Annotation properties:</h4>
	    <%ArrayList<ClassProperty> annot_Props= OntAccess.listAnnotationProps(model.getUri()) ; 
	    if(annot_Props.size()!=0){
	      for(ClassProperty annot_Prop :annot_Props){%>
		<div style="white-space: pre-wrap;"><b><%=annot_Prop.getName()%>:</b>  &#9675; Label: <i><%=annot_Prop.getLabel()%></i>  &#9675; Range: <i><%=annot_Prop.getRange()%></i></div>
		<%}}else { %><p>No annotation properties found.</p>
		<%} %>
		<h4>&#9658;Symmetric properties:</h4>
	    <%ArrayList<ClassProperty> sym_Props= OntAccess.listSymmetricProps(model.getUri()) ; 
	    if(sym_Props.size()!=0){
	      for(ClassProperty sym_Prop :sym_Props){%>
		<div style="white-space: pre-wrap;"><b><%=sym_Prop.getName()%>:</b>  &#9675; Label: <i><%=sym_Prop.getLabel()%></i>  &#9675; Range: <i><%=sym_Prop.getRange()%></i></div>
		<%}}else { %><p>No symmetric properties found.</p>
		<%} %>
		<h4>&#9658;Irreflexive properties:</h4>
	    <%ArrayList<ClassProperty> irref_Props= OntAccess.listIrreflexiveProps(model.getUri()) ; 
	    if(irref_Props.size()!=0){
	      for(ClassProperty irref_Prop :irref_Props){%>
		<div style="white-space: pre-wrap;"><b><%=irref_Prop.getName()%>:</b>  &#9675; Label: <i><%=irref_Prop.getLabel()%></i>  &#9675; Range: <i><%=irref_Prop.getRange()%></i></div>
		<%}}else { %><p>No irreflexive properties found.</p>
		<%} %>
		<h4>&#9658;Functional properties:</h4>
	    <%ArrayList<ClassProperty> funct_Props= OntAccess.listFunctionalProps(model.getUri()) ; 
	    if(funct_Props.size()!=0){
	      for(ClassProperty funct_Prop :funct_Props){%>
		<div style="white-space: pre-wrap;"><b><%=funct_Prop.getName()%>:</b>  &#9675; Label: <i><%=funct_Prop.getLabel()%></i>  &#9675; Range: <i><%=funct_Prop.getRange()%></i></div>
		<%}}else { %><p>No functional properties found.</p>
		<%} %>
		<h4>&#9658;Class properties:</h4>
	    <%ArrayList<ClassProperty> class_Props= OntAccess.listClassProps(model.getUri()) ; 
	    if(class_Props.size()!=0){
	      for(ClassProperty class_Prop :class_Props){%>
		<div style="white-space: pre-wrap;"><b><%=class_Prop.getName()%>:</b>  &#9675; Label: <i><%=class_Prop.getLabel()%></i>  &#9675; Range: <i><%=class_Prop.getRange()%></i></div>
		<%}}else { %><p>No class properties found.</p>
		<%}} %>
		<% List<StmtModel> stmts =model.getOtherProperties(); %>
	            <h4>&#9658;Other Properties:</h4>
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