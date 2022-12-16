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
package org.apache.ctakes.core.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.cas.ConstraintFactory;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIntConstraint;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeaturePath;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;

import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;

public class FSUtil {
	
	@Deprecated
	public static FSIterator getAnnotationsInSpanIterator(JCas jcas, int type, int beginSpan, int endSpan)
	{
		ConstraintFactory constraintFactory = jcas.getConstraintFactory();
	    FSIntConstraint windowConstraint = constraintFactory.createIntConstraint();
	    windowConstraint.gt(beginSpan-1);
	    windowConstraint.lt(endSpan);
	    
	    Type annotType = jcas.getCasType(type);
	    Feature beginSpanFeature = annotType.getFeatureByBaseName("begin");
	    Feature endSpanFeature = annotType.getFeatureByBaseName("end");
	    
	    FeaturePath beginFeaturePath = jcas.createFeaturePath();
	    beginFeaturePath.addFeature(beginSpanFeature);
	    FSMatchConstraint beginSpanConstraint = constraintFactory.embedConstraint(beginFeaturePath, windowConstraint);
	    
	    FeaturePath endFeaturePath = jcas.createFeaturePath();
	    endFeaturePath.addFeature(endSpanFeature);
	    FSMatchConstraint endSpanConstraint = constraintFactory.embedConstraint(endFeaturePath, windowConstraint);
	    
	    FSMatchConstraint spanConstraint = constraintFactory.and(beginSpanConstraint, endSpanConstraint);
	    
	    JFSIndexRepository indexes = jcas.getJFSIndexRepository();
	    FSIndex<?> annotIndex = indexes.getAnnotationIndex(type);
	    FSIterator<?> annotsInSpanItr = jcas.createFilteredIterator(annotIndex.iterator(), spanConstraint);
	    return annotsInSpanItr;
	}
	
	/**
	 * For correct behavior, requires types to be listed in TypePriorities so that the subiterator works as expected
	 */
	public static FSIterator getAnnotationsIteratorInSpan(JCas jcas, int type, int beginSpan, int endSpan)
	{
	    Annotation ann = new Annotation(jcas, beginSpan, endSpan);
	    ann.addToIndexes();
	    AnnotationIndex<?> annIdx = jcas.getAnnotationIndex(type);
	    FSIterator<?> itr = annIdx.subiterator(ann);
	    ann.removeFromIndexes();
	    return itr;
	}

	/**
	 * Does not use {@link #getAnnotationsInSpan(JCas, int, int, int, int[])} so we don't create a collection
	 * unnecessarily.
	 */
	public static int countAnnotationsInSpan(JCas jcas, int type, int beginSpan, int endSpan, int[] validNeTypes)
	{
	    int count=0;
	    Iterator<?> itr = getAnnotationsIteratorInSpan(jcas, type, beginSpan, endSpan);
	    while(itr.hasNext())
	    {
	    	IdentifiedAnnotation ne = (IdentifiedAnnotation)itr.next();
	    	if(isValidNE(ne.getTypeID(), validNeTypes))
	    		count++;
	    }
	    return count;
	}
	
	private static boolean isValidNE(int currNeType, int[] neTypes)
	{
	    for(int i=0; i<neTypes.length; i++)
		if(currNeType == neTypes[i])
		    return true;
	    
	    return false;
	}
	
	public static List getAnnotationsInSpan(JCas jcas, int type, int beginSpan, int endSpan, int[] validNeTypes)
	{
	    List<IdentifiedAnnotation> list = new ArrayList<IdentifiedAnnotation>();
	    Iterator<?> itr = getAnnotationsIteratorInSpan(jcas, type, beginSpan, endSpan);
	    while(itr.hasNext())
	    {
    		IdentifiedAnnotation ne = (IdentifiedAnnotation)itr.next(); // might be an EventMention or an EntityMention
	    	if(isValidNE(ne.getTypeID(), validNeTypes))
	    		list.add(ne);
	    }
	    return list;
	}

	
	
	/**
	 * returns the number of annotations of specified type in the 
	 */
	public static int countAnnotationsInSpan(JCas jcas, int type, int beginSpan, int endSpan)
	{
	    Annotation ann = new Annotation(jcas, beginSpan, endSpan);
	    ann.addToIndexes();
	    AnnotationIndex<?> annIdx = jcas.getAnnotationIndex(type);
	    ann.removeFromIndexes();
	    return annIdx.size();
	}

	/**
	 * returns a true if the annotation type is present in the span 
	 */
	public static boolean isAnnotationPresentInSpan(JCas jcas, int type, int beginSpan, int endSpan)
	{
	    return (countAnnotationsInSpan(jcas, type, beginSpan, endSpan)>0);
	}	
}
