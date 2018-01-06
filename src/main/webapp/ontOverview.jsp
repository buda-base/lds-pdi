<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="io.bdrc.ontology.service.core.*"%>
<%@page import="java.util.List"%>
<%
List<OntClassModel> rootClasses=OntAccess.getOntRootClasses();
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Ontology - <%=OntAccess.getName() %></title>
</head>
<body>
<h2>Ontology - <%=OntAccess.getName() %></h2>
<p>Retrieved from <a href="<%=OntAccess.getOwlURL() %>"><%=OntAccess.getOwlURL()+".html"%></a></p>
<p>Defines <%=OntAccess.getNumPrefixes() %> prefixes,
           <%=OntAccess.getNumClasses() %> Classes,
           <%=OntAccess.getNumObjectProperties()%> ObjectProperties,
           <%=OntAccess.getNumDatatypeProperties()%> DatatypeProperties,
           <%=OntAccess.getNumAnnotationProperties()%> AnnotationProperties
</p>
<p>There are <%=OntAccess.getNumRootClasses()%> simple root OntClass(es):</p>
<ul>
          <% for(OntClassModel root:rootClasses){ %>
            <li><a href="ontology?classUri=<%=root.getUri()%>"><%=root.getId()%></a></li>
          <%} %>
        </ul>
</body>
</html>