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
package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.JCasUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.*;


/* 
 * When a given type of overlap is found between two annotations
 * of interest, this annotator either modifies one annotation (begin and end offsets)
 * or deletes one (or both) of the annotations, depending on the action
 * from the config parameters.
 *
 * Config parameters control the types of annotations to compare,
 * the type(s) of overlap interested in,
 * and the action to take when the overlap is found.
 * 
 * One example use is to delete annotations of type A that are subsumed
 * by other annotations of type A if only looking for the longest annotations
 * of the given type.
 * 
 * Another possible use is to modify the begin and end offsets for an 
 * annotation to prevent the annotation from ending in the middle of another
 * annotation where it would not make sense for it to.
 * For example, a sentence should not end in the middle of a decimal number.
 * 
 */
@PipeBitInfo(
      name = "Overlap Annotator",
      description = "Removes or modifies annotations that overlap.",
      role = PipeBitInfo.Role.SPECIAL,
      dependencies = { PipeBitInfo.TypeProduct.BASE_TOKEN }
)
public class OverlapAnnotator extends JCasAnnotator_ImplBase {
	// LOG4J logger based on class name
	private Logger iv_logger = Logger.getLogger(getClass().getName());

	/**
	 * No overlap at all between A and B
	 */
	public static final byte OVERLAP_NONE = 0;
	/**
	 * A and B only partially overlap
	 */
	public static final byte OVERLAP_PARTIAL = 1;
	/**
	 * A and B have identical offsets
	 */
	public static final byte OVERLAP_EXACT = 2;
	/**
	 * A completely envelopes B
	 */
	public static final byte OVERLAP_A_ENV_B = 3;
	/**
	 * B completely envelopes A
	 */
	public static final byte OVERLAP_B_ENV_A = 4;

	private final int NUM_OVERLAP_BITS = 5;
	private BitSet iv_typesOfOverlapToProcess;

	private int iv_aAnnotType;
	private int iv_bAnnotType;
	private Action iv_action;

   @Override
   public void initialize( UimaContext annotCtx )
         throws ResourceInitializationException {
      super.initialize(annotCtx);

		try {
			String classname;
			classname = (String) annotCtx
					.getConfigParameterValue("A_ObjectClass");
			iv_aAnnotType = JCasUtil.getType(classname);

			classname = (String) annotCtx
					.getConfigParameterValue("B_ObjectClass");
			iv_bAnnotType = JCasUtil.getType(classname);

			String overlapTypeStr = (String) annotCtx
					.getConfigParameterValue("OverlapType");
			parseOverlapTypeString(overlapTypeStr);

			String actionTypeStr = (String) annotCtx
					.getConfigParameterValue("ActionType");
			if (actionTypeStr.equalsIgnoreCase("MODIFY")) {
				String[] argArr = (String[]) annotCtx
						.getConfigParameterValue("ModifyAction");
				iv_action = buildModifyAction(argArr);

			} else if (actionTypeStr.equalsIgnoreCase("DELETE")) {
				String[] argArr = (String[]) annotCtx
						.getConfigParameterValue("DeleteAction");
				iv_action = buildDeleteAction(argArr);
			} else {
				throw new Exception("Invalid action type: " + actionTypeStr);
			}
		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
	}

	/**
	 * Parses the overlap type string and populates the user bitset.
	 * 
	 * @param str
	 * @throws Exception
	 */
	private void parseOverlapTypeString(String str) throws Exception {
		iv_typesOfOverlapToProcess = new BitSet(NUM_OVERLAP_BITS);
		StringTokenizer st = new StringTokenizer(str, "|");
		while (st.hasMoreTokens()) {
			String tok = st.nextToken();
			if (tok.equalsIgnoreCase("NONE")) {
				iv_typesOfOverlapToProcess.set(OVERLAP_NONE, true);
			} else if (tok.equalsIgnoreCase("PARTIAL")) {
				iv_typesOfOverlapToProcess.set(OVERLAP_PARTIAL, true);
			} else if (tok.equalsIgnoreCase("EXACT")) {
				iv_typesOfOverlapToProcess.set(OVERLAP_EXACT, true);
			} else if (tok.equalsIgnoreCase("A_ENV_B")) {
				iv_typesOfOverlapToProcess.set(OVERLAP_A_ENV_B, true);
			} else if (tok.equalsIgnoreCase("B_ENV_A")) {
				iv_typesOfOverlapToProcess.set(OVERLAP_B_ENV_A, true);
			} else {
				throw new Exception("Invalid overlap type: " + tok);
			}
		}

		// check the NONE boundary case
		if (iv_typesOfOverlapToProcess.get(OVERLAP_NONE)
				&& (iv_typesOfOverlapToProcess.cardinality() > 1)) {
			throw new Exception(
					"NONE overlap type is exclusive and cannot be combined with other types.");
		}

		if (iv_logger.isDebugEnabled()) {
			iv_logger.debug("Overlap bitset: " + iv_typesOfOverlapToProcess);
		}
	}

	/**
	 * Builds a ModifyAction implementation.
	 * 
	 * @param argArr
	 * @return
	 * @throws Exception
	 */
	private Action buildModifyAction(String[] argArr) throws Exception {
		String selectorStr = null;
		String beginStr = null;
		String endStr = null;
		for (int i = 0; i < argArr.length; i++) {
			StringTokenizer st = new StringTokenizer(argArr[i], "=");
			String key = st.nextToken();
			String val = st.nextToken();

			if (key.equalsIgnoreCase("selector")) {
				selectorStr = val;
			} else if (key.equalsIgnoreCase("begin")) {
				beginStr = val;
			} else if (key.equalsIgnoreCase("end")) {
				endStr = val;
			}
		}

		byte selector;
		if (selectorStr != null) {
			if (selectorStr.equalsIgnoreCase("A")) {
				selector = ModifyActionImpl.SELECT_A;
			} else if (selectorStr.equalsIgnoreCase("B")) {
				selector = ModifyActionImpl.SELECT_B;
			} else {
				throw new Exception("ModifyAction selector invalid: "
						+ selectorStr);
			}
		} else {
			throw new Exception("ModifyAction selector not specified.");
		}

		byte beginSrc;
		if (beginStr != null) {
			if (beginStr.equalsIgnoreCase("A")) {
				beginSrc = ModifyActionImpl.BEGIN_A;
			} else if (beginStr.equalsIgnoreCase("B")) {
				beginSrc = ModifyActionImpl.BEGIN_B;
			} else if (beginStr.equalsIgnoreCase("SMALLEST")) {
				beginSrc = ModifyActionImpl.BEGIN_SMALLEST;
			} else if (beginStr.equalsIgnoreCase("LARGEST")) {
				beginSrc = ModifyActionImpl.BEGIN_LARGEST;
			} else {
				throw new Exception("ModifyAction begin invalid: " + beginStr);
			}
		} else {
			throw new Exception("ModifyAction begin not specified.");
		}

		byte endSrc;
		if (endStr != null) {
			if (endStr.equalsIgnoreCase("A")) {
				endSrc = ModifyActionImpl.END_A;
			} else if (endStr.equalsIgnoreCase("B")) {
				endSrc = ModifyActionImpl.END_B;
			} else if (endStr.equalsIgnoreCase("SMALLEST")) {
				endSrc = ModifyActionImpl.END_SMALLEST;
			} else if (endStr.equalsIgnoreCase("LARGEST")) {
				endSrc = ModifyActionImpl.END_LARGEST;
			} else {
				throw new Exception("ModifyAction end invalid: " + endStr);
			}
		} else {
			throw new Exception("ModifyAction end not specified.");
		}

		return new ModifyActionImpl(selector, beginSrc, endSrc);
	}

	/**
	 * Builds a DeleteAction implementation.
	 * 
	 * @param argArr
	 * @return
	 * @throws Exception
	 */
	private Action buildDeleteAction(String[] argArr) throws Exception {
		String selectorStr = null;
		for (int i = 0; i < argArr.length; i++) {
			StringTokenizer st = new StringTokenizer(argArr[i], "=");
			String key = st.nextToken();
			String val = st.nextToken();

			if (key.equalsIgnoreCase("selector")) {
				selectorStr = val;
			}
		}

		if (selectorStr != null) {
			byte selector;
			if (selectorStr.equalsIgnoreCase("A")) {
				selector = DeleteActionImpl.SELECT_A;
			} else if (selectorStr.equalsIgnoreCase("B")) {
				selector = DeleteActionImpl.SELECT_B;
			} else if (selectorStr.equalsIgnoreCase("BOTH")) {
				selector = DeleteActionImpl.SELECT_A_B;
			} else {
				throw new Exception("DeleteAction selector invalid: "
						+ selectorStr);
			}

			return new DeleteActionImpl(selector);
		} else {
			throw new Exception("DeleteAction selector not specified.");
		}
	}

   @Override
   public void process( JCas jcas ) throws AnalysisEngineProcessException {
      iv_logger.info( "process(JCas)" );

		JFSIndexRepository indexes = jcas.getJFSIndexRepository();

		// store annotations in a separate list
		// because UIMA iterators don't handle annotations
		// from the iterator being removed from the CAS.
		List<Annotation> aAnnotList = storeAnnotationsToList(indexes,
				iv_aAnnotType);
		List<Annotation> bAnnotList = storeAnnotationsToList(indexes,
				iv_bAnnotType);

		Iterator<Annotation> aAnnotItr = aAnnotList.iterator();
		while (aAnnotItr.hasNext()) {
			Annotation aAnnot = aAnnotItr.next();

			// for each A, iterate over B annotations
			Iterator<Annotation> bAnnotItr = bAnnotList.iterator();
			while (bAnnotItr.hasNext()) {
				Annotation bAnnot = bAnnotItr.next();

				// get type of overlap between A and B
				BitSet jcasOverlapType = getOverlapType(aAnnot, bAnnot);

				// determine if overlap is one of the type(s) requested -
				// perform AND operation between bitsets
				jcasOverlapType.and(iv_typesOfOverlapToProcess);
				if (jcasOverlapType.cardinality() > 0) {
					iv_action.performAction(aAnnot, bAnnot);
				}
			}
		}
	}

	/**
	 * create a list of the annotations of the given type
	 * 
	 * @param indexes
	 * @param annotType
	 * @return
	 */
	private List<Annotation> storeAnnotationsToList(JFSIndexRepository indexes,
			int annotType) {
		List<Annotation> l = new ArrayList<Annotation>();
		FSIterator<Annotation> annotItr = indexes.getAnnotationIndex(annotType)
				.iterator();
		while (annotItr.hasNext()) {
         Annotation annot = annotItr.next();
         l.add( annot );
      }
		return l;
	}

	/**
	 * Determines the type of overlap between annotations A and B.
	 * 
	 * @param a
	 * @param b
	 * @return BitSet with the bit corresponding to the overlap type set to
	 *         true.
	 */
	private BitSet getOverlapType(Annotation a, Annotation b) {
		BitSet bitSet = new BitSet(NUM_OVERLAP_BITS);
		if ((a.getBegin() == b.getBegin()) && (a.getEnd() == b.getEnd())) {
			bitSet.set(OVERLAP_EXACT, true);
			return bitSet;
		} else if ((a.getBegin() <= b.getBegin()) && (a.getEnd() >= b.getEnd())) {
			bitSet.set(OVERLAP_A_ENV_B, true);
			return bitSet;
		} else if ((b.getBegin() <= a.getBegin()) && (b.getEnd() >= a.getEnd())) {
			bitSet.set(OVERLAP_B_ENV_A, true);
			return bitSet;
		} else if ((a.getBegin() < b.getBegin()) && (a.getEnd() > b.getBegin())
				&& (a.getEnd() < b.getEnd())) {
			bitSet.set(OVERLAP_PARTIAL, true);
			return bitSet;
		} else if ((b.getBegin() < a.getBegin()) && (b.getEnd() > a.getBegin())
				&& (b.getEnd() < a.getEnd())) {
			bitSet.set(OVERLAP_PARTIAL, true);
			return bitSet;
		} else {
			bitSet.set(OVERLAP_NONE, true);
			return bitSet;
		}
	}

	/**
	 * Generic interface for performing an action if the overlap constraint is
	 * met between annotations A and B.
	 * 
	 * @author Mayo Clinic
	 * 
	 */
	interface Action {
      void performAction( Annotation aAnnot, Annotation bAnnot );
   }

	/**
	 * Implementation that modifies feature values of either annotation A or B.
	 * Currently supports modifying begin offset and end offset.
	 */
	class ModifyActionImpl implements Action {
		public static final byte SELECT_A = 0;
		public static final byte SELECT_B = 1;
		private byte iv_selector;

		private static final byte BEGIN_A = 0;
		private static final byte BEGIN_B = 1;
		private static final byte BEGIN_SMALLEST = 2;
		private static final byte BEGIN_LARGEST = 3;
		private byte iv_beginSrc;

		private static final byte END_A = 0;
		private static final byte END_B = 1;
		private static final byte END_SMALLEST = 2;
		private static final byte END_LARGEST = 3;
		private byte iv_endSrc;

		public ModifyActionImpl(byte selector, byte beginSrc, byte endSrc) {
			iv_selector = selector;
			iv_beginSrc = beginSrc;
			iv_endSrc = endSrc;
		}

      @Override
      public void performAction( Annotation aAnnot, Annotation bAnnot ) {
         Annotation ann = null;
         if (iv_selector == SELECT_A) {
				ann = aAnnot;
			} else {
				ann = bAnnot;
			}

			modifyBegin(ann, aAnnot, bAnnot);
			modifyEnd(ann, aAnnot, bAnnot);

		}

		private void modifyBegin(Annotation ann, Annotation aAnnot,
				Annotation bAnnot) {
			switch (iv_beginSrc) {
			case BEGIN_A:
				ann.setBegin(aAnnot.getBegin());
				break;
			case BEGIN_B:
				ann.setBegin(bAnnot.getBegin());
				break;
			case BEGIN_SMALLEST:
				ann.setBegin(Math.min(aAnnot.getBegin(), bAnnot.getBegin()));
				break;
			case BEGIN_LARGEST:
				ann.setBegin(Math.max(aAnnot.getBegin(), bAnnot.getBegin()));
				break;
			}
		}

		private void modifyEnd(Annotation ann, Annotation aAnnot,
				Annotation bAnnot) {
			switch (iv_endSrc) {
			case END_A:
				ann.setEnd(aAnnot.getEnd());
				break;
			case END_B:
				ann.setEnd(bAnnot.getEnd());
				break;
			case END_SMALLEST:
				ann.setEnd(Math.min(aAnnot.getEnd(), bAnnot.getEnd()));
				break;
			case END_LARGEST:
				ann.setEnd(Math.max(aAnnot.getEnd(), bAnnot.getEnd()));
				break;
			}
		}

	}

	/**
	 * Implementation that deletes either the A, B, or both.
	 * 
	 * @author Mayo Clinic
	 * 
	 */
	class DeleteActionImpl implements Action {
		public static final byte SELECT_A = 0;
		public static final byte SELECT_B = 1;
		public static final byte SELECT_A_B = 2;
		private byte iv_selector;

		public DeleteActionImpl(byte selector) {
			iv_selector = selector;
		}

      @Override
      public void performAction( Annotation aAnnot, Annotation bAnnot ) {
         if ( iv_selector == SELECT_A ) {
            aAnnot.removeFromIndexes();
			} else if (iv_selector == SELECT_B) {
				bAnnot.removeFromIndexes();
			} else {
				aAnnot.removeFromIndexes();
				bAnnot.removeFromIndexes();
			}
		}
	}
}