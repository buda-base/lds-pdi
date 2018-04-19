package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;

import io.bdrc.ldspdi.results.Field;

public class ResourceDesc {
    
    public ArrayList<Field> matching;
    
    public ResourceDesc() {        
        matching=new ArrayList<>();        
    }
    
    public void addMatch(Field f) {
        matching.add(f);
    }

}
