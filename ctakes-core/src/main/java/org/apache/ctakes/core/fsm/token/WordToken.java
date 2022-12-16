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
package org.apache.ctakes.core.fsm.token;

public interface WordToken extends TextToken {
	// constants
	public static final byte CAPS_UNKNOWN = 0;
	public static final byte CAPS_NONE = 1;
	public static final byte CAPS_MIXED = 2;
	public static final byte CAPS_FIRST_ONLY = 3;
	public static final byte CAPS_ALL = 4;

	public static final byte NUM_NONE = 0;
	public static final byte NUM_FIRST = 1;
	public static final byte NUM_MIDDLE = 2;
	public static final byte NUM_LAST = 3;

	public byte getCaps();

	public byte getNumPosition();
}
