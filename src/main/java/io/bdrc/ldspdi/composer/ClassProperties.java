package io.bdrc.ldspdi.composer;

import java.util.ArrayList;
import java.util.HashMap;

/*******************************************************************************
 * Copyright (c) 2018 Buddhist Digital Resource Center (BDRC)
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

public class ClassProperties {
	
	HashMap<String,ArrayList<ClassProperty>> map;
	
	public ClassProperties() {
		map=new HashMap<>();
		map.put(ClassProperty.ANNOTATION_PROPERTY, new ArrayList<ClassProperty>());
		map.put(ClassProperty.DATA_TYPE_PROPERTY, new ArrayList<ClassProperty>());
		map.put(ClassProperty.OBJECT_PROPERTY, new ArrayList<ClassProperty>());
		map.put(ClassProperty.SYMMETRIC_PROPERTY, new ArrayList<ClassProperty>());
		map.put(ClassProperty.FUNCTIONAL_PROPERTY, new ArrayList<ClassProperty>());
		map.put(ClassProperty.CLASS_PROPERTY, new ArrayList<ClassProperty>());
		map.put(ClassProperty.IRREFLEXIVE_PROPERTY, new ArrayList<ClassProperty>());
	}
	
	public void addClassProperty(ClassProperty prop) {	
		map.get(prop.getRdfType()).add(prop);		
	}
	
	public ArrayList<ClassProperty> getAnnotationProps(){
		return map.get(ClassProperty.ANNOTATION_PROPERTY);
	}
	
	public ArrayList<ClassProperty> getDataTypeProps(){
		return map.get(ClassProperty.DATA_TYPE_PROPERTY);
	}
	
	public ArrayList<ClassProperty> getObjectProps(){
		return map.get(ClassProperty.OBJECT_PROPERTY);
	}
	
	public ArrayList<ClassProperty> getSymmetricProps(){
		return map.get(ClassProperty.SYMMETRIC_PROPERTY);
	}
	
	public ArrayList<ClassProperty> getFunctionalProps(){
		return map.get(ClassProperty.FUNCTIONAL_PROPERTY);
	}
	
	public ArrayList<ClassProperty> getClassProps(){
		return map.get(ClassProperty.CLASS_PROPERTY);
	}
	
	public ArrayList<ClassProperty> getIrreflexiveProps(){
		return map.get(ClassProperty.IRREFLEXIVE_PROPERTY);
	}
	
	public ArrayList<ClassProperty> getProps(String rdfType){
		return map.get(rdfType);
	}

	@Override
	public String toString() {
		return "ClassProperties [map=" + map + "]";
	}
	
	

}
