package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;

import io.bdrc.ldspdi.results.Field;

public class PersonMatch {
    
    public String gender;
    public ArrayList<Field> matching;
    public ArrayList<Field> options;
    
    public PersonMatch() {
        gender="";
        matching=new ArrayList<>();
        options=new ArrayList<>();
    }
    
    public void addMatch(Field f) {
        matching.add(f);
    }
    
    public void addOptions(Field f) {
        options.add(f);
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public ArrayList<Field> getMatching() {
        return matching;
    }

    public void setMatching(ArrayList<Field> matching) {
        this.matching = matching;
    }

    @Override
    public String toString() {
        return "PersonMatch [gender=" + gender + ", matching=" + matching + "]";
    }

    
    
}
