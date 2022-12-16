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
package org.apache.ctakes.smokingstatus.cc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import org.apache.ctakes.smokingstatus.type.SmokingDocumentClassification;

import org.apache.ctakes.smokingstatus.patientLevel.PatientLevelSmokingStatus;
import org.apache.ctakes.typesystem.type.structured.DocumentID;

public class RecordResolutionCasConsumer extends CasConsumer_ImplBase
{
  /**
   * The name of the parameter that is specifies the path of the output file.
   */
  public static final String PARAM_OUTPUT_FILE = "OutputFile";

  /**
   * The name of the parameter that is specifies the delimiter for the output
   * file.
   */
  public static final String PARAM_DELIMITER = "Delimiter";
  
  /**
   * Specifies whether the cas should be handled as CDA (via 'plaintext' sofa view) or default (flat file)
   *  
   */
  public static final String CDA_PROCESSING = "ProcessingCDADocument";
  
  /**
   * Specifies whether post process should be run which provides the patient level classification
   */
  public static final String PATIENT_LEVEL_PROCESSING = "RunPatientLevelClassification";
  
  /**
   * Optional path and file name of the output file which holds the patient level classification summary
   * 
   */
  public static final String FINAL_CLASS_FILE = "FinalClassificationOutputFile";
  
  public void initialize() throws ResourceInitializationException 
  {
    File outFile;
    
    iv_sb = new StringBuffer();
    
    try
    {
        String filename = (String) getConfigParameterValue(PARAM_OUTPUT_FILE);
        outFile = new File(filename);
        if (!outFile.exists())
          outFile.createNewFile();
        iv_bw = new BufferedWriter(new FileWriter(outFile));
        
        iv_delimiter = (String) getConfigParameterValue(PARAM_DELIMITER);
        iv_useCDAProcess = (Boolean) getConfigParameterValue(CDA_PROCESSING);
        iv_postPatientLvlProcess = (Boolean) getConfigParameterValue(PATIENT_LEVEL_PROCESSING);
        iv_patient_level_file = (String) getConfigParameterValue(FINAL_CLASS_FILE);
        
        if (iv_postPatientLvlProcess) {
    		patientSmokingStatus = new PatientLevelSmokingStatus();
    		patientSmokingStatus.setInputFile(filename);
        }

    } catch (Exception ioe)
    {
        throw new ResourceInitializationException(ioe);
    }
  }
  
  public void processCas(CAS cas) throws ResourceProcessException
  {
    try
    {
      JCas jcas;
      if (iv_useCDAProcess)
    	  jcas = cas.getJCas().getView("plaintext");
      else 
    	  jcas = cas.getJCas();
      JFSIndexRepository indexes = jcas.getJFSIndexRepository();
      
	 	FSIterator<TOP> documentIDIterator = indexes.getAllIndexedFS(DocumentID.type);
      if(documentIDIterator.hasNext())
      {
        DocumentID dia = (DocumentID)documentIDIterator.next();
        iv_sb.append(dia.getDocumentID());
      }
      else
      {
        iv_sb.append("Error in CasInitializer(?) NO_DOC_ID");
      }
      
      iv_sb.append(iv_delimiter);
      
      Iterator<?> docClsItr = indexes.getAnnotationIndex(SmokingDocumentClassification.type).iterator();
      
      //there should be just one SmokingDocumentClassification
      if (docClsItr.hasNext())
      {
        SmokingDocumentClassification dc = (SmokingDocumentClassification)docClsItr.next();
        iv_sb.append(dc.getClassification());
        iv_sb.append(NEW_LINE);
      }
      else
      {
        iv_sb.append("Error in RecordResolutionCasConsumer:NO classification");
      }
      
      iv_bw.write(iv_sb.toString());      
    }
    catch(Exception exception)
    {
      throw new ResourceProcessException(exception);
    }
    finally
    {
      iv_sb.delete(0, iv_sb.length());
    }
  }

  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException, IOException
  {
    super.collectionProcessComplete(arg0);
    File outFile = null;
    try
    {
      iv_bw.flush();
      iv_bw.close();
   
    }
    catch(Exception e)
    { throw new ResourceProcessException(e); }
    if (iv_postPatientLvlProcess) {
        String filename = (String) getConfigParameterValue(PARAM_OUTPUT_FILE);
        
        if (iv_patient_level_file != null)
        	outFile = new File(iv_patient_level_file);
        else
        	outFile = new File(filename.replace(filename, filename+"_patientLevel.txt"));
        
        if (!outFile.exists())
          outFile.createNewFile();
        patientSmokingStatus.setOutputFile(outFile.getAbsolutePath());
	    patientSmokingStatus.collectCounts("\\"+iv_delimiter);
	    patientSmokingStatus.assignPatientLevelSmokingStatus();
	    patientSmokingStatus.printToFile();
    }
  }
  
  /**
   * The buffered writer used to write the document classification
   */
  private BufferedWriter iv_bw = null;
  
  /**
   * buffer used to compile results of a given doc
   */
  private StringBuffer iv_sb;

  private String iv_patient_level_file = null;
  private String iv_delimiter;
  
  /**
   * System new line character
   * @throws ResourceInitializationException
   */
  private static String NEW_LINE = System.getProperty("line.separator");
  private boolean iv_postPatientLvlProcess = false;
  private boolean iv_useCDAProcess = false;
  private PatientLevelSmokingStatus patientSmokingStatus = null;
}
