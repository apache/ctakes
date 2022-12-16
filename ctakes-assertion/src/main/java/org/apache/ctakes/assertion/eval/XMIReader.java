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
package org.apache.ctakes.assertion.eval;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * UIMA CollectionReader that reads in CASes from XMI files.
 */
@PipeBitInfo(
      name = "XMI Reader (4)",
      description = "Reads document texts and annotations from XMI files specified in a provided list.",
      role = PipeBitInfo.Role.READER,
      products = { PipeBitInfo.TypeProduct.DOCUMENT_ID }
)
public class XMIReader extends JCasCollectionReader_ImplBase {

  public static final String PARAM_FILES = "files";

  @ConfigurationParameter(
      name = PARAM_FILES,
      mandatory = true,
      description = "The XMI files to be loaded")
  private List<File> files;

  private Iterator<File> filesIter;

  private int completed;

  @Override
  public void initialize(UimaContext context) throws ResourceInitializationException {
    super.initialize(context);
    this.filesIter = files.iterator();
    this.completed = 0;
  }

  @Override
  public Progress[] getProgress() {
    return new Progress[] { new ProgressImpl(this.completed, this.files.size(), Progress.ENTITIES) };
  }

  @Override
  public boolean hasNext() throws IOException, CollectionException {
    return this.filesIter.hasNext();
  }

@Override
  public void getNext(JCas jCas) throws IOException, CollectionException {
	try(FileInputStream inputStream = new FileInputStream(this.filesIter.next())){
      XmiCasDeserializer.deserialize(new BufferedInputStream(inputStream), jCas.getCas());
    } catch (SAXException e) {
      throw new CollectionException(e);
    }
    this.completed += 1;
  }

}