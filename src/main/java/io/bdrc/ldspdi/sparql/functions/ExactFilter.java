package io.bdrc.ldspdi.sparql.functions;

import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase2;

public class ExactFilter extends FunctionBase2{
	
public ExactFilter() { super() ; }
	
	public NodeValue exec(NodeValue value1, NodeValue value2){
		String s1=value1.asString();
		String s2=value2.asString();
		if(s1.endsWith("/")) {s1=s1.substring(0,s1.length()-1);}
		if(s2.endsWith("/")) {s2=s2.substring(0,s2.length()-1);}		
        return NodeValue.makeBoolean(s1.equals(s2)); 
    }

}
