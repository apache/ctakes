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
package org.apache.ctakes.utils.struct;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// This class is a simplifying class which makes it easy to build hashes to keep track of counts
// and write less boilerplate code.  If you just call it with an object, it will increment the
// object's count by 1, initializing it to zero first if necessary.
public class CounterMap<K> extends HashMap<K, java.lang.Integer> {

	@Override
	public Integer get(Object key) {
		if(super.containsKey(key))	return super.get(key);
		else{
			return 0;
		}
	}
	
	public void add(K key){
		add(key, 1);
	}
	
	public void add(K key, Integer i){
		if(!super.containsKey(key)){
			super.put(key,0); 
		}
		super.put(key, super.get(key)+i);
	}
	
	public List<K> getKeysSortedByValue(){
	  return entrySet()
        .stream()
        .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
        .map(e -> e.getKey())
        .collect(Collectors.toList());
   	}
}
