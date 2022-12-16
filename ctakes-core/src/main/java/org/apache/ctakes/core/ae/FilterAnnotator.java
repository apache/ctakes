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

/**
 * This simple implementation is intended to remove annotations other 
 * than the one specified form CAS.
 * 
 * @author m039575 
 */

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

@PipeBitInfo(
      name = "Annotation Remover",
      description = "Removes annotations of a given type from the JCas.",
		role = PipeBitInfo.Role.SPECIAL,
      dependencies = { PipeBitInfo.TypeProduct.BASE_TOKEN }
)
public class FilterAnnotator extends JCasAnnotator_ImplBase {
	// TODO parameterize retainAttrTypeId = DISORDER_ANNOTATIONS = 2
	private static int DISORDER_ANNOTATIONS = 2;

	public FilterAnnotator() {
		super();

		retainAnnType = EntityMention.type;
		retainAttrTypeId = DISORDER_ANNOTATIONS;
	}

   @Override
   public void initialize( UimaContext annotCtx )
         throws ResourceInitializationException {
      super.initialize(annotCtx);

		removeList = new java.util.ArrayList<Annotation>();
	}

	/**
	 * Checks if the annotation is of the type to be retained. If not, removes
	 * it from the index. Uses helper method isValid(Annotation).
	 */
   @Override
   public void process( JCas jcas ) throws AnalysisEngineProcessException {
      removeList.clear();

		// iterate over source objects in JCas
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		FSIterator<Annotation> srcObjItr = indexes.getAnnotationIndex(
				retainAnnType).iterator();

		while (srcObjItr.hasNext()) {
         Annotation ann = srcObjItr.next();
         if ( !isValid( ann ) )
            removeList.add(ann);
		}

		for (int i = 0; i < removeList.size(); i++)
			removeList.get(i).removeFromIndexes();

	}

	private boolean isValid(Annotation ann) {
      return ((IdentifiedAnnotation)ann).getTypeID() == retainAttrTypeId;

   }

	// -- private datamembers ----
	private int retainAnnType; // annotation type you want to retain
	private int retainAttrTypeId; // annotation type id you want to retain
	private java.util.List<Annotation> removeList; // collection to hold
													// annotations to be removed
}
