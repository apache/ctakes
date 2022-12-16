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
package org.apache.ctakes.dependency.parser.util;

import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.fit.util.JCasUtil;

import org.apache.ctakes.typesystem.type.textsem.Predicate;
import org.apache.ctakes.typesystem.type.textsem.SemanticRoleRelation;

public class SRLUtility {
	public static String dumpSRLOutput(Annotation annotation) {
		StringBuilder builder = new StringBuilder();
		for (Predicate predicate : JCasUtil.selectCovered(Predicate.class, annotation)) {
			builder.append(predicate.getCoveredText() + predicate.getFrameSet() + "(");
			for (SemanticRoleRelation relation : JCasUtil.select(predicate.getRelations(), SemanticRoleRelation.class)) {
				builder.append(String.format("%s=%s ", relation.getArgument().getLabel(), relation.getArgument().getCoveredText()));
			}
			builder.append(")\n");
		}
		return builder.toString();
	}
}
