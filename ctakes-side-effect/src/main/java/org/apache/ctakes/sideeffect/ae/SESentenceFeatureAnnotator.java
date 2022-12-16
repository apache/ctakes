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
package org.apache.ctakes.sideeffect.ae;

import org.apache.ctakes.core.util.FSUtil;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.sideeffect.type.PSESentence;
import org.apache.ctakes.sideeffect.type.PSESentenceFeature;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Extract SE sentence (from PSESentence) features and add them to
 * PSESentenceFeature.
 * 
 * @author Mayo Clinic
 */
public class SESentenceFeatureAnnotator extends JCasAnnotator_ImplBase {
	private Map metaKeywords; // key:metakeyword, value:set of keywords
	private List metaKey; // metaKeywords.keySet()

	private class SideEffect {
		String text;
		int begin, end;
	}

	public void initialize(UimaContext annotCtx)
			throws ResourceInitializationException {
		super.initialize(annotCtx);
		metaKey = new ArrayList();

		try {
			String metaKeywordsFileName = (String) annotCtx
					.getConfigParameterValue("MetaKeywordsFile");
			metaKeywords = readMetaKeywordsFromFile(metaKeywordsFileName,
					metaKey);
		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
	}

	public void process(JCas jcas)
			throws AnalysisEngineProcessException {
      String docName = DocIdUtil.getDocumentID( jcas );
		System.out.println("---" + docName + "---");

		// add features to cas
		annotatePSESentenceFeatures(jcas);
	}

	/**
	 * Annotate PSESentenceFeature to be used to classify SE sentences based on
	 * the previously trained model
	 * 
	 * @param jcas
	 */
	private void annotatePSESentenceFeatures(JCas jcas) {
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator pseSenIter = indexes.getAnnotationIndex(PSESentence.type)
				.iterator();

		while (pseSenIter.hasNext()) {
			PSESentence ps = (PSESentence) pseSenIter.next();
			List fea = new ArrayList();

			// get features
			fea.addAll(getLocationFeaturesFromMetaKeywords(jcas, ps));

			PSESentenceFeature psf = new PSESentenceFeature(jcas);
			StringArray feaArray = new StringArray(jcas, fea.size());

			// cf) In FSArray the argument must be FeatureStructure ("String"
			// does not work)
			for (int i = 0; i < fea.size(); i++) {
				feaArray.set(i, (String) fea.get(i));
			}

			// set PSESentenceFeature
			if (feaArray != null)
				psf.setFeatures(feaArray); // TODO ?? - could be no text in
											// sentence??
			psf.setPseSen(ps);

			// add to CAS
			psf.addToIndexes();
		}
	}

	/**
	 * Return the List of location features of meta keywords metaKeywords -
	 * key:metakeyword, value:actual keyword
	 * 
	 * @param jcas
	 * @param ps
	 *            PSESentence
	 * @return
	 */
	private List getLocationFeaturesFromMetaKeywords(JCas jcas, PSESentence ps) {
		List feature = new ArrayList();
		List drug = new ArrayList();
		List pse = new ArrayList();

		Iterator neIter = FSUtil.getAnnotationsIteratorInSpan(jcas, IdentifiedAnnotation.type, ps.getBegin(), ps.getEnd() + 1);
		while (neIter.hasNext()) {
			IdentifiedAnnotation n = (IdentifiedAnnotation) neIter.next();
			// drug
			if (n.getTypeID() == 1) {
				drug.add(n);
			}
			// signs/symptoms or disease/disorders
			if (n.getTypeID() == 2 || n.getTypeID() == 3) {
				pse.add(n);
			}
		}

		// for each metaKey
		Iterator metaKeyIter = metaKey.iterator();
		while (metaKeyIter.hasNext()) {
			String mk = (String) metaKeyIter.next();
			Set kwSet = (Set) metaKeywords.get(mk);

			// for each keyword in a given metaKey
			Iterator kwIter = kwSet.iterator();
			String kw = "";
			int kwPos = -1;
			boolean foundKw = false;

			while (kwIter.hasNext()) {
				String pseSenText = ps.getCoveredText().toLowerCase();
				kw = (String) kwIter.next();
				kwPos = pseSenText.indexOf(kw);
				if (kwPos == -1)
					continue;
				// if 1st before & after char is non-alphabet
				int kwB = kwPos - 1;
				int kwA = kwPos + kw.length();
				// cf) end is the end index + 1;
				if ((pseSenText.length() <= kwA) || // == would be satisfied
						(kwPos == 0 && pseSenText.substring(kwA, kwA + 1)
								.matches("\\W"))
						|| (pseSenText.substring(kwA, kwA + 1).matches("\\W") && pseSenText
								.substring(kwB, kwB + 1).matches("\\W"))) {
					foundKw = true;
					break;
				}

				// String lcCoveredText = ps.getCoveredText().toLowerCase();
				// if( (kwPos==0 && ( lcCoveredText.length() >= kwA+1 &&
				// lcCoveredText.substring(kwA,kwA+1).matches("\\W"))) ||
				// ((lcCoveredText.length() >= kwA+1
				// && lcCoveredText.length() >= kwB+1 &&
				// lcCoveredText.substring(kwA,kwA+1).matches("\\W")
				// && lcCoveredText.substring(kwB,kwB+1).matches("\\W")) )) {
				// foundKw = true;
				// break;
				// }

			}

			// if not found the keyword, go to next keyword
			if (!foundKw) {
				feature.add("nul");
				continue;
			}

			int kwBegin = kwPos + ps.getBegin();
			int kwEnd = kwBegin + kw.length(); // index of end ch + 1
			boolean beforePse = false;
			boolean afterPse = false;
			boolean betweenPseAndDrug = false;
			boolean betweenDrugAndPse = false;

			// check if keyword exists between PSE and Drug
			Iterator iter1, iter2;
			iter1 = pse.iterator();
			while (iter1.hasNext()) {
				IdentifiedAnnotation pseNE = (IdentifiedAnnotation) iter1.next();
				if (kwBegin > pseNE.getEnd()) {
					iter2 = drug.iterator();
					while (iter2.hasNext()) {
						IdentifiedAnnotation drugNE = (IdentifiedAnnotation) iter2.next();
						if (kwEnd < drugNE.getBegin()) {
							betweenPseAndDrug = true;
							break;
						}
					}
				}
				if (betweenPseAndDrug)
					break;
			}

			// check if keyword exists between Drug and PSE
			iter1 = drug.iterator();
			while (iter1.hasNext()) {
				IdentifiedAnnotation drugNE = (IdentifiedAnnotation) iter1.next();
				if (kwBegin > drugNE.getEnd()) {
					iter2 = pse.iterator();
					while (iter2.hasNext()) {
						IdentifiedAnnotation pseNE = (IdentifiedAnnotation) iter2.next();
						if (kwEnd < pseNE.getBegin()) {
							betweenDrugAndPse = true;
							break;
						}
					}
				}
				if (betweenDrugAndPse)
					break;
			}

			if ((!betweenPseAndDrug) && (!betweenDrugAndPse)) {
				Iterator pseIter = pse.iterator();
				while (pseIter.hasNext()) {
					IdentifiedAnnotation n = (IdentifiedAnnotation) pseIter.next();
					if (kwEnd < n.getBegin())
						beforePse = true;
					if (kwBegin > n.getEnd())
						afterPse = true;
				}
			}

			if (mk.equals("SideEffectWord")) {
				feature.add("pre");
			} else {
				if (betweenPseAndDrug && betweenDrugAndPse)
					feature.add("bet");
				else if (betweenPseAndDrug)
					feature.add("bpd");
				else if (betweenDrugAndPse)
					feature.add("bdp");
				else if (beforePse && afterPse)
					feature.add("bap");
				else if (beforePse)
					feature.add("bep");
				else if (afterPse)
					feature.add("afp");
				else
					feature.add("any");
			}
		}

		return feature;
	}

	/**
	 * Return LinkedHashMap (key:metakeyword, value:set of actual keywords
	 * belonging to metakeyword) and assign key in the insertion order (cf)
	 * LinkedHashMap.keySet() keeps the order)
	 * 
	 * input file format: metakeyword|keyword|keyword...
	 * metakeyword|keyword|keyword...
	 */
	public Map readMetaKeywordsFromFile(String fileName, List key)
			throws IOException {
		Map returnValues = new LinkedHashMap();
		File file = new File(fileName);
		BufferedReader fileReader = new BufferedReader(new FileReader(file));

		String line;
		while ((line = fileReader.readLine()) != null) {
			if (line.startsWith("//"))
				continue;
			if (line.length() == 0)
				continue;

			String[] stk = line.trim().split("\\|");
			Set keySet = new LinkedHashSet();
			for (int i = 1; i < stk.length; i++) {
				keySet.add(stk[i].trim());
			}

			key.add(stk[0].trim());
			returnValues.put(stk[0].trim(), keySet);
		}

		return returnValues;
	}
}
