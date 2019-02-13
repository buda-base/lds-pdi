package io.bdrc.ldspdi.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.bdrc.ldspdi.ontology.service.core.OntParams;

public class Config {
	
	public List<OntParams> ontologies;
	public Map<String,OntParams> map=new HashMap<>();

	public List<OntParams> getOntologies() {
		return ontologies;
	}

	public void setOntologies(List<OntParams> ontologies) {
		this.ontologies = ontologies;
	}
	
	private Map<String,OntParams> getOntologiesMap(){
		if(map.isEmpty()) {			
			for(OntParams pr:ontologies) {
				map.put(pr.getName(), pr);
			}
		}
		return map;
	}
	
	public OntParams getOntology(String name){
		if(map.isEmpty()) {			
			map=getOntologiesMap();
		}
		return map.get(name);
	}
}
