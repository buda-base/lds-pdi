package io.bdrc.ldspdi.sparql.functions;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase2;

public class LevenshteinFilter extends FunctionBase2 {
	
	public LevenshteinFilter() { super() ; }
	
	public NodeValue exec(NodeValue value1, NodeValue value2){
		LevenshteinDistance LD=new LevenshteinDistance();
        int i = LD.apply(value1.asString(), value2.asString()); 
        return NodeValue.makeInteger(i); 
    }
}
