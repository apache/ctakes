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
package org.apache.ctakes.preprocessor.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.preprocessor.ClinicalNotePreProcessor;
import org.apache.ctakes.preprocessor.DocumentMetaData;
import org.apache.ctakes.preprocessor.PreProcessor;
import org.apache.ctakes.preprocessor.SegmentMetaData;


/**
 * 
 * @author Mayo Clinic
 */
public class TestDriver
{

    public static void main(String[] args)
    {
        if (args.length < 1 || args.length > 2)
        {
            System.out.println("Usage: TestDriver <input file> [<dtd file>]");
            // example parameters: "test/data/testpatient_cn_1.xml"
            // example parameters: "test/data/testpatient_cn_1.xml" "resources/cda/NotesIIST_RTF.DTD"
            System.exit(1);
        }
        try
        {
            String hl7Text = load(args[0]);
            String dtdFilename = "resources/cda/NotesIIST_RTF.DTD";
            if (args.length > 1) {
            	dtdFilename = args[1];
            }

            
            long timestamp, elapsedTime;
            Set sectionNames;
            Iterator snItr;

            PreProcessor pp = new ClinicalNotePreProcessor(FileLocator.getAsStream(dtdFilename), false);

            timestamp = System.currentTimeMillis();
            DocumentMetaData dmd = pp.process(hl7Text);
            elapsedTime = System.currentTimeMillis() - timestamp;
            System.out.println("PreProcessor Took " + elapsedTime + "ms");

            System.out.println("Plain Text Start");
            System.out.println(dmd.getText());
            System.out.println("Plain Text End");

            sectionNames = dmd.getSegmentIdentifiers();
            snItr = sectionNames.iterator();
            while (snItr.hasNext())
            {
                String sectionId = (String) snItr.next();
                SegmentMetaData smd = dmd.getSegment(sectionId);
                System.out.println("SECTION="
                        + sectionId
                        + "\tSTART_OFFSET="
                        + smd.span.start
                        + "\tEND_OFFSET="
                        + smd.span.end);
                //System.out.println(dmd.getText().substring(smd.span.start, smd.span.end));
            }

            Map metaDataMap = dmd.getMetaData();
            Iterator keyItr = metaDataMap.keySet().iterator();
            while (keyItr.hasNext())
            {
                Object key = keyItr.next();
                Object value = metaDataMap.get(key);
                System.out.println("MetaData KEY="
                        + key.toString()
                        + " VALUE="
                        + value.toString());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Loads text from a file.
     * 
     * @param filename
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static String load(String filename)
            throws FileNotFoundException, IOException
    {
        String msg = "";
        File f = new File(filename);
        exists(f); // output error message if file does not exist
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

    public static boolean exists(File f) {
    	
    	if (f.exists()) {
    		return true;
    	}
    	
    	System.out.println("cwd = " + new File(".").getAbsolutePath());
    	System.out.println("File " + f + " does not exist.");
    	return false;
    }
}
