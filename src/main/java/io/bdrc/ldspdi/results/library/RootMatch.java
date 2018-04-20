package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;

import io.bdrc.ldspdi.results.Field;

public class RootMatch {
    
    public String type;
    public ArrayList<Field> prefLabel;
    public ArrayList<Field> matching;
    
    public RootMatch() {
        type="";
        matching=new ArrayList<>();
        prefLabel=new ArrayList<>();
    }
    
    public void addPrefLabel(Field f) {
        prefLabel.add(f);
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
