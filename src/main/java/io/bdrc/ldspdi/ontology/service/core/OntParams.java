package io.bdrc.ldspdi.ontology.service.core;

public class OntParams {
	
	public String name;
	public String fileurl;
	public String type;
	public String endpoint;
	public String graph;
	public String baseuri;
	
	public OntParams() {
		super();
		// TODO Auto-generated constructor stub
	}

	public OntParams(String name, String fileurl, String type, String endpoint, String graph, String baseuri) {
		super();
		this.name = name;
		this.fileurl = fileurl;
		this.type = type;
		this.endpoint = endpoint;
		this.graph = graph;
		this.baseuri = baseuri;
	}
	
	public String getBaseuri() {
		return baseuri;
	}
	public void setBaseuri(String baseuri) {
		this.baseuri = baseuri;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getFileurl() {
		return fileurl;
	}
	public void setFileurl(String fileurl) {
		this.fileurl = fileurl;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getEndpoint() {
		return endpoint;
	}
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
	public String getGraph() {
		return graph;
	}
	public void setGraph(String graph) {
		this.graph = graph;
	}

	@Override
	public String toString() {
		return "OntParams [name=" + name + ", fileurl=" + fileurl + ", type=" + type + ", endpoint=" + endpoint
				+ ", graph=" + graph + ", baseuri=" + baseuri + "]";
	}

}
