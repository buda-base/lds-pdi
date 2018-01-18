package io.bdrc.ldspdi.Utils;

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

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;

import io.bdrc.ldspdi.sparql.functions.Wylie;

public class StringHelpers {
	
	public static String removeAccents(String text) {		
		String f=text;
		return f == null ? null :
	        Normalizer.normalize(f, Form.NFD)
	            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}
	public static boolean isTibUni(String s) {		
		return s.matches("[\u0f00-\u0fff]+");
	}
	public static boolean isWylie(String s) {		
		 Wylie wl = new Wylie(true, false, false, true);
		 ArrayList<String> warn=new ArrayList<>();
		 wl.fromWylie(s, warn);
		 return warn.size()==0;
	}
	
	public static String bdrcEncode(String url) {
		String encoded=url.replace("\"", "%22");
		encoded=encoded.replace(' ', '+');
		encoded=encoded.replace("\'", "%27");
		return encoded;
	}
}
