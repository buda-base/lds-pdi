package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import io.bdrc.ldspdi.results.Field;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;
import io.bdrc.taxonomy.Taxonomy;


public class RootResults {
    
    public static HashMap<String,Object> getResultsMap(Model mod) throws RestException{
        HashMap<String,Object> res=new HashMap<>();
        HashMap<String,ArrayList<Field>> works=new HashMap<>(); 
        HashMap<String,ArrayList<Field>> people=new HashMap<>();
        HashMap<String,ArrayList<Field>> places=new HashMap<>();
        HashMap<String,ArrayList<Field>> lineages=new HashMap<>();
        HashMap<String,ArrayList<Field>> roles=new HashMap<>();
        HashMap<String,ArrayList<Field>> topics=new HashMap<>();
        HashMap<String,Integer> count=new HashMap<>();
        ArrayList<String> processed=new ArrayList<>();
        StmtIterator it=mod.listStatements();
        while(it.hasNext()) {
            Statement st=it.next();            
            String type=mod.getProperty(st.getSubject(), mod.getProperty(Taxonomy.TYPE)).getObject().asResource().getURI().toString();
            Integer ct=count.get(type);
            if(!processed.contains(st.getSubject().getURI())) {
                if(ct!=null) {
                    count.put(type, ct.intValue()+1);
                }
                else {
                    count.put(type, 1);
                }
            }
            if(!st.getPredicate().getURI().equals(Taxonomy.TYPE)) {
                switch (type) {            
                    case Taxonomy.WORK:                                          
                        ArrayList<Field> wl=works.get(st.getSubject().getURI());                        
                        if(wl==null) {
                            wl=new ArrayList<Field>();
                        }                    
                        wl.add(Field.getField(st)); 
                        works.put(st.getSubject().getURI(),wl);                    
                        break;
                    case Taxonomy.TOPIC:
                        ArrayList<Field> tl=topics.get(st.getSubject().getURI());
                        if(tl==null) {
                            tl=new ArrayList<Field>();
                        }
                        tl.add(Field.getField(st)); 
                        topics.put(st.getSubject().getURI(),tl);
                        break;                        
                    case Taxonomy.LINEAGE:
                        ArrayList<Field> pli=lineages.get(st.getSubject().getURI());
                        if(pli==null) {
                            pli=new ArrayList<Field>();
                        }
                        pli.add(Field.getField(st)); 
                        lineages.put(st.getSubject().getURI(),pli);
                        break;
                    case Taxonomy.PERSON:
                        if(!st.getPredicate().getURI().equals(st.getObject().toString())) {
                            ArrayList<Field> pl=people.get(st.getSubject().getURI());
                            if(pl==null) {
                                pl=new ArrayList<Field>();
                            }
                            pl.add(Field.getField(st)); 
                            people.put(st.getSubject().getURI(),pl);
                        }
                        break;
                    case Taxonomy.ROLE:
                        ArrayList<Field> rl=places.get(st.getSubject().getURI());
                        if(rl==null) {
                            rl=new ArrayList<Field>();
                        }
                        rl.add(Field.getField(st)); 
                        roles.put(st.getSubject().getURI(),rl);
                        break;
                    case Taxonomy.PLACE:
                        ArrayList<Field> pla=places.get(st.getSubject().getURI());
                        if(pla==null) {
                            pla=new ArrayList<Field>();
                        }
                        pla.add(Field.getField(st)); 
                        places.put(st.getSubject().getURI(),pla);
                        break;
                    default:
                        throw new RestException(500,new LdsError(LdsError.UNKNOWN_ERR).
                                setContext(" type in RootResults.getResultsMap(Model mod) >> "+type));
        
                }
                processed.add(st.getSubject().getURI());
            }
        
            
        }
        res.put("metadata",count);
        res.put("works",works);
        res.put("persons",people);
        res.put("lineages",lineages);
        res.put("topics",topics);
        res.put("places",places);
        res.put("roles",roles);
        return res;
    }

}