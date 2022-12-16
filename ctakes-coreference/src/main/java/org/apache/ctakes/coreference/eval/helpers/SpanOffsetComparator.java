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
package org.apache.ctakes.coreference.eval.helpers;

import java.util.Comparator;

public class SpanOffsetComparator implements Comparator<Span> {

	@Override
	public int compare(Span o1, Span o2) {
		int ret;
		ret = o1.get(0)[0] - o2.get(0)[0];
		if (ret!=0) return ret;
		else {
			ret = o1.get(0)[1] - o2.get(0)[1];
			if (ret!=0) return ret;
			else {
				int s1 = o1.size();
				int s2 = o2.size();
				if (s1==1 && s2>1) return -1;
				else if (s1>1 && s2==1) return 1;
				else if (s1==1 && s2==1) return 0;
				else return compare(o1.tail(), o2.tail());
			}
		}
	}

}
