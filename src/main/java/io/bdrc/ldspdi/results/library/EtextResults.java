package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.results.Field;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;
import io.bdrc.taxonomy.Taxonomy;

public class EtextResults {
    
    public final static Logger log=LoggerFactory.getLogger(EtextResults.class.getName());
    
    public static HashMap<String,Object> getResultsMap(Model mod) throws RestException{
        HashMap<String,Object> res=new HashMap<>();
        HashMap<String,ArrayList<Field>> etexts=new HashMap<>();  
        HashMap<String,ArrayList<Field>> chunks=new HashMap<>();
        HashMap<String,Integer> topics=new HashMap<>();
        HashMap<String,HashSet<String>> Wtopics=new HashMap<>();
        HashMap<String,HashSet<String>> WorkBranch=new HashMap<>();
        HashSet<String> tops=new HashSet<>(); 
        HashSet<String> authors=new HashSet<>();
        StmtIterator iter=mod.listStatements();
        while(iter.hasNext()) {
            Statement st=iter.next();
            
            String type=mod.getProperty(st.getSubject(), mod.getProperty(Taxonomy.TYPE)).getObject().asResource().getURI().toString();
            switch (type) {
                case Taxonomy.ETEXT:
                    ArrayList<Field> etextl=etexts.get(st.getSubject().getURI());
                    if(etextl==null) {
                        etextl=new ArrayList<Field>();
                    }
                    String pred_uri=st.getPredicate().getURI();
                    if(pred_uri.equals(Taxonomy.WORK_GENRE) || pred_uri.equals(Taxonomy.WORK_IS_ABOUT)) {                        
                        Taxonomy.processTopicStatement(st, tops, Wtopics, WorkBranch, topics);
                    }
                    if(pred_uri.equals(Taxonomy.WORK_MAIN_AUTHOR)) {                        
                        authors.add(st.getObject().asResource().getURI());
                    }
                    etextl.add(Field.getField(st));
                    etexts.put(st.getSubject().getURI(),etextl);
                    break;
                case Taxonomy.ETEXT_CHUNK:
                    ArrayList<Field> chunksl=chunks.get(st.getSubject().toString());
                    if(chunksl==null) {
                        chunksl=new ArrayList<Field>();
                    }
                    chunksl.add(Field.getField(st));
                    chunks.put(st.getSubject().toString(),chunksl);
                    break;
                default:
                    throw new RestException(500,new LdsError(LdsError.UNKNOWN_ERR).
                            setContext(" type in WorkAllResults.getResultsMap(Model mod) >> "+type));
               }
        }
        res.put("chunks",chunks);
        res.put("authors",authors); 
        res.put("etexts",etexts);        
        res.put("tree",Taxonomy.buildFacetTree(tops, topics));
        return res;
    }

}
