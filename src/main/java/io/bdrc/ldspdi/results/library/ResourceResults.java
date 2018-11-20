package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import io.bdrc.ldspdi.results.Field;


public class ResourceResults {


    public static HashMap<String,Object> getResultsMap(Model mod){
        HashMap<String,Object> res=new HashMap<>();
        HashMap<String,ArrayList<Field>> resources=new HashMap<>();
        StmtIterator it=mod.listStatements();
        while(it.hasNext()) {
            Statement st=it.next();
            String uri=null;
            if(st.getSubject().isAnon()) {
                uri="_:"+st.getSubject().asNode().getBlankNodeLabel();
            }else {
                uri=st.getSubject().getURI();
            }
            ArrayList<Field> f=resources.get(uri);
            if(f==null) {
                f=new ArrayList<Field>();
            }
            f.add(Field.getField(st));
            resources.put(uri,f);
        }
        res.put("data",resources);
        return res;
    }


}
