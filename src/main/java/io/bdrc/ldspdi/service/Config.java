package io.bdrc.ldspdi.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.bdrc.ldspdi.ontology.service.core.OntParams;

public class Config {

    public List<OntParams> ontologies;
    public Map<String, OntParams> map = new HashMap<>();
    public Map<String, OntParams> mapByBase = new HashMap<>();

    public List<OntParams> getOntologies() {
        return ontologies;
    }

    public void setOntologies(List<OntParams> ontologies) {
        this.ontologies = ontologies;
    }

    private Map<String, OntParams> getOntologiesMap() {
        if (map.isEmpty()) {
            for (OntParams pr : ontologies) {
                map.put(pr.getName(), pr);
            }
        }
        return map;
    }

    private Map<String, OntParams> getOntologiesMapByBase() {
        if (mapByBase.isEmpty()) {
            for (OntParams pr : ontologies) {
                mapByBase.put(pr.getBaseuri(), pr);
            }
        }
        return mapByBase;
    }

    public OntParams getOntology(String name) {
        if (map.isEmpty()) {
            map = getOntologiesMap();
        }
        return map.get(name);
    }

    public OntParams getOntologyByBase(String name) {
        if (mapByBase.isEmpty()) {
            mapByBase = getOntologiesMapByBase();
        }
        return mapByBase.get(name);
    }

    public boolean isBaseUri(String uri) {
        if (mapByBase.isEmpty()) {
            mapByBase = getOntologiesMapByBase();
        }
        return mapByBase.keySet().contains(uri);
    }

    public ArrayList<String> getValidBaseUri() {
        if (mapByBase.isEmpty()) {
            mapByBase = getOntologiesMapByBase();
        }
        ArrayList<String> valid = new ArrayList<>();
        Set<String> bases = mapByBase.keySet();
        for (String s : bases) {
            OntParams p = getOntologyByBase(s);
            if (!p.getName().endsWith("test") && p.getEndpoint().length() > 0) {
                valid.add(s.substring(0, s.length() - 1));
            }
        }
        return valid;
    }

    public ArrayList<String> getValidNames() {
        if (map.isEmpty()) {
            map = getOntologiesMap();
        }
        ArrayList<String> valid = new ArrayList<>();
        Set<String> names = map.keySet();
        for (String s : names) {
            if (!s.endsWith("test")) {
                valid.add(s);
            }
        }
        return valid;
    }

}
