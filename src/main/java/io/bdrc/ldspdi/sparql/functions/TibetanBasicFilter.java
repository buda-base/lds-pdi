package io.bdrc.ldspdi.sparql.functions;

import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase2;

import io.bdrc.ldspdi.Utils.StringHelpers;

public class TibetanBasicFilter extends FunctionBase2{
	
	public TibetanBasicFilter() { super() ; }	
	
	public NodeValue exec(NodeValue value1, NodeValue value2){
		Wylie wyl=new Wylie(true,false,false,true);
		String v1=value1.asString(); 		
		String v2=value2.asString();
		
		if(StringHelpers.isWylie(v1.trim())) {			
			v1=wyl.fromWylie(v1);
		}
		if(StringHelpers.isWylie(v2.trim())){ 
			v2=wyl.fromWylie(v2);
		}
		boolean b=(v1.contains(v2));
		return NodeValue.makeBoolean(b);
	}
}

