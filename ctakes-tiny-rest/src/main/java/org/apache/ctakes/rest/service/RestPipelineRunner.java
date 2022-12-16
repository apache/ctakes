package org.apache.ctakes.rest.service;

/*
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

import org.apache.ctakes.core.pipeline.PipelineBuilder;
import org.apache.ctakes.core.pipeline.PiperFileReader;
import org.apache.ctakes.rest.service.response.ResponseFormatter;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.JCasPool;

import java.io.IOException;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/5/2019
 */
public enum RestPipelineRunner {
   INSTANCE;

   static public RestPipelineRunner getInstance() {
      return INSTANCE;
   }

   static private final Logger LOGGER = Logger.getLogger( "RestPipelineRunner" );

   // Use a constant piper name.
   // This piper can wrap (load *) another piper that contains the actual desired pipeline.
   static private final String REST_PIPER_FILE_PATH = "TinyRestPipeline.piper";

   private final AnalysisEngine _engine;
   private final JCasPool _pool;

   RestPipelineRunner() {
      try {
         final PiperFileReader reader = new PiperFileReader( REST_PIPER_FILE_PATH );
         final PipelineBuilder builder = reader.getBuilder();

         final AnalysisEngineDescription pipeline = builder.getAnalysisEngineDesc();
         _engine = UIMAFramework.produceAnalysisEngine( pipeline );
         _pool = new JCasPool( 2, _engine );
      } catch ( IOException | UIMAException multE ) {
         Logger.getLogger( "RestPipelineRunner" ).error( multE.getMessage() );
         throw new ExceptionInInitializerError( multE );
      }
   }

   public String process( final ResponseFormatter formatter, final String text )
         throws AnalysisEngineProcessException {
      if ( text == null || text.trim().isEmpty() ) {
         return "";
      }
      synchronized ( REST_PIPER_FILE_PATH ) {
         final JCas jcas = _pool.getJCas( -1 );
         if ( jcas == null ) {
            throw new AnalysisEngineProcessException( new Throwable( "Could not acquire JCas from pool." ) );
         }
         try {
            jcas.reset();
            jcas.setDocumentText( text );
            _engine.process( jcas );
            final String resultText = formatter.getResultText( jcas );
            _pool.releaseJCas( jcas );
            return resultText;
         } catch ( CASRuntimeException | AnalysisEngineProcessException multE ) {
            LOGGER.error( "Error processing text." );
            throw new AnalysisEngineProcessException( multE );
         }
      }
   }


}
