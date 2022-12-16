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
package org.apache.ctakes.core.fsm.state;

import net.openai.util.fsm.State;

/**
 * Used mainly for aiding the debugging process.
 * 
 * @author Mayo Clinic
 */
@SuppressWarnings("serial")
public class NamedState extends State {

	public NamedState(String name) {
		setName(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.openai.util.fsm.State#enter(java.lang.Object)
	 */
	public void enter(Object arg0) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.openai.util.fsm.State#exit()
	 */
	public Object exit() {
		// TODO Auto-generated method stub
		return null;
	}

}
