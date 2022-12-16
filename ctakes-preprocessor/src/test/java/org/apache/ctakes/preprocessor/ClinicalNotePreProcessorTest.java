
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

package org.apache.ctakes.preprocessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Map;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.preprocessor.ClinicalNotePreProcessor;
import org.apache.ctakes.preprocessor.DocumentMetaData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for the ClinicalNotePreProcessor class.  These test the class
 * against clinical note XML data and determine whether the preprocessor
 * successfully parsed out the correct info.
 * 
 * @author Mayo Clinic
 */
public class ClinicalNotePreProcessorTest
{
    private ClinicalNotePreProcessor iv_cnotePreProcessor;
    private String iv_cnoteXML;

    @Before
    public void setUp() throws Exception
    {
        String dtdLocation = "src/test/resources/NotesIIST_RTF.DTD";
        iv_cnotePreProcessor = new ClinicalNotePreProcessor(FileLocator.getAsStream(dtdLocation), false);

        String cnoteLocationOnCp = "src/test/resources/testpatient_cn_1.xml";
        String cnoteLocation = new File(cnoteLocationOnCp).getPath();
        
        if (cnoteLocation == null) {
        	throw new FileNotFoundException("Unable to find: " + cnoteLocationOnCp);
        }
        iv_cnoteXML = load(cnoteLocation);
    }

    /**
     * Tests the process method.
     */
    @Test
    public void testProcess()
    {
        try
        {
            DocumentMetaData dmd = iv_cnotePreProcessor.process(iv_cnoteXML);

            // validate document properties
            String docID = "000000000";
            String serviceCode = "MNT";
            Map docProperties = dmd.getMetaData();
            String cnote_docID =
                (String) docProperties.get(
                    ClinicalNotePreProcessor.MD_KEY_DOC_ID);
            String cnote_serviceCode =
                (String) docProperties.get(
                    ClinicalNotePreProcessor.MD_KEY_SERVICE_CODE);
            assertEquals(docID, cnote_docID);
            assertEquals(serviceCode, cnote_serviceCode);

            // validate each section
            // TODO Consider validating each section           
        }
        catch (Exception e)
        {
        	e.printStackTrace(System.err);
            fail(e.getMessage());
        }
    }

    /**
     * Loads text from a file.
     * @param filename
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private String load(String filename)
        throws FileNotFoundException, IOException
    {
        String msg = "";
        File f = new File(filename);
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line = br.readLine();
        while (line != null)
        {
            msg += line + "\n";
            line = br.readLine();
        }
        br.close();

        return msg;
    }
}
