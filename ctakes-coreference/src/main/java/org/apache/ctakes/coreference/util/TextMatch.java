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
package org.apache.ctakes.coreference.util;

//TODO: Should be upgraded
public class TextMatch {

	public static boolean startMatch (String a, String b) {
		int ia = a.indexOf(" ");
		int ib = b.indexOf(" ");
		String aa = a.substring(0, ia==-1?(a.length()>5?5:a.length()):ia);
		String bb = b.substring(0, ib==-1?(b.length()>5?5:b.length()):ib);
		return aa.equalsIgnoreCase(bb);
	}

	public static boolean endMatch (String a, String b) {
		int ia = a.lastIndexOf(" ");
		int ib = b.lastIndexOf(" ");
		String aa = a.substring(ia==-1?(a.length()>5?a.length()-5:0):ia);
		String bb = b.substring(ib==-1?(b.length()>5?b.length()-5:0):ib);
		return aa.equalsIgnoreCase(bb);
	}
}
