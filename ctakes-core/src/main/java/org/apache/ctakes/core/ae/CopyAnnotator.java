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
import org.apache.ctakes.core.util.ParamUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceInitializationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Utility annotator that copy's data from an existing JCas object into a new
 * JCas object.
 * 
 * @author Mayo Clinic
 * 
 */
@PipeBitInfo(
      name = "JCas Copy Annotator",
      description = "Copies document text and all annotations into a new JCas.",
      role = PipeBitInfo.Role.SPECIAL
)
public class CopyAnnotator extends JCasAnnotator_ImplBase {
  public static final String PARAM_SOURCE_CLASS = "srcObjClass";
  @ConfigurationParameter(
      name = PARAM_SOURCE_CLASS,
      mandatory = true,
      description = "Name of source class"
      )
  private String srcClassName;
  
  public static final String PARAM_DEST_CLASS = "destObjClass";
  @ConfigurationParameter(
      name = PARAM_DEST_CLASS,
      mandatory = true,
      description = "Name of destination class"
      )
  private String destClassName;
  
  public static final String PARAM_METHOD_MAP = "dataBindMap";
  @ConfigurationParameter(
      name = PARAM_METHOD_MAP,
      mandatory = true,
      description = "Mapping between source methods and destination methods in a bar (\"|\") separated format"
      )
  private String[] methodMapArray;
  
	private Class<? extends TOP> srcClass;

	// LOG4J logger based on class name
	private Logger logger = Logger.getLogger(getClass().getName());

	// constructor used to create a new instance of the destination
	// JCas object
	private Constructor<?> iv_destContr;

	// key = source getter method (java.lang.reflect.Method)
	// val = destination setter method (java.lang.reflect.Method)
	private Map<Method, Method> iv_getSetMap;

  @SuppressWarnings("unchecked")
  @Override
  public void initialize(UimaContext annotCtx)
			throws ResourceInitializationException {
    super.initialize(annotCtx);

    
    try {
			srcClass = (Class<? extends TOP>) Class.forName(srcClassName);
		  if(!TOP.class.isAssignableFrom(srcClass)) throw new ResourceInitializationException();
		  
			Class<?> destClass = Class.forName(destClassName);
			Class<?>[] constrArgs = { JCas.class };
			iv_destContr = destClass.getConstructor(constrArgs);

			Map<String, String> m = ParamUtil.getStringParameterValuesMap(
					methodMapArray, "|");
			iv_getSetMap = new HashMap<>();
			Iterator<String> getterItr = m.keySet().iterator();
			while (getterItr.hasNext()) {
				String getterMethName = getterItr.next();
				String setterMethName = m.get(getterMethName);

				Method getterMeth = srcClass.getMethod(getterMethName,
						(Class[]) null);

				// get corresponding setter that has compatible args
				Class<?>[] setterArgs = { getterMeth.getReturnType() };
				Method setterMeth = destClass.getMethod(setterMethName,
						setterArgs);

				iv_getSetMap.put(getterMeth, setterMeth);
			}
		} catch (ClassNotFoundException e) {
			throw new ResourceInitializationException(e);
		} catch (NoSuchMethodException e) {
      throw new ResourceInitializationException(e);
    } catch (SecurityException e) {
      throw new ResourceInitializationException(e);
    }
	}

	@Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {
		logger.info("process(JCas)");

		// iterate over source objects in JCas
		Collection<? extends TOP> srcObjs = JCasUtil.select(jcas, srcClass);
		for(TOP srcObj : srcObjs){
			Object[] constrArgVals = { jcas };
			try {
				// create new destination object
				TOP destObj = (TOP) iv_destContr.newInstance(constrArgVals);

				// copy data from source to destination
				Iterator<Method> getterItr = iv_getSetMap.keySet().iterator();
				while (getterItr.hasNext()) {
					Method getterMeth = getterItr.next();
					Method setterMeth = iv_getSetMap.get(getterMeth);

					Object val = getterMeth.invoke(srcObj, (Object[]) null);
					Object[] setterArgs = { val };
					setterMeth.invoke(destObj, setterArgs);
				}
				// add new destination object to JCas
				destObj.addToIndexes();
			} catch (Exception e) {
				throw new AnalysisEngineProcessException(e);
			}
		}
	}

}