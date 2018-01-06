package io.bdrc.ldspdi.service;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.bdrc.ontology.service.core.OntClassModel;

public class OntologyController extends HttpServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		String uri = request.getParameter("classUri");
		//RequestDispatcher rd=request.getRequestDispatcher("index.jsp");
		request.setAttribute("model", new OntClassModel(uri));
        RequestDispatcher rd=request.getRequestDispatcher("ontClass.jsp");
        rd.forward(request,response);        
	}

}
