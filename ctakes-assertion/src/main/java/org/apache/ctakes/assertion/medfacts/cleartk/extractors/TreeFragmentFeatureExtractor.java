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
package org.apache.ctakes.assertion.medfacts.cleartk.extractors;

import org.apache.ctakes.assertion.util.SemanticClasses;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.utils.tree.FragmentUtils;
import org.apache.ctakes.utils.tree.SimpleTree;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.util.CleartkInitializationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;



/* 
 * This class implements a ClearTK feature extractor for tree kernel fragment features
 * as derived using the flink toolkit (http://danielepighin.net/cms/software/flink).
 * Model location is hardcoded as of right now.
 * TODO: Parameterize this so that, e.g., multiple projects could use this feature if necessary.
 */
public abstract class TreeFragmentFeatureExtractor implements FeatureExtractor1<IdentifiedAnnotation> {

	static private final String CUE_WORDS_FILE = "org/apache/ctakes/assertion/cue_words/all_cues.txt";
	public static final String PARAM_OUTPUTDIR = "outputDir";
	public static final String PARAM_SEMDIR = "semDir";
	protected HashSet<SimpleTree> frags = null;
	protected SemanticClasses sems = null;
	protected String prefix = null;

	public TreeFragmentFeatureExtractor(String prefix, String resourceFilename) throws CleartkInitializationException{
		initializeFrags(resourceFilename);
		this.prefix = prefix;
		try{
			sems = new SemanticClasses(FileLocator.getAsStream(CUE_WORDS_FILE));
//		  sems = new SemanticClasses(FileLocator.getAsStream("org/apache/ctakes/assertion/all_cues.txt"));
		}catch(Exception e){
		  throw new CleartkInitializationException(e, CUE_WORDS_FILE, "Could not find semantic classes resource.", new Object[]{});
		}
	}

	private void initializeFrags(String resourceFilename){
		frags = new HashSet<SimpleTree>();
		InputStream fragsFilestream = null;
		try{
			fragsFilestream = FileLocator.getAsStream(resourceFilename);
			Scanner scanner = new Scanner(fragsFilestream);
			while(scanner.hasNextLine()){
				frags.add(FragmentUtils.frag2tree(scanner.nextLine().trim()));
			}
      fragsFilestream.close();      
		}catch(IOException e){
			System.err.println("Trouble with tree fragment file: " + e);
		}
	}

	@Override
	public abstract List<Feature> extract(JCas jcas, IdentifiedAnnotation annotation);
}
