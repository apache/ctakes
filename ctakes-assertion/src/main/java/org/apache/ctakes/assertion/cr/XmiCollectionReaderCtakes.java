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
package org.apache.ctakes.assertion.cr;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A simple collection reader that reads CASes in XMI format from a directory in the filesystem.
 */
@PipeBitInfo(
      name = "XMI in Dir Reader (2)",
      description = "Reads document texts and annotations from XMI files in a directory.",
      role = PipeBitInfo.Role.READER,
      products = { PipeBitInfo.TypeProduct.DOCUMENT_ID }
)
public class XmiCollectionReaderCtakes extends CollectionReader_ImplBase {
  /**
   * Name of configuration parameter that must be set to the path of a directory containing the XMI
   * files.
   */
  public static final String PARAM_INPUTDIR = "InputDirectory";
  
  /**
   * Name of the configuration parameter that must be set to indicate if the
   * execution fails if an encountered type is unknown
   */
  public static final String PARAM_FAILUNKNOWN = "FailOnUnknownType";

  private Boolean mFailOnUnknownType;
  
  private ArrayList mFiles;

  private int mCurrentIndex;

  /**
   * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
   */
  public void initialize() throws ResourceInitializationException {
	mFailOnUnknownType = (Boolean) getConfigParameterValue(PARAM_FAILUNKNOWN);
	if (null == mFailOnUnknownType) {
	  mFailOnUnknownType = true;  // default to true if not specified
	}
    File directory = new File(((String) getConfigParameterValue(PARAM_INPUTDIR)).trim());
    mCurrentIndex = 0;

    // if input directory does not exist or is not a directory, throw exception
    if (!directory.exists() || !directory.isDirectory()) {
      throw new ResourceInitializationException(ResourceConfigurationException.DIRECTORY_NOT_FOUND,
              new Object[] { PARAM_INPUTDIR, this.getMetaData().getName(), directory.getPath() });
    }

    // get list of .xmi files in the specified directory
    mFiles = new ArrayList();
    File[] files = directory.listFiles();
    for (int i = 0; i < files.length; i++) {
      if (!files[i].isDirectory() && files[i].getName().endsWith(".xmi")) {
        mFiles.add(files[i]);
      }
    }
  }

  /**
   * @see org.apache.uima.collection.CollectionReader#hasNext()
   */
  public boolean hasNext() {
    return mCurrentIndex < mFiles.size();
  }

  /**
   * @see org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas.CAS)
   */
  public void getNext(CAS aCAS) throws IOException, CollectionException {
    File currentFile = (File) mFiles.get(mCurrentIndex++);
    FileInputStream inputStream = new FileInputStream(currentFile);
    try {
    	XmiCasDeserializer.deserialize(inputStream, aCAS, ! mFailOnUnknownType);
    } catch (SAXException e) {
      throw new CollectionException(e);
    } finally {
      inputStream.close();
    }
  }

  /**
   * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#close()
   */
  public void close() throws IOException {
  }

  /**
   * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#getProgress()
   */
  public Progress[] getProgress() {
    return new Progress[] { new ProgressImpl(mCurrentIndex, mFiles.size(), Progress.ENTITIES) };
  }

}
