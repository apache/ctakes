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
package org.apache.ctakes.utils.tree;

public class FragmentUtils {

	public static SimpleTree frag2tree(String frag){
		char[] chars = frag.toCharArray();
		int ind = frag.indexOf('(', 1);
		if(ind < 0){
		  ind = frag.indexOf(')', 1);
		  // fragment is just a single token:
		  return new SimpleTree(frag.substring(1,ind));
		}
		String type = frag.substring(1, ind);
		SimpleTree root = new SimpleTree(type);
		SimpleTree cur = root;
		int lpar, rpar, oldind;
		while(ind < chars.length){
			if(chars[ind] == '('){
				SimpleTree nt = null;
				lpar = frag.indexOf('(', ind+1);
				rpar = frag.indexOf(')', ind+1);
				oldind = ind;
				ind = (lpar < rpar  && lpar != -1 ? lpar : rpar);
				type = frag.substring(oldind+1, ind);
				nt = new SimpleTree(type, cur);
				cur.addChild(nt);
				cur = nt;
			}else if(chars[ind] == ')'){
				// if close paren, go up a level and move to next index,
				// which is guaranteed to be another paren
				cur = cur.parent;
				ind++;
			}
		}
		return root;
	}	
}
