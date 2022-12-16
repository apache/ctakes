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

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.log4j.Logger;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//import org.apache.uima.jcas.tcas.DocumentAnnotation;

/**
 * 
 * The original code was copied from org.apache.uima.examples.cpe.FileSystemCollectionReader
 * and modified for Mayo use.
 *
 * This collection reader facilitates reading "documents" from a single file.  Each
 * line in the document will be considered an entity to be analyzed by the CPE.  That
 * is each line will be treated as a "document" and will have its own CAS.
 * 
 * Extremely large files will require large memory resources as each line is read into
 * memory upon initialization.  This was done to simplify implementation.  
 * 
 * @author Philip V. Ogren
 *
 */

@PipeBitInfo(
      name = "Lines in File Reader",
      description = "Reads a document texts from a single text file, treating each line as a document.",
      role = PipeBitInfo.Role.READER,
      products = { PipeBitInfo.TypeProduct.DOCUMENT_ID }
)
public class LinesFromFileCollectionReader extends CollectionReader_ImplBase {

	/**
	 * This parameter will be used the descriptor file to specify the location of the
	 * file that will be run through this collection reader.
	 */
	public static final String PARAM_INPUT_FILE_NAME = "InputFileName";
	/**
	 * Optional parameter specifies a comment string.  Any line that begins with the string
	 * will be ignored and not be added as a "document" to the CPE. 
	 */
	public static final String PARAM_COMMENT_STRING = "CommentString";
	/**
	 * Optional parameter determines whether a blank line will be processed as a document or
	 * will be ignored.  The default will be set to 'true'.  
	 */
	public static final String PARAM_IGNORE_BLANK_LINES = "IgnoreBlankLines";
	
  /**
   * Name of optional configuration parameter that contains the language of
   * the documents in the input directory.  If specified this information will
   * be added to the CAS.
   */
    public static final String PARAM_LANGUAGE = "Language";
    
    /**
     * Name of optional configuration parameter that specifies a character (or string) that delimits
     * the id of the document from the text of the document.  For example, if the parameter is 
     * set to '|' then the following line from a file:
     * <code>1234|this is some text</code>
     * would have an id of 1234 and text <code>this is some text</code>.  
     * If this parameter is not set, then
     * the id of a document will be its line number in the file.      
     */

    public static final String PARAM_ID_DELIMETER = "IdDelimeter";
    
	List<String> iv_linesFromFile;
	int iv_currentIndex = 0;
	String iv_language; 
	String iv_delimeter;
	
	private Logger iv_logger = Logger.getLogger(getClass().getName());

   @Override
   public void initialize() throws ResourceInitializationException {
      BufferedReader fileReader = null;
		try
		{
			String fileLocation = (String) getConfigParameterValue(PARAM_INPUT_FILE_NAME);
			String commentSeq = (String)getConfigParameterValue(PARAM_COMMENT_STRING);
			iv_language = (String)getConfigParameterValue(PARAM_LANGUAGE);
			Boolean paramValue = (Boolean)getConfigParameterValue(PARAM_IGNORE_BLANK_LINES);
			boolean ignoreBlankLines = true;
			if(paramValue != null) 
			{
				ignoreBlankLines = paramValue.booleanValue();
			}
			iv_delimeter =  (String)getConfigParameterValue(PARAM_ID_DELIMETER);
				
			iv_linesFromFile = new ArrayList<String>();
			fileReader = new BufferedReader(new FileReader(fileLocation));
			String line;
			while((line = fileReader.readLine()) != null)
			{
				if(commentSeq != null)
				{
					if(line.startsWith(commentSeq)) continue;
				}
				if(ignoreBlankLines && line.trim().length() == 0) continue;
				iv_linesFromFile.add(line);
			}
		}
		catch(IOException fnfe)
		{
			throw new ResourceInitializationException(fnfe);
		}
		finally
		{
			if(fileReader != null)
			try { fileReader.close(); } catch(IOException ioe) {}
		}
	}

   @Override
   public void getNext( CAS cas ) throws IOException, CollectionException {
      JCas jcas;
	  	try
	    {
	  		jcas = cas.getJCas();

          String line = iv_linesFromFile.get( iv_currentIndex );
          int lineNumber = iv_currentIndex + 1;
          String id;
		  	String text;
		  	if(iv_delimeter != null)
			{
				int delimeterLoc = line.indexOf(iv_delimeter);
				if(delimeterLoc <= 0)
					throw new CollectionException(new Exception("Line in file number "+lineNumber+" is not well formatted.  " +
							"\nIt should have the format:" +
							"\n<doc_id>"+iv_delimeter+"<doc_text>"));
				id = line.substring(0,delimeterLoc);
				text = line.substring(delimeterLoc+iv_delimeter.length());
			}
		  	else
		  	{
		  		id = Integer.toString(lineNumber); //id will one more than its index into iv_linesFromFile (iv_currentIndex has already been incremented)
		  		text = line;
		  	}
	  		

		  	iv_logger.debug("id="+id);
		  	iv_logger.debug("text="+text);
		  	
				jcas.setDocumentText(text);

          //set language if it was explicitly specified as a configuration parameter
		    if (iv_language != null)
		    {
//		      ((DocumentAnnotation)jcas.getDocumentAnnotationFs()).setLanguage(iv_language);
		    }
		    
		    
		    DocumentID documentIDAnnotation = new DocumentID(jcas);
		    documentIDAnnotation.setDocumentID(id);
		    documentIDAnnotation.addToIndexes();

	    } 
	    catch (CASException e)
	    {
	      throw new CollectionException(e);
	    }
	    finally
	    {
	    	iv_currentIndex++;
	    }
	    
	}

   @Override
   public boolean hasNext() throws IOException, CollectionException {
      return iv_currentIndex < iv_linesFromFile.size();
	}

   @Override
   public Progress[] getProgress() {
      return new Progress[] {
            new ProgressImpl(iv_currentIndex, iv_linesFromFile.size(),Progress.ENTITIES)};
	}

	 /**
	   * Gets the total number of documents that will be returned by this
	   * collection reader.  
	   * @return the number of documents in the collection
	   */
	  public int getNumberOfDocuments()
	  {
	    return iv_linesFromFile.size();
	  }

   @Override
   public void close() throws IOException {
   }
}
