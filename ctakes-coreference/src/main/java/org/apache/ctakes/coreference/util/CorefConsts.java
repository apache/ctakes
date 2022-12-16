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

public class CorefConsts {

	public final static String NE = "ne";
	public final static String DEM = "dem";
	public final static String PRON = "pronoun";
	
	// Max length of sentences to go back when generating a markable pair
	public final static int NEDIST = 20;
	public final static int PRODIST = 3;
	
	// Normalization constants in the attribute calculators (move into there?)
	public final static int TOKDIST = 600;
	public final static int SENTDIST = NEDIST;
	
	// threshold
	public final static double COREF_THRESHOLD = 0.5;
	
}
