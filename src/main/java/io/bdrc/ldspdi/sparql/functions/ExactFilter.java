package io.bdrc.ldspdi.sparql.functions;

import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase2;

public class ExactFilter extends FunctionBase2{
	
public ExactFilter() { super() ; }
	
	public NodeValue exec(NodeValue value1, NodeValue value2){
		
        return NodeValue.makeBoolean(value1.asString().equals(value2.asString())); 
    }

}
