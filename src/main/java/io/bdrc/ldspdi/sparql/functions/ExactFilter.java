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
