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


import org.apache.ctakes.core.resource.FileResource;
import org.apache.ctakes.dictionary.lookup.DictionaryException;
import org.apache.ctakes.dictionary.lookup.MetaDataHit;
import org.apache.ctakes.dictionary.lookup.lucene.LuceneDictionaryImpl;
import org.apache.log4j.Logger;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;


/**
 * Implementation that takes UMLS dictionary lookup hits and stores as NamedEntity
 * objects only the ones that have a SNOMED synonym, by looking in a lucene index
 * for SNOMED codes that map to the identified CUI.
 *
 * @author Mayo Clinic
 */
public class UmlsToSnomedLuceneConsumerImpl extends UmlsToSnomedConsumerImpl {

   // LOG4J logger based on class name
   private Logger logger = Logger.getLogger( getClass().getName() );

   //ohnlp-Bugs-3296301 limits the search results to fixed 100 records.
   // Added 'MaxListSize'
   private static int iv_maxListSize;
   private final String SNOMED_MAPPING_PRP_KEY = "snomedCodeMappingField";
   private final String CUI_MAPPING_PRP_KEY = "cuiMappingField";
   private final String SNOMED_CODE_LIST_CONFIG_PARM = "CodesListIndexDirectory";

   private LuceneDictionaryImpl snomedLikeCodesIndex;

   public UmlsToSnomedLuceneConsumerImpl( UimaContext aCtx, Properties properties )
         throws Exception {
      this( aCtx, properties, Integer.MAX_VALUE );
   }

   // ohnlp Bugs tracker ID: 3390078 do not reload lucene index for each document, load in constructor
   public UmlsToSnomedLuceneConsumerImpl( UimaContext aCtx, Properties properties, int maxListSize )
         throws Exception {
      super( aCtx, properties );
      iv_maxListSize = maxListSize;

      IndexReader indexReader;
      String indexDirAbsPath = null;
      try {

         // ohnlp Bugs tracker ID: 3425014 SNOMED lucene dictionary lookup hardcodes resource path
         FileResource fResrc = (FileResource) aCtx.getResourceObject( SNOMED_CODE_LIST_CONFIG_PARM );
         if ( fResrc == null ) {
            logger.error( "Unable to find config parm " + SNOMED_CODE_LIST_CONFIG_PARM + "." );
         }
         File indexDir = fResrc.getFile();
         indexDirAbsPath = indexDir.getAbsolutePath();

         try {
            logger.info( "Using lucene index: " + indexDir.getAbsolutePath() );
         } catch ( Exception e ) {
            throw new AnnotatorConfigurationException( e );
         }

         // For the sample dictionary, we use the following lucene index.
         //indexPath = "lookup/snomed-like_codes_sample";

         indexReader = DirectoryReader.open( FSDirectory.open( indexDir ) );

         IndexSearcher indexSearcher = new IndexSearcher( indexReader );
         String lookupFieldName = props.getProperty( CUI_MAPPING_PRP_KEY );

         // We will lookup entries based on lookupFieldName
         snomedLikeCodesIndex = new LuceneDictionaryImpl( indexSearcher, lookupFieldName, iv_maxListSize );

         logger.info( "Loaded Lucene index with " + indexReader.numDocs() + " entries." );

      } catch ( IOException ioe ) {
         logger.info( "Lucene index: " + indexDirAbsPath );
         throw new DictionaryException( ioe );
      }
   }


   /**
    * Find all Snomed codes that map to the given UMLS code (CUI),
    * by looking in a lucene index
    *
    * @param umlsCode a UMLS CUI
    * @return Set of Snomed codes that map to the given UMLS code (CUI).
    * @see getSnomedCodes in <code>UmlsToSnomedConsumerImpl</code> for example of using a database
    */
   @Override
  protected Set<String> getSnomedCodes( final String umlsCode ) throws DictionaryException {
      final Set<String> codeSet = new HashSet<>();
      final String valueFieldName = props.getProperty( SNOMED_MAPPING_PRP_KEY );
      // Get the entries with field lookupFieldName having value umlsCode
      final Collection<MetaDataHit> mdhCollection = snomedLikeCodesIndex.getEntries( umlsCode );
      for ( MetaDataHit mdh : mdhCollection ) {
         codeSet.add( mdh.getMetaFieldValue( valueFieldName ) );
      }
      return codeSet;

   }

}
