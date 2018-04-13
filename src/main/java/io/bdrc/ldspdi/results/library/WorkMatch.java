package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;

import io.bdrc.ldspdi.results.Field;


public class WorkMatch {
    
    public String access;
    public String license;
    public String status;
    public String langScript;
    public String prefLabel;
    public ArrayList<Field> matching;
    public ArrayList<String> topics;
    public ArrayList<String> taxonomies;
    
    public WorkMatch() {
        access="";
        license="";
        status="";
        langScript="";
        prefLabel="";
        matching=new ArrayList<>();
        topics=new ArrayList<>();
        taxonomies=new ArrayList<>();
    }

    public void addMatch(Field f) {
        matching.add(f);
    }
    
    public void addTopic(String t) {
        topics.add(t);
    }
        
    public void addTaxonomy(String tax) {
        taxonomies.add(tax);
    }

    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLangScript() {
        return langScript;
    }

    public void setLangScript(String langScript) {
        this.langScript = langScript;
    }

    public ArrayList<Field> getMatching() {
        return matching;
    }

    public void setMatching(ArrayList<Field> matching) {
        this.matching = matching;
    }

    public String getPrefLabel() {
        return prefLabel;
    }

    public void setPrefLabel(String prefLabel) {
        this.prefLabel = prefLabel;
    }
    
}
