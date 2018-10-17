package io.bdrc.ldspdi.results.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import io.bdrc.ldspdi.results.Field;
import io.bdrc.restapi.exceptions.LdsError;
import io.bdrc.restapi.exceptions.RestException;
import io.bdrc.taxonomy.Taxonomy;

public class ChunksResults {

    public static final String CHUNK_ABOUT="http://purl.bdrc.io/ontology/tmp/etextAbout";
    public static final String CHUNK_GENRE="http://purl.bdrc.io/ontology/tmp/etextGenre";

    public static HashMap<String,Object> getResultsMap(Model mod) throws RestException{
        HashMap<String,Object> res=new HashMap<>();
        HashMap<String,ArrayList<Field>> chunks=new HashMap<>();
        HashMap<String,Integer> topics=new HashMap<>();
        HashMap<String,HashSet<String>> Wtopics=new HashMap<>();
        HashMap<String,HashSet<String>> WorkBranch=new HashMap<>();
        HashSet<String> tops=new HashSet<>();
        HashSet<String> authors=new HashSet<>();
        StmtIterator iter=mod.listStatements();
        while(iter.hasNext()) {
            Statement st=iter.next();

            String type=mod.getProperty(st.getSubject(), RDF.type).getObject().asResource().getURI();
            switch (type) {
            case Taxonomy.ETEXT_CHUNK:
                ArrayList<Field> chunksl=chunks.get(st.getSubject().toString());
                String pred_uri=st.getPredicate().getURI();
                if(pred_uri.equals(CHUNK_ABOUT) || pred_uri.equals(CHUNK_GENRE)) {
                    Taxonomy.processTopicStatement(st, tops, Wtopics, WorkBranch, topics);
                }
                if(pred_uri.equals(Taxonomy.WORK_MAIN_AUTHOR)) {
                    authors.add(st.getObject().asResource().getURI());
                }
                if(chunksl==null) {
                    chunksl=new ArrayList<Field>();
                }
                chunksl.add(Field.getField(st));
                chunks.put(st.getSubject().toString(),chunksl);
                break;
            default:
                throw new RestException(500,new LdsError(LdsError.UNKNOWN_ERR).
                        setContext("unknown type in WorkAllResults.getResultsMap(Model mod) >> "+type));
            }
        }
        res.put("authors",authors);
        res.put("chunks",chunks);
        res.put("tree",Taxonomy.buildFacetTree(tops, topics));
        return res;
    }

}
