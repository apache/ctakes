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

import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.TOP;

import java.util.List;

import org.apache.uima.jcas.JCas;

/**
 * Factory to build List of built-in CAS types such as FloatList, IntegerList,
 * StringList, and FSList.
 * 
 * @author Mayo Clinic
 */
public class ListFactory {

	/**
	 * Builds a FSList from an array of JCas objects.
	 * @param jcas The current JCas.
	 * @param objArr Array of JCas objects (objects must extend from TOP).
	 * @return FSList populated with the JCas objects.
	 */
	public static FSList buildList(JCas jcas, TOP[] objArr)
	{
		if (objArr.length == 0)
		{
			return new EmptyFSList(jcas);
		}
		
		NonEmptyFSList firstList = new NonEmptyFSList(jcas);
		NonEmptyFSList list = firstList;		
		for (int i=0; i < objArr.length; i++)
		{			
			list.setHead(objArr[i]);
			list.addToIndexes();
			
			if ((i+1) < objArr.length)
			{
				NonEmptyFSList nextList = new NonEmptyFSList(jcas);
				list.setTail(nextList);
				list = nextList;
			}
		}
		
		// set tail to empty list
		list.setTail(new EmptyFSList(jcas));		
		
		return firstList;
	}
	
	public static FSList buildList(JCas jcas, List<? extends TOP> objArr){
	  return buildList(jcas, objArr.toArray(new TOP[]{}));
	}
	
	public static void append(JCas jcas, FSList list, TOP element){
	  if(list instanceof EmptyFSList) return;
	  	  
	  NonEmptyFSList cur = (NonEmptyFSList) list;
	  while(cur.getTail() instanceof NonEmptyFSList){
	    cur = (NonEmptyFSList) cur.getTail();
	  }
    NonEmptyFSList newElList = new NonEmptyFSList(jcas);
    newElList.setHead(element);
    newElList.setTail(new EmptyFSList(jcas));
    cur.setTail(newElList);
    newElList.addToIndexes();
    newElList.getTail().addToIndexes();
	}
}
