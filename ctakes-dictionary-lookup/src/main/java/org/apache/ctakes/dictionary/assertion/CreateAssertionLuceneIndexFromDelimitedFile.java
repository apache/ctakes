/*
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
package org.apache.ctakes.dictionary.assertion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import org.apache.ctakes.core.nlp.tokenizer.OffsetComparator;
import org.apache.ctakes.core.nlp.tokenizer.Token;
import org.apache.ctakes.core.nlp.tokenizer.TokenizerPTB;

/**
 * Driver for populating a Lucene Index with assertion cue phrases, so that the
 * tokenization of the dictionary entries matches the tokenization that will be
 * done to clinical text during pipeline processing. Just as the pipeline can
 * use a file of hyphenated words to control which words should be considered
 * as a single token, the creation of the dictionary entries can use a file of
 * hyphenated words so the dictionary entries are tokenized in the same way as
 * the clinical text will be.
 */
public class CreateAssertionLuceneIndexFromDelimitedFile {
	private static TokenizerPTB tokenizer = new TokenizerPTB();

	// The path to a directory containing one or more pipe-delimited files
  // A new directory "assertion_cue_phrase_index" will be created in the
  // parent. This new directory will be the lucene index directory.
	private static String directoryOfDelimitedFiles = null;
	// directoryOfDelimitedFiles =
	// "/temp/pipe-delimited-dictionary-data/RxNorm";

	private IndexWriter iwriter = null;

	private int idCount = 0;

	private final String ID = "UNIQUE_DOCUMENT_IDENTIFIER_FIELD";

	private final String rxNormCode = "codeRxNorm";
	private final String Code = "code";
	private final String CodeToken = "codeTokenized";
	private final String FirstWord = "first_word";
	private final String OtherDesig = "other_designation";
	private final String PreferDesig = "preferred_designation";
	
	public static final String CUE_PHRASE_FIELD_NAME = "cuePhrase";
	public static final String CUE_PHRASE_CATEGORY_FIELD_NAME = "cuePhraseCategory";
  public static final String CUE_PHRASE_FAMILY_FIELD_NAME = "cuePhraseFamily";
  public static final String CUE_PHRASE_FIRST_WORD_FIELD_NAME = "cuePhraseFirstWord";
	

	/**
	 * Constructor
	 * 
	 * @param Tokenizer
	 *            Used to tokenize the dictionary entries
	 */
	public CreateAssertionLuceneIndexFromDelimitedFile(TokenizerPTB tokenizer)
			throws Exception {
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
		String defaultLoc = new File(directoryOfDelimitedFiles)
				.getAbsolutePath();
		boolean error = false;
		long numEntries = 0;
		try {
			Directory directory = FSDirectory.open(new File(
					new File(defaultLoc).getParent() + "/assertion_cue_phrase_index"));

			IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_40, analyzer);
			iwriter = new IndexWriter(directory, indexWriterConfig);
//      iwriter = new IndexWriter(directory, analyzer, true,
//          IndexWriter.MaxFieldLength.LIMITED);
			// Process multiple files in directory

			File file = new File(defaultLoc);
			if (file.isDirectory()) {
				String[] processFiles = file.list();
				for (int i = 0; i < processFiles.length; i++) {
					System.out.println("Process Each File in " + file.getName()
							+ "...");
					File nextFile = new File(directoryOfDelimitedFiles + "/"
							+ processFiles[i]);

					BufferedReader br = new BufferedReader(new FileReader(
							nextFile));
					String record = "";
					while ((record = br.readLine()) != null) {
						// System.out.println(" record so far out of " + record
						// );
					  
					  String splitRecord[] = record.split("\\|");
					  if (splitRecord.length == 0)
					  { continue; }
					  String cuePhrase = splitRecord[0];
					  String cuePhraseCategory = "default_category";
					  String cuePhraseFamily = "default_family";
					  if (splitRecord.length >= 2)
					  {
					    cuePhraseCategory = splitRecord[1];
					    cuePhraseFamily   = splitRecord[2];
					    if (cuePhraseCategory == null || cuePhraseCategory.isEmpty())
					    {
					      cuePhraseCategory = "category__" + cuePhraseFamily;
					    }
					  }

//						String ssubstring = cueWordCategory.substring(cueWordCategory
//								.indexOf('|') + 1);
//						String source = ssubstring.substring(0,
//								ssubstring.indexOf('|'));
//
//						String tsubstring = ssubstring.substring(ssubstring
//								.indexOf('|') + 1);
//						String codeFromSource = tsubstring.substring(0,
//								tsubstring.indexOf('|'));
//
//						String usubstring = tsubstring.substring(tsubstring
//								.indexOf('|') + 1);
//						String isPreferred = usubstring.substring(0,
//								usubstring.indexOf('|'));
//
//						String semIds = usubstring.substring(usubstring
//								.indexOf('|') + 1);
						// System.out.println(" " + cui +
						// " processed so far out of " +
						// propertyValue + " -- " + sourceCC +" ispref "
						// +isPreferred +
						// " semIds " +semIds);
//						writeToFormatLucene(cueWord, propertyValue, source,
//								codeFromSource, isPreferred, semIds);
            writeToFormatLucene(cuePhrase, cuePhraseCategory, cuePhraseFamily);
						numEntries++;
					}
				}
			}
		} catch (IOException io) {
			System.out.println("IO exception caught");
			error = true;
		} finally {
			try {
				iwriter.maybeMerge();
				iwriter.close();
				if (!error) {
					System.out.println("Index created with " + numEntries
							+ " entries.");
				}
			} catch (IOException io) {
				System.out.println("IO exception caught");
			}
		}
	}

	public static void main(String[] args) {
		System.gc();

		if (args.length == 1) { // If no file of hyphenated words given
			try {
				directoryOfDelimitedFiles = args[0];
				tokenizer = new TokenizerPTB();
				new CreateAssertionLuceneIndexFromDelimitedFile(tokenizer);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (args.length == 3) { // else, use the file of hyphenated words
										// during tokenization
			try {

				directoryOfDelimitedFiles = args[0];
				// ** hyphnated file no longer needed. using the new PTB
				// tokenizer instead. **
				// String hyphFileLoc = args[1];
				// int freqCutoff = Integer.parseInt(args[2]);
				// Map hyphMap = loadHyphMap(hyphFileLoc);
				// System.out.println("Processing hyphMap from : " +
				// hyphFileLoc);

				tokenizer = new TokenizerPTB();
				new CreateAssertionLuceneIndexFromDelimitedFile(tokenizer);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println(getUsage());
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
	public static String load(String filename) throws FileNotFoundException,
			IOException {
		String msg = "";
		File f = new File(filename);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = br.readLine();
		while (line != null) {
			msg += line + "\n";
			line = br.readLine();
		}
		br.close();

		return msg;
	}

	/**
	 * Loads hyphenated words and a frequency value for each, from a file.
	 * 
	 * @param filename
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static Map loadHyphMap(String filename)
			throws FileNotFoundException, IOException {
		Map hyphMap = new HashMap();
		File f = new File(filename);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = br.readLine();
		while (line != null) {
			StringTokenizer st = new StringTokenizer(line, "|");
			if (st.countTokens() == 2) {
				String hyphWord = st.nextToken();
				Integer freq = new Integer(st.nextToken());
				hyphMap.put(hyphWord.toLowerCase(), freq);
			} else {
				System.out.println("Invalid hyphen file line: " + line);
			}
			line = br.readLine();
		}
		br.close();

		return hyphMap;
	}

	/**
	 * Prints out the tokenized results, for debug use.
	 * 
	 * @param text
	 * @param results
	 */
	public static void printResults(String text, List results) {
		System.out.println("Text: " + text);
		for (int i = 0; i < results.size(); i++) {
			Token t = (Token) results.get(i);
			String typeStr = "";
			switch (t.getType()) {
			case Token.TYPE_WORD:
				typeStr = "word       ";
				break;
			case Token.TYPE_PUNCT:
				typeStr = "punctuation";
				break;
			case Token.TYPE_NUMBER:
				typeStr = "number     ";
				break;
			case Token.TYPE_EOL:
				typeStr = "end of line";
				break;
			case Token.TYPE_CONTRACTION:
				typeStr = "contraction";
				break;
			case Token.TYPE_SYMBOL:
				typeStr = "symbol     ";
				break;
			default:
				typeStr = "unknown    ";
			}

			String capsStr = "";
			switch (t.getCaps()) {
			case Token.CAPS_ALL:
				capsStr = "A";
				break;
			case Token.CAPS_NONE:
				capsStr = "N";
				break;
			case Token.CAPS_MIXED:
				capsStr = "M";
				break;
			case Token.CAPS_FIRST_ONLY:
				capsStr = "F";
				break;
			default:
				capsStr = "?";
			}

			String numPosStr = "";
			switch (t.getNumPosition()) {
			case Token.NUM_FIRST:
				numPosStr = "F";
				break;
			case Token.NUM_MIDDLE:
				numPosStr = "M";
				break;
			case Token.NUM_LAST:
				numPosStr = "L";
				break;
			case Token.NUM_NONE:
				numPosStr = "N";
				break;
			default:
				numPosStr = "?";
			}

			String intStr = "";
			if (t.isInteger()) {
				intStr = "Y";
			} else {
				intStr = "N";
			}

			System.out.println("Token:" + " type=[" + typeStr + "]" + " caps=["
					+ capsStr + "]" + " npos=[" + numPosStr + "]" + " int=["
					+ intStr + "]" + " offsets=[" + t.getStartOffset() + ","
					+ t.getEndOffset() + "]" + "\t\t" + "text=["
					+ text.substring(t.getStartOffset(), t.getEndOffset())
					+ "]");
		}
	}

	/**
	 * @return A string showing usage example (parameters)
	 */
	public static String getUsage() {
		return "java LucenePopulateDriver <dir-containing-textfile(s)> [hyphenfile] [freqcutoff]";
	}

	protected void writeToFormatLucene(String cuePhrase, String cuePhraseCategory, String cuePhraseFamily) {

		Document doc = new Document();

		try {

			// Print the name out

			idCount++;
			//if (idCount % 10000 == 0)
				System.out.println(" " + idCount
						+ " processed so far out of total");
				
			doc.add(new TextField("cuePhrase", cuePhrase, Field.Store.YES));
			doc.add(new StringField("cuePhraseCategory", cuePhraseCategory, Field.Store.YES));
			doc.add(new StringField("cuePhraseFamily", cuePhraseFamily, Field.Store.YES));
			

			List list = tokenizer.tokenize(cuePhrase);
			Collections.sort(list, new OffsetComparator());

			Iterator tokenItr = list.iterator();
			Token t;
			int tCount = 0;
			String firstTokenText = "";
			String tokenizedCuePhrase = "";

			while (tokenItr.hasNext()) {
				tCount++;
				t = (Token) tokenItr.next();
				if (tCount == 1) {
					firstTokenText = t.getText(); // first token (aka
													// "first word")
					tokenizedCuePhrase += t.getText();
				} else { // use blank to separate tokens
					tokenizedCuePhrase = tokenizedCuePhrase + " " + t.getText();
				}

			}

			doc.add(new StringField(CUE_PHRASE_FIRST_WORD_FIELD_NAME, firstTokenText, Field.Store.YES));

			iwriter.addDocument(doc);

			//String data = cui + "|" + firstTokenText + "|" + tokenizedDesc + "|" + codeInSource + "|" + source + "|" + semId + '\n';
			String data = cuePhrase + "|" + cuePhraseCategory + "|" + tokenizedCuePhrase + '\n';
			writeToFile (data);

		} catch (IOException io) {
			System.out.println("IOException in document : io "
					+ io.getLocalizedMessage());

		} catch (Exception exc) {
			System.out.println("Exception in document : exc "
					+ exc.getLocalizedMessage());
		}

		// writeToOutPutFile(cui + "|" + desc + "|" + source + "|" + cc + "|" +
		// termStatus + "|" + semId);
	}

	public void writeToFile(String str) {
		try {
			// Create the output file of sample.txt
			FileWriter fstream = new FileWriter(
					"sample.txt",
					true);

			// Write data into the file
			BufferedWriter out = new BufferedWriter(fstream);

			out.write(str);
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
