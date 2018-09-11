package io.bdrc.ldspdi.results;

import java.util.List;

import org.apache.jena.datatypes.xsd.impl.RDFLangString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.Util;

import io.bdrc.ldspdi.sparql.Prefixes;

public class CsvRow {

    public final static String DEL=",";
    String csvCols="";
    String csv="";
    String s_csvCols="";
    String s_csv="";

    public CsvRow(List<String> headers,QuerySolution qs) {

        for(String key:headers) {
            s_csvCols=s_csvCols+key+DEL;
            csvCols=csvCols+key+"-value"+DEL;
            csvCols=csvCols+key+"-type"+DEL;
            RDFNode node=qs.get(key);
            if(node !=null) {
                if(node.isResource()) {
                    csv=csv+node.asResource().getURI()+DEL;
                    csv=csv+"uri"+DEL;
                    s_csv=s_csv+getPrefix(node.asResource().getURI())+node.asResource().getLocalName()+DEL;
                }
                if(node.isLiteral()) {
                    Literal lit=node.asLiteral();
                    if ( Util.isSimpleString(lit) || Util.isLangString(lit) ) {
                        if(node.asNode().getLiteralDatatype().equals(RDFLangString.rdfLangString)) {
                            s_csv=s_csv+node.asLiteral().getValue().toString()+DEL;
                            csvCols=csvCols+key+"-lang"+DEL;
                            csv=csv+node.asLiteral().getValue().toString()+DEL;
                            csv=csv+"literal"+DEL;
                            csv=csv+node.asNode().getLiteralLanguage()+DEL;
                        }else {
                            s_csv=s_csv+node.asLiteral().getValue().toString()+DEL;
                            csv=csv+node.asLiteral().getValue().toString()+DEL;
                            csv=csv+"literal"+DEL;
                        }
                    }else {
                        s_csv=s_csv+node.asLiteral().getValue().toString()+DEL;
                        csvCols=csvCols+key+"-datatype"+DEL;
                        csv=csv+node.asLiteral().getValue().toString()+DEL;
                        csv=csv+"literal"+DEL;
                        csv=csv+node.asLiteral().getDatatypeURI()+DEL;
                    }
                }
                if(node.isAnon()) {
                    s_csv=s_csv+node.toString()+DEL;
                    csv=csv+node.toString()+DEL;
                    csv=csv+"bnode"+DEL;
                }
            }
        }

        csvCols=csvCols.substring(0, csvCols.length()-1);
        csv=csv.substring(0, csv.length()-1);
        s_csvCols=s_csvCols.substring(0, s_csvCols.length()-1);
        s_csv=s_csv.substring(0, s_csv.length()-1);
    }

    public String getCsvCols() {
        return csvCols;
    }

    public String getCsv() {
        return csv;
    }

    public String getSimpleCols() {
        return s_csvCols;
    }

    public String getSimpleCsv() {
        return s_csv;
    }

    public void setCsv(String csv) {
        this.csv = csv;
    }

    public static String getPrefix(String st) {
        if(st!=null) {
            return Prefixes.getPrefix(st.substring(0,st.lastIndexOf("/")+1));
        }
        return "";
    }

}
