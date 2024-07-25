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
package org.apache.ctakes.core.ae;


import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;

@PipeBitInfo(
      name = "Document ID Printer",
      description = "Logs the Document ID to Log4j and Standard Output.",
      role = PipeBitInfo.Role.SPECIAL,
      dependencies = { PipeBitInfo.TypeProduct.DOCUMENT_ID }
)
public class DocumentIdPrinterAnalysisEngine extends JCasAnnotator_ImplBase
{
  static private final Logger LOGGER = LogManager.getLogger( "DocumentIdPrinterAnalysisEngine" );

  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException
  {
     String documentId = DocIdUtil.getDocumentID( jcas );
    String logMessage = String.format("##### current file document id: \"%s\"", documentId);
    LOGGER.info(logMessage);
    System.out.println(logMessage);
  }

}
