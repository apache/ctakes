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
import org.apache.ctakes.core.resource.JdbcConnectionResource;
import org.apache.ctakes.core.resource.LuceneIndexReaderResource;
import org.apache.ctakes.dictionary.lookup.Dictionary;
import org.apache.ctakes.dictionary.lookup.DictionaryEngine;
import org.apache.ctakes.dictionary.lookup.algorithms.LookupAlgorithm;
import org.apache.ctakes.dictionary.lookup.filter.StringPreLookupFilterImpl;
import org.apache.ctakes.dictionary.lookup.jdbc.JdbcDictionaryImpl;
import org.apache.ctakes.dictionary.lookup.lucene.LuceneDictionaryImpl;
import org.apache.ctakes.dictionary.lookup.strtable.StringTable;
import org.apache.ctakes.dictionary.lookup.strtable.StringTableDictionaryImpl;
import org.apache.ctakes.dictionary.lookup.strtable.StringTableFactory;
import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.resource.ResourceAccessException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.util.*;


// TODO Finish this refactor

/**
 * @author Mayo Clinic
 */
final public class LookupParseUtilitiesRefactor {

   static private final Logger CLASS_LOGGER = Logger.getLogger( LookupParseUtilitiesRefactor.class );

   private LookupParseUtilitiesRefactor() {}

   //returns a set of LookupSpec objects
   public static Set<LookupSpec> parseDescriptor( final File descFile, final UimaContext aContext, final int maxListSize )
         throws JDOMException, IOException, AnnotatorContextException, ResourceAccessException {
      final SAXBuilder saxBuilder = new SAXBuilder();
      final Document doc = saxBuilder.build( descFile );
      MAX_LIST_SIZE = maxListSize;   //ohnlp-Bugs-3296301 fixes limit the search results to fixed 100 records.
      final Map<String,DictionaryEngine> dictMap = parseDictionaries( aContext,
                                                                      doc.getRootElement().getChild( "dictionaries" ) );
      //ohnlp-Bugs-3296301
      return parseLookupBindingXml( aContext, dictMap, doc.getRootElement().getChild( "lookupBindings" ) );
   }

   public static Set<LookupSpec> parseDescriptor( final File descFile, final UimaContext aContext )
         throws JDOMException, IOException, AnnotatorContextException, ResourceAccessException {
      return parseDescriptor( descFile, aContext, Integer.MAX_VALUE );
   }

   private static Map<String,DictionaryEngine> parseDictionaries( final UimaContext aContext,
                                                                  final Element dictetteersEl )
         throws AnnotatorContextException, ResourceAccessException {
      final Map<String,DictionaryEngine> m = new HashMap<>();
      final List<Element> dictatteerChildren = dictetteersEl.getChildren();
      for ( Element dictEl : dictatteerChildren ) {
         final String id = dictEl.getAttributeValue( "id" );
         final DictionaryEngine dictEngine = LookupParseUtilitiesRefactor.parseDictionaryXml( aContext, dictEl );
         m.put( id, dictEngine );
      }
      return m;
   }

   private static DictionaryEngine parseDictionaryXml( final UimaContext annotCtx, final Element rootDictEl )
         throws ResourceAccessException {
      final String extResrcKey = rootDictEl.getAttributeValue( "externalResourceKey" );
      // UimaContext.getResourceObject(..) throws ResourceAccessException
      final Object extResrc = annotCtx.getResourceObject( extResrcKey );
      if ( extResrc == null ) {
         throw new ResourceAccessException( "Unable to find external resource with key:" + extResrcKey, null );
      }

      final Element lookupFieldEl = rootDictEl.getChild( "lookupField" );
      final String lookupFieldName = lookupFieldEl.getAttributeValue( "fieldName" );

      Dictionary dict;
      try {
         if (rootDictEl.getChild( "implementation" ).getChildren().isEmpty() ) {
            throw new ResourceAccessException( new IndexOutOfBoundsException() );
         }
         final Element implEl = (Element) rootDictEl.getChild( "implementation" ).getChildren().get( 0 );
         final String implType = implEl.getName();
         if ( implType.equals( "luceneImpl" ) ) {
            if ( !(extResrc instanceof LuceneIndexReaderResource) ) {
               throw new ResourceAccessException( "Expected external resource to be:"
                                          + LuceneIndexReaderResource.class, new Object[]{extResrc} );
            }
            final IndexReader indexReader = ((LuceneIndexReaderResource) extResrc).getIndexReader();
            final IndexSearcher indexSearcher = new IndexSearcher( indexReader );
            // Added 'MaxListSize' ohnlp-Bugs-3296301
            dict = new LuceneDictionaryImpl( indexSearcher, lookupFieldName, MAX_LIST_SIZE );
         } else if ( implType.equals( "jdbcImpl" ) ) {
            final String tableName = implEl.getAttributeValue( "tableName" );
            if ( !(extResrc instanceof JdbcConnectionResource) ) {
               throw new ResourceAccessException( "Expected external resource to be:"
                                          + JdbcConnectionResource.class, new Object[]{extResrc} );
            }
            final Connection conn = ((JdbcConnectionResource) extResrc).getConnection();
            dict = new JdbcDictionaryImpl( conn, tableName, lookupFieldName );
         } else if ( implType.equals( "csvImpl" ) ) {
            final String fieldDelimiter = implEl.getAttributeValue( "delimiter" );
            if ( !(extResrc instanceof FileResource) ) {
               throw new ResourceAccessException( "Expected external resource to be:"
                                          + FileResource.class, new Object[]{extResrc} );
            }

            final String idxFieldNameStr = implEl.getAttributeValue( "indexedFieldNames" );
            final StringTokenizer st = new StringTokenizer( idxFieldNameStr, "," );
            int arrIdx = 0;
            String[] idxFieldNameArr = new String[st.countTokens()];
            while ( st.hasMoreTokens() ) {
               idxFieldNameArr[arrIdx++] = st.nextToken().trim();
            }

            final File csvFile = ((FileResource) extResrc).getFile();
            try {
               final StringTable strTable = StringTableFactory.build( new FileReader( csvFile ),
                     fieldDelimiter, idxFieldNameArr, true );
               dict = new StringTableDictionaryImpl( strTable, lookupFieldName );
            } catch ( FileNotFoundException fnfE ) {
               throw new ResourceAccessException( "Could not open csv file", new Object[]{csvFile} );
            } catch (IOException ioE ) {
               throw new ResourceAccessException( "Could not open csv file", new Object[]{csvFile} );
            }
         } else {
            throw new ResourceAccessException( "Unsupported impl type:" + implType, new Object[]{implType} );
         }

         final List<Element> rootDictChildren = rootDictEl.getChild( "metaFields" ).getChildren();
         for ( Element metaFieldEl : rootDictChildren ) {
            final String metaFieldName = metaFieldEl.getAttributeValue( "fieldName" );
            dict.retainMetaData( metaFieldName );
         }
      } catch ( NullPointerException npE ) {
         // thrown all over this method ...
         throw new ResourceAccessException( npE );
      }
      final boolean keepCase = Boolean.parseBoolean( rootDictEl.getAttributeValue( "caseSensitive" ) );
      final DictionaryEngine dictEngine = new DictionaryEngine( dict, keepCase );
      final Element excludeList = rootDictEl.getChild( "excludeList" );
      if ( excludeList != null && excludeList.getChildren() != null && !excludeList.getChildren().isEmpty() ) {
         addExcludeList( dictEngine, excludeList.getChildren() );
      }
      return dictEngine;
   }


   /*
    * Word(s) not to look up
    * TODO Consider adding common words as possible performance improvement
    */
   private static void addExcludeList( final DictionaryEngine dictionaryEngine, final List<Element> elementList ) {
      final Set<String> excludeValues = new HashSet<>( elementList.size() );
      for ( Element item : elementList ) {
         final String excludeValue = item.getAttributeValue( "value" );
         CLASS_LOGGER.info( "Adding exclude value[" + excludeValue + "]" );
         excludeValues.add( excludeValue );
      }
      final StringPreLookupFilterImpl filter = new StringPreLookupFilterImpl( excludeValues );
      dictionaryEngine.addPreLookupFilter( filter );
   }


   private static Set<LookupSpec> parseLookupBindingXml( final UimaContext annotCtx,
                                                         final Map<String,DictionaryEngine> dictMap,
                                                         final Element lookupBindingsEl )
         throws AnnotatorContextException {
      final Class<?>[] constrArgs = {UimaContext.class, Properties.class};
      final Class<?>[] constrArgsConsum = {UimaContext.class, Properties.class, int.class};//ohnlp-Bugs-3296301
      final Class<?>[] constrArgsConsumB = {UimaContext.class, Properties.class};

      final Set<LookupSpec> lsSet = new HashSet<>();
      final List<Element> bindingChildren = lookupBindingsEl.getChildren();
      try {
         for ( Element bindingEl : bindingChildren ) {
            final Element dictEl = bindingEl.getChild( "dictionaryRef" );
            final String dictID = dictEl.getAttributeValue( "idRef" );
            final DictionaryEngine dictEngine = dictMap.get( dictID );
            if ( dictEngine == null ) {
               throw new AnnotatorContextException( "Dictionary undefined: " + dictID, null );
            }

            final Element lookupInitEl = bindingEl.getChild( "lookupInitializer" );
            final String liClassName = lookupInitEl.getAttributeValue( "className" );
            final Element liPropertiesEl = lookupInitEl.getChild( "properties" );
            final Properties liProps = parsePropertiesXml( liPropertiesEl );
            final Class<?> liClass = Class.forName( liClassName );
            final Constructor<?> liConstr = liClass.getConstructor( constrArgs );
            final Object[] liArgs = {annotCtx, liProps};
            final LookupInitializer li = (LookupInitializer) liConstr.newInstance( liArgs );

            final Element lookupConsumerEl = bindingEl.getChild( "lookupConsumer" );
            final String lcClassName = lookupConsumerEl.getAttributeValue( "className" );
            final Element lcPropertiesEl = lookupConsumerEl.getChild( "properties" );
            final Properties lcProps = parsePropertiesXml( lcPropertiesEl );
            final Class<?> lcClass = Class.forName( lcClassName );
            final Constructor<?>[] consts = lcClass.getConstructors();
            Constructor<?> lcConstr = null;
            Object[] lcArgs = null;
            for ( Constructor<?> constConstr : consts ) {
               lcConstr = constConstr;
               if ( Arrays.equals( constrArgsConsum, lcConstr.getParameterTypes() ) ) {
                  lcConstr = lcClass.getConstructor( constrArgsConsum );
                  lcArgs = new Object[]{annotCtx, lcProps, MAX_LIST_SIZE};//ohnlp-Bugs-3296301
               } else if ( Arrays.equals( constrArgsConsumB, lcConstr.getParameterTypes() ) ) {
                  lcConstr = lcClass.getConstructor( constrArgsConsumB );
                  lcArgs = new Object[]{annotCtx, lcProps};
               }
            }

            final LookupConsumer lc = (LookupConsumer) lcConstr.newInstance( lcArgs );
            final LookupAlgorithm la = li.getLookupAlgorithm( dictEngine );

            final LookupSpec ls = new LookupSpec( la, li, lc );

            lsSet.add( ls );
         }
         // TODO refactor to catch ( ex1 | ex2 | ex3 ) when cTakes moves to java 7
      } catch ( ClassNotFoundException cnfE ) {
         // thrown by Class.forName(..)
         throw new AnnotatorContextException( cnfE );
      } catch ( NoSuchMethodException nsmE ) {
         // thrown by Class.getConstructor(..)
         throw new AnnotatorContextException( nsmE );
      } catch ( SecurityException secE ) {
         // thrown by Class.getConstructor(..)
         throw new AnnotatorContextException( secE );
      } catch ( InstantiationException instE ) {
         // thrown by Class.newInstance(..)
         throw new AnnotatorContextException( instE );
      } catch ( IllegalAccessException iaE ) {
         // thrown by Class.newInstance(..)
         throw new AnnotatorContextException( iaE );
      } catch ( InvocationTargetException itE ) {
         // thrown by Class.newInstance(..)
         throw new AnnotatorContextException( itE );
      } catch ( AnnotatorInitializationException aiE ) {
         // thrown by LookupInitializer.getLookupAlgorithm(..)
         throw new AnnotatorContextException( aiE );
      } catch ( ClassCastException ccE ) {
         // thrown everywhere in this method ...
         throw new AnnotatorContextException( ccE );
      } catch ( NullPointerException npE ) {
         // thrown everywhere in this method ...
         throw new AnnotatorContextException( npE );
      }
      return lsSet;
   }

//   /**
//    * Get the maximum list size to be returned from a lucene index
//    *
//    * @return MAX_LIST_SIZE
//    */
//   public static int getMaxSizeList() {
//      return MAX_LIST_SIZE;
//   }
//
//   /**
//    * Set the maximum list size to be returned from a lucene index
//    *
//    * @return MAX_LIST_SIZE
//    */
//   public static void setMaxSizeList( int maxListSize ) {
//      MAX_LIST_SIZE = maxListSize;
//   }

   private static Properties parsePropertiesXml( final Element propsEl ) {
      final Properties props = new Properties();
      final List<Element> propertyChildren = propsEl.getChildren();
      for ( Element propEl : propertyChildren ) {
         final String key = propEl.getAttributeValue( "key" );
         final String value = propEl.getAttributeValue( "value" );
         props.put( key, value );
      }
      return props;
   }

   // Added 'maxListSize'.  Size equals max int by default
   private static int MAX_LIST_SIZE = Integer.MAX_VALUE; //ohnlp-Bugs-3296301

}
