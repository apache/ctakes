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
package org.apache.ctakes.core.cc;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.log4j.Logger;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;


/**
 * CasConsumer that writes a JCas (the current view) to an xml file
 *
 * @author Mayo Clinic
 */
@PipeBitInfo(
      name = "XMI Writer 2",
      description = "Writes XMI files with full representation of input text and all extracted information.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { PipeBitInfo.TypeProduct.DOCUMENT_ID }
)
public class CasConsumer extends CasConsumer_ImplBase {
   // LOG4J logger based on class name
   private Logger iv_logger = Logger.getLogger( getClass().getName() );

   private String iv_outputDir = null;

   // iv_procCount is used to name the output files sequentially if there
   // is a problem with naming based on source names
   private int iv_procCount = 0;


   /**
    * Read in configuration parameters
    */
   @Override
   public void initialize() throws ResourceInitializationException {
      iv_outputDir = (String)getConfigParameterValue( "outputDir" );
   }


   /**
    * Write a formatted xml file containing data from the view.
    * The file name will come from the DocumentID annotation,
    * which is associated with a view.
    * We append .xml to the DocumentID/filename
    */
   private void processView( JCas view ) throws Exception {
      // String docText = view.getDocumentText();

      String docName = DocIdUtil.getDocumentID( view );

      File outputFile;
      if ( docName == null || docName.equals( DocIdUtil.NO_DOCUMENT_ID ) ) {
         docName = "doc" + iv_procCount + ".xml";
      } else {
         docName = docName + ".xml";
         //	if (!docName.endsWith(".xml")) {
         //    	docName = docName + ".xml";
         //	}
      }

      OutputStream out = null;
      try {
         File outputDir = new File( iv_outputDir );
         outputDir.mkdirs();
         outputFile = new File( iv_outputDir + File.separatorChar + docName );
         out = new FileOutputStream( outputFile );
         XCASSerializer.serialize( view.getCas(), out, true ); // true -> formats the output
      } finally {
         iv_procCount++;
         if ( out != null ) {
            out.close();
         }
      }

   }


   /**
    * Create an xml file from the data in the cas.
    */
   @Override
   public void processCas( CAS cas ) throws ResourceProcessException {

      iv_logger.info( "Started" );

      try {

         JCas currentView = cas.getCurrentView().getJCas();
         processView( currentView );

      } catch ( Exception e ) {
         throw new ResourceProcessException( e );
      }

   }

}