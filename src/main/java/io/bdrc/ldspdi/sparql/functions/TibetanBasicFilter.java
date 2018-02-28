package io.bdrc.ldspdi.sparql.functions;

/*******************************************************************************
 * Copyright (c) 2017 Buddhist Digital Resource Center (BDRC)
 * 
 * If this file is a derivation of another work the license header will appear below; 
 * otherwise, this work is licensed under the Apache License, Version 2.0 
 * (the "License"); you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase2;

import io.bdrc.ldspdi.utils.Helpers;

public class TibetanBasicFilter extends FunctionBase2{
	
	public TibetanBasicFilter() { super() ; }	
	
	public NodeValue exec(NodeValue value1, NodeValue value2){
		Wylie wyl=new Wylie(true,false,false,true);
		String v1=value1.asString(); 		
		String v2=value2.asString();
		
		if(Helpers.isWylie(v1.trim())) {			
			v1=wyl.fromWylie(v1);
		}
		if(Helpers.isWylie(v2.trim())){ 
			v2=wyl.fromWylie(v2);
		}
		boolean b=(v1.contains(v2));
		return NodeValue.makeBoolean(b);
	}
}

