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
package org.apache.ctakes.assertion.eval;

import org.apache.ctakes.assertion.util.AssertionConst;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.component.NoOpAnnotator;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.DOCUMENT_ID;
import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION;

/**
 * 
 * Read in gold annotations from XMI and create a view within the current CAS, and copy the 
 * gold annotations into the new view within the current CAS.
 * Written to handle testing the cTAKES 2.5 assertion (polarity) value against the 
 * gold standard, using XMI that had already been created by the Apache cTAKES 3.0 gold standard reader. 
 *
 */
@PipeBitInfo(
		name = "Gold View Merger",
		description = "Read in gold annotations from XMI and create a view within the current CAS, and copy the" +
				" gold annotations into the new view within the current CAS.",
		role = PipeBitInfo.Role.SPECIAL,
		dependencies = { DOCUMENT_ID },
		products = { IDENTIFIED_ANNOTATION }
)
public class MergeGoldViewFromOneCasIntoInitialViewOfAnotherCas extends JCasAnnotator_ImplBase {

	static final Logger LOGGER = Logger.getLogger(MergeGoldViewFromOneCasIntoInitialViewOfAnotherCas.class.getName());


	private static final String dirWithGoldViews = AssertionConst.testDirectories.get("polarity");// TODO parameterize this
	static {
		LOGGER.info("Copying information from gold views in " + dirWithGoldViews);
	}
	
	private static final File goldViewDir = new File(dirWithGoldViews);
	
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
	}

	/**
	 * Copy all annotations of the given types from the corresponding cas that has a gold view to the current cas
	 */
	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		
		if (!goldViewDir.exists()) throw new AnalysisEngineProcessException(new RuntimeException("Directory with gold view annotations not found:" + dirWithGoldViews));
		if (!goldViewDir.isDirectory()) throw new AnalysisEngineProcessException(new RuntimeException("What is supposed to be a directory with gold view annotations is not a directory:" + dirWithGoldViews));

      String docId = DocIdUtil.getDocumentID( jCas );
		JCas correspondingCasThatHasGoldAnnotations = getCorrespondingCasThatHasGoldAnnotations(docId);
		JCas viewWithPreexistingGoldAnnotations = null;
		JCas newGoldView = null;

		viewWithPreexistingGoldAnnotations = getViewWithGoldAnnotations(correspondingCasThatHasGoldAnnotations);

		// Create the new view in the current CAS so the current CAS has both views
		// org.apache.uima.fit.util.ViewNames.INITIAL_VIEW; // org.apache.uima.fit.examples.experiment.pos.ViewNames


		//newGoldView = jCas.createView(AssertionEvaluation.GOLD_VIEW_NAME);
		newGoldView = ViewCreatorAnnotator.createViewSafely(jCas, AssertionEvaluation.GOLD_VIEW_NAME);
		try {
		newGoldView.setSofaDataString(jCas.getSofaDataString(), jCas.getSofaMimeType());
		} catch (org.apache.uima.cas.CASRuntimeException e) {
			LOGGER.info("Warning, error setting sofa string - ignore if using TestAttributeModels");
		}
		
		int countCopied = 0;
		int countSkipped = 0;
		if (viewWithPreexistingGoldAnnotations==null) throw new RuntimeException("viewWithPreexistingGoldAnnotations is null");
		Collection<? extends Annotation> annotations = JCasUtil.select(viewWithPreexistingGoldAnnotations, Annotation.class);
		LOGGER.debug("Found " + annotations.size() + " annotations.");
		//		Collection<? extends Annotation> evm = JCasUtil.select(viewWithPreexistingGoldAnnotations, EventMention.class);
		//		LOGGER.debug("Found " + evm.size() + " evm.");
		//		Collection<? extends Annotation> entm = JCasUtil.select(viewWithPreexistingGoldAnnotations, EntityMention.class);
		//		LOGGER.debug("Found " + entm.size() + " entm.");
		//		Collection<? extends Annotation> s = JCasUtil.select(viewWithPreexistingGoldAnnotations, Sentence.class);
		//		LOGGER.debug("Found " + s.size() + " s.");
		
		for (Annotation a: annotations) {
			if (isInstanceOfOneOfClassesToCopy(a)) {
				copyAnnotation(a, newGoldView);
				countCopied++;
			} else {
				countSkipped++;
			}
		}
		
		
		LOGGER.debug("Copied " + countCopied + " gold annotations out of " + (countSkipped+countCopied) + " to cas, which now has " + newGoldView.getAnnotationIndex().size() + " indexed annotations in " +newGoldView.getViewName());
		LOGGER.debug(" and has " + jCas.getAnnotationIndex().size() + " indexed annotations in " +jCas.getViewName());

	}

	private static JCas getViewWithGoldAnnotations(JCas correspondingCasThatHasGoldAnnotations) {
		JCas viewWithPreexistingGoldAnnotations = null;
		try {
			viewWithPreexistingGoldAnnotations = correspondingCasThatHasGoldAnnotations.getView(AssertionEvaluation.GOLD_VIEW_NAME);
		} catch (org.apache.uima.cas.CASRuntimeException cre) {
			// Let it just continue if there's an exception and check for null later
		} catch (org.apache.uima.cas.CASException viewException) {
			// Let it just continue if there's an exception and check for null later
		} catch (NullPointerException npe) {
			// Let it just continue if there's an exception and check for null later
		}
		if (viewWithPreexistingGoldAnnotations == null) {
			viewWithPreexistingGoldAnnotations = correspondingCasThatHasGoldAnnotations;
			LOGGER.debug("Using view " + viewWithPreexistingGoldAnnotations.getViewName());
			int n  = viewWithPreexistingGoldAnnotations.getAnnotationIndex().size();
			LOGGER.debug("With " + n + " annotations");
			if (n==0) {
				Iterator<CAS> iter = viewWithPreexistingGoldAnnotations.getCas().getViewIterator();
				while (iter.hasNext()) {
					CAS cas = iter.next();
					LOGGER.debug("view " + cas.getViewName() + " has " + cas.getAnnotationIndex().size() + " indexed annotations.");
					
				}
				throw new RuntimeException("n==0");
			}
		}
		return viewWithPreexistingGoldAnnotations;
	}

	/**
	 * 
	 * @param goldAnnotation
	 * @param jcas
	 */
	private static void copyAnnotation(Annotation goldAnnotation, JCas jcas) {
		
		Annotation newAnno;
		if (goldAnnotation instanceof IdentifiedAnnotation) {
			IdentifiedAnnotation ia = new IdentifiedAnnotation(jcas);
			ia.setConditional(((IdentifiedAnnotation) goldAnnotation).getConditional());
			ia.setConfidence(((IdentifiedAnnotation) goldAnnotation).getConfidence());
	        ia.setDiscoveryTechnique(((IdentifiedAnnotation)goldAnnotation).getDiscoveryTechnique());
			ia.setGeneric(((IdentifiedAnnotation) goldAnnotation).getGeneric());
			ia.setHistoryOf(((IdentifiedAnnotation) goldAnnotation).getHistoryOf());
			ia.setPolarity(((IdentifiedAnnotation) goldAnnotation).getPolarity());
			ia.setSegmentID(((IdentifiedAnnotation) goldAnnotation).getSegmentID());
			ia.setSentenceID(((IdentifiedAnnotation) goldAnnotation).getSentenceID());
			ia.setSubject(((IdentifiedAnnotation) goldAnnotation).getSubject());
			ia.setTypeID(((IdentifiedAnnotation) goldAnnotation).getTypeID());
			ia.setUncertainty(((IdentifiedAnnotation) goldAnnotation).getUncertainty());
			newAnno = ia;
		} else {
			throw new RuntimeException("Unexpected class of object " + goldAnnotation.getClass());
		}

		newAnno.setBegin(goldAnnotation.getBegin());
		newAnno.setEnd(goldAnnotation.getEnd());
		newAnno.addToIndexes();
		
	}

	private static boolean isInstanceOfOneOfClassesToCopy(Annotation a) {
		if (a instanceof EventMention) return true;
		if (a instanceof EntityMention) return true;
		return false;
	}

	
	private static JCas getCorrespondingCasThatHasGoldAnnotations(String docId) {
		File f = new File(goldViewDir, docId);
		if (!f.exists()) f = new File(goldViewDir, docId+".xml");
		if (!f.exists()) f = new File(goldViewDir, docId+".xcas");
		if (!f.exists()) f = new File(goldViewDir, docId+".xmi");
		if (!f.exists()) f = new File(goldViewDir, docId+".xcas.xml");
		if (!f.exists()) f = new File(goldViewDir, docId+".xmi.xml");
		
		if (!f.exists())
			try {
				throw new RuntimeException("Unable to find file for doc ID " + docId + " in " + goldViewDir.getName() + " aka " + goldViewDir.getCanonicalPath());
			} catch (IOException e) {
				throw new RuntimeException("Unable to find file for doc ID " + docId + " in " + goldViewDir.getName());
			}
		return getJcas(f);
	}

	private static JCas getJcas(File f) {
		List<File> list = new ArrayList<>();
		list.add(f);
		CollectionReader cr;
		AggregateBuilder builder;
		
		try {
			cr = getCollectionReader(list);
			builder = new AggregateBuilder();

			// uimafit find available type systems on classpath
			TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription();

			AnalysisEngineDescription noOp = AnalysisEngineFactory.createEngineDescription(NoOpAnnotator.class, typeSystemDescription);
			builder.add(noOp);
		} catch (ResourceInitializationException e) {
			throw new RuntimeException(e);
		}

		try {
			//SimplePipeline.runPipeline(cr, builder.createEngine());
			AnalysisEngine engine = builder.createAggregate();

			final List<ResourceMetaData> metaData = new ArrayList<>();
			metaData.add(cr.getMetaData());
			metaData.add(engine.getMetaData());
			
			final CAS cas;
			cas = CasCreationUtils.createCas(metaData);
			
			if (cr.hasNext()) { // assumes just one document to process
				cr.getNext(cas);
				engine.process(cas); // SimplePipeline.runPipeline(cas, engine);
			}

			engine.collectionProcessComplete();

			return cas.getJCas();

		} catch (ResourceInitializationException e) {
			throw new RuntimeException(e);
		} catch (UIMAException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	  public static CollectionReader getCollectionReader(List<File> items) throws ResourceInitializationException {
		    String[] paths = new String[items.size()];
		    for (int i = 0; i < paths.length; ++i) {
		      paths[i] = items.get(i).getPath();
		    }
		    return CollectionReaderFactory.createReader(
		        XMIReader.class,
		        TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(),
		        XMIReader.PARAM_FILES,
		        paths);
		  }

}
 