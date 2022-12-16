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

import java.util.Date;

import org.apache.log4j.Logger;

/**
 * parses a line of data from the negex gold standard
 * For the few instances where there is somethign wrong with the gold standard, corrects/rejects 
 * some mistakes in gold standard (e.g. where entity is longer than the sentence!)
 */
public class NegExAnnotation {
	static Logger LOGGER = Logger.getLogger(NegExAnnotation.class);

	/**
	 * Each line consist of following fields
	 * line number
	 * Condition (from within sentence)
	 * sentence	
	 * negation_status (Negated, Affirmed) also handles if possible)
	 * 
	 * 
	 * @param args
	 */
	
	String lineNumber;
	String entityCoveredText;
	String sentenceText;
	String polarity; // -1 means negated. 1 means not negated. Note, shares field with possible
	String possible; // 1 means possible. 0 = either negated or affirmed. shares field with negated/polarity
	String temporality;
	String experiencer;
	String begin;
	String end;
	
	public NegExAnnotation(String lineWithAnnotation) {
		
		String s = lineWithAnnotation.trim();
		if (s.length()==0) throw new RuntimeException("no annotation or sentence data found");
		
		String [] fields = lineWithAnnotation.split("\t");
		int numRequiredFields = 4;
		if (fields.length < numRequiredFields) { 
			throw new RuntimeException("Not enough fields on line '" + lineWithAnnotation + "', need at least " + numRequiredFields + "fields.");
		}
		
		lineNumber = fields[0].trim();
		
		String INCORRECT_LINE1 = "OSTEOCHONDRAL IRREGULARITY WITHIN THE 45 DEGREE FLEXION ZONE  OF THE LATERAL FEMORAL CONDYLE COMPATIBLE WITH OSTEOCHONDRAL  LESION. INCREASED SCLEROSIS WITHIN THIS REGION";
		String CORRECTED_LINE1 = "OSTEOCHONDRAL IRREGULARITY WITHIN THE 45 DEGREE FLEXION ZONE  OF THE LATERAL FEMORAL CONDYLE COMPATIBLE WITH OSTEOCHONDRAL  LESION.";


		entityCoveredText = fields[1].trim();
		if (entityCoveredText.toLowerCase().equals(INCORRECT_LINE1.toLowerCase())) { // correct an error in the gold standard
			entityCoveredText = CORRECTED_LINE1;
		}
		if (entityCoveredText.length()<1) throw new RuntimeException("Error parsing entityCoveredText from line '" + lineWithAnnotation + "'");

		if (entityCoveredText.startsWith("Pharynx good.")) entityCoveredText = "Pharynx good.";
		if (entityCoveredText.toLowerCase().startsWith("neck:  supple.")) entityCoveredText = entityCoveredText.substring(0,"NECK:  Supple.".length());
		
		String INCORRECT_LINE3 = "RIGHT THYROID:  SATISFACTORY FOR INTERPRETATION.  NEGATIVE FOR MALIGNANT CELLS.  COLLOID NODULE";
		String CORRECTED_LINE3 = "RIGHT THYROID:  SATISFACTORY FOR INTERPRETATION.";
		if (entityCoveredText.toLowerCase().equals(INCORRECT_LINE3.toLowerCase())) { // correct an error in the gold standard
			entityCoveredText = CORRECTED_LINE3;
		}
		
		if (entityCoveredText.toLowerCase().equals("tolerating p.o. intake")) {
			//1290	tolerating p.o. intake	intake and voiding without difficulty and ambulating   independently.	Affirmed
			LOGGER.warn("Unable to handle at this time because gold standard is incorrect");
			throw new RuntimeException("Skip this one as gold standard has a problem");
		}
		sentenceText = fields[2].trim();
		String INCORRECT_LINE2 = "The patient states   that she was able to tolerate some p.o.";
		String CORRECTED_LINE2 = "The patient states   that she was able to tolerate some p.o. fluids";
		if (sentenceText.equals(INCORRECT_LINE2)) sentenceText = CORRECTED_LINE2; // correct an error in the gold standard

		int position = sentenceText.toLowerCase().indexOf(entityCoveredText.toLowerCase());
		char DQUOTE = '"';
		if (position<0) {
			if (entityCoveredText.charAt(0)==DQUOTE) entityCoveredText = entityCoveredText.substring(1);
			int last = entityCoveredText.length()-1;
			if (entityCoveredText.charAt(last)==DQUOTE) entityCoveredText = entityCoveredText.substring(0, last);
			position = sentenceText.toLowerCase().indexOf(entityCoveredText.toLowerCase());
			if (position<0) {
				throw new RuntimeException("Did not find entity text '" + entityCoveredText + "' within sentence '" + sentenceText + "'");
			}
		}
		String rest = sentenceText.substring(position+1);
		if (rest.contains(entityCoveredText)) {
			LOGGER.error("Assuming 2nd occurrence is correct occurenence of '" + entityCoveredText + "'.");
			position = sentenceText.toLowerCase().indexOf(entityCoveredText.toLowerCase(), position+1);
			//throw new RuntimeException("Unable to handle two occurences of entity within sentence");
		}
		begin = position + "";
		end = (position +  entityCoveredText.length()) + "";
		
		String field3LowerCase = fields[3].trim().toLowerCase();
		
		if (field3LowerCase.equals("possible")) {
			polarity = "1";
			possible = "1";
		} else if (field3LowerCase.equals("affirmed")) {
			polarity = "1";
			possible = "0";
		} else if (field3LowerCase.equals("negated")) {
			polarity = "-1";
			possible = "0";
		}
		
//		if (fields.length > 4 && fields[4]!=null && fields[4].length()>0) throw new RuntimeException("Does not support temporality yet");
//		if (fields.length > 5 && fields[5]!=null && fields[5].length()>0) throw new RuntimeException("Does not support experiencer yet");

	}
	
	public String toString() {
		 
		String s =  entityCoveredText + " (" + begin + ", " + end + ")  polarity=" + polarity + " possible=" + possible;
		s = s + "\n" + "in '" + sentenceText + "'";
		return s;
		
	}
	/**
	 * test a single line
	 * @param args
	 */
	public static void main(String[] args) {
		String line = "2	pulmonic regurgitation	There is trace PULMONIC REGURGITATION.	Affirmed";
		NegExAnnotation anno = new NegExAnnotation(line);
		System.out.println("Was able to create NegExAnnotation successfully at " + new Date());
		System.out.println(anno.toString());
		
	}

}
