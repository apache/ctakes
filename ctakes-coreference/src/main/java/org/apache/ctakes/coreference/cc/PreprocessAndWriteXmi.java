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
package org.apache.ctakes.coreference.cc;

import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.XMLSerializer;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class PreprocessAndWriteXmi {
   public static class Options {

      @Option(name = "-t",
            aliases = "--textRoot",
            usage = "specify the directory contraining the textFiles (for example /NLP/Corpus/Relations/mipacq/text/train",
            required = true)
      public String textRoot;

      // TODO - fix to use an xml collection reader instead of the hacky way it's done now...
      //		@Option(name = "-x",
      //				aliases = "--xmlRoot",
      //				usage = "specify the directory containing the knowtator xml files (for example: /NLP/Corpus/Relations/mipacq/xml/train",
      //        required = true)
      //		public File xmlRoot;

      @Option(name = "-o",
            aliases = "--outputRoot",
            usage = "specify the directory to write out CAS XMI files",
            required = true)
      public File outputRoot;
   }

   /**
    * @param args
    * @throws IOException
    * @throws UIMAException
    * @throws CmdLineException
    */
   public static void main( String[] args ) throws UIMAException, IOException, CmdLineException {
      Options options = new Options();
      CmdLineParser parser = new CmdLineParser( options );
      parser.parseArgument( args );

      File outputRoot = options.outputRoot;
      String inputRoot = options.textRoot;
//		TypeSystemDescription typeSystem = 
//			TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath("../common-type-system/desc/common_type_system.xml", 
//																			 "../assertion/desc/medfactsTypeSystem.xml");

      AnalysisEngine ae = AnalysisEngineFactory.createEngineFromPath( "desc/analysis_engine/ODIESvmVectorCreator.xml" );

      CollectionReader reader = CollectionReaderFactory.createReaderFromPath(
            "../ctakes-core/desc/collection_reader/FilesInDirectoryCollectionReader.xml",
            ConfigParameterConstants.PARAM_INPUTDIR,
            inputRoot );

      AnalysisEngine serializer = AnalysisEngineFactory.createEngine(
            PreprocessAndWriteXmi.SerializeDocumentToXMI.class,
//				typeSystem,
            PreprocessAndWriteXmi.SerializeDocumentToXMI.PARAM_OUTPUT_DIRECTORY,
            outputRoot.getPath() );

      SimplePipeline.runPipeline( reader, ae, serializer );
   }

   public static class SerializeDocumentToXMI extends JCasAnnotator_ImplBase {
      public static final String PARAM_OUTPUT_DIRECTORY = "OutputDirectory";

      @ConfigurationParameter(name = PARAM_OUTPUT_DIRECTORY, mandatory = true, description = "Specifies the output directory in which to write xmi files")
      private File outputDirectory;

      @Override
      public void initialize( UimaContext context ) throws ResourceInitializationException {
         super.initialize( context );
         if ( !this.outputDirectory.exists() ) {
            this.outputDirectory.mkdirs();
         }
      }

      @Override
      public void process( JCas jCas ) throws AnalysisEngineProcessException {
         try {
            // FIXME - not using this right now, just use default jcas
//				JCas goldView = jCas.getView(RelationExtractorEvaluation.GOLD_VIEW_NAME);
            JCas goldView = jCas;
            String documentID = DocIdUtil.getDocumentID( goldView );
            if ( documentID == null || documentID.equals( DocIdUtil.NO_DOCUMENT_ID ) ) {
               throw new IllegalArgumentException( "No documentID for CAS:\n" + jCas );
            }
            File outFile = new File( this.outputDirectory, documentID + ".xmi" );
            ContentHandler handler = new XMLSerializer( new FileOutputStream( outFile ) ).getContentHandler();
            new XmiCasSerializer( jCas.getTypeSystem() ).serialize( jCas.getCas(), handler );
         } catch ( CASRuntimeException e ) {
            throw new AnalysisEngineProcessException( e );
         } catch ( SAXException e ) {
            throw new AnalysisEngineProcessException( e );
         } catch ( FileNotFoundException e ) {
            throw new AnalysisEngineProcessException( e );
//			} catch (CASException e) {
//				throw new AnalysisEngineProcessException(e);
         }
      }

   }

}

