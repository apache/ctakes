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

import java.util.Map;

import org.apache.ctakes.ytex.kernel.tree.Node;
import org.springframework.beans.factory.InitializingBean;


/**
 * weighted polynomial mixing kernel: <code>
 * 	(\sum w_i * k(x_i, y_i) \div \sum w_i ) ^ l 
 * </code>
 * <ul>
 * <li>Apply the delegate kernel to the respective 'parts' of this node (x_i,
 * y_i)
 * <li>Multiply the result by the weight (w_i * k(x_i,y_i))
 * <li>Sum everything up, divide by the sum of the weights
 * <li>take the power of everything to l
 * </ul>
 * 
 * 
 * {@link #pow} the power to raise things to
 * <p/>
 * {@link #attributeKey} the children of this node form a 'vector'. This is the
 * attribute that we use to 'index' this vector
 * <p/>
 * {@link #mapIndexWeight} the indices (i.e. the value of
 * <code>child.getValue().get(attributeKey)</code> and the corresponding
 * weights. The indices must be integers. The
 * class must be identical to the class of the child - take care that the
 * numeric types match.
 * <p/>
 * {@link #delegateKernel} the kernel to apply to pairs of children.
 * 
 * @author vijay
 * 
 */
public class WeightedPolynomialMixingKernel implements Kernel, InitializingBean {

	private int pow = 1;
	private String attributeKey;
	private Map<Integer, Double> mapIndexWeight;
	private Kernel delegateKernel;

	public int getPow() {
		return pow;
	}

	public void setPow(int pow) {
		this.pow = pow;
	}

	public String getAttributeKey() {
		return attributeKey;
	}

	public void setAttributeKey(String attributeKey) {
		this.attributeKey = attributeKey;
	}

	public Map<Integer, Double> getMapIndexWeight() {
		return mapIndexWeight;
	}

	public void setMapIndexWeight(Map<Integer, Double> mapIndexWeight) {
		this.mapIndexWeight = mapIndexWeight;
	}

	public Kernel getDelegateKernel() {
		return delegateKernel;
	}

	public void setDelegateKernel(Kernel delegateKernel) {
		this.delegateKernel = delegateKernel;
	}

	private double scalingFactor;

	@Override
	public double evaluate(Object o1, Object o2) {
		double retVal = 0;
		// both objects must be nodes
		if ((o1 instanceof Node) && (o2 instanceof Node)) {
			double keval = 0;
			// iterate through the 'indices' and the weights
			for (Map.Entry<Integer, Double> indexWeight : mapIndexWeight
					.entrySet()) {
				// get the pair of matching nodes
				Node n1 = getNodeForIndex(indexWeight.getKey(), (Node) o1);
				Node n2 = getNodeForIndex(indexWeight.getKey(), (Node) o2);
				if (n1 != null && n2 != null) {
					// evaluate the kernel, multiply by weight, add to running
					// sum
					keval += (delegateKernel.evaluate(n1, n2) * indexWeight
							.getValue());
				}
			}
			if (keval != 0) {
				// raise to the power, divide by the scaling factor
				retVal = Math.pow(keval, pow) / scalingFactor;
			}
		}
		return retVal;

	}

	/**
	 * @param index
	 *            the attribute has to match this
	 * @param o1
	 *            the node whose children we're going to search
	 * @return node if found, else null
	 */
	private Node getNodeForIndex(int index, Node o1) {
		for (Node n : o1.getChildren()) {
			Integer attribute = (Integer)n.getValue().get(attributeKey);
			if (attribute != null && index == attribute.intValue())
				return n;
		}
		return null;
	}

	/**
	 * precompute the scaling factor - we will always divide by this
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		double totalWeight = 0d;
		for (double weight : this.mapIndexWeight.values()) {
			totalWeight += weight;
		}
		this.scalingFactor = Math.pow(totalWeight, pow);
	}

}
