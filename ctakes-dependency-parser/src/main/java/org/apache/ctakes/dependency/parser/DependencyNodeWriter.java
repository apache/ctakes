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
package org.apache.ctakes.dependency.parser;
/*
 * Copyright: (c) 2010   Mayo Foundation for Medical Education and 
 * Research (MFMER). All rights reserved. MAYO, MAYO CLINIC, and the
 * triple-shield Mayo logo are trademarks and service marks of MFMER.
 *
 * Except as contained in the copyright notice above, or as used to identify 
 * MFMER as the author of this software, the trade names, trademarks, service
 * marks, or product names of the copyright holder shall not be used in
 * advertising, promotion or otherwise in connection with this software without
 * prior written authorization of the copyright holder.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */


import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;


/**
 * CasConsumer that writes a JCas (the current view) to an xml file
 *
 * @author Mayo Clinic
 */
@PipeBitInfo(
      name = "Dependency Node Writer",
      description = "Writes information about Dependency Nodes to file.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.DEPENDENCY_NODE }
)
public class DependencyNodeWriter extends CasConsumer_ImplBase {
   // LOG4J logger based on class name
   private Logger iv_logger = Logger.getLogger( getClass().getName() );

   private String iv_outputDir = null;
   private String iv_outputFormat = null;

   // iv_procCount is used to name the output files sequentially if there
   // is a problem with naming based on source names
   private int iv_procCount = 0;


   /**
    * Read in configuration parameters
    */
   public void initialize() throws ResourceInitializationException {
      iv_outputDir = (String)getConfigParameterValue( "outputDir" );
      iv_outputFormat = (String)getConfigParameterValue( "outputFormat" );
   }


   /**
    * Write a tab-delimited file containing data from the view.
    * The file name will come from the DocumentID annotation,
    * which is associated with a view.
    */
   private void processView( JCas jCas ) throws Exception {
      // String docText = view.getDocumentText();

      String docName = DocIdUtil.getDocumentID( jCas );

      File outputFile;
      if ( docName == null || docName.equals( DocIdUtil.NO_DOCUMENT_ID ) ) {
         docName = "doc" + iv_procCount + "." + iv_outputFormat.toLowerCase();
      } else {
         docName = docName + "." + iv_outputFormat.toLowerCase();
         //	if (!docName.endsWith(".xml")) {
         //    	docName = docName + ".xml";
         //	}
      }

//        OutputStream out=null;
      try {
         File outputDir = new File( iv_outputDir );
         outputDir.mkdirs();
         outputFile = new File( iv_outputDir + File.separatorChar + docName );
//            out = new FileOutputStream(outputFile);
//            XCASSerializer.serialize(view.getCas(), out, true); // true -> formats the output
         outputFile.createNewFile();
         BufferedWriter bw = new BufferedWriter( new FileWriter( outputFile ) );

         AnnotationIndex nodeIndex = jCas.getAnnotationIndex( ConllDependencyNode.type );
         FSIterator sentences = jCas.getAnnotationIndex( Sentence.type ).iterator();

         while ( sentences.hasNext() ) {
            Sentence sentence = (Sentence)sentences.next();


            ConllDependencyNode node = null;
            FSIterator nodeIterator = nodeIndex.subiterator( sentence );
            while ( nodeIterator.hasNext() ) {
//                    int pID = (node==null)? 0 : node.getID();
               node = (ConllDependencyNode)nodeIterator.next();

               if ( node.getId() != 0 ) { // && node.getID() !=pID) {

                  if ( iv_outputFormat.toLowerCase().contains( "min" ) ) {
                     bw.write( node.getId() + "\t" );
                     bw.write( node.getForm() + "\t" );
                     bw.write( (node.getHead() == null ? "_" : node.getHead().getId()) + "\t" );
                     bw.write( node.getDeprel() + "\n" );
                  } else if ( iv_outputFormat.toLowerCase().contains( "mpos" ) ) {
                     bw.write( node.getId() + "\t" );
                     bw.write( node.getForm() + "\t" );
                     bw.write( node.getPostag() + "\t" );
                     bw.write( (node.getHead() == null ? "_" : node.getHead().getId()) + "\t" );
                     bw.write( node.getDeprel() + "\n" );
                  } else if ( iv_outputFormat.toLowerCase().contains( "mlem" ) ) {
                     bw.write( node.getId() + "\t" );
                     bw.write( node.getForm() + "\t" );
                     bw.write( node.getLemma() + "\t" );
                     bw.write( (node.getHead() == null ? "_" : node.getHead().getId()) + "\t" );
                     bw.write( node.getDeprel() + "\n" );
                  } else if ( iv_outputFormat.toLowerCase().contains( "dep" ) ) {
                     bw.write( node.getId() + "\t" );
                     bw.write( node.getForm() + "\t" );
                     bw.write( node.getLemma() + "\t" );
                     bw.write( node.getPostag() + "\t" );
                     bw.write( (node.getHead() == null ? "_" : node.getHead().getId()) + "\t" );
                     bw.write( node.getDeprel() + "\n" );
                  } else { //if (iv_outputFormat.toLowerCase().contains("conll")) {
                     bw.write( node.getId() + "\t" );
                     bw.write( node.getForm() + "\t" );
                     bw.write( node.getLemma() + "\t" );
                     bw.write( node.getCpostag() + "\t" );
                     bw.write( node.getPostag() + "\t" );
                     bw.write( node.getFeats() + "\t" );
                     bw.write( (node.getHead() == null ? "_" : node.getHead().getId()) + "\t" );
                     bw.write( node.getDeprel() + "\t" );
                     bw.write( (node.getPhead() == null ? "_" : node.getPhead().getId()) + "\t" );
                     bw.write( node.getPdeprel() + "\n" );
                  }
               }

            }
            bw.write( "\n" );


         }
         bw.flush();

      } finally {
         iv_procCount++;
//	        if (out != null) {
//	        	out.close();
//	        }
      }

   }


   /**
    * Create an xml file from the data in the cas.
    */
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