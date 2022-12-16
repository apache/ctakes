package org.apache.ctakes.coreference.ae.features;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;

public class StringMatchingFeatureExtractor implements
		RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

	@Override
	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		List<Feature> feats = new ArrayList<>();
		
		// don't extract sim features if one of the markables is a pronoun
		if(isPronoun(arg1) || isPronoun(arg2)) return feats;
		
		String s1 = arg1.getCoveredText();
		String s2 = arg2.getCoveredText();
		Set<String> words1 = contentWords(arg1);
		Set<String> words2 = contentWords(arg2);
		
		feats.add(new Feature("MATCH_EXACT",
				s1.equalsIgnoreCase(s2)));
		feats.add(new Feature("MATCH_START",
				startMatch(s1,s2)));
		feats.add(new Feature("MATCH_END",
				endMatch(s1,s2)));
		feats.add(new Feature("MATCH_SOON",
				soonMatch(s1,s2)));
		feats.add(new Feature("MATCH_OVERLAP",
				wordOverlap(words1, words2)));
		feats.add(new Feature("MATCH_SUBSTRING",
				wordSubstring(words1, words2)));
		return feats;
	}

	public static boolean startMatch (String a, String b) {
		int ia = a.indexOf(" ");
		int ib = b.indexOf(" ");
		String aa = a.substring(0, ia==-1?(a.length()>5?5:a.length()):ia);
		String bb = b.substring(0, ib==-1?(b.length()>5?5:b.length()):ib);
		return aa.equalsIgnoreCase(bb);
	}

	public static boolean endMatch (String a, String b) {
		int ia = a.lastIndexOf(" ");
		int ib = b.lastIndexOf(" ");
		String aa = a.substring(ia==-1?(a.length()>5?a.length()-5:0):ia+1);
		String bb = b.substring(ib==-1?(b.length()>5?b.length()-5:0):ib+1);
		return aa.equalsIgnoreCase(bb);
	}

	public static boolean soonMatch (String s1, String s2) {
		String sl1 = nonDetSubstr(s1.toLowerCase());
		String sl2 = nonDetSubstr(s2.toLowerCase());
		return sl1.equals(sl2);
	}

	public static String nonDetSubstr (String s) {
		if(s.startsWith("the ")) return s.substring(4);
		if(s.startsWith("a ")) return s.substring(2);
		if(s.startsWith("this ")) return s.substring(5);
		if(s.startsWith("that ")) return s.substring(5);
		if(s.startsWith("these ")) return s.substring(6);
		if(s.startsWith("those ")) return s.substring(6);
		return s;
	}

	public static boolean wordOverlap(Set<String> t1, Set<String> t2) {
		for (String s : t2){
			if (t1.contains(s)){
				return true;
			}
		}
		return false;
	}

	public static boolean wordSubstring(Set<String> t1, Set<String> t2){
	  for(String s1 : t1){
	    for(String s2 : t2){
	      if(s1.contains(s2) || s2.contains(s1)) return true;
	    }
	  }
	  return false;
	}
	
	public static Set<String> contentWords(Annotation a1){
		Set<String> words = new HashSet<>();
		for(BaseToken tok : JCasUtil.selectCovered(BaseToken.class, a1)){
			words.add(tok.getCoveredText().toLowerCase());
		}
		return words;
	}
	
	public static boolean isPronoun(IdentifiedAnnotation a1){
	  List<BaseToken> tokens = JCasUtil.selectCovered(BaseToken.class, a1);
	  
	  if(tokens.size() != 1){
	    return false;
	  }
	  
	  BaseToken token = tokens.get(0);
	  if(token.getPartOfSpeech() == null){
	    return false;
	  }
	  if(token.getPartOfSpeech().startsWith("PRP")) return true;
	  if(token.getPartOfSpeech().equals("DT")) return true;
	  
	  
	  return false;
	}
	
	public static boolean inQuote(JCas jcas, Annotation a){
	  boolean inQuote = false;
	  String docText = jcas.getDocumentText();
	  
	  // Logic: Find the newline preceding this mention, if there is a quote in between
	  // the start of the line and the start of the mention then the mention is inside quotes.
	  // not foolproof but probably pretty accurate.
	  int lastNewline = docText.lastIndexOf("\n", a.getBegin());
	  if(lastNewline != 0){
	    int firstQuote = docText.indexOf('"', lastNewline);
	    if(firstQuote != 0){
	      inQuote = true;
	    }
	  }
	  
	  return inQuote;
	}
}
