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
package org.apache.ctakes.ytex.kernel.evaluator;

import org.apache.ctakes.ytex.kernel.tree.Node;

/**
 * Evaluate negation status and certainty of named entities. If negation status
 * differs, multiply convolution on concepts by -1. If certainty differs,
 * multiply by 0.5. This assumes that possible values for certainty are
 * certain/uncertain.
 */
public class NamedEntityNegationKernel extends ConvolutionKernel {
	private static final String CONF_ATTR = "confidence";
	private static final String CERT_ATTR = "certainty";

	@Override
	public double evaluate(Object c1, Object c2) {
		Node ne1 = (Node) c1;
		Node ne2 = (Node) c2;
		Number confidence1 = (Number) ne1.getValue().get(CONF_ATTR);
		Number confidence2 = (Number) ne2.getValue().get(CONF_ATTR);
		Integer certainty1 = (Integer) ne1.getValue().get(CERT_ATTR);
		Integer certainty2 = (Integer) ne2.getValue().get(CERT_ATTR);
		double negationFactor = 1;
		if (confidence1 != null && confidence2 != null
				&& !confidence1.equals(confidence2))
			negationFactor = -1;
		double certaintyFactor = 1;
		if (certainty1 != null && certainty1 != null
				&& !certainty1.equals(certainty2))
			certaintyFactor = 0.5;
		return negationFactor * certaintyFactor * super.evaluate(c1, c2);
	}

}
