package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import io.bdrc.ldspdi.results.Field;
import io.bdrc.ldspdi.results.LiteralStringField;


public class RootResults {
    
    static final String PREFLABEL="http://www.w3.org/2004/02/skos/core#prefLabel"; 
    
    public final static String TYPE="http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    public final static String PERSON="http://purl.bdrc.io/ontology/core/Person";
    public final static String WORK="http://purl.bdrc.io/ontology/core/Work";
    public final static String PLACE="http://purl.bdrc.io/ontology/core/Place";
    public final static String LINEAGE="http://purl.bdrc.io/ontology/core/Lineage";
    public final static String TOPIC="http://purl.bdrc.io/ontology/core/Topic";
    public final static String ROLE="http://purl.bdrc.io/ontology/core/Role";
    
    
    public static HashMap<String,Object> getResultsMap(Model mod){
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
            String type=mod.getProperty(st.getSubject(), mod.getProperty(TYPE)).getObject().asResource().getURI().toString();
            Integer ct=count.get(type);
            if(!processed.contains(st.getSubject().getURI())) {
                if(ct!=null) {
                    count.put(type, ct.intValue()+1);
                }
                else {
                    count.put(type, 1);
                }
            }
            if(!st.getPredicate().getURI().equals(TYPE)) {
                switch (type) {            
                    case WORK:                                          
                        ArrayList<Field> wl=works.get(st.getSubject().getURI());                        
                        if(wl==null) {
                            wl=new ArrayList<Field>();
                        }                    
                        if(st.getObject().isLiteral()) {
                            wl.add(new LiteralStringField(st.getPredicate().getURI(),st.getObject().asLiteral().getLanguage(),st.getObject().asLiteral().getValue().toString()));  
                        }else {
                            wl.add(new Field(st.getPredicate().getURI(),st.getObject().toString()));
                        }
                        works.put(st.getSubject().getURI(),wl);                    
                        break;
                    case TOPIC:
                        ArrayList<Field> tl=topics.get(st.getSubject().getURI());
                        if(tl==null) {
                            tl=new ArrayList<Field>();
                        }
                        if(st.getObject().isLiteral()) {
                            tl.add(new LiteralStringField(st.getPredicate().getURI(),st.getObject().asLiteral().getLanguage(),st.getObject().asLiteral().getValue().toString()));  
                        }else {
                            tl.add(new Field(st.getPredicate().getURI(),st.getObject().toString()));
                        }
                        topics.put(st.getSubject().getURI(),tl);
                        break;                        
                    case LINEAGE:
                        ArrayList<Field> pli=lineages.get(st.getSubject().getURI());
                        if(pli==null) {
                            pli=new ArrayList<Field>();
                        }
                        if(st.getObject().isLiteral()) {
                            pli.add(new LiteralStringField(st.getPredicate().getURI(),st.getObject().asLiteral().getLanguage(),st.getObject().asLiteral().getValue().toString()));  
                        }else {
                            pli.add(new Field(st.getPredicate().getURI(),st.getObject().toString()));
                        }
                        lineages.put(st.getSubject().getURI(),pli);
                        break;
                    case PERSON:
                        if(!st.getPredicate().getURI().equals(st.getObject().toString())) {
                            ArrayList<Field> pl=people.get(st.getSubject().getURI());
                            if(pl==null) {
                                pl=new ArrayList<Field>();
                            }
                            if(st.getObject().isLiteral()) {
                                pl.add(new LiteralStringField(st.getPredicate().getURI(),st.getObject().asLiteral().getLanguage(),st.getObject().asLiteral().getValue().toString()));  
                            }else {
                                pl.add(new Field(st.getPredicate().getURI(),st.getObject().toString()));
                            }
                            people.put(st.getSubject().getURI(),pl);
                        }
                        break;
                    case ROLE:
                        ArrayList<Field> rl=places.get(st.getSubject().getURI());
                        if(rl==null) {
                            rl=new ArrayList<Field>();
                        }
                        if(st.getObject().isLiteral()) {
                            rl.add(new LiteralStringField(st.getPredicate().getURI(),st.getObject().asLiteral().getLanguage(),st.getObject().asLiteral().getValue().toString()));  
                        }else {
                            rl.add(new Field(st.getPredicate().getURI(),st.getObject().toString()));
                        }
                        roles.put(st.getSubject().getURI(),rl);
                        break;
                    case PLACE:
                        ArrayList<Field> pla=places.get(st.getSubject().getURI());
                        if(pla==null) {
                            pla=new ArrayList<Field>();
                        }
                        if(st.getObject().isLiteral()) {
                            pla.add(new LiteralStringField(st.getPredicate().getURI(),st.getObject().asLiteral().getLanguage(),st.getObject().asLiteral().getValue().toString()));  
                        }else {
                            pla.add(new Field(st.getPredicate().getURI(),st.getObject().toString()));
                        }
                        places.put(st.getSubject().getURI(),pla);
                        break;
                        
                }
                processed.add(st.getSubject().getURI());
            }
        
            
        }
        res.put("metadata",count);
        res.put("works",works);
        res.put("people",people);
        res.put("lineages",lineages);
        res.put("topics",topics);
        res.put("places",places);
        res.put("roles",roles);
        return res;
    }

}
