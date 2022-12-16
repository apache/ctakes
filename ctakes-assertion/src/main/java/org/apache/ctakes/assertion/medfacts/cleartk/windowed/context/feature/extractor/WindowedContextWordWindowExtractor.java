package org.apache.ctakes.assertion.medfacts.cleartk.windowed.context.feature.extractor;


import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/26/2018
 */
public class WindowedContextWordWindowExtractor extends AbstractWindowedFeatureExtractor1<IdentifiedAnnotation> {

   private HashMap<String, Double> termVals = null;
   private static final Pattern linePatt = Pattern.compile( "^([^ ]+) : (.+)$" );
   private static double[] weights = new double[ 50 ];

   static {
      weights[ 0 ] = 1.0;
      for ( int i = 1; i < weights.length; i++ ) {
         weights[ i ] = 1.0 / i;
      }
   }

   public WindowedContextWordWindowExtractor( String resourceFilename ) {
      termVals = new HashMap<>();
      InputStream is = getClass().getClassLoader().getResourceAsStream( resourceFilename );

      Scanner scanner = new Scanner( is );
      Matcher m = null;
      double max = 0.0;
      double maxNeg = 0.0;
      while ( scanner.hasNextLine() ) {
         String line = scanner.nextLine().trim();
         m = linePatt.matcher( line );
         if ( m.matches() ) {
            double val = Double.parseDouble( m.group( 2 ) );
            termVals.put( m.group( 1 ), val );
            if ( Math.abs( val ) > max ) {
               max = Math.abs( val );
            }
            if ( val < maxNeg ) {
               maxNeg = val;
            }
         }
      }
      try {
         is.close();
      } catch ( IOException e ) {
         e.printStackTrace();
      }
      max = max - maxNeg;
      for ( String key : termVals.keySet() ) {
         termVals.put( key, (termVals.get( key ) - maxNeg) / max );
      }
   }

   @Override
   public List<Feature> extract( JCas view, IdentifiedAnnotation mention )
         throws CleartkExtractorException {
      ArrayList<Feature> feats = new ArrayList<>();

      final List<BaseToken> tokens = _baseTokens;

      int startIndex = -1;
      int endIndex = -1;

      for ( int i = 0; i < tokens.size(); i++ ) {
         if ( tokens.get( i ).getBegin() == mention.getBegin() ) {
            startIndex = i;
         }
         if ( tokens.get( i ).getEnd() == mention.getEnd() ) {
            endIndex = i;
         }
      }

      double score = 0.0;
      double z = 0.0;
      String key = null;
      double weight;
      for ( int i = 0; i < tokens.size(); i++ ) {
         key = tokens.get( i ).getCoveredText().toLowerCase();
         int dist = Math.min( Math.abs( startIndex - i ), Math.abs( endIndex - i ) );
         weight = weightFunction( dist );
         z += weight;
         if ( termVals.containsKey( key ) ) {
            score += (weight * termVals.get( key ));
         }
      }

      score /= z;  // weight by actual amount of context so we don't penalize begin/end of sentence.
      feats.add( new Feature( "WORD_SCORE", score ) );
      return feats;
   }

   private static final double weightFunction( int dist ) {
      if ( dist >= weights.length ) {
         return 0.0;
      }

      // quick decay
//		return 1.0 / dist;

      // linear decay
//		return 1.0 - dist * (1.0/50.0);

      // no decay:
      return 1.0;
   }

}
