package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.results.Field;


public class PersonResults {
    
    public final static Logger log=LoggerFactory.getLogger(PersonResults.class.getName());
    
    static final String LABEL_MATCH="http://purl.bdrc.io/ontology/tmp/prefLabelMatch";
    static final String PREF_LABEL="http://www.w3.org/2004/02/skos/core#prefLabel";
    static final String RELATION_TYPE="http://purl.bdrc.io/ontology/tmp/relationType";
    static final String WORK_ABOUT="http://purl.bdrc.io/ontology/core/workIsAbout";
    static final String PERSONGENDER="http://purl.bdrc.io/ontology/core/personGender";    
    
       
    public static HashMap<String,Object> getResultsMap(Model mod){
        HashMap<String,Object> res=new HashMap<>();
        HashMap<String,ArrayList<Field>> people=new HashMap<>();
        HashMap<String,Integer> count=new HashMap<>();
        StmtIterator it=mod.listStatements();
        while(it.hasNext()) {
            Statement st=it.next();            
            if(!st.getPredicate().getURI().equals(st.getObject().toString())) {
                if(st.getPredicate().getURI().equals(PERSONGENDER)) {
                    String gender=st.getObject().asNode().getURI();
                    Integer ct=count.get(gender);
                    if(ct!=null) {
                        count.put(gender, ct.intValue()+1);
                    }
                    else {
                        count.put(gender, 1);
                    }
                }
                ArrayList<Field> pl=people.get(st.getSubject().getURI());
                if(pl==null) {
                    pl=new ArrayList<Field>();
                }
                pl.add(Field.getField(st)); 
                people.put(st.getSubject().getURI(),pl);
            }
            
        }
        res.put("metadata",count);
        res.put("persons",people);
        return res;
    }

}
