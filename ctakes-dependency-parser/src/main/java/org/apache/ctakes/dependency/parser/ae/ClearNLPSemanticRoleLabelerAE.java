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
import com.googlecode.clearnlp.dependency.*;
import com.googlecode.clearnlp.reader.AbstractReader;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.ListFactory;
import org.apache.ctakes.dependency.parser.ae.shared.SRLSharedParserModel;
import org.apache.ctakes.dependency.parser.ae.shared.SRLSharedPredictionModel;
import org.apache.ctakes.dependency.parser.ae.shared.SRLSharedRoleModel;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.textsem.Predicate;
import org.apache.ctakes.typesystem.type.textsem.SemanticArgument;
import org.apache.ctakes.typesystem.type.textsem.SemanticRoleRelation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *This class provides a UIMA wrapper for the ClearNLP Semantic Role Labeler, which is
 * available here.
 * <p>
 * http://code.google.com/p/clearnlp
 * <p>
 * Before using this AnalysisEngine, you should run a Tokenizer, POS-tagger, Lemmatizer, and the
 * CLEAR parser dependency parser.
 * <p>
 * Please see /ClearNLP-wrapper/resources/dependency/clear/README for
 * important information pertaining to the models provided for this parser.
 * <p>
 *
 */
@TypeCapability(
      inputs = {
            "org.apache.ctakes.typesystem.type.syntax.BaseToken:partOfSpeech",
            "org.apache.ctakes.typesystem.type.syntax.BaseToken:tokenNumber",
            "org.apache.ctakes.typesystem.type.syntax.BaseToken:end",
            "org.apache.ctakes.typesystem.type.syntax.BaseToken:begin",
            "org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode"
      } )
@PipeBitInfo(
      name = "ClearNLP Semantic Role Labeler",
      description = "Adds Semantic Roles Relations.",
      role = PipeBitInfo.Role.ANNOTATOR,
      dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.BASE_TOKEN,
                       PipeBitInfo.TypeProduct.DEPENDENCY_NODE },
      products = { PipeBitInfo.TypeProduct.SEMANTIC_RELATION }
)
public class ClearNLPSemanticRoleLabelerAE extends JCasAnnotator_ImplBase {
   final String language = AbstractReader.LANG_EN;
   public Logger logger = Logger.getLogger( getClass().getName() );

   public static final String SRL_PRED_MODEL_KEY = "SrlPredModel";
   @ExternalResource(key = SRL_PRED_MODEL_KEY, mandatory=false)
   private SRLSharedPredictionModel predModel=null;

   public static final String SRL_PARSER_MODEL_KEY = "SrlParserModel";
   @ExternalResource(key = SRL_PARSER_MODEL_KEY, mandatory=false)
   private SRLSharedParserModel parserModel=null;
   
   public static final String SRL_ROLE_MODEL_KEY = "SrlRoleModel";
   @ExternalResource(key = SRL_ROLE_MODEL_KEY, mandatory=false)
   private SRLSharedRoleModel roleModel=null;

   protected static ExternalResourceDescription defaultParserResource = ExternalResourceFactory.createExternalResourceDescription(
       SRLSharedParserModel.class, 
       SRLSharedParserModel.DEFAULT_SRL_MODEL_FILE_NAME);
   protected static ExternalResourceDescription defaultPredictionResource = ExternalResourceFactory.createExternalResourceDescription(
       SRLSharedPredictionModel.class, 
       SRLSharedPredictionModel.DEFAULT_PRED_MODEL_FILE_NAME);
   protected static ExternalResourceDescription defaultRoleResource = ExternalResourceFactory.createExternalResourceDescription(
       SRLSharedRoleModel.class, 
       SRLSharedRoleModel.DEFAULT_ROLE_MODEL_FILE_NAME);
   
   protected AbstractComponent parser;
   protected AbstractComponent identifier;
   protected AbstractComponent classifier;


   @Override
   public void initialize( UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );

      logger.info("Initializing ClearNLP semantic role labeler");
      try {
        if(this.predModel == null){
          this.identifier = SRLSharedPredictionModel.getDefaultModel();
        }else{
          this.identifier = predModel.getComponent();
        }
        if(this.roleModel == null){
          this.classifier = SRLSharedRoleModel.getDefaultModel();
        }else{
          this.classifier = roleModel.getComponent();
        }
        if(this.parserModel == null){
          this.parser = SRLSharedParserModel.getDefaultModel();
        }else{
          this.parser = parserModel.getComponent();
        }
      } catch ( Exception e ) {
         throw new ResourceInitializationException( e );
      }
   }

   @Override
   public void process( JCas jCas ) throws AnalysisEngineProcessException {
      for ( Sentence sentence : JCasUtil.select( jCas, Sentence.class ) ) {
         List<BaseToken> printableTokens = new ArrayList<>();
         for ( BaseToken token : JCasUtil.selectCovered( jCas, BaseToken.class, sentence ) ) {
            if ( token instanceof NewlineToken ) {
               continue;
            }
            printableTokens.add( token );
         }
         DEPTree tree = new DEPTree();

         // Build map between CAS dependency node and id for later creation of
         // ClearNLP dependency node/tree
         Map<ConllDependencyNode, Integer> depNodeToID = new HashMap<>();
         int nodeId = 1;
         for ( ConllDependencyNode depNode : JCasUtil.selectCovered( jCas, ConllDependencyNode.class, sentence ) ) {
            //if (depNode instanceof TopDependencyNode) {
            if ( depNode.getHead() == null ) {
               // A node without the head is the head of the sentence
               depNodeToID.put( depNode, 0 );
            } else {
               depNodeToID.put( depNode, nodeId );
               nodeId++;
            }
         }

         int[] headIDs = new int[ printableTokens.size() ];
         String[] deprels = new String[ printableTokens.size() ];

         // Initialize Token / Sentence info for the ClearNLP Semantic Role Labeler
         // we are filtering out newline tokens
         // use idIter as the non-newline token index counter
         int idIter = 0;
         for ( int i = 0; i < printableTokens.size(); i++ ) {
            BaseToken token = printableTokens.get( i );
            // ignore newline tokens within a sentence - newline = whitespace = non-token
            if ( !(token instanceof NewlineToken) ) {
               // Determine HeadId
               List<ConllDependencyNode> casDepNodes = JCasUtil.selectCovered( jCas, ConllDependencyNode.class, token );

               ConllDependencyNode casDepNode = casDepNodes.get( 0 );
               if ( casDepNode.getId() == 0 ) {
                  casDepNode = casDepNodes.get( 1 );
               }

               deprels[ i ] = casDepNode.getDeprel();
               ConllDependencyNode head = casDepNode.getHead();
               // If there is no head, this is the head node, set node to 0
               final Integer headIdIndex = (head == null) ? 0 : depNodeToID.get( head );
               if ( headIdIndex != null ) {
                  headIDs[ i ] = headIdIndex;
               } else {
                  logger.error( "No dependency node for index " + head + ".  Map size is " + depNodeToID.size()
                                + "\nSetting head ID to 0" );
                  headIDs[ i ] = 0;
               }
               // Populate Dependency Node / Tree information
               int id = idIter + 1;
               String form = casDepNode.getForm();
               String pos = casDepNode.getPostag();
               String lemma = casDepNode.getLemma();

               DEPNode node = new DEPNode( id, form, lemma, pos, new DEPFeat() );
               tree.add( node );
               idIter++;
            }
         }

         for ( int i = 1; i < tree.size(); i++ ) {
            DEPNode node = tree.get( i );
            DEPNode head = tree.get( headIDs[ i - 1 ] );
            String label = deprels[ i - 1 ];
            node.setHead( head, label );
         }

         tree.initSHeads();

         // Run the SRL
         identifier.process( tree );
         classifier.process( tree );
         parser.process( tree );


         // Convert ClearNLP SRL output to CAS types
         extractSRLInfo( jCas, printableTokens, tree );


      }

   }

   /**
    * Converts the output from the ClearNLP Semantic Role Labeler to the ClearTK Predicate and
    * SemanticArgument Types.
    *
    * @param jCas
    * @param tokens - In order list of tokens
    * @param tree   - DepdendencyTree output by ClearNLP SRLPredict
    */
   private void extractSRLInfo( JCas jCas, List<BaseToken> tokens, DEPTree tree ) {
      Map<DEPNode, Predicate> headIdToPredicate = new HashMap<DEPNode, Predicate>();
      Map<Predicate, List<SemanticArgument>> predicateArguments = new HashMap<Predicate, List<SemanticArgument>>();

      for ( int i = 1; i < tree.size(); i++ ) {
         // Every ClearNLP parserNode will contain an srlInfo field.
         DEPNode parserNode = tree.get( i );
         BaseToken token = tokens.get( i - 1 );
         String rolesetId;

         if ( (rolesetId = parserNode.getFeat( DEPLib.FEAT_PB )) != null ) {
            if ( !headIdToPredicate.containsKey( parserNode ) ) {
               // We have not encountered this predicate yet, so create it
               Predicate pred = this.createPredicate( jCas, rolesetId, token );
               headIdToPredicate.put( parserNode, pred );
               pred.setRelations( new EmptyFSList( jCas ) );
            }
         }
      }


      // Start at node 1, since node 0 is considered the head of the sentence
      for ( int i = 1; i < tree.size(); i++ ) {
         // Every ClearNLP parserNode will contain an srlInfo field.
         DEPNode parserNode = tree.get( i );
         BaseToken token = tokens.get( i - 1 );

         for ( DEPArc head : parserNode.getSHeads() ) {
            Predicate predicate = headIdToPredicate.get( head.getNode() );

            // Append this argument to the predicate's list of arguments
            if ( !predicateArguments.containsKey( predicate ) ) {
               predicateArguments.put( predicate, new ArrayList<SemanticArgument>() );
            }
            List<SemanticArgument> argumentList = predicateArguments.get( predicate );

            // Create the semantic argument and store for later link creation
            SemanticArgument argument = createArgument( jCas, head, token );
            argumentList.add( argument );
         }
      }

      // Create relations between predicates and arguments
      for ( Map.Entry<Predicate, List<SemanticArgument>> entry : predicateArguments.entrySet() ) {
         Predicate predicate = entry.getKey();

         List<SemanticRoleRelation> relations = new ArrayList<SemanticRoleRelation>();
         for ( SemanticArgument argument : entry.getValue() ) {
            SemanticRoleRelation relation = new SemanticRoleRelation( jCas );
            relation.setArgument( argument );
            relation.setPredicate( predicate );
            relation.setCategory( argument.getLabel() );
            relation.addToIndexes();
            relations.add( relation );
            argument.setRelation( relation );
         }

         FSList relationsList = ListFactory.buildList( jCas, relations.toArray( new TOP[ relations.size() ] ) );
         predicate.setRelations( relationsList );
      }
   }

   private Predicate createPredicate( JCas jCas, String rolesetId, BaseToken token ) {
      Predicate pred = new Predicate( jCas, token.getBegin(), token.getEnd() );
      pred.setFrameSet( rolesetId );
      pred.addToIndexes();
      return pred;
   }

   private SemanticArgument createArgument( JCas jCas, DEPArc head, BaseToken token ) {
      SemanticArgument argument = new SemanticArgument( jCas, token.getBegin(), token.getEnd() );
      argument.setLabel( head.getLabel() );
      argument.addToIndexes();
      return argument;
   }

   public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException{
     return AnalysisEngineFactory.createEngineDescription(
         ClearNLPSemanticRoleLabelerAE.class,
         SRL_PARSER_MODEL_KEY,
         defaultParserResource,
         SRL_PRED_MODEL_KEY,
         defaultPredictionResource,
         SRL_ROLE_MODEL_KEY,
         defaultRoleResource);
   }
}
