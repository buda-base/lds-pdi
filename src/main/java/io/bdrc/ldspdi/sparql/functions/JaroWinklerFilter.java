package io.bdrc.ldspdi.sparql.functions;

import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase2;

public class JaroWinklerFilter extends FunctionBase2 {
	
	public JaroWinklerFilter() { super() ; }
	
	public NodeValue exec(NodeValue value1, NodeValue value2){
		JaroWinklerDistance LD=new JaroWinklerDistance();
        Double d = LD.apply(value1.asString(), value2.asString());
        return NodeValue.makeDouble(new Double(d)); 
    }
	
}
