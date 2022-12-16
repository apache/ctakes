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
package org.apache.ctakes.dictionary.lookup2.dictionary;

import org.apache.ctakes.dictionary.lookup2.concept.ConceptFactory;
import org.apache.ctakes.dictionary.lookup2.consumer.TermConsumer;
import org.apache.ctakes.dictionary.lookup2.util.DefaultDictionarySpec;
import org.apache.ctakes.dictionary.lookup2.util.DictionarySpec;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Parses the XML descriptor indicated by the {@code externalResource} for {@code DictionaryDescriptorFile}
 * in the XML descriptor for the Rare Word Term Lookup Annotator
 * {@link org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator}
 * </p>
 * If there is a problem with the descriptor then the whole pipeline goes down, so care must be taken by the User
 * and any messages (logged or otherwise) produced by this class should be as specific as possible.  Devs take notice.
 * <p/>
 * <p/>
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 11/20/13
 */
final public class DictionaryDescriptorParser {

   // LOG4J logger based on class name
   static private final Logger LOGGER = Logger.getLogger( "DictionaryDescriptorParser" );

   /**
    * A <B>Utility Class</B> cannot be instantiated
    */
   private DictionaryDescriptorParser() {
   }


   static private final Object[] EMPTY_OBJECT_ARRAY = new Object[ 0 ];

   /**
    * XML keys specifying the main sections that define dictionaries, concept factories, and the pairing of the two
    */
   static private final String DICTIONARIES_KEY = "dictionaries";
   static private final String CONCEPT_FACTORIES_KEY = "conceptFactories";
   static private final String PAIRS_KEY = "dictionaryConceptPairs";
   /**
    * XML key specifying the section that defines the single
    * {@link org.apache.ctakes.dictionary.lookup2.consumer.TermConsumer} that should be used to consume discovered terms.
    */
   static private final String CONSUMER_KEY = "rareWordConsumer";

   /**
    * Each dictionary, concept factory, pairing and term consumer should have a unique name
    */
   static private final String NAME_KEY = "name";

   /**
    * Each {@link org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary},
    * {@link org.apache.ctakes.dictionary.lookup2.concept.ConceptFactory},
    * and {@link org.apache.ctakes.dictionary.lookup2.consumer.TermConsumer} must have a java implementation.
    */
   private static final String IMPLEMENTATION_KEY = "implementationName";

   /**
    * pairings must have specified dictionaries and concept factories
    */
   static private final String PAIR_DICTIONARY_NAME = "dictionaryName";
   static private final String PAIR_CONCEPT_FACTORY_NAME = "conceptFactoryName";

   /**
    * everything else is implementation-specific and therefore optional and therefore set as a property
    */
   static private final String PROPERTIES_KEY = "properties";

   // Added 'maxListSize'.  Size equals max int by default  - used for lucene dictionaries
   final private static int MAX_LIST_SIZE = Integer.MAX_VALUE; //ohnlp-Bugs-3296301

   /**
    * Initiates the parsing of the XML descriptor file containing definition of dictionaries and a consumer for the
    * Rare Word Term dictionary paradigm
    *
    * @param descriptorFile XML-formatted file, see the dictionary-lookup resources file {@code RareWordTermsUMLS.xml}
    *                       for an example
    * @param uimaContext    -
    * @return {@link org.apache.ctakes.dictionary.lookup2.util.DefaultDictionarySpec} with specification of dictionaries and a consumer as read from the
    * {@code descriptorFile}
    * @throws AnnotatorContextException if the File could not be found/read or the xml could not be parsed
    */
   static public DictionarySpec parseDescriptor( final InputStream descriptorFile, final UimaContext uimaContext )
         throws AnnotatorContextException {
      LOGGER.info( "Parsing dictionary specifications: ");
      final SAXBuilder saxBuilder = new SAXBuilder();
      Document doc;
      try {
         doc = saxBuilder.build( descriptorFile );
      } catch ( JDOMException | IOException jdomioE ) {
         throw new AnnotatorContextException(
               "Could not parse ", EMPTY_OBJECT_ARRAY, jdomioE );
      }
      final Map<String, RareWordDictionary> dictionaries
            = parseDictionaries( uimaContext, doc.getRootElement().getChild( DICTIONARIES_KEY ) );
      final Map<String, ConceptFactory> conceptFactories
            = parseConceptFactories( uimaContext, doc.getRootElement().getChild( CONCEPT_FACTORIES_KEY ) );
      final Map<String, String> pairDictionaryNames
            = parsePairingNames( doc.getRootElement().getChild( PAIRS_KEY ), PAIR_DICTIONARY_NAME );
      final Map<String, String> pairConceptFactoryNames
            = parsePairingNames( doc.getRootElement().getChild( PAIRS_KEY ), PAIR_CONCEPT_FACTORY_NAME );
      final TermConsumer consumer = parseConsumerXml( uimaContext, doc.getRootElement().getChild( CONSUMER_KEY ) );
      return new DefaultDictionarySpec( pairDictionaryNames, pairConceptFactoryNames, dictionaries, conceptFactories,
            consumer );
   }

   /**
    * Creates dictionary engines by parsing the section defined by {@link this.DICTIONARIES_KEY}
    *
    * @param uimaContext         -
    * @param dictionariesElement contains definition of all dictionaries
    * @return Mapping of dictionary names to new {@link RareWordDictionary} instances
    * @throws AnnotatorContextException if the resource specified by {@link this.EXTERNAL_RESOURCE} does not match
    *                                   the type specified by {@link this.IMPLEMENTATION} or for some reason could not be used
    */
   static private Map<String, RareWordDictionary> parseDictionaries( final UimaContext uimaContext,
                                                                     final Element dictionariesElement )
         throws AnnotatorContextException {
      final Map<String, RareWordDictionary> dictionaries = new HashMap<>();
      final Collection dictionaryElements = dictionariesElement.getChildren();
      for ( Object dictionaryElement : dictionaryElements ) {
         if ( dictionaryElement instanceof Element ) {
            final RareWordDictionary dictionary = parseDictionary( uimaContext, (Element)dictionaryElement );
            if ( dictionary != null ) {
               dictionaries.put( dictionary.getName(), dictionary );
            }
         }
      }
      return dictionaries;
   }


   /**
    * Creates a dictionary by parsing each child element of {@link this.DICTIONARIES_KEY}
    *
    * @param uimaContext       -
    * @param dictionaryElement contains the definition of a single dictionary
    * @return a dictionary or null if there is a problem
    * @throws AnnotatorContextException if any of a dozen things goes wrong
    */
   private static RareWordDictionary parseDictionary( final UimaContext uimaContext, final Element dictionaryElement )
         throws AnnotatorContextException {
      final Class[] constructionArgs = { String.class, UimaContext.class, Properties.class };

      final String name = getName( "Dictionary Name", dictionaryElement );
      final String className = dictionaryElement.getChildText( IMPLEMENTATION_KEY ).trim();
      final Element propertiesElement = dictionaryElement.getChild( PROPERTIES_KEY );
      final Properties properties = parsePropertiesXml( propertiesElement );
      Class dictionaryClass;
      try {
         dictionaryClass = Class.forName( className );
      } catch ( ClassNotFoundException cnfE ) {
         throw new AnnotatorContextException( "Unknown class " + className, EMPTY_OBJECT_ARRAY, cnfE );
      }
      if ( !RareWordDictionary.class.isAssignableFrom( dictionaryClass ) ) {
         throw new AnnotatorContextException( className + " is not a Rare Word Dictionary", EMPTY_OBJECT_ARRAY );
      }
      final Constructor[] constructors = dictionaryClass.getConstructors();
      for ( Constructor constructor : constructors ) {
         try {
            if ( Arrays.equals( constructionArgs, constructor.getParameterTypes() ) ) {
               final Object[] args = new Object[] { name, uimaContext, properties };
               return (RareWordDictionary)constructor.newInstance( args );
            }
         } catch ( InstantiationException | IllegalAccessException | InvocationTargetException iniaitE ) {
            throw new AnnotatorContextException( "Could not construct " + className, EMPTY_OBJECT_ARRAY, iniaitE );
         }
      }
      throw new AnnotatorContextException( "No Constructor for " + className, EMPTY_OBJECT_ARRAY );
   }


   /**
    * Creates concept factories by parsing the section defined by {@link this.CONCEPT_FACTORY_KEY
    *
    * @param uimaContext             -
    * @param conceptFactoriesElement contains definition of all concept factories
    * @return Mapping of concept factory names to new {@link ConceptFactory} instances
    * @throws AnnotatorContextException if the resource specified by {@link this.EXTERNAL_RESOURCE} does not match
    *                                   the type specified by {@link this.IMPLEMENTATION} or for some reason could not be used
    */
   static private Map<String, ConceptFactory> parseConceptFactories( final UimaContext uimaContext,
                                                                     final Element conceptFactoriesElement )
         throws AnnotatorContextException {
      final Map<String, ConceptFactory> conceptFactories = new HashMap<>();
      final Collection conceptFactoryElements = conceptFactoriesElement.getChildren();
      for ( Object conceptFactoryElement : conceptFactoryElements ) {
         if ( conceptFactoryElement instanceof Element ) {
            final ConceptFactory conceptFactory = parseConceptFactory( uimaContext, (Element)conceptFactoryElement );
            if ( conceptFactory != null ) {
               conceptFactories.put( conceptFactory.getName(), conceptFactory );
            }
         }
      }
      return conceptFactories;
   }

   /**
    * Creates a dictionary by parsing each child element of {@link this.DICTIONARIES_KEY}
    *
    * @param uimaContext           -
    * @param conceptFactoryElement contains the definition of a single dictionary
    * @return a dictionary or null if there is a problem
    * @throws AnnotatorContextException if any of a dozen things goes wrong
    */
   private static ConceptFactory parseConceptFactory( final UimaContext uimaContext,
                                                      final Element conceptFactoryElement )
         throws AnnotatorContextException {
      final Class[] constructionArgs = { String.class, UimaContext.class, Properties.class };
      final String name = getName( "Concept Factory Name", conceptFactoryElement );
      final String className = conceptFactoryElement.getChildText( IMPLEMENTATION_KEY ).trim();
      final Element propertiesElement = conceptFactoryElement.getChild( PROPERTIES_KEY );
      final Properties properties = parsePropertiesXml( propertiesElement );
      Class conceptFactoryClass;
      try {
         conceptFactoryClass = Class.forName( className );
      } catch ( ClassNotFoundException cnfE ) {
         throw new AnnotatorContextException( "Unknown class " + className, EMPTY_OBJECT_ARRAY, cnfE );
      }
      if ( !ConceptFactory.class.isAssignableFrom( conceptFactoryClass ) ) {
         throw new AnnotatorContextException( className + " is not a Concept Factory", EMPTY_OBJECT_ARRAY );
      }
      final Constructor[] constructors = conceptFactoryClass.getConstructors();
      for ( Constructor constructor : constructors ) {
         try {
            if ( Arrays.equals( constructionArgs, constructor.getParameterTypes() ) ) {
               final Object[] args = new Object[] { name, uimaContext, properties };
               return (ConceptFactory)constructor.newInstance( args );
            }
         } catch ( InstantiationException | IllegalAccessException | InvocationTargetException iniaitE ) {
            throw new AnnotatorContextException( "Could not construct " + className, EMPTY_OBJECT_ARRAY, iniaitE );
         }
      }
      throw new AnnotatorContextException( "No Constructor for " + className, EMPTY_OBJECT_ARRAY );
   }


   /**
    * @param pairingsElement -
    * @param pairingName     one of "dictionaryName" or "conceptFactoryName"
    * @return -
    * @throws AnnotatorContextException -
    */
   static private Map<String, String> parsePairingNames( final Element pairingsElement, final String pairingName )
         throws AnnotatorContextException {
      final Map<String, String> pairConceptFactoryNames = new HashMap<>();
      final Collection pairingElements = pairingsElement.getChildren();
      for ( Object pairingElement : pairingElements ) {
         if ( pairingElement instanceof Element ) {
            final String pairName = getName( "Dictionary - Concept Factory Pairing", (Element)pairingElement );
            final String conceptFactorName = ((Element)pairingElement).getChildText( pairingName );
            pairConceptFactoryNames.put( pairName, conceptFactorName );
         }
      }
      return pairConceptFactoryNames;
   }

   static private String getName( final String elementName, final Element element ) throws AnnotatorContextException {
      final String name = element.getChildText( NAME_KEY );
      if ( name == null || name.isEmpty() ) {
         throw new AnnotatorContextException( "Missing name for " + elementName, EMPTY_OBJECT_ARRAY );
      }
      return name;
   }

   /**
    * Creates a term consumer by parsing section defined by {@link this.CONSUMER_KEY}
    *
    * @param uimaContext           -
    * @param lookupConsumerElement contains the definition of the term consumer
    * @return a term consumer
    * @throws AnnotatorContextException if any of a dozen things goes wrong
    */
   private static TermConsumer parseConsumerXml( final UimaContext uimaContext,
                                                 final Element lookupConsumerElement ) throws
                                                                                       AnnotatorContextException {
      Class[] constrArgsConsum = { UimaContext.class, Properties.class, int.class };//ohnlp-Bugs-3296301
      Class[] constrArgsConsumB = { UimaContext.class, Properties.class };

      String consumerClassName = lookupConsumerElement.getChildText( IMPLEMENTATION_KEY ).trim();
      Element consumerPropertiesElement = lookupConsumerElement.getChild( PROPERTIES_KEY );
      Properties consumerProperties = parsePropertiesXml( consumerPropertiesElement );
      Class consumerClass;
      try {
         consumerClass = Class.forName( consumerClassName );
      } catch ( ClassNotFoundException cnfE ) {
         throw new AnnotatorContextException( "Unknown class " + consumerClassName, EMPTY_OBJECT_ARRAY, cnfE );
      }
      if ( !TermConsumer.class.isAssignableFrom( consumerClass ) ) {
         throw new AnnotatorContextException( consumerClassName + " is not a TermConsumer", EMPTY_OBJECT_ARRAY );
      }
      final Constructor[] constructors = consumerClass.getConstructors();
      for ( Constructor constructor : constructors ) {
         try {
            if ( Arrays.equals( constrArgsConsum, constructor.getParameterTypes() ) ) {
               final Object[] args = new Object[] { uimaContext, consumerProperties,
                                                    MAX_LIST_SIZE }; //ohnlp-Bugs-3296301
               return (TermConsumer)constructor.newInstance( args );
            } else if ( Arrays.equals( constrArgsConsumB, constructor.getParameterTypes() ) ) {
               final Object[] args = new Object[] { uimaContext, consumerProperties };
               return (TermConsumer)constructor.newInstance( args );
            }
         } catch ( InstantiationException | IllegalAccessException | InvocationTargetException multE ) {
            throw new AnnotatorContextException(
                  "Could not construct " + consumerClassName, EMPTY_OBJECT_ARRAY, multE );
         }
      }
      throw new AnnotatorContextException( "No Constructor for " + consumerClassName, EMPTY_OBJECT_ARRAY );
   }

   /**
    * Builds a collection of key, value properties
    *
    * @param propertiesElement element with key, value pairs
    * @return Properties
    */
   private static Properties parsePropertiesXml( final Element propertiesElement ) {
      final Properties properties = new Properties();
      final Collection propertyElements = propertiesElement.getChildren();
      for ( Object value : propertyElements ) {
         final Element propertyElement = (Element)value;
         final String key = propertyElement.getAttributeValue( "key" );
         final String propertyValue = propertyElement.getAttributeValue( "value" );
         properties.put( key, propertyValue );
      }
      return properties;
   }

}
