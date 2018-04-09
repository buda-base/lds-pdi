package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;

import io.bdrc.ldspdi.results.Field;
import io.bdrc.ldspdi.results.LiteralStringField;

public class RootMatch {
    
    public String type;
    public LiteralStringField label;
    public ArrayList<Field> matching;
    
    public RootMatch() {
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

    public LiteralStringField getLabel() {
        return label;
    }

    public void setLabel(LiteralStringField label) {
        this.label = label;
    }

    public ArrayList<Field> getMatching() {
        return matching;
    }

    public void setMatching(ArrayList<Field> matching) {
        this.matching = matching;
    }

    @Override
    public String toString() {
        return "ResourceMatch [type=" + type + ", prefLabel=" + label + ", matching=" + matching + "]";
    }
    
    

}
