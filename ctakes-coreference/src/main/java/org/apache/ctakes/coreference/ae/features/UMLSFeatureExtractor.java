package org.apache.ctakes.coreference.ae.features;

import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.cleartk.ml.Feature;
import org.cleartk.util.ViewUriUtil;

import java.util.*;

public class UMLSFeatureExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

  String docId = null;
  Map<ConllDependencyNode,Collection<IdentifiedAnnotation>> coveringMap = null;
  
	@Override
	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		List<Feature> feats = new ArrayList<>();
		
		if(docId == null || !getDocId(jCas).equals(docId)){
		  docId = getDocId(jCas);
		  coveringMap = JCasUtil.indexCovering(jCas, ConllDependencyNode.class, IdentifiedAnnotation.class);
		}
		
		
		if(arg1 instanceof Markable && arg2 instanceof Markable){
//		  feats.add(new Feature("AntecedentSalience", arg1.getConfidence()));
//		  feats.add(new Feature("AnaphorSalience", arg2.getConfidence()));
		  
		  // get the head of each markable
		  ConllDependencyNode head1 = DependencyUtility.getNominalHeadNode(jCas, arg1);
		  ConllDependencyNode head2 = DependencyUtility.getNominalHeadNode(jCas, arg2);
		  List<IdentifiedAnnotation> rmList = new ArrayList<>();
		  
		  if(head1 != null && head2 != null){
		    List<IdentifiedAnnotation> ents1 = new ArrayList<>(coveringMap.get(head1)); //JCasUtil.selectCovering(jCas, IdentifiedAnnotation.class, head1.getBegin(), head1.getEnd());'
		    for(IdentifiedAnnotation ann : ents1){
		      if(!(ann instanceof EntityMention || ann instanceof EventMention) || ann.getClass() == EventMention.class){
		        rmList.add(ann);
		      }
		    }
		    for(IdentifiedAnnotation toRm : rmList){
		      ents1.remove(toRm);
		    }
		    rmList.clear();
		    List<IdentifiedAnnotation> ents2 = new ArrayList<>(coveringMap.get(head2)); //JCasUtil.selectCovering(jCas, IdentifiedAnnotation.class, head2.getBegin(), head2.getEnd());
		    for(IdentifiedAnnotation ann : ents2){
		      if(!(ann instanceof EntityMention || ann instanceof EventMention)|| ann.getClass() == EventMention.class){
		        rmList.add(ann);
		      }
		    }
		    for(IdentifiedAnnotation toRm : rmList){
		      ents2.remove(toRm);
		    }
		    
		    if(ents1.size() == 0 && ents2.size() > 0){
		      feats.add(new Feature("Arg1NoCui_Arg2Cui", true));
		    }else if(ents1.size() > 0 && ents2.size() == 0){
		      feats.add(new Feature("Arg1Cui_Arg2NoCui", true));		      
		    }else if(ents1.size() == 0 && ents2.size() == 0){
		      feats.add(new Feature("Arg1Arg2NoCui", true));
		    }else{
		      feats.add(new Feature("Arg1Arg2BothCui", true));
		    }
		    
		    if((ents1.size() == 0 && ents2.size() > 0) ||
		        (ents1.size() > 0 && ents2.size() == 0)){
		      feats.add(new Feature("Arg1OrArg2NoCui", true));
		    }
		    
		    for(IdentifiedAnnotation ent1 : ents1){
	        HashSet<String> a1Tuis = new HashSet<>(); 
		      String a1SemType = ent1.getClass().getSimpleName();
		      feats.add(new Feature("Arg1SemType" + a1SemType, true));
		      FSArray cons1 = ent1.getOntologyConceptArr();
		      if(cons1 != null){
		        for(int i = 0; i < cons1.size(); i++){
		          if(cons1.get(i) instanceof UmlsConcept){
		            a1Tuis.add(((UmlsConcept)cons1.get(i)).getTui());
		          }
		        }
		      }
		      for(IdentifiedAnnotation ent2 : ents2){
		        HashSet<String> a2Tuis = new HashSet<>();
		        String a2SemType = ent2.getClass().getSimpleName();
	          feats.add(new Feature("Arg2SemType" + a2SemType, true));
		        if(alias(ent1, ent2)){
		          feats.add(new Feature("UMLS_ALIAS", true));
//		          break;
		        }
//		        if(!alias(ent1, ent2) && isHypernym(ent1, ent2)){
//		          feats.add(new Feature("IS_HYPERNYM", true));
//		        }
//		        if(!alias(ent1, ent2) && isHyponym(ent1, ent2)){
//		          feats.add(new Feature("IS_HYPONYM", true));
//		        }
		        feats.add(new Feature("Arg1Arg2SemType" + a1SemType + "_" + a2SemType, true));
		        
		        FSArray cons2 = ent2.getOntologyConceptArr();
		        if(cons2 != null){
		          for(int i = 0; i < cons2.size(); i++){
		            if(cons2.get(i) instanceof UmlsConcept){
		              a2Tuis.add(((UmlsConcept)cons2.get(i)).getTui());
		            }
		          }
		        }
		        for(String tui1 : a1Tuis){
		          feats.add(new Feature("Arg1Tui_" +  tui1, true));
		          for(String tui2 : a2Tuis){
		            feats.add(new Feature("Arg1Tui_" + tui1 + "_Arg2Tui_ " + tui2, true));
		            if(tui1.equals(tui2)){
		              feats.add(new Feature("Arg1Arg2TuiMatch", true));
		            }
		          }
		        }
		        for(String tui2 : a2Tuis){
		          feats.add(new Feature("Arg2Tui_" + tui2, true));
		        }
		      }
		    }
		  }
		}
		return feats;
	}

	public static String getDocId(JCas jcas) {
	  String docId = null;

      docId = DocIdUtil.getDocumentID( jcas );
      if ( docId != DocIdUtil.NO_DOCUMENT_ID ) {
         return docId;
      }
	  
	  try{
	    if(jcas.getView(ViewUriUtil.URI) != null){
	      docId = ViewUriUtil.getURI(jcas).toString();
	    }
	  }catch(Exception e){
	    // don't need to do anything -- just return null
	  }
	  return docId;
  }

  public static boolean alias(IdentifiedAnnotation a1, IdentifiedAnnotation a2){  
	  if(a1 != null && a2 != null){
	    for(UmlsConcept concept1 : JCasUtil.select(a1.getOntologyConceptArr(), UmlsConcept.class)){
	      String cui = concept1.getCui();
	      for(UmlsConcept concept2 : JCasUtil.select(a2.getOntologyConceptArr(), UmlsConcept.class)){
	        if(cui.equals(concept2.getCui())){
	          return true;
	        }
	      }
	    }	  
	  }
		return false;
	}
  
/*  
  public static boolean isHypernym(IdentifiedAnnotation a1, IdentifiedAnnotation a2){
    if(a1 != null && a2 != null){
      for(UmlsConcept concept1 : JCasUtil.select(a1.getOntologyConceptArr(), UmlsConcept.class)){
        for(UmlsConcept concept2 : JCasUtil.select(a2.getOntologyConceptArr(), UmlsConcept.class)){
          if(GraphFunctions.isa(concept1.getCui(), concept2.getCui())){
            return true;
          }
        }
      }
    }
    return false;
  }
  
  public static boolean isHyponym(IdentifiedAnnotation a1, IdentifiedAnnotation a2){
    return isHypernym(a2, a1);
  }
  
  // returns distance in graph. For isa relation the distance will be positive and for
  // inverse isa it will be negative, thus the absolute value comparisons.
  public static int graphDistance(IdentifiedAnnotation a1, IdentifiedAnnotation a2){
    int distance = Integer.MAX_VALUE;
    
    if(a1 != null && a2 != null){
      for(UmlsConcept concept1 : JCasUtil.select(a1.getOntologyConceptArr(), UmlsConcept.class)){
        String cui1 = concept1.getCui();
        for(UmlsConcept concept2 : JCasUtil.select(a2.getOntologyConceptArr(), UmlsConcept.class)){
          String cui2 = concept2.getCui();
          int len = GraphFunctions.minDistance(cui1, cui2);
          if(len < 0){
            len = GraphFunctions.minDistance(cui2, cui1);
            if(len < 0){
              len = Integer.MAX_VALUE;
            }else{
              len = -len;
            }
          }
          if(Math.abs(len) < Math.abs(distance)){
            distance = len;
          }
        }
      }
    }
    return distance;
  }
*/
}
