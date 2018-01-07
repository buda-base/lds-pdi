package io.bdrc.ldspdi.sparql.functions;


import org.apache.commons.lang3.StringUtils;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase2;

import io.bdrc.ldspdi.Utils.StringHelpers;

public class SanskritBasicFilter extends FunctionBase2{
	
public SanskritBasicFilter() { super() ; }
	
	public NodeValue exec(NodeValue value1, NodeValue value2){
		String v1=value1.asString();
		String v2=value2.asString();
		String v11=StringHelpers.removeAccents(v1);
		
		if(v11.indexOf(v2)>=0) {
			String test=v2+"-āīūṛṝḷḹṃḥ'ṭḍṅñṇśṣ";			
			return NodeValue.makeBoolean(StringUtils.containsAny(v1,test));
		}
		else {
			return NodeValue.makeBoolean(false);
		}
    }
	
	public static void main(String[] args) {
		SanskritBasicFilter sbf=new SanskritBasicFilter();
		NodeValue nv=sbf.exec(new NodeValueString("prajnaparamita"), new NodeValueString("prajna paramita"));
		System.out.println(nv);
	}

}
