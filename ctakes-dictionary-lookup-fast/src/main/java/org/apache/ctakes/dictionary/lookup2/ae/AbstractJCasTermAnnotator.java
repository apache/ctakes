/*
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
package org.apache.ctakes.dictionary.lookup2.ae;

import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.collection.CollectionMap;
import org.apache.ctakes.core.util.collection.HashSetMap;
import org.apache.ctakes.dictionary.lookup2.concept.Concept;
import org.apache.ctakes.dictionary.lookup2.concept.ConceptFactory;
import org.apache.ctakes.dictionary.lookup2.dictionary.DictionaryDescriptorParser;
import org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary;
import org.apache.ctakes.dictionary.lookup2.textspan.TextSpan;
import org.apache.ctakes.dictionary.lookup2.util.DictionarySpec;
import org.apache.ctakes.dictionary.lookup2.util.FastLookupToken;
import org.apache.ctakes.typesystem.type.syntax.*;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Performs the basic initialization with uima context, including the parse of the dictionary specifications file.
 * Has a
 * <p/>
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 12/6/13
 */
abstract public class AbstractJCasTermAnnotator extends JCasAnnotator_ImplBase
      implements JCasTermAnnotator, WindowProcessor {

   // LOG4J logger based on interface name
   final static private Logger LOGGER = Logger.getLogger( "AbstractJCasTermAnnotator" );

   //   private int _lookupWindowType;
   private Class<? extends Annotation> _lookupClass;
   private DictionarySpec _dictionarySpec;
   private final Set<String> _exclusionPartsOfSpeech = new HashSet<>();

   @ConfigurationParameter( name = ConfigParameterConstants.PARAM_LOOKUP_XML, mandatory = false,
         description = ConfigParameterConstants.DESC_LOOKUP_XML, defaultValue = DEFAULT_DICT_DESC_PATH )
   private String _lookupXml;

   /**
    * @deprecated replaced by _lookupXml
    */
   @Deprecated
   @ConfigurationParameter( name = JCasTermAnnotator.DICTIONARY_DESCRIPTOR_KEY, mandatory = false,
         description = "Path to Dictionary spec xml", defaultValue = DEFAULT_DICT_DESC_PATH )
   private String _descriptorFilePath;

   // type of lookup window to use, typically "LookupWindowAnnotation" or "Sentence"
   @ConfigurationParameter( name = JCasTermAnnotator.PARAM_WINDOW_ANNOT_KEY, mandatory = false,
         description = "Type of Lookup window to use", defaultValue = DEFAULT_LOOKUP_WINDOW )
   private String _windowClassName;

   // set of exclusion POS tags (lower cased), may be null
   @ConfigurationParameter( name = JCasTermAnnotator.PARAM_EXC_TAGS_KEY, mandatory = false,
         description = "Set of exclusion POS tags", defaultValue = DEFAULT_EXCLUSION_TAGS )
   private String _exclusionPosTags;

   // minimum span required to accept a term
   @ConfigurationParameter( name = JCasTermAnnotator.PARAM_MIN_SPAN_KEY, mandatory = false,
         description = "Minimum number of characters for a term" )
   protected int _minimumLookupSpan = DEFAULT_MINIMUM_SPAN;

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext uimaContext ) throws ResourceInitializationException {
      super.initialize( uimaContext );
      try {
         final Class<?> windowClass = Class.forName( _windowClassName );
         if ( !Annotation.class.isAssignableFrom( windowClass ) ) {
            LOGGER.error( "Lookup Window Class " + _windowClassName + " not found" );
            throw new ResourceInitializationException( new ClassNotFoundException() );
         }
         _lookupClass = (Class<? extends Annotation>) windowClass;
      } catch ( ClassNotFoundException cnfE ) {
         LOGGER.error( "Lookup Window Class " + _windowClassName + " not found" );
         throw new ResourceInitializationException( cnfE );
      }

      LOGGER.info( "Using dictionary lookup window type: " + _windowClassName );
//      _lookupWindowType = JCasUtil.getType( _windowClassName );
      final String[] tagArr = _exclusionPosTags.split( "," );
      for ( String tag : tagArr ) {
         _exclusionPartsOfSpeech.add( tag.toUpperCase() );
      }
      final List<String> posList = new ArrayList<>( _exclusionPartsOfSpeech );
      Collections.sort( posList );
      final StringBuilder sb = new StringBuilder();
      for ( String pos : posList ) {
         sb.append( pos ).append( " " );
      }
      LOGGER.info( "Exclusion tagset loaded: " + sb.toString() );

      // optional minimum span, default is 3
      final Object minimumSpan = uimaContext.getConfigParameterValue( PARAM_MIN_SPAN_KEY );
      if ( minimumSpan != null ) {
         _minimumLookupSpan = parseInt( minimumSpan, PARAM_MIN_SPAN_KEY, _minimumLookupSpan );
      }
      LOGGER.info( "Using minimum term text span: " + _minimumLookupSpan );
      String descriptorFilePath = _descriptorFilePath;
      if ( _lookupXml != null && !_lookupXml.isEmpty() ) {
         descriptorFilePath = _lookupXml;
      }
      LOGGER.info( "Using Dictionary Descriptor: " + descriptorFilePath );
      try ( InputStream descriptorStream = FileLocator.getAsStream( descriptorFilePath ) ) {
         _dictionarySpec = DictionaryDescriptorParser.parseDescriptor( descriptorStream, uimaContext );
      } catch ( IOException | AnnotatorContextException multE ) {
         throw new ResourceInitializationException( multE );
      }
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Named Entities ..." );
//      final JFSIndexRepository indexes = jcas.getJFSIndexRepository();
//      final AnnotationIndex<Annotation> lookupWindows = indexes.getAnnotationIndex( _lookupWindowType );
//      if ( lookupWindows == null ) {  // I don't trust AnnotationIndex.size(), so don't check
//         return;
//      }
      final Map<Annotation, Collection<BaseToken>> windowTokens = org.apache.uima.fit.util.JCasUtil.indexCovered( jcas, _lookupClass, BaseToken.class );
      final Map<RareWordDictionary, CollectionMap<TextSpan, Long, ? extends Collection<Long>>> dictionaryTermsMap
            = new HashMap<>( getDictionaries().size() );
      for ( RareWordDictionary dictionary : getDictionaries() ) {
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> textSpanCuis = new HashSetMap<>();
         dictionaryTermsMap.put( dictionary, textSpanCuis );
      }
      try {
//         for ( Object window : lookupWindows ) {
//            if ( isWindowOk( (Annotation)window ) ) {
//               processWindow( jcas, (Annotation)window, dictionaryTermsMap );
//            }
//         }
         for ( Map.Entry<Annotation, Collection<BaseToken>> entry : windowTokens.entrySet() ) {
//            if ( isWindowOk( entry.getKey() ) ) {
            processWindow( jcas, entry.getValue(), dictionaryTermsMap );
//            }
         }
      } catch ( ArrayIndexOutOfBoundsException iobE ) {
         // JCasHashMap will throw this every once in a while.  Assume the windows are done and move on
         LOGGER.warn( iobE.getMessage() );
      }
      // Let the consumer handle uniqueness and ordering - some may not care
      final Collection<Long> allDictionaryCuis = new HashSet<>();
      final CollectionMap<Long, Concept, ? extends Collection<Concept>> allConceptsMap = new HashSetMap<>();
      for ( Map.Entry<RareWordDictionary, CollectionMap<TextSpan, Long, ? extends Collection<Long>>> dictionaryCuis : dictionaryTermsMap
            .entrySet() ) {
         allDictionaryCuis.clear();
         final RareWordDictionary dictionary = dictionaryCuis.getKey();
         final CollectionMap<TextSpan, Long, ? extends Collection<Long>> textSpanCuis = dictionaryCuis.getValue();
         for ( Collection<Long> cuiCodes : textSpanCuis.getAllCollections() ) {
            allDictionaryCuis.addAll( cuiCodes );
         }
         final Collection<ConceptFactory> conceptFactories
               = _dictionarySpec.getPairedConceptFactories( dictionary.getName() );
         allConceptsMap.clear();
         for ( ConceptFactory conceptFactory : conceptFactories ) {
            final Map<Long, Concept> conceptMap = conceptFactory.createConcepts( allDictionaryCuis );
            allConceptsMap.placeMap( conceptMap );
         }
         _dictionarySpec.getConsumer().consumeHits( jcas, dictionary, textSpanCuis, allConceptsMap );
      }
      LOGGER.info( "Finished processing" );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<RareWordDictionary> getDictionaries() {
      return _dictionarySpec.getDictionaries();
   }

//   /**
//    * Skip windows that are section headers/footers.  Kludge, but worth doing
//    *  read these string values as parameters from uimaContext ?
//    * {@inheritDoc}
//    */
//   @Override
//   public boolean isWindowOk( final Annotation window ) {
//      final String coveredText = window.getCoveredText();
//      return !coveredText.equals( "section id" )
//             && !coveredText.startsWith( "[start section id" )
//             && !coveredText.startsWith( "[end section id" )
//             && !coveredText.startsWith( "[meta rev_" );
//   }


//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public void processWindow( final JCas jcas, final Annotation window,
//                              final Map<RareWordDictionary, CollectionMap<TextSpan, Long, ? extends Collection<Long>>> dictionaryTerms ) {
//      final List<FastLookupToken> allTokens = new ArrayList<>();
//      final List<Integer> lookupTokenIndices = new ArrayList<>();
//      getAnnotationsInWindow( jcas, window, allTokens, lookupTokenIndices );
//      findTerms( getDictionaries(), allTokens, lookupTokenIndices, dictionaryTerms );
//   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void processWindow( final JCas jcas, final Collection<BaseToken> windowBaseTokens,
                              final Map<RareWordDictionary, CollectionMap<TextSpan, Long, ? extends Collection<Long>>> dictionaryTerms ) {
      final List<FastLookupToken> allTokens = new ArrayList<>();
      final List<Integer> lookupTokenIndices = new ArrayList<>();
      getAnnotationsInWindow( jcas, windowBaseTokens, allTokens, lookupTokenIndices );
      findTerms( getDictionaries(), allTokens, lookupTokenIndices, dictionaryTerms );
   }

   /**
    * Given a set of dictionaries, tokens, and lookup token indices, populate a terms map with discovered terms
    *
    * @param dictionaries       -
    * @param allTokens          -
    * @param lookupTokenIndices -
    * @param dictionaryTermsMap -
    */
   private void findTerms( final Iterable<RareWordDictionary> dictionaries,
                           final List<FastLookupToken> allTokens, final List<Integer> lookupTokenIndices,
                           final Map<RareWordDictionary, CollectionMap<TextSpan, Long, ? extends Collection<Long>>> dictionaryTermsMap ) {
      for ( RareWordDictionary dictionary : dictionaries ) {
         CollectionMap<TextSpan, Long, ? extends Collection<Long>> termsFromDictionary = dictionaryTermsMap
               .get( dictionary );
         findTerms( dictionary, allTokens, lookupTokenIndices, termsFromDictionary );
      }
   }


//   /**
//    * For the given lookup window fills two collections with 1) All tokens in the window,
//    * and 2) indexes of tokens in the window to be used for lookup
//    *
//    * @param jcas               -
//    * @param window             annotation lookup window
//    * @param allTokens          filled with all tokens, including punctuation, etc.
//    * @param lookupTokenIndices filled with indices of tokens to use for lookup
//    */
//   protected void getAnnotationsInWindow( final JCas jcas, final AnnotationFS window,
//                                          final List<FastLookupToken> allTokens,
//                                          final Collection<Integer> lookupTokenIndices ) {
//      final List<BaseToken> allBaseTokens = org.apache.uima.fit.util.JCasUtil
//            .selectCovered( jcas, BaseToken.class, window );
//      for ( BaseToken baseToken : allBaseTokens ) {
//         if ( baseToken instanceof NewlineToken ) {
//            continue;
//         }
//         final boolean isNonLookup = baseToken instanceof PunctuationToken
//                                     || baseToken instanceof NumberToken
//                                     || baseToken instanceof ContractionToken
//                                     || baseToken instanceof SymbolToken;
//         // We are only interested in tokens that are -words-
//         if ( !isNonLookup ) {
//            // POS exclusion logic for first word lookup
//            final String partOfSpeech = baseToken.getPartOfSpeech();
//            if ( partOfSpeech == null || !_exclusionPartsOfSpeech.contains( partOfSpeech ) ) {
//               lookupTokenIndices.add( allTokens.size() );
//            }
//         }
//         final FastLookupToken lookupToken = new FastLookupToken( baseToken );
//         allTokens.add( lookupToken );
//      }
//   }

   /**
    * For the given lookup window fills two collections with 1) All tokens in the window,
    * and 2) indexes of tokens in the window to be used for lookup
    *
    * @param jcas               -
    * @param windowBaseTokens baseTokens in window in which to search for terms
    * @param allTokens          filled with all tokens, including punctuation, etc.
    * @param lookupTokenIndices filled with indices of tokens to use for lookup
    */
   protected void getAnnotationsInWindow( final JCas jcas, final Collection<BaseToken> windowBaseTokens,
                                          final List<FastLookupToken> allTokens,
                                          final Collection<Integer> lookupTokenIndices ) {
//      final List<BaseToken> allBaseTokens = org.apache.uima.fit.util.JCasUtil
//            .selectCovered( jcas, BaseToken.class, window );
      for ( BaseToken baseToken : windowBaseTokens ) {
         if ( baseToken instanceof NewlineToken ) {
            continue;
         }
         final boolean isNonLookup = baseToken instanceof PunctuationToken
                                     || baseToken instanceof NumToken
                                     || baseToken instanceof ContractionToken
                                     || baseToken instanceof SymbolToken;
         // We are only interested in tokens that are -words-
         if ( !isNonLookup ) {
            // POS exclusion logic for first word lookup
            final String partOfSpeech = baseToken.getPartOfSpeech();
            if ( partOfSpeech == null || !_exclusionPartsOfSpeech.contains( partOfSpeech ) ) {
               lookupTokenIndices.add( allTokens.size() );
            }
         }
         final FastLookupToken lookupToken = new FastLookupToken( baseToken );
         allTokens.add( lookupToken );
      }
   }

   static protected int parseInt( final Object value, final String name, final int defaultValue ) {
      if ( value instanceof Integer ) {
         return (Integer)value;
      } else if ( value instanceof String ) {
         try {
            return Integer.parseInt( (String)value );
         } catch ( NumberFormatException nfE ) {
            LOGGER.warn( "Could not parse " + name + " " + value + " as an integer" );
         }
      } else {
         LOGGER.warn( "Could not parse " + name + " " + value + " as an integer" );
      }
      return defaultValue;
   }


}
