package io.bdrc.ldspdi.sparql.functions;

import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase1;


public class ToUnicodeFilter extends FunctionBase1{

	public ToUnicodeFilter() { super() ; }
	
	public NodeValue exec(NodeValue wylie){
		Wylie wl =new Wylie();
        String unicode = wl.fromWylie(wylie.asString()); 
        return NodeValue.makeString(unicode); 
    }
}
