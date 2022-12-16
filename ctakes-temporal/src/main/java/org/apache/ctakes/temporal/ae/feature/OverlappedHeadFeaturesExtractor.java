package org.apache.ctakes.temporal.ae.feature;

import org.apache.ctakes.relationextractor.ae.features.TokenFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Extract the overlapping head words of two arguments. Head words: the NNs of NP + the VBs of VP
 *
 * @author CH151862
 */
public class OverlappedHeadFeaturesExtractor extends TokenFeaturesExtractor {

   static private final String FEATURE_NAME_ROOT = "overlappingHeadTerms";
   static private final String NOT_NN_VB_POS = "NOT_NN_VB_POS";

   /**
    * {@inheritDoc}
    */
   @Override
   public List<Feature> extract( final JCas jCas,
                                 final IdentifiedAnnotation mention1,
                                 final IdentifiedAnnotation mention2 ) throws AnalysisEngineProcessException {
      final Collection<WordToken> currentTokens = JCasUtil.selectCovered( jCas, WordToken.class, mention1 );
      final Collection<WordToken> nextTokens = JCasUtil.selectCovered( jCas, WordToken.class, mention2 );
      if ( currentTokens == null || currentTokens.isEmpty() || nextTokens == null || nextTokens.isEmpty() ) {
         return Collections.emptyList();
      }
      final List<Feature> features = new ArrayList<>();
      //iterate through the tokens of two arguments
      int headSize1 = 0;
      int headSize2 = 0;
      int matches = 0;
      for ( WordToken t1 : currentTokens ) {
         final String t1_pos = getNnVbPos( t1 );
         if ( !t1_pos.equals( NOT_NN_VB_POS ) ) {
            headSize1++;
            for ( WordToken t2 : nextTokens ) {
               if ( !getNnVbPos( t2 ).equals( NOT_NN_VB_POS ) ) {
                  headSize2++;
                  final String t1str = t1.getCanonicalForm();
                  if ( t1str != null && t1str.equals( t2.getCanonicalForm() ) ) {
                     features.add( createFeature( "CanonicalForm", t1str ) );
                     features.add( createFeature( "length", t1str.length() ) );
                     features.add( createFeature( "POS", t1_pos ) );
                     matches++;
                  }
               }
            }
         }
      }
      if ( matches > 0 ) {
         //feature of counting times of matches
         features.add( createFeature( "count", matches ) );
         //ratio of the count of matches to the shorter length of tokens between the two arguments
         final float matchShortRatio = (float)matches / (float)Math.min( headSize1, headSize2 );
         features.add( createFeature( "shortRatio", matchShortRatio ) );
         //ratio of the count of matches to the longer length of tokens between the two arguments
         final float matchLongRatio = (float)matches / (float)Math.max( headSize1, headSize2 );
         features.add( createFeature( "longRatio", matchLongRatio ) );
      }
      return features;
   }

   static private String getNnVbPos( final BaseToken baseToken ) {
      final String pos = baseToken.getPartOfSpeech();
      if ( pos.startsWith( "NN" ) || pos.startsWith( "VB" ) ) {
         return pos;
      }
      return NOT_NN_VB_POS;
   }

   static private Feature createFeature( final String suffix, final Object value ) {
      return new Feature( FEATURE_NAME_ROOT + "_" + suffix, value );
   }

}
