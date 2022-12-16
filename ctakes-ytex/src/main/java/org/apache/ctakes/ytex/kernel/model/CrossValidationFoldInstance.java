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
package org.apache.ctakes.ytex.kernel.model;

import java.io.Serializable;

public class CrossValidationFoldInstance implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private long instanceId;
	private boolean train;
	public long getInstanceId() {
		return instanceId;
	}
	public void setInstanceId(long instanceId) {
		this.instanceId = instanceId;
	}
	public boolean isTrain() {
		return train;
	}
	public void setTrain(boolean train) {
		this.train = train;
	}
	public CrossValidationFoldInstance(long instanceId, boolean train) {
		super();
		this.instanceId = instanceId;
		this.train = train;
	}
	public CrossValidationFoldInstance() {
		super();
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (instanceId ^ (instanceId >>> 32));
		result = prime * result + (train ? 1231 : 1237);
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CrossValidationFoldInstance other = (CrossValidationFoldInstance) obj;
		if (instanceId != other.instanceId)
			return false;
		if (train != other.train)
			return false;
		return true;
	}

	
}
