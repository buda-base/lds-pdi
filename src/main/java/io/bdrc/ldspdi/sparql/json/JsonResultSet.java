package io.bdrc.ldspdi.sparql.json;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;

public class JsonResultSet implements Serializable{
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private ArrayList<ArrayList<AbstractMap.SimpleEntry<String,String>>> data;

    public JsonResultSet(ResultSet rs) {
        
        data=new ArrayList<>();
        List<String> l=rs.getResultVars();        
        while(rs.hasNext()) {
            ArrayList<AbstractMap.SimpleEntry<String,String>> rows=new ArrayList<>();
            QuerySolution qs=rs.next();            
            for(String str:l) {
                RDFNode node=qs.get(str);
                if(node !=null) {
                    if(node.isResource()) {
                        AbstractMap.SimpleEntry<String,String> ent=new AbstractMap.SimpleEntry<>(str,qs.get(str).asNode().getLocalName());
                        rows.add(ent);
                    }
                    else if(node.isLiteral()) {                        
                        AbstractMap.SimpleEntry<String,String> ent=new AbstractMap.SimpleEntry<>(str,qs.get(str).asLiteral().toString());
                        rows.add(ent);
                    }
                }
            }
            data.add(rows);
        }
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public ArrayList<ArrayList<AbstractMap.SimpleEntry<String, String>>> getData() {
        return data;
    }

}
