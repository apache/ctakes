/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.dependency.parser.ae;

import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.dependency.DEPFeat;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.morphology.AbstractMPAnalyzer;
import com.googlecode.clearnlp.reader.AbstractReader;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.dependency.parser.ae.shared.DependencySharedModel;
import org.apache.ctakes.dependency.parser.ae.shared.LemmatizerSharedModel;
import org.apache.ctakes.dependency.parser.util.ClearDependencyUtility;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.ArrayList;
import java.util.List;

/**
 * <br>
 * This class provides a UIMA wrapper for the CLEAR dependency parser. This parser is available here:
 * <p>
 * http://code.google.com/p/clearnlp
 * <p>
 * Please see
 * /ClearNLP-wrapper/resources/dependency/README
 * for important information pertaining to the models provided for this parser. In particular, note
 * that the output of the CLEAR parser is different than that of the Malt parser and so these two
 * parsers may not be interchangeable (without some effort) for most use cases.
 * <p>
 *
 *
 */
@TypeCapability(
      inputs = {
            "org.apache.ctakes.typesystem.type.syntax.BaseToken:partOfSpeech",
            "org.apache.ctakes.typesystem.type.syntax.BaseToken:normalizedForm",
            "org.apache.ctakes.typesystem.type.syntax.BaseToken:tokenNumber",
            "org.apache.ctakes.typesystem.type.syntax.BaseToken:end",
            "org.apache.ctakes.typesystem.type.syntax.BaseToken:begin"
      } )
@PipeBitInfo(
      name = "ClearNLP Dependency Parser",
      description = "Analyses Sentence Structure, storing information in nodes.",
      role = PipeBitInfo.Role.SPECIAL,
      dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.BASE_TOKEN },
      products = { PipeBitInfo.TypeProduct.DEPENDENCY_NODE }
)
public class ClearNLPDependencyParserAE extends JCasAnnotator_ImplBase {

   public final static Object LOCK = new Object();
   
   final String language = AbstractReader.LANG_EN;
   //  public Logger LOGGER = LoggerFactory.getLogger(getClass().getName());
   static private final Logger LOGGER = LoggerFactory.getLogger( ClearNLPDependencyParserAE.class.getSimpleName() );

   // single class-based model:
//   protected static ExternalResourceDescription defaultParserResource = ExternalResourceFactory.createExternalResourceDescription(
//         DependencySharedModel.class,
//         DependencySharedModel.DEFAULT_MODEL_FILE_NAME );
//   protected static ExternalResourceDescription defaultLemmatizerResource = ExternalResourceFactory.createExternalResourceDescription(
//         LemmatizerSharedModel.class,
//         LemmatizerSharedModel.ENG_LEMMATIZER_DATA_FILE );
   protected static ExternalResourceDescription defaultParserResource = ExternalResourceFactory.createSharedResourceDescription(
         DependencySharedModel.DEFAULT_MODEL_FILE_NAME, DependencySharedModel.class );
   protected static ExternalResourceDescription defaultLemmatizerResource = ExternalResourceFactory.createSharedResourceDescription(
         LemmatizerSharedModel.ENG_LEMMATIZER_DATA_FILE, LemmatizerSharedModel.class  );

   // Configuration Parameters
   @Deprecated
   public static final String PARAM_PARSER_MODEL_FILE_NAME = "ParserModelFileName";
   @Deprecated
   @ConfigurationParameter(
         name = PARAM_PARSER_MODEL_FILE_NAME,
         description = "This parameter provides the file name of the dependency parser model required " +
               "by the factory method provided by ClearNLPUtil.  If not specified, this " +
               "analysis engine will use a default model from the resources directory",
         defaultValue = DependencySharedModel.DEFAULT_MODEL_FILE_NAME )
   protected String parserModelPath;

   @Deprecated
   static private final String PARAM_LEMMATIZER_DATA_FILE = "LemmatizerDataFile";
   @Deprecated
   @ConfigurationParameter(
         name = PARAM_LEMMATIZER_DATA_FILE,
         description = "This parameter provides the data file required for the MorphEnAnalyzer. If not "
               + "specified, this analysis engine will use a default model from the resources directory",
         defaultValue = LemmatizerSharedModel.ENG_LEMMATIZER_DATA_FILE )
   private String lemmaDataPath;


   static private final String PARAM_USE_LEMMATIZER = "UseLemmatizer";
   @ConfigurationParameter(
         name = PARAM_USE_LEMMATIZER,
         defaultValue = "true",
         description = "If true, use the default ClearNLP lemmatizer, otherwise use lemmas from the BaseToken normalizedToken field" )
   private boolean useLemmatizer;

   public static final String PARAM_MAX_TOKENS = "MaxTokens";
   @ConfigurationParameter(name = PARAM_MAX_TOKENS,
           mandatory=false,
           description="The maximum length sentence to parse. Longer sentences will have a basic dependency structure created where every node's head is the sentence node.")
   private int maxTokens=-1;

   public static final String DEP_MODEL_KEY = "DepModel";
   @ExternalResource( key = DEP_MODEL_KEY, mandatory = false )
   private DependencySharedModel parserModel = null;

   public static final String LEM_MODEL_KEY = "LemmatizerModel";
   @ExternalResource( key = LEM_MODEL_KEY, mandatory = false )
   private LemmatizerSharedModel lemmatizerModel = null;

   protected AbstractComponent parser = null;
   protected AbstractMPAnalyzer lemmatizer = null;

   @Override
   public void initialize( UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      LOGGER.info( "Initializing ClearNLP dependency parser, using lemmatizer: " + useLemmatizer );

      if ( useLemmatizer ) {
         if ( lemmatizerModel == null ) {
//            try {
//          this.lemmatizer = EngineGetter.getMPAnalyzer(language, FileLocator.getAsStream(LemmatizerSharedModel.ENG_LEMMATIZER_DATA_FILE));
//            } catch ( FileNotFoundException e ) {
//               e.printStackTrace();
//               throw new ResourceInitializationException( e );
//            }
            logDeprecation( PARAM_LEMMATIZER_DATA_FILE, LEM_MODEL_KEY );
            this.lemmatizer = LemmatizerSharedModel.getAnalyzer( lemmaDataPath, LemmatizerSharedModel.DEFAULT_LANGUAGE );
         } else {
            // Note: If lemmatizer data file is not specified, then use lemmas from the BaseToken normalizedToken field.
            // Initialize lemmatizer
            this.lemmatizer = lemmatizerModel.getLemmatizerModel();
         }
      }
      if ( this.parserModel == null ) {
//      this.parser = DependencySharedModel.getDefaultModel();
         logDeprecation( PARAM_PARSER_MODEL_FILE_NAME, DEP_MODEL_KEY );
         this.parser = DependencySharedModel.getModel( parserModelPath, DependencySharedModel.DEFAULT_LANGUAGE );
      } else {
         this.parser = parserModel.getParser();
      }
   }

   @Override
   public synchronized void process( JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Dependency parser starting with thread:" + Thread.currentThread().getName() );
      for ( Sentence sentence : JCasUtil.select( jCas, Sentence.class ) ) {
         List<BaseToken> printableTokens = new ArrayList<>();
         for ( BaseToken token : JCasUtil.selectCovered( jCas, BaseToken.class, sentence ) ) {
            if ( token instanceof NewlineToken ) continue;
            printableTokens.add( token );
         }

         if ( printableTokens.isEmpty() ) {
            // If there are no printable tokens then #convert fails
            continue;
         }
         DEPTree tree = new DEPTree();

         // Convert CAS data into structures usable by ClearNLP
         for ( int i = 0; i < printableTokens.size(); i++ ) {
            BaseToken token = printableTokens.get( i );
            String lemma = useLemmatizer ? lemmatizer.getLemma( token.getCoveredText(), token.getPartOfSpeech() )
                                         : token.getNormalizedForm();
            DEPNode node = new DEPNode( i + 1, token.getCoveredText(), lemma, token.getPartOfSpeech(), new DEPFeat() );
            // in case we don't end up actually processing, point everyone at the root - created in DEPTree::new.
            node.setHead( tree.get( 0 ) );
            node.setLabel( "root" );
            tree.add( node );
         }

         // Run parser and convert output back to CAS friendly data types
         synchronized(LOCK){
           if(this.maxTokens <=0 || printableTokens.size() <= this.maxTokens) parser.process( tree );
           ArrayList<ConllDependencyNode> nodes = ClearDependencyUtility.convert( jCas, tree, sentence, printableTokens );
           DependencyUtility.addToIndexes( jCas, nodes );
         }
      }
      LOGGER.info( "Dependency parser ending with thread:" + Thread.currentThread().getName() );
   }

   static private void logDeprecation( final String parameterName, final String resourceName ) {
      LOGGER.warn( "Use of configuration parameter " + parameterName
            + " may be deprecated in the future in favor of external resource " + resourceName );
   }

   // If someone calls this, they want the default model, lazy initialization of the external resources:
   public static synchronized AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
      return createAnnotatorDescription( defaultParserResource, defaultLemmatizerResource );
   }

   public static synchronized AnalysisEngineDescription createAnnotatorDescription(int maxTokens) throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription(
              ClearNLPDependencyParserAE.class,
              DEP_MODEL_KEY,
              defaultParserResource,
              LEM_MODEL_KEY,
              defaultLemmatizerResource,
              PARAM_MAX_TOKENS,
              maxTokens);
   }

   public static AnalysisEngineDescription createAnnotatorDescription( ExternalResourceDescription parserDesc, ExternalResourceDescription lemmaDesc ) throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription(
            ClearNLPDependencyParserAE.class,
            DEP_MODEL_KEY,
            parserDesc,
            LEM_MODEL_KEY,
            lemmaDesc );
   }
}
