package org.apache.ctakes.rest.service.response;

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

import org.apache.ctakes.core.cc.pretty.plaintext.PrettyTextWriter;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;


/*
 * Rest web service that takes clinical text
 * as input and produces extracted text as output
 */
final public class PrettyPrintFormatter implements ResponseFormatter {


   /**
    * Returns the document information in a marked plain text format.
    * {@inheritDoc}
    */
   @Override
   public String getResultText( final JCas jCas ) throws AnalysisEngineProcessException {
      try ( final StringWriter stringWriter = new StringWriter();
            final BufferedWriter writer = new BufferedWriter( stringWriter ) ) {
         final Collection<Sentence> sentences = JCasUtil.select( jCas, Sentence.class );
         for ( Sentence sentence : sentences ) {
            PrettyTextWriter.writeSentence( jCas, sentence, writer );
         }
         return stringWriter.toString();
      } catch ( IOException ioE ) {
         throw new AnalysisEngineProcessException( ioE );
      }
   }


}
