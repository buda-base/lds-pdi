package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import io.bdrc.ldspdi.results.Field;
import io.bdrc.ldspdi.results.LiteralStringField;
import io.bdrc.restapi.exceptions.RestException;

public class PersonAllResults {
    
    public final static String TYPE="http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    public final static String PERSON="http://purl.bdrc.io/ontology/core/Person";
    public final static String WORK="http://purl.bdrc.io/ontology/core/Work";
    public final static String PLACE="http://purl.bdrc.io/ontology/core/Place";
    public final static String LINEAGE="http://purl.bdrc.io/ontology/core/Lineage";
    
    public static HashMap<String,Object> getResultsMap(Model mod) throws RestException{
        HashMap<String,ArrayList<Field>> works=new HashMap<>(); 
        HashMap<String,ArrayList<Field>> people=new HashMap<>();
        HashMap<String,ArrayList<Field>> places=new HashMap<>();
        HashMap<String,ArrayList<Field>> lineages=new HashMap<>();
        HashMap<String,Object> res=new HashMap<>();
        StmtIterator it=mod.listStatements();
        while(it.hasNext()) {
            Statement st=it.next();
            String type=mod.getProperty(st.getSubject(), mod.getProperty(TYPE)).getObject().asResource().getURI().toString();
                        
            switch (type) {
                case WORK:
                    ArrayList<Field> wl=works.get(st.getSubject().getURI());
                    if(wl==null) {
                        wl=new ArrayList<Field>();
                    }
                    wl.add(Field.getField(st)); 
                    works.put(st.getSubject().getURI(),wl);
                    break;
                case PLACE:
                    ArrayList<Field> pla=places.get(st.getSubject().getURI());
                    if(pla==null) {
                        pla=new ArrayList<Field>();
                    }
                    pla.add(Field.getField(st)); 
                    places.put(st.getSubject().getURI(),pla);
                    break;
                case LINEAGE:
                    ArrayList<Field> pli=lineages.get(st.getSubject().getURI());
                    if(pli==null) {
                        pli=new ArrayList<Field>();
                    }
                    pli.add(Field.getField(st));                    
                    lineages.put(st.getSubject().getURI(),pli);
                    break;
                case PERSON:
                    if(!st.getPredicate().getURI().equals(st.getObject().toString())) {
                        ArrayList<Field> pl=people.get(st.getSubject().getURI());
                        if(pl==null) {
                            pl=new ArrayList<Field>();
                        }
                        pl.add(Field.getField(st)); 
                        people.put(st.getSubject().getURI(),pl);
                    }
                    break;
                default:
                    throw new RestException(500,RestException.GENERIC_APP_ERROR_CODE,"Unknown type in PersonAllResults >> "+type);
            
            }
        }
        res.put("associatedWorks",works);
        res.put("associatedPeople",people);
        res.put("associatedPlaces",places);
        res.put("associatedLineages",lineages);
        return res;
        
    }

}
