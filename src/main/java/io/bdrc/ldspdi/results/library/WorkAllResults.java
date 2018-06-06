package io.bdrc.ldspdi.results.library;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.bdrc.formatters.JSONLDFormatter;
import io.bdrc.ldspdi.results.Field;
import io.bdrc.restapi.exceptions.RestException;
import io.bdrc.taxonomy.Taxonomy;

public class WorkAllResults {
    
    public final static String TYPE="http://www.w3.org/1999/02/22-rdf-syntax-ns#type";    
    public final static String WORK="http://purl.bdrc.io/ontology/core/Work";    
    public final static String LINEAGE="http://purl.bdrc.io/ontology/core/Lineage";
    public final static String WORK_GENRE="http://purl.bdrc.io/ontology/core/workGenre";
    public final static String WORK_IS_ABOUT="http://purl.bdrc.io/ontology/core/workIsAbout";
    
    public static HashMap<String,Object> getResultsMap(Model mod) throws RestException{
        HashMap<String,Object> res=new HashMap<>();
        HashMap<String,ArrayList<Field>> works=new HashMap<>(); 
        HashMap<String,ArrayList<Field>> lineages=new HashMap<>();
        HashMap<String,Integer> topics=new HashMap<>();
        HashMap<String,HashSet<String>> Wtopics=new HashMap<>();
        HashMap<String,HashSet<String>> WorkBranch=new HashMap<>();
        HashSet<String> tops=new HashSet<>(); 
        StmtIterator iter=mod.listStatements();
        while(iter.hasNext()) {
            Statement st=iter.next();
            String type=mod.getProperty(st.getSubject(), mod.getProperty(TYPE)).getObject().asResource().getURI().toString();
            switch (type) {
                case WORK:
                    ArrayList<Field> wl=works.get(st.getSubject().getURI());
                    if(wl==null) {
                        wl=new ArrayList<Field>();
                    }
                    if(st.getPredicate().getURI().equals(WORK_GENRE) || st.getPredicate().getURI().equals(WORK_IS_ABOUT)) {                        
                        tops.add(st.getObject().asNode().getURI());
                        HashSet<String> tmp=Wtopics.get(st.getObject().asNode().getURI());
                        if (tmp==null) {
                            tmp=new HashSet<>();
                        }
                        tmp.add(st.getSubject().asNode().getURI());
                        Wtopics.put(st.getObject().asNode().getURI(), tmp);                                     
                        Integer t=topics.get(st.getObject().asNode().getURI());                
                        LinkedList<String> nodes=Taxonomy.getRootToLeafPath(st.getObject().asNode().getURI());
                        if(!nodes.isEmpty()) {
                            nodes.removeFirst();
                            nodes.removeLast();
                        }
                        for(String s:nodes) {
                            HashSet<String> bt=WorkBranch.get(s);
                            if (bt==null) {
                                bt=new HashSet<>();
                            }
                            bt.add(st.getSubject().asNode().getURI());
                            WorkBranch.put(s, bt);
                            topics.put(s, bt.size());
                        }
                        topics.put(st.getObject().asNode().getURI(), Wtopics.get(st.getObject().asNode().getURI()).size());
                    }
                    wl.add(Field.getField(st)); 
                    works.put(st.getSubject().getURI(),wl);
                    break;
                case LINEAGE:
                    ArrayList<Field> pli=lineages.get(st.getSubject().getURI());
                    if(pli==null) {
                        pli=new ArrayList<Field>();
                    }
                    pli.add(Field.getField(st));                    
                    lineages.put(st.getSubject().getURI(),pli);
                    break;
                default:
                    throw new RestException(500,RestException.GENERIC_APP_ERROR_CODE,"Unknown type in PersonAllResults >> "+type);
            
            }
        }
        JsonNode nn=null;
        if(tops.size()>0) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                Graph g=Taxonomy.getPartialLDTreeTriples(Taxonomy.ROOT, tops,topics);
                ByteArrayOutputStream baos=new ByteArrayOutputStream();        
                JSONLDFormatter.writeModelAsCompact(ModelFactory.createModelForGraph(g),baos);
                nn=mapper.readTree(baos.toString());
                baos.close();
            } catch (IOException ex) {
                throw new RestException(500,RestException.GENERIC_APP_ERROR_CODE,"WorkResults was unable to write Taxonomy Tree : \""+ex.getMessage()+"\"");              
            }
        }
        res.put("associatedWorks",works);
        res.put("associatedLineages",lineages);
        res.put("tree",nn);
        return res;
    }

}
