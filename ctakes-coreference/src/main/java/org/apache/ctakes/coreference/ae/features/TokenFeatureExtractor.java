package org.apache.ctakes.coreference.ae.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.coreference.util.MarkableCacheRelationExtractor;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;

public class TokenFeatureExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation>, MarkableCacheRelationExtractor {

	private Map<Markable,ConllDependencyNode> cache = null;

	@Override
	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		List<Feature> feats = new ArrayList<>();
		
		String s1 = arg1.getCoveredText().toLowerCase();
		String s2 = arg2.getCoveredText().toLowerCase();
		
		boolean dem1 = isDemonstrative(s1);
		boolean dem2 = isDemonstrative(s2);
		
		feats.add(new Feature("TOKEN_DEM1", dem1));
		feats.add(new Feature("TOKEN_DEM2", dem2));
		feats.add(new Feature("TOKEN_DEF1", isDefinite(s1)));
		feats.add(new Feature("TOKEN_DEF2", isDefinite(s2)));
		feats.add(new Feature("TOKEN_NUMAGREE",
				numberSingular(jCas, arg1, s1, cache.get((Markable)arg1)) == numberSingular(jCas, arg2, s2, cache.get((Markable)arg2))));

		String gen1 = getGender(s1);
		String gen2 = getGender(s2);
		feats.add(new Feature("TOKEN_GEN1", gen1));
		feats.add(new Feature("TOKEN_GEN2", gen2));
		feats.add(new Feature("TOKEN_GENAGREE", gen1.equals(gen2)));
		
//		String p1 = getPerson(s1);
//		String p2 = getPerson(s2);
//		feats.add(new Feature("TOKEN_PERSON1", p1));
//		feats.add(new Feature("TOKEN_PERSON2", p2));
//		feats.add(new Feature("TOKEN_PERSONAGREE", p1.equals(p2)));
//		feats.add(new Feature("TOKEN_PERSONPAIR", p1+"-"+p2));
//		feats.add(new Feature("IS_TITLE1", isTitle(s1)));
//		feats.add(new Feature("IS_TITLE2", isTitle(s2)));
		
//		feats.add(new Feature("IS_DOCTOR1", s1.startsWith("dr.")));
//		feats.add(new Feature("IS_DOCTOR2", s2.startsWith("dr.")));
//		feats.add(new Feature("BOTH_DOCTOR", s1.startsWith("dr.") && s2.startsWith("dr.")));
		
//		boolean a1IsHuman = false;
//		boolean a2IsHuman = false;
		
		// if has some person (1st, 2nd, 3rd) or gender (masc., fem), is doctor
//		a1IsHuman |= (!p1.equals("NONE"));
//		a1IsHuman |= (!gen1.equals("NEUTER"));
//		a1IsHuman |= (isTitle(s1));
//		    
//    a2IsHuman |= (!p2.equals("NONE"));
//    a2IsHuman |= (!gen2.equals("NEUTER"));
//    a2IsHuman |= (isTitle(s2));
//		
//		feats.add(new Feature("IS_HUMAN1", a1IsHuman));
//		feats.add(new Feature("IS_HUMAN2", a2IsHuman));
//		feats.add(new Feature("BOTH_HUMAN", a1IsHuman && a2IsHuman));
//		feats.add(new Feature("NEITHER_HUMAN", !a1IsHuman && !a2IsHuman));
		
		// is it a section header?
		List<BaseToken> nextToks = JCasUtil.selectFollowing(jCas, BaseToken.class, arg1, 1);
		if(nextToks.size() > 0 && nextToks.get(0) instanceof NewlineToken){
		  feats.add(new Feature("IS_HEADER1", true));
		}
		nextToks = JCasUtil.selectFollowing(jCas, BaseToken.class, arg2, 1);
		if(nextToks.size() > 0 && nextToks.get(0) instanceof NewlineToken){
		  feats.add(new Feature("IS_HEADER2", true));
		}
		return feats;
	}
	
	public static boolean isDemonstrative (String s) {
		if (s.startsWith("this") ||
				s.startsWith("that") ||
				s.startsWith("these") ||
				s.startsWith("those")){
				return true;
		}
		return false;
	}
	
	public static boolean isDefinite (String s) {
		return s.startsWith("the ");
	}

	// FYI - old code used treebanknode types and found head using head rules filled in by the parser
	// not sure if there is an appreciable difference...
	public static boolean numberSingular(JCas jcas, Annotation arg, String s1, ConllDependencyNode head){
//		List<BaseToken> tokens = new ArrayList<>(JCasUtil.selectCovered(BaseToken.class, arg));
//		for (int i = tokens.size()-1; i >=0; i--){
//			BaseToken t = tokens.get(i);
//			String pos = t.getPartOfSpeech();
	  if(head != null && head.getPostag() != null){
	    String pos = head.getPostag();
	    if ("NN".equals(pos) || "NNP".equals(pos)){
	      return true;
	    }else if ("NNS".equals(pos) || "NNPS".equals(pos)){
	      return false;
	    }else if(s1.equals("we") || s1.equals("they")){
	      return false;
	    }
	  }
//		}
		return true;
	}
	
	public static String getGender(String s1){
	  if(s1.equals("he") || s1.equals("his") || s1.equals("him") || s1.startsWith("mr.")) return "MALE";
	  else if(s1.equals("she") || s1.equals("her") || s1.startsWith("mrs.") || s1.startsWith("ms.")) return "FEMALE";
	  else return "NEUTER";
	}
	
	public static String getPerson(String s1){
	  if(s1.equals("i") || s1.equals("my")) return "FIRST";
	  else if(s1.equals("he") || s1.equals("she") || s1.equals("his") || s1.equals("her") || s1.equals("hers")){
	    return "THIRD";
	  }else if(s1.equals("you") || s1.equals("your")) return "SECOND";
	  else if(s1.equals("we") || s1.equals("our")) return "FIRST_PLURAL";
	  else if(s1.equals("they") || s1.equals("their")) return "THIRD_PLURAL";
	  else return "NONE";
	}
	
	public static boolean getAnimate(String s1){
	  if(s1.equals("i")) return true;
	  return false;
	}
	
	public static boolean isTitle(String s1){
	  return s1.startsWith("dr.") || s1.startsWith("mr.") || s1.startsWith("mrs.") || s1.startsWith("ms.");
	}
	
	public static boolean isNegated(IdentifiedAnnotation mention){
	  return mention.getPolarity() == CONST.NE_POLARITY_NEGATION_PRESENT;
	}
	
	public static boolean isUncertain(IdentifiedAnnotation mention){
	  return mention.getUncertainty() == CONST.NE_UNCERTAINTY_PRESENT;
	}
	
	public static boolean isGeneric(IdentifiedAnnotation mention){
	  return mention.getGeneric() == CONST.NE_GENERIC_TRUE;
	}
	
	public static boolean isPatient(IdentifiedAnnotation mention){
	  return mention.getSubject() == CONST.ATTR_SUBJECT_PATIENT;
	}
	
	public static boolean isHistory(IdentifiedAnnotation mention){
	  return mention.getHistoryOf() == CONST.NE_HISTORY_OF_PRESENT;
	}

	@Override
	public void setCache(Map<Markable, ConllDependencyNode> cache) {
		this.cache = cache;
	}
}
