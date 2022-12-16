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
package org.apache.ctakes.utils.xcas_comparison;

import java.util.HashSet;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public abstract class Const {
	public static String UIMA_TCAS_DOCUMENT = "uima.tcas.Document";
	public static String UIMA_FSARRAY = "uima.cas.FSArray";
	public static String UIMA_INTARRAY = "uima.cas.IntegerArray";
	public static String UIMA_NONEMPTY_FSLIST = "uima.cas.NonEmptyFSList";
	public static String UIMA_EMPTY_FSLIST = "uima.cas.EmptyFSList";
	public static String ID = "_id";
	public static String REF_PREFIX = "_ref_";
	public static String UIMA_ARRAY_INDEX_KEYWORD = "i";
	public static String UIMA_ARRAY_SIZE_KEYWORD = "size";
	public static String UIMA_LIST_HEAD_KEYWORD = "_ref_head";
	public static String UIMA_LIST_TAIL_KEYWORD = "_ref_tail";
	public static String UIMA_CAS = "CAS";
	public static String UIMA_REF_SOFA = "_ref_Sofa";
	public static String UIMA_SOFA = "uima.cas.Sofa";
	public static String UIMA_SOFA_STRING = "sofaString";
	public static String HIDDEN_TEXT = "...";
	public static HashSet<String> ATTRIBUTES_TO_IGNORE = new HashSet<String>();

	public static void init() {
		// uid is always ignored;
		ATTRIBUTES_TO_IGNORE.add("uid");
		// read in attributes from file
		File f = new File("attributes_to_ignore.properties");
		if (!f.exists()) return;
		Properties p = new Properties();
		try {
			p.load(new FileInputStream(f));
			String s = p.getProperty("ATTRIBUTES_TO_IGNORE");
			if (s!=null) {
				String[] a = s.split(";");
				for (String i : a) ATTRIBUTES_TO_IGNORE.add(i);
			}
		} catch (IOException e) {}
	}
}
