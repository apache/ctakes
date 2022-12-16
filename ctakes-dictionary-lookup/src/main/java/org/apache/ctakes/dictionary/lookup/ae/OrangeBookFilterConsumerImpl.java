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
package org.apache.ctakes.dictionary.lookup.ae;

import org.apache.ctakes.core.resource.LuceneIndexReaderResource;
import org.apache.ctakes.dictionary.lookup.MetaDataHit;
import org.apache.ctakes.dictionary.lookup.vo.LookupHit;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.MedicationMention;
import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceAccessException;

import java.io.IOException;
import java.util.*;

/**
 * Implementation that takes Rxnorm dictionary lookup hits and stores only the
 * ones that are also present in the Orange Book.
 *
 * @author Mayo Clinic
 */
public class OrangeBookFilterConsumerImpl extends BaseLookupConsumerImpl {
   // LOG4J logger based on class name
   private final Logger iv_logger = Logger.getLogger( getClass().getName() );

   static private final String CODE_MF_PRP_KEY = "codeMetaField";

   static private final String CODING_SCHEME_PRP_KEY = "codingScheme";

   static private final String LUCENE_FILTER_RESRC_KEY_PRP_KEY = "luceneFilterExtResrcKey";

   final private Properties _properties;

   final private IndexSearcher _indexSearcher;
   //ohnlp-Bugs-3296301 limits the search results to fixed 100 records.
   // Added 'MaxListSize'
   final private int _maxListSize;

   public OrangeBookFilterConsumerImpl( final UimaContext aCtx, final Properties props, final int maxListSize )
         throws ResourceAccessException, NullPointerException {
      // TODO property validation could be done here
      _properties = props;
      _maxListSize = maxListSize;
      final String resrcName = _properties.getProperty( LUCENE_FILTER_RESRC_KEY_PRP_KEY );
      // UimaContext.getResourceObject(..) throws ResourceAccessException
      final LuceneIndexReaderResource resrc = (LuceneIndexReaderResource) aCtx.getResourceObject( resrcName );
      // Possible npE with resrc.getIndexReader()
      _indexSearcher = new IndexSearcher( resrc.getIndexReader() );
   }

   public OrangeBookFilterConsumerImpl( final UimaContext aCtx, final Properties props )
         throws Exception {
      this( aCtx, props, Integer.MAX_VALUE );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void consumeHits( final JCas jcas, final Iterator<LookupHit> lhItr ) throws AnalysisEngineProcessException {
      final String CODE_MF = _properties.getProperty( CODE_MF_PRP_KEY );
      final Map<LookupHitKey, Set<LookupHit>> lookupHitMap = createLookupHitMap( lhItr );
      for ( Map.Entry<LookupHitKey, Set<LookupHit>> entry : lookupHitMap.entrySet() ) {
         // iterate over the LookupHit objects
         // code is only valid if the covered text is also present in the filter
         final int neBegin = entry.getKey().__start;
         final int neEnd = entry.getKey().__end;
         final String text = jcas.getDocumentText().substring( neBegin, neEnd ).trim().toLowerCase();
         final boolean isValid = isValid( "trade_name", text ) || isValid( "ingredient", text );
         if ( isValid ) {
            final Set<String> validCodes = new HashSet<>();
            for ( LookupHit lookupHit : entry.getValue() ) {
               final MetaDataHit mdh = lookupHit.getDictMetaDataHit();
               final String code = mdh.getMetaFieldValue( CODE_MF );
               validCodes.add( code );
            }
            final FSArray ocArr = createOntologyConceptArr( jcas, validCodes );
            IdentifiedAnnotation neAnnot = new MedicationMention( jcas ); // medication NEs are EventMention
            neAnnot.setTypeID( CONST.NE_TYPE_ID_DRUG );
            neAnnot.setBegin( neBegin );
            neAnnot.setEnd( neEnd );
            neAnnot.setDiscoveryTechnique( CONST.NE_DISCOVERY_TECH_DICT_LOOKUP );
            neAnnot.setOntologyConceptArr( ocArr );
            neAnnot.addToIndexes();
         } else {
            iv_logger.warn( "Filtered out: " + text );
         }
      }
   }

   /**
    * For each valid code, a corresponding JCas OntologyConcept object is
    * created and stored in a FSArray.
    *
    * @param jcas       -
    * @param validCodes -
    * @return -
    */
   private FSArray createOntologyConceptArr( final JCas jcas, final Collection<String> validCodes ) {
      final String CODING_SCHEME = _properties.getProperty( CODING_SCHEME_PRP_KEY );
      final FSArray ocArr = new FSArray( jcas, validCodes.size() );
      int ocArrIdx = 0;
      for ( String validCode : validCodes ) {
         final OntologyConcept oc = new OntologyConcept( jcas );
         oc.setCode( validCode );
         oc.setCodingScheme( CODING_SCHEME );
         ocArr.set( ocArrIdx, oc );
         ocArrIdx++;
      }
      return ocArr;
   }

   private boolean isValid( final String fieldName, final String text ) throws AnalysisEngineProcessException {
      try {
         final Query q = new TermQuery( new Term( fieldName, text ) );
         final TopDocs topDoc = _indexSearcher.search( q, _maxListSize );
         final ScoreDoc[] hits = topDoc.scoreDocs;
         return hits != null && hits.length > 0;
      } catch ( IOException ioE ) {
         // thrown by IndexSearcher.search(..)
         throw new AnalysisEngineProcessException( ioE );
      }
   }
}
