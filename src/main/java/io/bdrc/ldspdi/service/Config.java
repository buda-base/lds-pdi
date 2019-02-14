package io.bdrc.ldspdi.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.bdrc.ldspdi.ontology.service.core.OntParams;

public class Config {
	
	public List<OntParams> ontologies;
	public Map<String,OntParams> map=new HashMap<>();
	public Map<String,OntParams> mapByBase=new HashMap<>();

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
	
	private Map<String,OntParams> getOntologiesMapByBase(){
		if(mapByBase.isEmpty()) {			
			for(OntParams pr:ontologies) {
				mapByBase.put(pr.getBaseuri(), pr);
			}
		}		
		return mapByBase;
	}
	
	public OntParams getOntology(String name){
		if(map.isEmpty()) {			
			map=getOntologiesMap();
		}
		return map.get(name);
	}
	
	public OntParams getOntologyByBase(String name){
		if(mapByBase.isEmpty()) {			
			mapByBase=getOntologiesMapByBase();
		}		
		return mapByBase.get(name);
	}
	
	public boolean isBaseUri(String uri) {	
		if(mapByBase.isEmpty()) {			
			mapByBase=getOntologiesMapByBase();
		}	
		return mapByBase.keySet().contains(uri);
	}
}
