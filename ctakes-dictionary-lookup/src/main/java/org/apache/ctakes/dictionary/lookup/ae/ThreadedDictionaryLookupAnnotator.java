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
package org.apache.ctakes.dictionary.lookup.ae;

import org.apache.ctakes.core.resource.FileResource;
import org.apache.ctakes.dictionary.lookup.MetaDataHit;
import org.apache.ctakes.dictionary.lookup.algorithms.LookupAlgorithm;
import org.apache.ctakes.dictionary.lookup.vo.LookupAnnotation;
import org.apache.ctakes.dictionary.lookup.vo.LookupHit;
import org.apache.ctakes.dictionary.lookup.vo.LookupToken;
import org.apache.ctakes.dictionary.lookup.vo.LookupTokenComparator;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.jdom.JDOMException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Date: 12/18/12
 */
public class ThreadedDictionaryLookupAnnotator extends JCasAnnotator_ImplBase {

   // LOG4J logger based on class name
   final private Logger _logger = Logger.getLogger(getClass().getName());

   // We need to start using types wrt generics
   private Set<LookupSpec> _lookupSpecSet = new HashSet<>();

   // used to prevent duplicate hits
   // key = hit begin,end key (java.lang.String)
   // val = Set of MetaDataHit objects
   private Map<LookupHitKey,Set<MetaDataHit>> _duplicateDataMap = new ConcurrentHashMap<>();

   @Override
  public void initialize( final UimaContext uimaContext ) throws ResourceInitializationException {
      super.initialize( uimaContext );
      configInit( uimaContext );
   }

   /**
    * Close db connections in UmlsToSnomedDbConsumerImpl
    * @throws org.apache.uima.analysis_engine.AnalysisEngineProcessException
    */
   @Override
  public void collectionProcessComplete() throws org.apache.uima.analysis_engine.AnalysisEngineProcessException {
      for ( Object value : _lookupSpecSet ) {
         if ( value instanceof LookupSpec ) {
            final LookupSpec ls = (LookupSpec)value;
            final LookupConsumer lookupConsumer = ls.getLookupConsumer();
            if ( lookupConsumer != null && lookupConsumer instanceof UmlsToSnomedDbConsumerImpl ) {
               ((UmlsToSnomedDbConsumerImpl)lookupConsumer).close();
            }
         }
      }
      super.collectionProcessComplete();
   }

   /**
    * Reads configuration parameters.
    */
   private void configInit( final UimaContext uimaContext ) throws ResourceInitializationException {
      try {
         final FileResource fResrc = (FileResource) uimaContext.getResourceObject("LookupDescriptor");
         final File descFile = fResrc.getFile();
         _logger.info( "Parsing descriptor: " + descFile.getAbsolutePath() );
         _lookupSpecSet = LookupParseUtilities.parseDescriptor( descFile, uimaContext );
      } catch ( ResourceAccessException raE ) {
         // thrown by uimaContext.getResourceObject
         throw new ResourceInitializationException( raE );
      } catch ( JDOMException jdomE ) {
         // thrown by LookupParseUtilities.parseDescriptor
         throw new ResourceInitializationException( jdomE );
      } catch ( IOException ioE ) {
         // thrown by LookupParseUtilities.parseDescriptor
         throw new ResourceInitializationException( ioE );
      } catch (Exception e) {
         // thrown by LookupParseUtilities.parseDescriptor - needs to be fixed (refined)
         throw new ResourceInitializationException(e);
      }
   }

   /**
    * Entry point for processing.
    */
   @Override
  public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      _logger.info( "process(JCas)" );
      _duplicateDataMap.clear();
      int specCount = 0;
      for ( Object value : _lookupSpecSet ) {
         if ( value instanceof LookupSpec ) {
            specCount++;
         }
      }
      final int procCount = Runtime.getRuntime().availableProcessors();
      final int threadCount = Math.min( specCount, procCount );
      final ExecutorService fixedThreadService = Executors.newFixedThreadPool( threadCount );
      final CompletionService<LookupDataStore> completionService
            = new ExecutorCompletionService<>( fixedThreadService );
      for ( Object value : _lookupSpecSet ) {
         if ( value instanceof LookupSpec ) {
            final LookupSpec ls = (LookupSpec)value;
            completionService.submit( new Callable<LookupDataStore>() {
               public LookupDataStore call() {
                  return getLookupData( jcas, ls );
               }
            });
         }
      }
      try {
         for ( int i=0; i<specCount; i++ ) {
            final Future<LookupDataStore> future = completionService.take();
            final LookupDataStore lookupDataStore = future.get();
            // consume hits - lookupConsumer.consumeHits may throw AnalysisEngineProcessException
            final LookupConsumer lookupConsumer = lookupDataStore.__lookupSpec.getLookupConsumer();
            lookupConsumer.consumeHits( jcas, lookupDataStore.__allHits.iterator() );
         }
      } catch (InterruptedException intE ) {
         // thrown by completionService.take(), future.get()
         throw new AnalysisEngineProcessException( intE );
      } catch ( ExecutionException exE ) {
         // thrown by future.get()
         throw new AnalysisEngineProcessException( exE );
      }
      try {
         fixedThreadService.shutdown();
      } catch ( SecurityException sE ) {
         _logger.debug( "Can ignore: " + sE.getMessage() );
      }
   }

   private LookupDataStore getLookupData( final JCas jcas, final LookupSpec lookupSpec ) {
      final LookupInitializer lookupInitializer = lookupSpec.getLookupInitializer();
      Iterator<Annotation> windowItr;
      try {
         windowItr = lookupInitializer.getLookupWindowIterator( jcas );
      } catch ( AnnotatorInitializationException aiE ) {
         return new LookupDataStore( lookupSpec, new ArrayList<LookupHit>(0) );
      }
      final LookupAlgorithm algorithm = lookupSpec.getLookupAlgorithm();
      final List<LookupHit> allHits = new ArrayList<LookupHit>();
      while (windowItr.hasNext()) {
         final Annotation window = windowItr.next();
         try {
            // ** Poor Form ** //
            // lookupInitializer.getLookupTokenIterator(jcas) depends on window, can't remove from loop
//            final Iterator tokenItr = lookupInitializer.getLookupTokenIterator( jcas );
//            final List<LookupToken> lookupTokensInWindow = constrainToWindow( window, tokenItr );
            final List<LookupToken> lookupTokensInWindow = lookupInitializer.getSortedLookupTokens(jcas, window);
            final Map<String,List<LookupAnnotation>> contextMap = lookupInitializer.getContextMap( jcas, window.getBegin(), window.getEnd());
            allHits.addAll( performLookup( algorithm, lookupTokensInWindow, contextMap ) );
         } catch ( AnnotatorInitializationException aiE ) {
            // thrown by lookupInitializer.getLookupTokenIterator and lookupInitializer.getContextMap
            // Don't throw here, just keep going
            // throw new AnalysisEngineProcessException(e);
         }
      }
      // TODO - last ditch memory reclamation, haven't tried yet  - spf
//      NDC.remove();
      return new LookupDataStore( lookupSpec, allHits );
   }


   /**
    * Executes the lookup algorithm on the lookup tokens. Hits are stored to
    * CAS.
    */
   private Collection<LookupHit> performLookup( final LookupAlgorithm algorithm,
                                                final List<LookupToken> lookupTokenList,
                                                final Map<String,List<LookupAnnotation>> contextMap) {
      // sort the lookup tokens (why? - spf)
      Collections.sort( lookupTokenList, LookupTokenComparator.getInstance() );
      // perform lookup
      Collection<LookupHit> lookupHits;
      try {
         lookupHits = algorithm.lookup( lookupTokenList, contextMap );
      } catch ( Exception e ) {
         // ** Should be refactored to not throw base Exception ** //
         return Collections.emptyList();
      }
      return filterHitDups( lookupHits );
   }

   /**
    * Filters out duplicate LookupHit objects.
    *
    * @param lookupHitCol -
    * @return             -
    */
   private Collection<LookupHit> filterHitDups(final Collection<LookupHit> lookupHitCol) {
      final List<LookupHit> uniqueHits = new ArrayList<>();
      for ( LookupHit lookupHit : lookupHitCol ) {
         if ( !isDuplicate( lookupHit ) ) {
            uniqueHits.add( lookupHit );
         }
      }
      return uniqueHits;
   }

   /**
    * Checks to see whether this hit is a duplicate.
    *
    * @param lookupHit
    * @return
    */
   private boolean isDuplicate( final LookupHit lookupHit ) {
      final MetaDataHit metaDataHit = lookupHit.getDictMetaDataHit();
      // iterate over MetaDataHits that have already been seen
      final LookupHitKey lookupHitKey = new LookupHitKey( lookupHit );
      Set<MetaDataHit> mdhDuplicateSet = _duplicateDataMap.get(lookupHitKey);
      if (mdhDuplicateSet != null && mdhDuplicateSet.contains( metaDataHit ) ) {
         // current LookupHit is a duplicate
         return true;
      }
      mdhDuplicateSet = new HashSet<>();
      // current LookupHit is new, add it to the duplicate set for future checks
      mdhDuplicateSet.add(metaDataHit);
      _duplicateDataMap.put( lookupHitKey, mdhDuplicateSet );
      return false;
   }

   /**
    * Storage for a LookupSpec and all of its unique LookupHits
    */
   static private class LookupDataStore {
      final private LookupSpec __lookupSpec;
      final private Collection<LookupHit> __allHits;
      private LookupDataStore( final LookupSpec lookupSpec, final Collection<LookupHit> allHits ) {
         __lookupSpec = lookupSpec;
         __allHits = allHits;
      }
   }

   /**
    * Using a String as a HashMap Key can be slow as
    * the hashCode is computed per character with each call - ditto for equals
    */
   static private class LookupHitKey {
      final private int __start;
      final private int __end;
      final private int __hashCode;
      private LookupHitKey( final LookupHit lookupHit ) {
         __start = lookupHit.getStartOffset();
         __end = lookupHit.getEndOffset();
         __hashCode = 1000 * __end + __start;
      }
      @Override
      public int hashCode() {
         return __hashCode;
      }
      @Override
      public boolean equals( final Object object ) {
         return object instanceof LookupHitKey
               && __start == ((LookupHitKey)object).__start
               && __end == ((LookupHitKey)object).__end;
      }
   }

}
