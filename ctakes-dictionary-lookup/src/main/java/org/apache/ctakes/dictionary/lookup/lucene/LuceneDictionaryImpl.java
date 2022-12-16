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
package org.apache.ctakes.dictionary.lookup.lucene;

import org.apache.ctakes.dictionary.lookup.AbstractBaseDictionary;
import org.apache.ctakes.dictionary.lookup.Dictionary;
import org.apache.ctakes.dictionary.lookup.DictionaryException;
import org.apache.ctakes.dictionary.lookup.MetaDataHit;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * @author Mayo Clinic
 */
public class LuceneDictionaryImpl extends AbstractBaseDictionary {
   final private IndexSearcher iv_searcher;
   final private String iv_lookupFieldName;
   //ohnlp-Bugs-3296301 limits the search results to fixed 100 records.
   private int iv_maxHits;
   // LOG4J logger based on class name
   private Logger iv_logger = Logger.getLogger( getClass().getName() );

   /**
    * Constructor
    */
   public LuceneDictionaryImpl( final IndexSearcher searcher, final String lookupFieldName ) {
      this( searcher, lookupFieldName, Integer.MAX_VALUE );

      // TODO Only take perfect matches?
   }

   /**
    * Constructor
    */
   public LuceneDictionaryImpl( final IndexSearcher searcher, final String lookupFieldName, final int maxListHits ) {
      iv_searcher = searcher;
      iv_lookupFieldName = lookupFieldName;
      // Added 'maxListHits'
      iv_maxHits = maxListHits;
      // TODO Only take perfect matches?
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<MetaDataHit> getEntries( final String text ) throws DictionaryException {
      final Set<MetaDataHit> metaDataHitSet = new HashSet<>();

      try {
         Query q = null;
         TopDocs topDoc = null;
         if ( text.indexOf( '-' ) == -1 ) {
            q = new TermQuery( new Term( iv_lookupFieldName, text ) );
            topDoc = iv_searcher.search( q, iv_maxHits );
         } else {  // needed the KeyworkAnalyzer for situations where the hypen was included in the f-word
            final QueryParser query = new QueryParser( Version.LUCENE_40, iv_lookupFieldName, new KeywordAnalyzer() );
            try {
               //CTAKES-63 - I believe all of the chars in the str token should be escaped to avoid issues such as a token ending with ']'
               //topDoc = iv_searcher.search(query.parse(text.replace('-', ' ')), iv_maxHits);
               final String escaped = QueryParserBase.escape( text.replace( '-', ' ' ) );
               topDoc = iv_searcher.search( query.parse( escaped ), iv_maxHits );
            } catch ( ParseException e ) {
               // thrown by QueryParser.parse()
               // TODO Auto-generated catch block
               e.printStackTrace();
            }
         }
         if ( topDoc == null ) {
            // avoids possible NPE on topDoc.scoreDocs 12-26-2012 SPF
            iv_logger.warn( getClass().getName() + " getEntries(..) topDoc is null, returning empty collection" );
            return Collections.emptySet();
         }
         if ( iv_maxHits == 0 ) {
            iv_maxHits = Integer.MAX_VALUE;
            iv_logger.warn( "iv_maxHits was 0, using Integer.MAX_VALUE instead" );
         }
         final ScoreDoc[] hits = topDoc.scoreDocs;
         if ( hits.length == iv_maxHits ) {
            iv_logger.warn( "'iv_maxHits' equals the list length returned by the lucene query (" + hits.length + ")." );
            iv_logger.warn(
                  "You may want to consider setting a higher value, since there may be more entries not being returned in the event greater than "
                        + iv_maxHits + " exist." );
         }
         for ( ScoreDoc scoreDoc : hits ) {
            final Document luceneDoc = iv_searcher.doc( scoreDoc.doc );
            final MetaDataHit mdh = new LuceneDocumentMetaDataHitImpl( luceneDoc );
            metaDataHitSet.add( mdh );
         }
         return metaDataHitSet;
      } catch ( IOException ioe ) {
         // thrown by IndexSearcher.search(), IndexSearcher.doc()
         throw new DictionaryException( ioe );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean contains( final String text ) throws DictionaryException {
      try {
         final Query q = new TermQuery( new Term( iv_lookupFieldName, text ) );

         final TopDocs topDoc = iv_searcher.search( q, iv_maxHits );
         final ScoreDoc[] hits = topDoc.scoreDocs;
         return hits != null && hits.length > 0;
      } catch ( IOException ioe ) {
         // thrown by IndexSearcher.search()
         throw new DictionaryException( ioe );
      }

   }
}
