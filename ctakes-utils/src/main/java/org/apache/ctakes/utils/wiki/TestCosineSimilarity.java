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
package org.apache.ctakes.utils.wiki;

import java.io.IOException;
import java.util.Scanner;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryparser.classic.ParseException;

public class TestCosineSimilarity {

	public static void main(String[] args) throws CorruptIndexException, IOException, ParseException {
		boolean approx = true;
//		WikiIndex wikipediaIndex = new WikiIndex(WikiIndex.defaultMaxHits, "/home/tmill/Documents/wiki/index_nometa", "text", approx);
		WikiIndex wikipediaIndex = new WikiIndex(5, "/home/tmill/Documents/wiki/index_med_5k", "text", approx);
		//		WikiIndex wikipediaIndex = new WikiIndex(WikiIndex.defaultMaxHits, "/home/tmill/mnt/prv/data/index_vectors_notext", "text");

		
		wikipediaIndex.initialize();
		//		wikipediaIndex.useCache = true;
		System.out.println("Index loaded... Press enter to continue...");
		Scanner scanner = new Scanner(System.in);
		String line = scanner.nextLine();
//		System.out.println("Thanks for typing: " + line);
		
		double sim = wikipediaIndex.getCosineSimilarity("the procedure", "continuous Baker Baker dialysis");
		System.out.println("Sim is: " + sim);
		sim = wikipediaIndex.getCosineSimilarity("the procedure", "an orthotic liver transplant");
		System.out.println("Sim is: " + sim);
		sim = wikipediaIndex.getCosineSimilarity("the procedure", "transplant");
		System.out.println("Sim is: " + sim);
		
		long start = System.currentTimeMillis();

		for(int i = 0; i < 10; i++){
			System.out.println("i = " + i);
			if(i == 1) start = System.currentTimeMillis();
			
			double cosine0 = wikipediaIndex.getCosineSimilarity("heart disease", "microsoft");
			System.out.println("Similarity score: " + cosine0 + " took " + (System.currentTimeMillis()-start) + " ms to compute.");

			double cosine1 = wikipediaIndex.getCosineSimilarity("heart disease", "smoking");
			System.out.println("Similarity score: " + cosine1 + " took " + (System.currentTimeMillis()-start) + " ms to compute.");

			double cosine2 = wikipediaIndex.getCosineSimilarity("aspirin", "tylenol");
			System.out.println("Similarity score: " + cosine2 + " took " + (System.currentTimeMillis()-start) + " ms to compute.");

			double cosine3 = wikipediaIndex.getCosineSimilarity("aspirin", "ibuprofen");
			System.out.println("Similarity score: " + cosine3 + " took " + (System.currentTimeMillis()-start) + " ms to compute.");

			double cosine4 = wikipediaIndex.getCosineSimilarity("advil", "ibuprofen");
			System.out.println("Similarity score: " + cosine4 + " took " + (System.currentTimeMillis()-start) + " ms to compute.");
		}
		System.out.println("10 iterations took: " + (System.currentTimeMillis()-start) + " ms to compute.");
		ApproximateMath.dumpCache();
		//		Scanner scanner = new Scanner(System.in);
		//		System.out.println("Enter concept 1:");
		//		while(scanner.hasNextLine()){
		//			String con1 = scanner.nextLine().trim();
		//			System.out.println("Enter concept 2: ");
		//			String con2 = scanner.nextLine().trim();
		//			double cos = wikipediaIndex.getCosineSimilarity(con1, con2);
		//			System.out.println("Similarity is: " + cos);
		//		}
		//		wikipediaIndex.close();


		wikipediaIndex.close();
	}
}
