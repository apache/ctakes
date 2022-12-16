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
package org.apache.ctakes.smokingstatus.MLutil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.ctakes.smokingstatus.Const;


public class GenerateTrainingData {
	Set<String> keywords;
	Set<String> stopwords;
	List<List<Comparable>> features;

	GenerateTrainingData(String keywordsFileName, String stopwordsFileName) {
		features = new ArrayList();
		stopwords = new HashSet<String>();
		keywords = new HashSet<String>();

		try {
			keywords = readLinesFromFile(keywordsFileName);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			stopwords = readLinesFromFile(stopwordsFileName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Set<String> readLinesFromFile(String fileName)
			throws IOException {
		Set<String> returnValues = new HashSet<String>();
		File file = new File(fileName);
		BufferedReader fileReader = new BufferedReader(new FileReader(file));

		String line;
		while ((line = fileReader.readLine()) != null) {
			if (!line.startsWith("//") && line.trim().length() > 0)
				returnValues.add(line.toLowerCase());
		}
		return returnValues;
	}

	/**
	 * set "features" - list of features. Each list is a feature vector (the
	 * last element is the class label)
	 * 
	 * format of input (fname): sentence|class_label (class_label: C, P, S)
	 */
	public void makeFeatures(String fname) {
		String str = "", cls = "", sen = "";

		try {
			BufferedReader fin = new BufferedReader(new FileReader(fname));
			while ((str = fin.readLine()) != null) {
				if (str.length() == 0)
					continue;

				StringTokenizer strTok = new StringTokenizer(str, "|");

				while (strTok.hasMoreTokens()) {
					sen = strTok.nextToken().trim();
					cls = strTok.nextToken().trim();
				}

				// System.out.println(sen);

				// assign class label
				if (cls.toLowerCase().startsWith("p"))
					cls = Const.CLASS_PAST_SMOKER;
				else if (cls.toLowerCase().startsWith("c"))
					cls = Const.CLASS_CURR_SMOKER;
				else if (cls.toLowerCase().startsWith("s"))
					cls = Const.CLASS_SMOKER;
				else {
					System.out.println("Undefined class label:" + cls);
					System.exit(1);
				}

				sen = sen.toLowerCase().replaceAll("[.?!:;()',\"{}<>#+]", " ")
						.trim();
				sen = sen.toLowerCase().replaceAll("-{2,}", " ").trim();

				String[] senTokens = sen.split("\\s");
				List<String> unigrams = new ArrayList();
				List bigrams = new ArrayList();

				for (int i = 0; i < senTokens.length; i++)
					if (!stopwords.contains(senTokens[i])
							&& senTokens[i].trim().length() > 0)
						unigrams.add(senTokens[i]);

				for (int i = 0; i < unigrams.size() - 1; i++)
					bigrams.add((String) unigrams.get(i) + "_"
							+ (String) unigrams.get(i + 1));

				List<Comparable> feature = new ArrayList();

				// make binary keywords vector
				Iterator<String> itr = keywords.iterator();
				while (itr.hasNext()) {
					String k = (String) itr.next();
					int val = 0;

					// bigram
					if (k.indexOf("_") != -1) {
						for (int i = 0; i < bigrams.size(); i++) {
							if (k.equalsIgnoreCase((String) bigrams.get(i))) {
								val = 1;
								break;
							}
						}
					}
					// uigram
					else {
						for (int i = 0; i < unigrams.size(); i++) {
							if (k.equalsIgnoreCase((String) unigrams.get(i))) {
								val = 1;
								break;
							}
						}
					}

					feature.add(new Integer(val));
				}

				// date feature - naive feature
				if (true) {
					int hasYear = 0;
					for (int i = 0; i < unigrams.size(); i++) {
						String s = (String) unigrams.get(i);
						// updated Apr-9-2009
						if (s.matches("19\\d\\d") || s.matches("19\\d\\ds")
								|| s.matches("20\\d\\d")
								|| s.matches("20\\d\\ds")
								|| s.matches("[1-9]0s")
								|| s.matches("\\d{1,2}[/-]\\d{1,2}")
								|| s.matches("\\d{1,2}[/-]\\d{4}")
								|| s.matches("\\d{1,2}[/-]\\d{1,2}[/-]\\d{2}")
								|| s.matches("\\d{1,2}[/-]\\d{1,2}[/-]\\d{4}")) {
							hasYear = 1;
							break;
						}
					}
					feature.add(new Integer(hasYear));
				}

				// add class label
				feature.add(new String(cls));

				// add feature to the feature set
				features.add(feature);
			}
			fin.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void printLibsvmDataToFile(String fname) {
		try {
			PrintStream fout = new PrintStream(new FileOutputStream(fname));

			Iterator<List<Comparable>> fIter = features.iterator();
			while (fIter.hasNext()) {
				List<?> f = (ArrayList<?>) fIter.next();
				String clsStr = (String) f.get(f.size() - 1);
				int cls = -1;

				if (clsStr.equals(Const.CLASS_CURR_SMOKER))
					cls = Const.CLASS_CURR_SMOKER_INT;
				else if (clsStr.equals(Const.CLASS_PAST_SMOKER))
					cls = Const.CLASS_PAST_SMOKER_INT;
				else if (clsStr.equals(Const.CLASS_SMOKER))
					cls = Const.CLASS_SMOKER_INT;
				else {
					System.out.println("Undefined class label:" + clsStr);
					System.exit(1);
				}

				fout.print(cls + " "); // class label
				for (int i = 0; i < f.size() - 1; i++) {
					fout.print((i + 1) + ":" + f.get(i) + " ");
				}
				fout.print('\n');
			}
			fout.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * keywordsFile and stopwordsFile must point to the files in the resources
	 * dataFile is your own sentence-level data in the format (class_label: C,
	 * P, S): sentence|class_label sentence|class_label ... libsvmDataFile is
	 * the file to write libsvm data
	 */
	public static void main(String[] args) {
		String keywordsFile = "C:/cTAKES-1.0.5/smoking status/resources/ss/data/PCS/keywords_PCS_NHGRI.txt";
		String stopwordsFile = "C:/cTAKES-1.0.5/smoking status/resources/ss/data/PCS/stopwords_PCS.txt";
		String dataFile = "C:/Temp/SentenceLevelSmokingStatus_PCS.txt";
		String libsvmDataFile = "C:/Temp/libsvm_data.txt";

		GenerateTrainingData gtd = new GenerateTrainingData(keywordsFile,
				stopwordsFile);
		gtd.makeFeatures(dataFile); // In feature the last element is a class
									// label
		gtd.printLibsvmDataToFile(libsvmDataFile);
	}
}
