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
package org.apache.ctakes.core.cr;

/**
 * @author Mayo Clinic
 * @version 1.0
 * The original code was copied from org.apache.uima.examples.cpe.FileSystemCollectionReader
 * and modified for Mayo use. This inherits from FilesInDirectoryCollectionReader and adds
 * the capability to specify the number of documents to process.
 * 
 * A simple collection reader that reads documents from a directory 
 * in the filesystem.  It can be configured with the following parameters:
 * <ul>
 *   <li><code>InputDirectory</code> - path to directory containing files</li>
 *   <li><code>Encoding</code> (optional) - character encoding of the input 
 *      files</li>
 *   <li><code>Language</code> (optional) - language of the input documents</li>
 *   <li><code>Extensions</code> (optional) - Name of optional configuration 
 *   parameter that specifies the extensions of the files that the 
 *   collection reader will read.  </li>
 *   <li><code>NumberOfIterations</code> (optional) - actual number of files to be processed</li>
 * </ul> 
 * 
 * TODO We may need to provide a way to specify some portion of the path of the file
 * to be included in the id of the document especially if we extend to recursively 
 * gather files in the directory from sub directories.    
 */

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.IOException;


@PipeBitInfo(
      name = "Files in Dir Cycle Reader",
      description = "Reads document texts from text files in a directory, repeating for a number of iterations.",
      role = PipeBitInfo.Role.READER,
      products = { PipeBitInfo.TypeProduct.DOCUMENT_ID }
)
public class FilesInDirectoryCollectionCyclicalReads extends FilesInDirectoryCollectionReader
{
	/**
	   * Name of configuration parameter that must be set to the path of
	   * a directory containing input files.
	   */
	  public static final String PARAM_INPUTDIR = "InputDirectory";

	  /**
	   * Name of configuration parameter that contains the character encoding used
	   * by the input files.  If not specified, the default system encoding will
	   * be used.
	   */
	  public static final String PARAM_ENCODING = "Encoding";

	  /**
	   * Name of optional configuration parameter that contains the language of
	   * the documents in the input directory.  If specified this information will
	   * be added to the CAS.
	   */
	  public static final String PARAM_LANGUAGE = "Language";

	  /**Name of optional configuration parameter that specifies the extensions
	     * of the files that the collection reader will read.  Values for this
	     * parameter should not begin with a dot <code>'.'</code>.
	     */

	  public static final String PARAM_EXTENSIONS = "Extensions";
	  
	  /**Arguement to equate to # of times it should read the files.
	   * Takes this argument to equate to # of times it should read the files. 
	   */  
	  
	  public static final String PARAM_NUMREADS = "NumberOfIterations";
	     
	  public static final String PARAM_RECURSE = "Recurse";
	  private int iv_iteration;
      private int scaleTime, totalNumFiles, remainTimes;
      
	  /**
	   * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
	   */
     @Override
     public void initialize() throws ResourceInitializationException {

		super.initialize();
		totalNumFiles = iv_files.size();
		iv_iteration = ((Integer) getConfigParameterValue(PARAM_NUMREADS))
				.intValue();
		if (iv_iteration > totalNumFiles) {
			scaleTime = iv_iteration / totalNumFiles;
			remainTimes = iv_iteration % totalNumFiles;
		} else
			scaleTime = -1;

	}
	
    /**
     * Similar to 'org.apache.uima.collection.CollectionReader' method hasNext() except
     * interations represents the actual number of documents to be processed, so if the 
     * total number of documents in a queue is more than the 'Iterations' value then only 
     * the iteration amount will be processed.  Multiples of the total available documents
     * will be provided to supplement the list required to meet the total iteration value.
     */
    @Override
    public boolean hasNext() {

       // If hasNext false then start over only if count that has been passed to the contructor hasn't been reached.
		boolean doNext = iv_currentIndex < totalNumFiles;
	
		if ((!doNext) && (scaleTime > 0)) {
			scaleTime--;
			if (scaleTime > 0) {
				iv_currentIndex = 0;
				doNext = true;
			}
			else if (remainTimes > 0){
				iv_currentIndex = 0;
				totalNumFiles = remainTimes;
				remainTimes=0;
				doNext = true;
			}

		}
		if (scaleTime == -1) {
         doNext = iv_currentIndex < iv_iteration;

		}
		
		return doNext;
	}

	  /**
	   * @see org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas.CAS)
	   */
     @Override
     public void getNext( CAS aCAS ) throws IOException, CollectionException {

		super.getNext(aCAS);
		
			  	
	  }


	  /**
	   * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#close()
	   */
     @Override
     public void close() throws IOException {
        super.close();
	  }

	  /**
	   * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#getProgress()
	   */
     @Override
     public Progress[] getProgress() {
        int offSet = iv_currentIndex;
        if ((scaleTime > 0) && (iv_currentIndex > 0))
			  offSet = iv_currentIndex*(1/scaleTime);
		  if (scaleTime == 0){
			  offSet = iv_iteration + remainTimes;
		  }

		return new Progress[] { new ProgressImpl( offSet , 
				iv_iteration, Progress.ENTITIES) };
	}

	  /**
		 * Gets the total number of documents that will be returned by this
		 * collection reader. This is not part of the general collection reader
		 * interface.
		 * 
		 * @return the number of documents in the collection
		 */
     @Override
     public int getNumberOfDocuments() {
        return iv_files.size();
	  }


}
