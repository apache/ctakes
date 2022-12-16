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
package org.apache.ctakes.core.util;

import org.apache.ctakes.typesystem.type.constants.CONST;

/**
 * A helper class that centralizes methods to assign all the 
 * default values from the SHARP corpus annotation guidelines.
 * 
 * @author stephen wu
 *
 */
public class SHARPKnowtatorXMLDefaults {

	public static String getSubject() {
		return CONST.ATTR_SUBJECT_PATIENT;
	}

	public static int getHistoryOf() {
		return CONST.NE_HISTORY_OF_ABSENT;
	}
}