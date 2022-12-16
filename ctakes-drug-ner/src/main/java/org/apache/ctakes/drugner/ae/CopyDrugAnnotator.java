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
package org.apache.ctakes.drugner.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.FSUtil;
import org.apache.ctakes.core.util.JCasUtil;
import org.apache.ctakes.core.util.ParamUtil;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceInitializationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * Utility annotator that copy's data from an existing JCas object into a new
 * JCas object.
 * 
 * @author duffp
 * 
 */
@PipeBitInfo(
		name = "Drug Copier",
		description = "Copies data from an existing JCas into a new JCas.",
		role = PipeBitInfo.Role.SPECIAL,
		dependencies = { PipeBitInfo.TypeProduct.SECTION }
)
public class CopyDrugAnnotator extends JCasAnnotator_ImplBase
{
	private int iv_srcType;

	private int iv_segmentType=0;
	
	/**
	 * Value is "sectionOverrideSet".  This parameter specifies which segments to skip.  
	 * The resulting range of tokens will include the entire contents of the span within
	 * the section(s) specified in the sectionOverrideSet parameters of the SegmentLookupWindow
	 * annotator.  The parameter should be of type String, should be multi-valued and optional. 
	 */
	public static final String PARAM_SEGMENTS_TO_SKIP = "sectionOverrideSet";
	    

    private Set iv_skipSegmentsSet;
	
	// constructor used to create a new instance of the destination
	// JCas object
	private Constructor iv_destContr;

	// key = source getter method (java.lang.reflect.Method)
	// val = destination setter method (java.lang.reflect.Method)
	private Map iv_getSetMap;

	public void initialize(UimaContext annotCtx)
			throws ResourceInitializationException
	{
		super.initialize(annotCtx);

		try
		{
			String className;
			className = (String) annotCtx.getConfigParameterValue("srcDrugObjClass");
			Class srcClass = Class.forName(className);
			iv_srcType = JCasUtil.getType(className);

			className = (String) annotCtx.getConfigParameterValue("destDrugObjClass");
			Class destClass = Class.forName(className);
			Class[] constrArgs = { JCas.class };
			iv_destContr = destClass.getConstructor(constrArgs);
			
			iv_skipSegmentsSet = ParamUtil.getStringParameterValuesSet(PARAM_SEGMENTS_TO_SKIP, annotCtx); 
			
			Map m = ParamUtil.getStringParameterValuesMap(
					"dataDrugBindMap",
					annotCtx,
					"|");
			iv_getSetMap = new HashMap();
			Iterator getterItr = m.keySet().iterator();
			while (getterItr.hasNext())
			{
				String getterMethName = (String) getterItr.next();
				String setterMethName = (String) m.get(getterMethName);

				Method getterMeth = srcClass.getMethod(getterMethName, (Class []) null);

				// get corresponding setter that has compatible args
				Class[] setterArgs = { getterMeth.getReturnType() };
				Method setterMeth = destClass.getMethod(
						setterMethName,
						setterArgs);

				iv_getSetMap.put(getterMeth, setterMeth);
			}
		}
		catch (Exception e)
		{
			throw new ResourceInitializationException(e);
		}
	}

	public void process(JCas jcas)
			throws AnalysisEngineProcessException
	{
		// iterate over source objects in JCas
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator srcObjItr = indexes.getAnnotationIndex(org.apache.ctakes.typesystem.type.textspan.Segment.type).iterator();
		org.apache.ctakes.typesystem.type.textspan.Segment segment = null;
		
		while (srcObjItr.hasNext())
		{
			TOP srcObj = (TOP) srcObjItr.next();

			Object[] constrArgVals = { jcas };
			try
			{
				// create new destination object
				TOP destObj = (TOP) iv_destContr.newInstance(constrArgVals);
				
				// Find the segments that are to be handled as complete sections
                boolean okayToSkip = false;
                boolean segmentMissing = iv_skipSegmentsSet.isEmpty();
				if (!segmentMissing){
					Iterator getSkipSegs = iv_skipSegmentsSet.iterator();
					segment = (org.apache.ctakes.typesystem.type.textspan.Segment) srcObj;
					while (getSkipSegs.hasNext()  && !okayToSkip){
						if (getSkipSegs.next().equals(segment.getId())){

							okayToSkip = true;
						}
					}
					
				}
				// copy data from source to destination
				Iterator getterItr = iv_getSetMap.keySet().iterator();
				

				while (getterItr.hasNext() && (okayToSkip || segmentMissing))
				{
					Method getterMeth = (Method) getterItr.next();
					Method setterMeth = (Method) iv_getSetMap.get(getterMeth);

					Object val = getterMeth.invoke(srcObj, (Object []) null);
					Object[] setterArgs = { val };
					setterMeth.invoke(destObj, setterArgs);
					
				}
				// add new destination object to JCas
				if (okayToSkip) {
					destObj.addToIndexes();
				} else {
					Iterator lookupWindows = FSUtil.getAnnotationsIteratorInSpan(jcas, org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation.type, segment.getBegin(), segment.getEnd());
					while (lookupWindows.hasNext()){
						org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation lookup = (org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation) lookupWindows.next();
						org.apache.ctakes.typesystem.type.textspan.DrugLookupWindowAnnotation drugLookup = new org.apache.ctakes.typesystem.type.textspan.DrugLookupWindowAnnotation(jcas, lookup.getBegin(), lookup.getEnd()); //Reorganized paths in 3.0 SPM
						drugLookup.addToIndexes();
					}
				}
	
			}
			catch (Exception e)
			{
				throw new AnalysisEngineProcessException(e);
			}
		}
	}
}