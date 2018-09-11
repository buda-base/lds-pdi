package io.bdrc.ldspdi.results;

import java.util.List;

import org.apache.jena.datatypes.xsd.impl.RDFLangString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.Util;

public class CsvRow {

    public final static String DEL=",";
    String csvCols="";
    String csv="";

    public CsvRow(List<String> headers,QuerySolution qs, String profile) {
        for(String key:headers) {
            csvCols=csvCols+key+"-value"+DEL;
            csvCols=csvCols+key+"-type"+DEL;
            RDFNode node=qs.get(key);
            if(node !=null) {
                if(node.isResource()) {
                    csv=csv+node.asResource().getURI()+DEL;
                    csv=csv+"uri"+DEL;
                }
                if(node.isLiteral()) {
                    Literal lit=node.asLiteral();
                    if ( Util.isSimpleString(lit) || Util.isLangString(lit) ) {
                        if(node.asNode().getLiteralDatatype().equals(RDFLangString.rdfLangString)) {
                            csvCols=csvCols+key+"-lang"+DEL;
                            csv=csv+node.asLiteral().getValue().toString()+DEL;
                            csv=csv+"literal"+DEL;
                            csv=csv+node.asNode().getLiteralLanguage()+DEL;
                        }else {
                            csv=csv+node.asLiteral().getValue().toString()+DEL;
                            csv=csv+"literal"+DEL;
                        }
                    }else {
                        csvCols=csvCols+key+"-datatype"+DEL;
                        csv=csv+node.asLiteral().getValue().toString()+DEL;
                        csv=csv+"literal"+DEL;
                        csv=csv+node.asLiteral().getDatatypeURI()+DEL;
                    }
                }
                if(node.isAnon()) {
                    csv=csv+node.toString()+DEL;
                    csv=csv+"bnode"+DEL;
                }
            }
        }
        csvCols=csvCols.substring(0, csvCols.length()-1);
        csv=csv.substring(0, csv.length()-1);
    }

    public String getCsvCols() {
        return csvCols;
    }

    public String getCsv() {
        return csv;
    }

    public void setCsv(String csv) {
        this.csv = csv;
    }


}
