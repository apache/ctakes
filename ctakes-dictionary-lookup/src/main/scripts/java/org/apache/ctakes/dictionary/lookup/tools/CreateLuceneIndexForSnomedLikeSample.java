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
package org.apache.ctakes.dictionary.lookup.tools;

/**
 * See http://www.onjava.com/pub/a/onjava/2003/01/15/lucene.html?page=1
 */
import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/** 
 * Used to create the example Lucene indexes for the opensource pipeline.
 * Creates entries for illustration purposes only, not real CUIs or codes. 
 *
 */
public class CreateLuceneIndexForSnomedLikeSample {

	/**
	 * Create 2 Lucene indexes. 
	 * One contains a dictionary of UMLS concepts and CUIs.
	 * The other contains mappings of codes from an ontology such as SNOMED-CT to UMLS CUIs.
	 * 
	 * NOTE: the CUIs and concept codes in this program are fictional, for illustration
	 * purposes only.
	 * The field names need to include the values of the fieldName attribute
	 * of the metaField elements and the lookupField element in the 
	 * LookupDescriptorFile for this dictionary.
	 * For example, if the LookupDescriptorFile being used is LookupDesc.xml
	 * and if it defines the following four elements for this dictionary:<br>
	 *   lookupField fieldName="first_word"<br>
	 *   metaField fieldName="cui"<br>
	 * 	 metaField fieldName="tui"<br>
	 * 	 metaField fieldName="text"<br>
	 * then the lucene index needs to include four fields with those names.
	 * 
	 * @param args unused/ignored
	 */
	public static void main(String args[]) throws Exception {

		File indexDir = new File("C:/temp/lucene/" + "snomed-like_sample"); // lookup by first_word, results contain UMLS CUIs
		File indexDir2 = new File("C:/temp/lucene/" + "snomed-like_codes_sample"); // for getting snomed codes for a CUI
		
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
		Analyzer analyzer2 = new StandardAnalyzer(Version.LUCENE_40);
		boolean createFlag = true;

		IndexWriter writer = new IndexWriter(FSDirectory.open(indexDir), analyzer, createFlag, IndexWriter.MaxFieldLength.LIMITED);

		ArrayList strings = new ArrayList();

		getEntries(strings); // Get the strings for the snomed-like_sample lucene index

		int tcount = 0;
		// for (String [] t : strings) {
		for (int loopi = 0; loopi < strings.size(); loopi++) {
			String[] t = (String[]) strings.get(loopi);
			int i = 0;
			System.out.println("t = " + t);
			// for (String s: t){
			for (int loopj = 0; loopj < t.length; loopj++) {
				String s = t[loopj];
				System.out.println("s= " + s);
			}
			Document document = new Document();
			//Field.Keyword("UNIQUE_DOCUMENT_IDENTIFIER_FIELD", t[i]));
			document.add(new Field("UNIQUE_DOCUMENT_IDENTIFIER_FIELD", t[i], Field.Store.YES, Field.Index.NO));
			i++;
			//Field.Keyword("cui", t[i]));
			document.add(new Field("cui", t[i], Field.Store.YES, Field.Index.NO));
			i++;
			//Field.Text("first_word", t[i]));
			document.add(new Field("first_word", t[i], Field.Store.YES, Field.Index.ANALYZED));
			i++;
			//Field.Text("text", t[i]));
			document.add(new Field("text", t[i], Field.Store.YES, Field.Index.ANALYZED));
			i++;
			//Field.Text("oid", t[i]));
			document.add(new Field("oid", t[i], Field.Store.YES, Field.Index.ANALYZED));
			i++;
			//Field.Text("oui", t[i]));
			document.add(new Field("oui", t[i], Field.Store.YES, Field.Index.ANALYZED));
			i++;
			//Field.Text("termStatus", t[i]));
			document.add(new Field("termStatus", t[i], Field.Store.YES, Field.Index.ANALYZED));
			i++;
			//Field.Text("tui", t[i]));
			document.add(new Field("tui", t[i], Field.Store.YES, Field.Index.ANALYZED));
			i++;

			tcount++;
			writer.addDocument(document);
		}
		writer.close();
		System.out.println("Wrote  " + writer + " at " + new Date());

		
		
		IndexWriter writer2 = new IndexWriter(FSDirectory.open(indexDir2), analyzer2, createFlag,  IndexWriter.MaxFieldLength.LIMITED);

		strings  = getSnomedCodes(); // Get the strings for the snomed-like_codes_sample lucene index

		tcount = 0;
		// for (String [] t : strings) {
		for (int loopi = 0; loopi < strings.size(); loopi++) {
			String[] t = (String[]) strings.get(loopi);
			int i = 0;
			System.out.println("t = " + t);
			// for (String s: t){
			for (int loopj = 0; loopj < t.length; loopj++) {
				String s = t[loopj];
				System.out.println("s= " + s);
			}
			Document document = new Document();
			//Field.Keyword("UNIQUE_DOCUMENT_IDENTIFIER_FIELD", t[i]));
			document.add(new Field("UNIQUE_DOCUMENT_IDENTIFIER_FIELD", t[i], Field.Store.YES, Field.Index.NO));
			i++;
			// allow upper case input on search
			document.add(new Field("cui", t[i], Field.Store.YES, Field.Index.NOT_ANALYZED));
			i++;
			//Field.Text("code", t[i]));
			document.add(new Field("code", t[i], Field.Store.YES, Field.Index.ANALYZED));
			i++;
			//Field.Text("termStatus", t[i]));
			document.add(new Field("termStatus", t[i], Field.Store.YES, Field.Index.ANALYZED));
			i++;

			tcount++;
			writer2.addDocument(document);
		}
		writer2.close();
		System.out.println("Wrote  " + writer + " at " + new Date());

	}


	private static void getEntries(ArrayList strings) {

		String [] []  entries =  {
				
				// <!-- the following values are used as TUIs for testing: T_AS, T_PR, T_DD, T_SS -->
				{ "00000001", "Cui0001", "transverse",       "transverse colon",        "CodingScheme#Cui0001#Ver", "Oui0001", "+", "T_AS"},
				{ "00000002", "Cui0002", "knee",             "knee",                    "CodingScheme#Cui0002#Ver", "Oui0002", "+", "T_AS"}, // same TUI
				
				{ "00000100", "Cui0100", "biopsy",           "biopsy",                  "CodingScheme#Cui0100#Ver", "Oui0003", "+", "T_PR"},
				{ "00000101", "Cui0101", "needle",           "needle biopsy",           "CodingScheme#Cui0101#Ver", "Oui0004", "+", "T_PR"},
				{ "00000102", "Cui0102", "resting",          "resting metabolic rate",  "CodingScheme#Cui0102#Ver", "Oui0005", "+", "T_PR"}, 
				
			 	{ "00000205", "Cui0205", "cough",            "cough",                   "CodingScheme#Cui0205#Ver", "Oui0006", "+", "T_SS"},
			 	{ "00000206", "Cui0206", "pain",             "pain",                    "CodingScheme#Cui0206#Ver", "Oui0007", "+", "T_SS"},
				
				{ "00000306", "Cui0306", "carcinoma",        "carcinoma",               "CodingScheme#Cui0306#Ver", "Oui0009", "+", "T_DD"},
				{ "0000a307", "Cui0307", "carcinoma",        "carcinoma secondary",     "CodingScheme#Cui0307#Ver", "Oui0009", "+", "T_DD"}, // same first_word
				{ "0000b307", "Cui0307", "carcinoma",        "carcinoma metastatic",    "CodingScheme#Cui0307#Ver", "Oui0009", "+", "T_DD"}, // same CUI
				{ "00000309", "Cui0309", "hyperlipidemia",   "hyperlipidemia",          "CodingScheme#Cui0309#Ver", "Oui0011", "+", "T_DD"},
				
				{ "xxxxxxxx", "Cuixxxx", "zyxabc123",        "zyxabc123 for testing",   "CodingScheme#Cuixxxx#Vxx", "Ouixxxx", "-", "T_xx"}  // termStatus "-"
				
				};
		
		
		for (int i=0; i < entries.length; i++) {
			strings.add(entries[i]);
		}
	}

	
	private static ArrayList getSnomedCodes() {
		
		String [] []  entries =  {
                // rowId     umlsCode   snomedCode
				{ "000001", "Cui0001", "Oui0001", "+"},  // pretend 2 Snomed codes for this umls term
				{ "000002", "Cui0001", "Oui2221", "+"},

				{ "000003", "Cui0002", "Oui0002", "+"},

				{ "000004", "Cui0100", "Oui0003", "+"},  // pretend 3 Snomed codes map to this umls term
				{ "000005", "Cui0100", "Oui2223", "+"},
				{ "000006", "Cui0100", "Oui3333", "+"},

				{ "000007", "Cui0101", "Oui0004", "+"},
				{ "000008", "Cui0102", "Oui0005", "+"},
				
				{ "000009", "Cui0205", "Oui0006", "+"},
				{ "000010", "Cui0206", "Oui0007", "+"},
				
				{ "000011", "Cui0306", "Oui0009", "+"},
				{ "000012", "Cui0307", "Oui0009", "+"},  // note pretend only 1 example snomed code for the 2 UMLS terms
				
				{ "000013", "Cui0309", "Oui0011", "+"},  // pretend 2 Snomed codes for this umls term
				{ "000014", "Cui0309", "Oui0012", "+"},
				{ "xxxxxx", "Cuixxxx", "zzzzzzz", "+"}

				
				};
		
		ArrayList strings = new ArrayList();
		for (int i=0; i < entries.length; i++) {
			strings.add(entries[i]);
		}
		
		return strings;
	}

}

