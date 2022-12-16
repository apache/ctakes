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
package org.apache.ctakes.smokingstatus.patientLevel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

/**
 * Patient-level smoking status classification used for the AMIA 2009 paper
 * From PatientLevelSmokingStatus_Jan09.java revised frequency based except for assigning past smoker
 * if exists only past and non-smoker    
 * @author Mayo Clinic
 */
public class PatientLevelSmokingStatus {	

	
	public static void main(String[] args) {
		if (args.length < 2)
			System.out.println("Using input file " +inputFile + " and output file " +outputFile+".  These values can be overridden by providing the input file and output file parameters, respectively, "+
					" when deploying this application.  By default the delimiter used to split out the patient information from the classification is ',', but a third parameter can be used to set this character value.");
		else {
			inputFile = args[0];
			outputFile = args[1];
		}
		if (args.length == 3)
			delimiter = args[2];
		collectCounts(delimiter);
		assignPatientLevelSmokingStatus();
		printToFile();
	}
	/*
	 * Set the path and name of input file to read document level classifications
	 */
	public void setInputFile(String fileName){
		inputFile = fileName;
	}
	/*
	 * SEt the path and name of output file to send patient level classifications
	 */
	public void setOutputFile(String fileName){
		outputFile = fileName;
	}	
	/*
	 * goes through the document level assignments for each patients and summarizes the counts
	 * stores them in a hashmap
	 */
	public static void collectCounts(String delim){
		try {
			FileReader fr = new FileReader(inputFile);
			BufferedReader reader = new BufferedReader(fr);
			String line = null;
			try {
				//Format of the input information to be processed:
				//Example: docSet35_0106487312_12_200812_12_2008701
				//docSet35 - is something that retrieval api assigns by default to output to a subset (in this case I think that is doc_link_id)
				//01064873 - is the MCN
				//12_12_2008 - this should have been the start date range(which it is not in this case because of programming error)
				//12_12_2008 - this should have been the end date range (which is is not in this case because of programming error)
				//The last n-digits is a sequential numbers.
				boolean currentSmoker = false;
				boolean nonSmoker = false;
				boolean pastSmoker = false;
				boolean smoker = false;

				//collect the counts and fill the hash map
				while ( (line = reader.readLine()) != null) {
					if(line.length()<1) continue;
					line = line.trim();
					System.out.println("Line: " + line);
					String[] parts = line.split(delim);
					String[] parts1 = parts[0].split("_"); //0106487312
					String clinicNumber = parts1[1].substring(0, 8);
					
					System.out.println(parts[1]); //class label
					if (parts[1].equals("PAST_SMOKER")){
						pastSmoker = true;
						System.out.println("past smoker doc");
					}
					if (parts[1].equals("NON_SMOKER")){
						nonSmoker = true;
						System.out.println("non smoker doc");
					}
					if (parts[1].equals("CURRENT_SMOKER")){
						currentSmoker = true;
						System.out.println("current smoker doc");
					}
					if (parts[1].equals("SMOKER")){
						smoker = true;
						System.out.println("smoker doc");
					}
					
					//if a new clinic number
					if (!patientsStatuses.containsKey(clinicNumber)){
						//create a vector for currentSmoker, nonSmoker, pastSmoker, and smoker values
						Vector<Integer> smokingStatusElements = new Vector<Integer>(4);
						if (currentSmoker == true){
							//smokingStatusElements
							smokingStatusElements.insertElementAt(new Integer(1), 0);
							smokingStatusElements.insertElementAt(new Integer(0), 1);
							smokingStatusElements.insertElementAt(new Integer(0), 2);
							smokingStatusElements.insertElementAt(new Integer(0), 3);
							System.out.println("incrementing current");
						}
						else if (nonSmoker == true){
							smokingStatusElements.insertElementAt(new Integer(0), 0);
							smokingStatusElements.insertElementAt(new Integer(1), 1);
							smokingStatusElements.insertElementAt(new Integer(0), 2);
							smokingStatusElements.insertElementAt(new Integer(0), 3);
							System.out.println("incrementing non");
						}
						else if (pastSmoker == true){
							//smokingStatusElements[0] = new Integer(0);
							
							smokingStatusElements.insertElementAt(new Integer(0), 0);
							smokingStatusElements.insertElementAt(new Integer(0), 1);
							smokingStatusElements.insertElementAt(new Integer(1), 2);
							smokingStatusElements.insertElementAt(new Integer(0), 3);
							System.out.println("incrementing past");
							
							//System.out.println("past smoker");
						}
						else if (smoker == true){
							//smokingStatusElements[0] = new Integer(0);
							
							smokingStatusElements.insertElementAt(new Integer(0), 0);
							smokingStatusElements.insertElementAt(new Integer(0), 1);
							smokingStatusElements.insertElementAt(new Integer(0), 2);
							smokingStatusElements.insertElementAt(new Integer(1), 3);
							System.out.println("incrementing past");
							
							//System.out.println("past smoker");
						}
						//account for the UNKNOWN category
						else {
							smokingStatusElements.insertElementAt(new Integer(0), 0);
							smokingStatusElements.insertElementAt(new Integer(0), 1);
							smokingStatusElements.insertElementAt(new Integer(0), 2);
							smokingStatusElements.insertElementAt(new Integer(0), 3);
						}
					
						//clinicNumber = parts1[0] + "_" + clinicNumber;
						patientsStatuses.put(clinicNumber, smokingStatusElements);
					}
					
					//if an existing clinic number
					else {
						Vector<Integer> smokingStatusElements = (Vector<Integer>) patientsStatuses.get(clinicNumber);
						
						//increment the respective smoking status
						if (currentSmoker == true){
							int currentValue = ((Integer) smokingStatusElements.elementAt(0)).intValue();
							currentValue = currentValue + 1;
							smokingStatusElements.setElementAt(new Integer(currentValue), 0);
							System.out.println("incrementing current: " + currentValue);
							patientsStatuses.put(clinicNumber, smokingStatusElements);
						}
						else if (nonSmoker == true){
							int currentValue = ((Integer) smokingStatusElements.elementAt(1)).intValue();
							currentValue = currentValue + 1;
							smokingStatusElements.setElementAt(new Integer(currentValue), 1);
							System.out.println("incrementing non: " + currentValue);
							patientsStatuses.put(clinicNumber, smokingStatusElements);
						}
						else if (pastSmoker == true){
							int currentValue = ((Integer) smokingStatusElements.elementAt(2)).intValue();
							currentValue = currentValue + 1;
							smokingStatusElements.setElementAt(new Integer(currentValue), 2);
							System.out.println("incrementing past: " + currentValue);
							patientsStatuses.put(clinicNumber, smokingStatusElements);
						}
						else if (smoker == true){
							int currentValue = ((Integer) smokingStatusElements.elementAt(3)).intValue();
							currentValue = currentValue + 1;
							smokingStatusElements.setElementAt(new Integer(currentValue), 3);
							System.out.println("incrementing smoker: " + currentValue);
							patientsStatuses.put(clinicNumber, smokingStatusElements);
						}
						//account for the UNKNOWN category
						else {
							//do nothing
						}
					
						//patientsStatuses.put(clinicNumber, smokingStatusElements);
					}
					
					currentSmoker = false;
					nonSmoker = false;
					pastSmoker = false;
					smoker = false;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			try {
				reader.close();
				fr.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * performs the patient-level smoking status classification based on document-level classification
	 * each patient is assigned only one final smoking status
	 * 
	 * RULE:
	 * If exists only U assign U
	 * Else if exist N and there is no PCS assign N
	 * Else if exist S and there is no PC assign S
	 * Else if exist P or C (can be both)
 	 *   If exists either P or C assign P or C respectively
	 *   Else if exist both P and C 
	 *     If(C freq >= P freq) assign C
	 *     Else assign P  
	 * 
	 */
	public static void assignPatientLevelSmokingStatus(){
		Set<String> clinicNumbers = patientsStatuses.keySet();
		Iterator<String> itClinicNumbers = clinicNumbers.iterator();
		while (itClinicNumbers.hasNext()){
			String clinicNumber = (String) itClinicNumbers.next();
			Vector<?> smokingStatuses = (Vector<?>) patientsStatuses.get(clinicNumber);
			
			int current = ((Integer) smokingStatuses.elementAt(0)).intValue();
			int non = ((Integer) smokingStatuses.elementAt(1)).intValue();
			int past = ((Integer) smokingStatuses.elementAt(2)).intValue();
			int smoker = ((Integer) smokingStatuses.elementAt(3)).intValue();

			if (current == 0 && non == 0 && past == 0 && smoker == 0){
				finalAssignment.put(clinicNumber, "UNKNOWN");
			}
			else if(non>0 && past==0 && current==0 && smoker==0) {
				finalAssignment.put(clinicNumber, "NON_SMOKER");
			}
			else if(smoker>0 && past==0 && current==0) {
				finalAssignment.put(clinicNumber, "SMOKER");
			}
			else if(past>0 || current>0) {
				if(past>0 && current==0) {
					finalAssignment.put(clinicNumber, "PAST_SMOKER");
				}
				else if(past==0 && current>0) {
					finalAssignment.put(clinicNumber, "CURRENT_SMOKER");
				}
				else if(past>0 && current>0) {
					if(past<=current) {
						finalAssignment.put(clinicNumber, "CURRENT_SMOKER");
					}
					else {
						finalAssignment.put(clinicNumber, "PAST_SMOKER");
					}
				}
			}
			else {
				System.out.println("Undefined case");
				System.exit(1);
			}
		}
	}
	
	/*
	 * writes the clinic number and the final smoking status to a file
	 */
	public static void printToFile(){
	    BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(outputFile, false));
			Set<String> clinicNumbers = finalAssignment.keySet();
			Iterator<String> itClinicNumbers = clinicNumbers.iterator();
			while (itClinicNumbers.hasNext()){
				String clinicNumber = (String) itClinicNumbers.next();
				String smokingStatus = (String) finalAssignment.get(clinicNumber);
				out.write("\n" + clinicNumber + "|" + smokingStatus);
			}
			out.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	private static String inputFile = "R:/Dept/projects/Text/NHGRI/October2010/smokingstatus/data/record_resolution.txt";
	private static String outputFile = "R:/Dept/projects/Text/NHGRI/October2010/smokingstatus/data/record_resolution_patient.txt";
	private static HashMap<String, Vector<Integer>> patientsStatuses = new HashMap<String, Vector<Integer>>();
	private static HashMap<String, String> finalAssignment = new HashMap<String, String>();
	private static String delimiter = "\\,";
}