package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;

import io.bdrc.ldspdi.results.Field;
import io.bdrc.ldspdi.results.LiteralStringField;

public class ResourceMatch {
    
    public String type;
    public LiteralStringField prefLabel;
    public ArrayList<Field> matching;
    
    public ResourceMatch() {
        type="";
        matching=new ArrayList<>();
    }
    
    public void addMatch(Field f) {
        matching.add(f);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LiteralStringField getPrefLabel() {
        return prefLabel;
    }

    public void setPrefLabel(LiteralStringField prefLabel) {
        this.prefLabel = prefLabel;
    }

    public ArrayList<Field> getMatching() {
        return matching;
    }

    public void setMatching(ArrayList<Field> matching) {
        this.matching = matching;
    }

    @Override
    public String toString() {
        return "ResourceMatch [type=" + type + ", prefLabel=" + prefLabel + ", matching=" + matching + "]";
    }
    
    

}
