package org.apache.ctakes.assertion.medfacts.cleartk.windowed.context.feature.extractor;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

import java.util.List;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/26/2018
 */
abstract public class AbstractWindowedFeatureExtractor1<T extends Annotation> implements FeatureExtractor1<T> {

   protected List<Sentence> _sentences;
   protected Sentence _sentence;
   protected int _sentenceIndex;
   protected List<BaseToken> _baseTokens;

   public void setSentences( final List<Sentence> sentences ) {
      _sentences = sentences;
   }

   public void setWindow( final Sentence sentence, final int sentenceIndex, final List<BaseToken> baseTokens ) {
      _sentence = sentence;
      _sentenceIndex = sentenceIndex;
      _baseTokens = baseTokens;
   }

}
