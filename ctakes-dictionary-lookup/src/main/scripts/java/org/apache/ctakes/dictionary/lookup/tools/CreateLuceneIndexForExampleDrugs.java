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

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
public class CreateLuceneIndexForExampleDrugs {

	/**
	 * Create a Lucene index containing some sample drug names
	 * The field names need to include the values of the fieldName attribute
	 * of the metaField elements and the lookupField element in the 
	 * LookupDescriptorFile for this dictionary.
	 * For example, if the LookupDescriptorFile being used is LookupDesc.xml
	 * and if it defines the following four elements for this dictionary:<br>
	 *   lookupField fieldName="first_word"<br>
	 *   metaField fieldName="code"<br>
	 * 	 metaField fieldName="preferred_designation"<br>
	 * 	 metaField fieldName="other_designation"<br>
	 * then the lucene index needs to include four fields with those names.
	 * 
	 * @param args unused/ignored
	 */
	public static void main(String args[]) throws Exception {

		// Name of the lucene index directory to be created 
		File indexDir = new File("C:/general_workspace/ctakes-dictionary-lookup/resources/lookup/drug_index");//C:/temp/lucene/" + "drug-index";
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
		boolean createFlag = true;

		IndexWriter writer = new IndexWriter(FSDirectory.open(indexDir), analyzer, createFlag, IndexWriter.MaxFieldLength.LIMITED);

		String [][] strings = {
				// rowID      code        (ignored)    first_word       preferred_designation
				{"5555555",  "C5555555",  "C5555555", "Acetaminophen", "Acetaminophen 80 mg chewable"}, 
				{"6666666",  "C6666666",  "C6666666", "Aspirin",       "Aspirin"}, 
				{"7777777",  "C7777777",  "C7777777", "Ibuprofen",     "Ibuprofen"}, 
				{"8888888",  "C8888888",  "C8888888", "Ibuprofen",     "Ibuprofen 200 mg"}, 
				{"99999999", "C99999999", "C99999999", "Ibuprofen",    "Ibuprofen 300 mg"}, 
				{"44444444", "C44444444", "C44444444", "Celexa",    "Celexa"}, 
				};
		
		int tcount = 0; 
		for (String [] t : strings) {
			int i=0;
			System.out.println("t = " + t);
			for (String s: t){
				System.out.println("s= " + s);			
			}
			Document document = new Document();
			//Field.Keyword("UNIQUE_DOCUMENT_IDENTIFIER_FIELD", t[i])); i++;
			document.add(new Field("UNIQUE_DOCUMENT_IDENTIFIER_FIELD", t[i], Field.Store.YES, Field.Index.NO));
			i++;
			//Field.Keyword("code", t[i])); i++;
			document.add(new Field("code", t[i], Field.Store.YES, Field.Index.NO));
			i++;
			//Field.Text("codeTokenized", t[i])); i++;
			document.add(new Field("codeTokenized", t[i], Field.Store.YES, Field.Index.ANALYZED));
			i++;
			//Field.Text("first_word", t[i])); i++;
			document.add(new Field("first_word", t[i], Field.Store.YES, Field.Index.ANALYZED));
			i++;
			//Field.Text("preferred_designation", t[i])); i++;
			document.add(new Field("preferred_designation", t[i], Field.Store.YES, Field.Index.ANALYZED));
			i++;
			tcount++;
			writer.addDocument(document);
		}
		writer.close();
		System.out.println("Wrote lucene index: " + writer);
	}
	
}



