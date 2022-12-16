/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.ytex.uima.lookup.ae;

import org.apache.ctakes.core.util.JCasUtil;
import org.apache.ctakes.dictionary.lookup.DictionaryEngine;
import org.apache.ctakes.dictionary.lookup.ae.LookupAnnotationToJCasAdapter;
import org.apache.ctakes.dictionary.lookup.ae.LookupInitializer;
import org.apache.ctakes.dictionary.lookup.algorithms.FirstTokenPermutationImpl;
import org.apache.ctakes.dictionary.lookup.algorithms.LookupAlgorithm;
import org.apache.ctakes.dictionary.lookup.phrasebuilder.PhraseBuilder;
import org.apache.ctakes.dictionary.lookup.phrasebuilder.VariantPhraseBuilderImpl;
import org.apache.ctakes.dictionary.lookup.vo.LookupAnnotation;
import org.apache.ctakes.dictionary.lookup.vo.LookupToken;
import org.apache.ctakes.typesystem.type.syntax.*;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * @author Mayo Clinic
 */
public class FirstTokenPermLookupInitializerImpl implements LookupInitializer {

   static private final String TRUE_STRING = Boolean.toString( true );
   static private final String FALSE_STRING = Boolean.toString( false );

   // LOG4J logger based on class name
   final private Logger iv_logger = Logger.getLogger( getClass().getName() );

   // properties for firstWordPermutation algorithm
   static private final String TEXT_MFS_PRP_KEY = "textMetaFields";
   static private final String MAX_P_LEVEL_PRP_KEY = "maxPermutationLevel";
   static private final String WINDOW_ANNOT_PRP_KEY = "windowAnnotations";
   static private final String EXC_TAGS_PRP_KEY = "exclusionTags"; // optional

   static private final String CANONICAL_VARIANT_ATTR = "canonicalATTR";

   final private Properties iv_props;

   // array of JCas window annotation type values
   final private int[] iv_annotTypeArr;

   // set of exclusion POS tags (lower cased)
   final private Set<String> iv_exclusionTagSet;

	/*
	 * vng - to support lookup using stemmed words
	 */
	protected Constructor lookupTokenAdapterCtor = null;
	/*
	 * vng - config key for lookupTokenAdapter class name
	 */
	private final String LOOKUP_TOKEN_ADAPTER = "lookupTokenAdapter";
	/**
	 * vng use the constructor identified during initialization to create the
	 * lookup token
	 * 
	 * @param bta
	 * @return
	 * @throws AnnotatorInitializationException
	 */
	private LookupToken annoToLookupToken(Annotation bta) 
			throws AnnotatorInitializationException {
		try {
			return (LookupToken) lookupTokenAdapterCtor.newInstance(bta);
		} catch (InvocationTargetException e) {
			throw new AnnotatorInitializationException(e);
		} catch (IllegalArgumentException e) {
			throw new AnnotatorInitializationException(e);
		} catch (InstantiationException e) {
			throw new AnnotatorInitializationException(e);
		} catch (IllegalAccessException e) {
			throw new AnnotatorInitializationException(e);
		}
	}
   
   public FirstTokenPermLookupInitializerImpl( final UimaContext uimaContext,
                                               final Properties props ) throws ClassNotFoundException,
                                                                               IllegalAccessException,
                                                                               NoSuchFieldException {
      // TODO property validation could be done here
      iv_props = props;

      // optional context window annotations
      final String windowAnnots = iv_props.getProperty( WINDOW_ANNOT_PRP_KEY );
      if ( windowAnnots != null ) {
         String[] windowAnnotArr = windowAnnots.split( "\\|" );
         iv_annotTypeArr = new int[windowAnnotArr.length];
         for ( int i = 0; i < windowAnnotArr.length; i++ ) {
            iv_annotTypeArr[i] = JCasUtil.getType( windowAnnotArr[i] );
         }
      } else {
         iv_annotTypeArr = null;
      }

      // optional exclusion POS tags
      final String tagStr = iv_props.getProperty( EXC_TAGS_PRP_KEY );
      if ( tagStr != null ) {
         iv_exclusionTagSet = new HashSet<String>();
         final String[] tagArr = tagStr.split( "," );
         for ( String tag : tagArr ) {
            iv_exclusionTagSet.add( tag.toLowerCase() );
         }
         iv_logger.info( "Exclusion tagset loaded: " + iv_exclusionTagSet );
      } else {
         iv_exclusionTagSet = null;
      }
		// vng change - get the lookupTokenAdapter class name from the
		// configuration properties
		// this is to support stemming
		String lookupTokenAdapterClazz = this.iv_props.getProperty(
				LOOKUP_TOKEN_ADAPTER,
				LookupAnnotationToJCasAdapter.class.getName());
		try {
			this.lookupTokenAdapterCtor = Class
					.forName(lookupTokenAdapterClazz).getConstructor(
							Annotation.class);
		} catch (NoSuchMethodException nsme) {
			throw new ClassNotFoundException(lookupTokenAdapterClazz, nsme);
		}
      
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public LookupAlgorithm getLookupAlgorithm( final DictionaryEngine dictEngine )
         throws AnnotatorInitializationException {
      final String textMetaFields = iv_props.getProperty( TEXT_MFS_PRP_KEY );
      String[] textMetaFieldNameArr;
      if ( textMetaFields == null ) {
         textMetaFieldNameArr = new String[0];
      } else {
         textMetaFieldNameArr = textMetaFields.split( "\\|" );
      }
      // variant support
      final String[] variantArr = {CANONICAL_VARIANT_ATTR};
      final PhraseBuilder pb = new VariantPhraseBuilderImpl( variantArr, true );
      final int maxPermutationLevel = Integer.parseInt( iv_props.getProperty( MAX_P_LEVEL_PRP_KEY ) );
      return new FirstTokenPermutationImpl( dictEngine, pb, textMetaFieldNameArr, maxPermutationLevel );
   }

   private boolean isTagExcluded( final String tag ) {
      return iv_exclusionTagSet != null && tag != null && iv_exclusionTagSet.contains( tag.toLowerCase() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Iterator<LookupToken> getLookupTokenIterator( final JCas jcas ) throws AnnotatorInitializationException {
      final List<LookupToken> ltList = new ArrayList<LookupToken>();

      final JFSIndexRepository indexes = jcas.getJFSIndexRepository();
      final AnnotationIndex<Annotation> annotationIndex = indexes.getAnnotationIndex( BaseToken.type );
      for ( Annotation annotation : annotationIndex ) {
         if ( !(annotation instanceof BaseToken) ) {
            iv_logger.warn( getClass().getName() + " getLookupTokenIterator(..) Annotation is not a BaseToken" );
            continue;
         }
         final boolean isNonLookup = annotation instanceof NewlineToken
               || annotation instanceof PunctuationToken
               || annotation instanceof ContractionToken
               || annotation instanceof SymbolToken;
         if ( isNonLookup ) {
            continue;
         }
         final BaseToken bta = (BaseToken) annotation;
         final LookupToken lt = new LookupAnnotationToJCasAdapter( bta );
         // POS exclusion logic for first word lookup
         if ( isTagExcluded( bta.getPartOfSpeech() ) ) {
            lt.addStringAttribute( FirstTokenPermutationImpl.LT_KEY_USE_FOR_LOOKUP, FALSE_STRING );
         } else {
            lt.addStringAttribute( FirstTokenPermutationImpl.LT_KEY_USE_FOR_LOOKUP, TRUE_STRING );
         }
         if ( bta instanceof WordToken ) {
            final WordToken wta = (WordToken) bta;
            final String canonicalForm = wta.getCanonicalForm();
            if ( canonicalForm != null ) {
               lt.addStringAttribute( CANONICAL_VARIANT_ATTR, canonicalForm );
            }
         }
         ltList.add( lt );
      }
      return ltList.iterator();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Iterator<Annotation> getLookupWindowIterator( final JCas jcas ) throws AnnotatorInitializationException {
      try {
         final JFSIndexRepository indexes = jcas.getJFSIndexRepository();
         final String objClassName = iv_props.getProperty( WINDOW_ANNOT_PRP_KEY );
         int windowType;
         try {
            windowType = JCasUtil.getType( objClassName );
         } catch ( IllegalArgumentException iaE ) {
            // thrown by JCasUtil.getType()
            throw new AnnotatorInitializationException( iaE );
         }
         return indexes.getAnnotationIndex( windowType ).iterator();
      } catch ( Exception e ) {
         // TODO specify exceptions, get rid of the catch for "Exception"
         throw new AnnotatorInitializationException( e );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Map<String, List<LookupAnnotation>> getContextMap( final JCas jcas,
                                                             final int windowBegin, final int windowEnd )
         throws AnnotatorInitializationException {
      if ( iv_annotTypeArr == null ) {
         return Collections.emptyMap();
      }
      final List<LookupAnnotation> list = new ArrayList<LookupAnnotation>();
      // algorithm depends on a window for permutations
      final JFSIndexRepository indexes = jcas.getJFSIndexRepository();
      for ( int annotationType : iv_annotTypeArr ) {
         final Iterator<Annotation> itr = indexes.getAnnotationIndex( annotationType ).iterator();
         list.addAll( constrainToWindow( windowBegin, windowEnd, itr ) );
      }
      final Map<String, List<LookupAnnotation>> m = new HashMap<String, List<LookupAnnotation>>( 1 );
      m.put( FirstTokenPermutationImpl.CTX_KEY_WINDOW_ANNOTATIONS, list );
      return m;
   }

   /**
    * Gets a list of LookupAnnotation objects within the specified window.
    *
    * @param annotItr -
    * @return list of lookup annotations
    */
   private List<LookupAnnotation> constrainToWindow( final int begin, final int end,
                                                     final Iterator<Annotation> annotItr ) 
      throws AnnotatorInitializationException
   {
      final List<LookupAnnotation> list = new ArrayList<LookupAnnotation>();
      while ( annotItr.hasNext() ) {
         final Annotation annot = annotItr.next();
         // only consider if it's within the window
         if ( (annot.getBegin() >= begin) && (annot.getEnd() <= end) ) {
            // vng list.add( new LookupAnnotationToJCasAdapter( annot ) );
			list.add(annoToLookupToken(annot));
         }
      }
      return list;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<LookupToken> getSortedLookupTokens( final JCas jcas,
                                                   final Annotation covering ) throws AnnotatorInitializationException {
      final List<LookupToken> ltList = new ArrayList<LookupToken>();
      final List<BaseToken> inputList = org.apache.uima.fit.util.JCasUtil.selectCovered( jcas, BaseToken.class, covering );
      for ( BaseToken bta : inputList ) {
         final boolean isNonLookup = bta instanceof NewlineToken
               || bta instanceof PunctuationToken
               || bta instanceof ContractionToken
               || bta instanceof SymbolToken;
         if ( isNonLookup ) {
            continue;
         }
         final LookupToken lt = new LookupAnnotationToJCasAdapter( bta );
         // POS exclusion logic for first word lookup
         if ( isTagExcluded( bta.getPartOfSpeech() ) ) {
            lt.addStringAttribute( FirstTokenPermutationImpl.LT_KEY_USE_FOR_LOOKUP, FALSE_STRING );
         } else {
            lt.addStringAttribute( FirstTokenPermutationImpl.LT_KEY_USE_FOR_LOOKUP, TRUE_STRING );
         }
         if ( bta instanceof WordToken ) {
            final WordToken wta = (WordToken) bta;
            final String canonicalForm = wta.getCanonicalForm();
            if ( canonicalForm != null ) {
               lt.addStringAttribute( CANONICAL_VARIANT_ATTR, canonicalForm );
            }
         }
         ltList.add( lt );
      }
      return ltList;
   }

}
