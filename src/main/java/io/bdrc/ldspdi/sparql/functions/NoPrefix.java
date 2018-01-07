package io.bdrc.ldspdi.sparql.functions;

import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase1;


public class NoPrefix extends FunctionBase1{
	
	public NoPrefix() { super() ; }
	
	public NodeValue exec(NodeValue value1){
		String val=value1.asString();
        return NodeValue.makeString(val.substring(val.lastIndexOf('/')+1)); 
    }
}
