package io.bdrc.ldspdi.Utils;

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
}
