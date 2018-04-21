package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import io.bdrc.ldspdi.results.Field;
import io.bdrc.ldspdi.results.LiteralStringField;

public class ResourceResults {
    
    static final String RELATION_TYPE="http://purl.bdrc.io/ontology/tmp/relationType";    
      
    public static HashMap<String,Object> getResultsMap(Model mod){
        HashMap<String,Object> res=new HashMap<>();
        HashMap<String,ArrayList<Field>> resources=new HashMap<>(); 
        StmtIterator it=mod.listStatements();
        while(it.hasNext()) {
            Statement st=it.next();
            String uri=st.getSubject().getURI();
            ArrayList<Field> f=resources.get(uri);
            if(f==null) {
                f=new ArrayList<Field>();
            }
            System.out.println("STAT >> "+st+" IsLiteral >> "+st.getObject().isLiteral());
            if(st.getObject().isLiteral()) {
                f.add(new LiteralStringField(st.getPredicate().getURI(),st.getObject().asLiteral().getLanguage(),st.getObject().asLiteral().getValue().toString()));  
            }else {
                f.add(new Field(st.getPredicate().getURI(),st.getObject().toString()));
            }
            resources.put(uri,f);            
        }
        res.put("data",resources);
        return res;
    }
        

}
