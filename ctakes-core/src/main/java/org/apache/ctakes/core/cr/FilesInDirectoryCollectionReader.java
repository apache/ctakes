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
 * The original code was copied from org.apache.uima.examples.cpe.FileSystemCollectionReader
 * and modified for Mayo use.
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
 *   collection reader will read.  
 * </ul> 
 * 
 * TODO We may need to provide a way to specify some portion of the path of the file
 * to be included in the id of the document especially if we extend to recursively 
 * gather files in the directory from sub directories.    
 */

import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.log4j.Logger;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

//import org.apache.uima.jcas.tcas.DocumentAnnotation;

/**
 * @deprecated Please use FileTreeReader.
 * FileTreeReader is more robust plus it can handle patient names, doc types, doc times, doc id prefixes, etc.
 */
@PipeBitInfo(
		name = "Files in Dir Reader",
		description = "Reads document texts from text files in a directory.",
		role = PipeBitInfo.Role.READER,
		products = { PipeBitInfo.TypeProduct.DOCUMENT_ID }
)
@Deprecated
public class FilesInDirectoryCollectionReader extends CollectionReader_ImplBase
{
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

	public static final String PARAM_RECURSE = "Recurse";

	protected ArrayList<File> iv_files;
	private String iv_encoding;
	private String iv_language;
	private static String[] iv_extensions;

	protected int iv_currentIndex;

	private boolean iv_recurse = false;

	private String iv_rootPath = "";

	/**
	 * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
	 */
	@Override
	public void initialize() throws ResourceInitializationException {
		Logger.getLogger( getClass().getSimpleName() ).warn( "Deprecated.  Please use FileTreeReader instead." );
		final String inputDirPath = (String)getConfigParameterValue( ConfigParameterConstants.PARAM_INPUTDIR );
		File directory;
		try {
			directory = FileLocator.getFile( inputDirPath );
		} catch ( IOException ioE ) {
			throw new ResourceInitializationException(
					ResourceConfigurationException.DIRECTORY_NOT_FOUND,
					new Object[] { ConfigParameterConstants.PARAM_INPUTDIR, getMetaData().getName(), inputDirPath } );
		}
		iv_encoding = (String)getConfigParameterValue( PARAM_ENCODING );
		iv_language = (String)getConfigParameterValue(PARAM_LANGUAGE);
		iv_extensions = (String[]) getConfigParameterValue(PARAM_EXTENSIONS);

		iv_currentIndex = 0;

		iv_recurse = false;
		Boolean recurse = (Boolean) getConfigParameterValue(PARAM_RECURSE);
		if(recurse != null)
			iv_recurse = recurse.booleanValue();
		iv_rootPath = directory.getPath();

		//if input directory does not exist or is not a directory, throw exception
		if (!directory.exists() || !directory.isDirectory()) {
			throw new ResourceInitializationException(
					ResourceConfigurationException.DIRECTORY_NOT_FOUND,
					new Object[] { ConfigParameterConstants.PARAM_INPUTDIR, this.getMetaData().getName(),
										directory.getPath() } );
		}


		//get list of files (not subdirectories) in the specified directory
		iv_files = new ArrayList<File>();
		if(!iv_recurse) {
			File[] files = directory.listFiles();
			for ( int i = 0; i < files.length; i++) {
				if (!files[i].isDirectory() && hasValidExtension(files[i])) {
					iv_files.add(files[i]);
				}
			}
		} else {
			try {
				collectFiles(directory, iv_files);
				System.out.println("iv_files.size()="+iv_files.size());
			} catch(IOException ioe) {
				throw new ResourceInitializationException(ioe);
			}
		}
	}

	private void collectFiles(File directory, List<File> files) throws IOException {
		File[] dirFiles = directory.listFiles();
		for( int i=0; i<dirFiles.length; i++) {
			if(dirFiles[i].isDirectory()) {
				collectFiles(dirFiles[i], files);
			} else if(hasValidExtension(dirFiles[i])) {
				files.add(dirFiles[i]);
			}
		}
	}


	private boolean hasValidExtension(File file) {
		if(iv_extensions == null) return true;
		for ( int i = 0; i < iv_extensions.length; i++) {
			if(file.getName().endsWith("."+iv_extensions[i])) {
				return true;
			}
		}
		return false;
	}


	/**
	 * @see org.apache.uima.collection.CollectionReader#hasNext()
	 */
	@Override
	public boolean hasNext() {
		return iv_currentIndex < iv_files.size();
	}

	/**
	 * @see org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas.CAS)
	 */
	@Override
	public void getNext( CAS aCAS ) throws IOException, CollectionException {
		JCas jcas;
		InputStream fileInputStream = null;
		Reader fileReader = null;

		try {
			jcas = aCAS.getJCas();

			//open input stream to file
			File file = iv_files.get( iv_currentIndex );
			fileInputStream = new FileInputStream( file );
			fileReader = new BufferedReader(new InputStreamReader(fileInputStream));

			DocumentID documentIDAnnotation = new DocumentID(jcas);
			String docID = createDocID(file);
			documentIDAnnotation.setDocumentID(docID);
			documentIDAnnotation.addToIndexes();

			byte[] contents = new byte[(int)file.length() ];
			fileInputStream.read( contents );
			String text;
			if (iv_encoding != null) {
				text = new String(contents, iv_encoding);
			} else {
				text = new String(contents);
			}
			//put document in CAS (assume CAS)
			jcas.setDocumentText(text);

			//set language if it was explicitly specified as a configuration parameter
			if (iv_language != null) {
				//      ((DocumentAnnotation)jcas.getDocumentAnnotationFs()).setLanguage(iv_language);
			}

		} catch (CASException e) {
			throw new CollectionException(e);
		} finally {
			if (fileInputStream != null)
				fileInputStream.close();
			iv_currentIndex++;
		}
	}

	private String createDocID(File file) {
		String docID = file.getPath();
		if( iv_rootPath.endsWith(""+File.separator) ||
			 iv_rootPath.equals("")) {
			docID = docID.substring(iv_rootPath.length());
		} else
			docID = docID.substring(iv_rootPath.length()+1);
		return docID;
	}

	/**
	 * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#close()
	 */
	@Override
	public void close() throws IOException {
	}

	/**
	 * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#getProgress()
	 */
	@Override
	public Progress[] getProgress() {
		return new Progress[]{
				new ProgressImpl(iv_currentIndex, iv_files.size(),Progress.ENTITIES)};
	}

	/**
	 * Gets the total number of documents that will be returned by this
	 * collection reader.  This is not part of the general collection reader
	 * interface.
	 *
	 * @return the number of documents in the collection
	 */
	public int getNumberOfDocuments() {
		return iv_files.size();
	}


}
