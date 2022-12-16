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
package org.apache.ctakes.core.cr;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileReadWriteUtil;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * UIMA CollectionReader that reads in Text from text files.
 */
@PipeBitInfo(
      name = "Text Files Reader",
      description = "Reads document texts from text files specified in a provided list.",
      role = PipeBitInfo.Role.READER,
      products = PipeBitInfo.TypeProduct.DOCUMENT_ID
)
public class TextReader extends JCasCollectionReader_ImplBase {

  public static final String PARAM_FILES = "files";

  @ConfigurationParameter(
      name = PARAM_FILES,
      mandatory = true,
      description = "The text files to be loaded")
  private List<File> _files;

   private Iterator<File> _filesIter;

   private int _completed;

  @Override
  public void initialize( final UimaContext context ) throws ResourceInitializationException {
     super.initialize( context );
     _filesIter = _files.iterator();
     _completed = 0;
  }

  @Override
  public Progress[] getProgress() {
     return new Progress[]{ new ProgressImpl( _completed, _files.size(), Progress.ENTITIES ) };
  }

  @Override
  public boolean hasNext() throws IOException, CollectionException {
     return _filesIter.hasNext();
  }

  @Override
  public void getNext(JCas jCas) throws IOException, CollectionException {
     final File currentFile = _filesIter.next();
     final String filename = currentFile.getName();
     final String text = FileReadWriteUtil.readText( filename );
     jCas.setDocumentText( text );

     final DocumentID documentIDAnnotation = new DocumentID( jCas );
     documentIDAnnotation.setDocumentID( filename );
    documentIDAnnotation.addToIndexes();

     _completed++;
  }

  
}