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

import java.lang.reflect.Field;

import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

public class JCasUtil {

	public static int getType(String className) {
		try {
			Class<?> cls = Class.forName(className);
			Class<? extends TOP> annotationCls = cls.asSubclass(Annotation.class);
			return JCasUtil.getType(annotationCls);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public static int getType(Class<? extends TOP> cls) {
		try {
			Field typeField = cls.getDeclaredField("type");
			// return value of static field
			return typeField.getInt(null);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}

	}
}
