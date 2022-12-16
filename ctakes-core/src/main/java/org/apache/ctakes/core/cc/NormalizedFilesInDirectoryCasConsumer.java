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
import org.apache.ctakes.core.util.annotation.WordTokenUtil;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;

import java.io.*;

/**
 * For each CAS a local file with the document text is written to a directory specifed by a parameter.
 * This CAS consumer does not make use of any annotation information in the cas except for the document
 * id specified the CommonTypeSystem.xml descriptor.  The document id will be the name of the file written
 * for each CAS.
 * <p/>
 * This CAS consumer may be useful if you want to write the results of a collection reader and/or CAS
 * initializer to the local file system.  For example, a JDBC Collection Reader may read XML documents
 * from a database and a specialized cas initializer may convert the XML to plain text.  The
 * FilesInDirectoryCasConsumer can now be used to write the plain text to local plain text files.
 */

@PipeBitInfo(
      name = "Document Text Writer (Dir)",
      description = "Writes Text files with original text from the document in a specified directory.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { PipeBitInfo.TypeProduct.DOCUMENT_ID, PipeBitInfo.TypeProduct.BASE_TOKEN }
)
public class NormalizedFilesInDirectoryCasConsumer extends CasConsumer_ImplBase {

   public static final String PARAM_OUTPUTDIR = "OutputDirectory";

   File iv_outputDirectory;

   @Override
   public void initialize() throws ResourceInitializationException {
      String outputDirectoryName = (String)getConfigParameterValue( PARAM_OUTPUTDIR );
      iv_outputDirectory = new File( outputDirectoryName );
      if ( !iv_outputDirectory.exists() || !iv_outputDirectory.isDirectory() ) {
         throw new ResourceInitializationException(
               new Exception( "Parameter setting 'OutputDirectory' does not point to an existing directory." ) );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void processCas( final CAS cas ) throws ResourceProcessException {
      try {
         final JCas jcas = cas.getJCas();
         final StringBuilder normalizedText = new StringBuilder();
         final JFSIndexRepository indexes = jcas.getJFSIndexRepository();
         // Determine and set the normalized form for each <code>BaseToken</code>
         for ( Annotation annotation : indexes.getAnnotationIndex( WordToken.type ) ) {
            if ( annotation instanceof WordToken ) {
               normalizedText.append( WordTokenUtil.getCanonicalForm( (WordToken)annotation ) );
               normalizedText.append( " " );
            }
         }
         final String documentID = DocIdUtil.getDocumentID( jcas );
         writeToFile( documentID, normalizedText.toString() );
      } catch ( CASException | IOException multE ) {
         throw new ResourceProcessException( multE );
      }
   }

   private void writeToFile( String documentID, String documentText ) throws IOException {
      File outputFile = new File( iv_outputDirectory, documentID );
      outputFile.createNewFile();
      OutputStream out = new BufferedOutputStream( new FileOutputStream( outputFile ) );
      out.write( documentText.getBytes() );
      out.flush();
      out.close();
   }
}
