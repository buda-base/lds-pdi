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
	
	public static void main(String[] args) {
		/*System.out.println(StringHelpers.removeAccents("prajñā"));
		System.out.println(StringHelpers.removeAccents("a-zA-āīūṛṝḷḹṃḥ'ṭḍṅñṇśṣ"));
		System.out.println(StringUtils.replaceChars("prajna", "an", "āñ"));
		System.out.println(StringUtils.containsOnly("ārya-sadhṛśāya","arya-sadhrsayaāīūṛṝḷḹṃḥ'ṭḍṅñṇśṣ"));*/
		System.out.println(isTibUni("ཐེག་པ་ཆེན་པོ་རྒྱུད་བླ་མའི་བསྟན་བཅོས་ཀྱི་ངེས་པའི་དོན་གསལ་བར་བྱེད་པ་རིན་པོ་ཆེའི་སྒྲོན་མེ་ཞེས་བྱ་བ་ཡུལ་དབུས་ཀྱི་བྱུང་པ་རིགས་པར་སྨྲ་བ་བློ་གྲོས་མཚུངས་མེད་ཀྱིས་སྐྱེས་བུ་ངེས་པའི་ཚོགས་ཀྱིས་བརྟེན་པའི་བསྟི་གནས་དཔལ་གསང་ཕུ་ནེའུ་ཐོག་གི་ཆོས་གྲྭ་ཆེན་པོར་སྦྱར་བའོ།།"));
		/*System.out.println(isTibUni("rgyas"));
		System.out.println(isWylie("ཆེན་པོས་དུས"));
		System.out.println(isWylie("rgyas pa chen po"));*/
	}
	
	

}
