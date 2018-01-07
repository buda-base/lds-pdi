package io.bdrc.ldspdi.sparql.functions;

import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase1;

public class ToWylieFilter extends FunctionBase1{

	public ToWylieFilter() { super() ; }
	
	public NodeValue exec(NodeValue unicode){
		Wylie wl =new Wylie();
        String wylie = wl.toWylie(unicode.asString()); 
        return NodeValue.makeString(wylie); 
    }
}

