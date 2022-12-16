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

import java.util.LinkedList;

import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;

public class DependencyPath extends LinkedList<ConllDependencyNode>{
	
	private ConllDependencyNode commonNode = null;

	private static String L_TOK_DELIM = Delim.L_TOK_DELIM; 
	private static String R_TOK_DELIM = Delim.R_TOK_DELIM;
	private static String L_POS_DELIM = Delim.L_POS_DELIM; 
	private static String R_POS_DELIM = Delim.R_POS_DELIM;
	private static String L_REL_DELIM = Delim.L_REL_DELIM; 
	private static String R_REL_DELIM = Delim.R_REL_DELIM;
	private static String UP_ARC_A    = Delim.UP_ARC_A;
	private static String UP_ARC_B    = Delim.UP_ARC_B;
	private static String DN_ARC_A    = Delim.DN_ARC_A;
	private static String DN_ARC_B    = Delim.DN_ARC_B;
		
	public DependencyPath() {
		super();
	}

	protected void setCommonNode(ConllDependencyNode node) {
		commonNode = node;
	}
	public ConllDependencyNode getCommonNode() {
		return commonNode;
	}	
	public boolean isCommonNode(ConllDependencyNode node) {
		return commonNode==node;
	}	

	public DependencyPath reverse() {
		DependencyPath current = this;
		DependencyPath newPath = new DependencyPath();
		for (int i=current.size()-1; i>=0; i--) {
			ConllDependencyNode tip = current.get(i);
			newPath.add( tip );
			if (isCommonNode(tip)) {
				newPath.setCommonNode(tip);
			}
		}
		return newPath;
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder();
		
		boolean foundCommon = false;
		for (ConllDependencyNode node : this) {
			if (isCommonNode(node)) {
				foundCommon = true;
				str.append( L_TOK_DELIM + node.getForm()   + R_TOK_DELIM );
				str.append( L_POS_DELIM + node.getPostag() + R_POS_DELIM );
				continue;
			}
			str.append( foundCommon ? 
					DN_ARC_A
					+ L_REL_DELIM + node.getDeprel() + R_REL_DELIM
					+ DN_ARC_B
					+ L_TOK_DELIM + node.getForm()   + R_TOK_DELIM
					+ L_POS_DELIM + node.getPostag() + R_POS_DELIM
					: 
						L_TOK_DELIM + node.getForm()   + R_TOK_DELIM
						+ L_POS_DELIM + node.getPostag() + R_POS_DELIM
						+ UP_ARC_B
						+ L_REL_DELIM + node.getDeprel() + R_REL_DELIM
						+ UP_ARC_A
			);
		}
		return str.toString();
	}
	
	public String toTripleString() {
		StringBuilder str = new StringBuilder();
		
		boolean foundCommon = false;
		int     ctr         = 0;
		for (ConllDependencyNode node : this) {
			ctr++;
			if (isCommonNode(node)) {
				foundCommon = true;
//				str.append( L_TOK_DELIM + node.getForm()   + R_TOK_DELIM );
//				str.append( L_POS_DELIM + node.getPostag() + R_POS_DELIM );
				continue;
			}
			str.append( foundCommon ? 
					L_TOK_DELIM + node.getHead().getForm()   + R_TOK_DELIM
					+ L_POS_DELIM + node.getHead().getPostag() + R_POS_DELIM
					+ DN_ARC_A
					+ L_REL_DELIM + node.getDeprel() + R_REL_DELIM
					+ DN_ARC_B
					+ L_TOK_DELIM + node.getForm()   + R_TOK_DELIM
					+ L_POS_DELIM + node.getPostag() + R_POS_DELIM
					: 
						L_TOK_DELIM + node.getForm()   + R_TOK_DELIM
						+ L_POS_DELIM + node.getPostag() + R_POS_DELIM
						+ UP_ARC_B
						+ L_REL_DELIM + node.getDeprel() + R_REL_DELIM
						+ UP_ARC_A
						+ L_TOK_DELIM + node.getHead().getForm()   + R_TOK_DELIM
						+ L_POS_DELIM + node.getHead().getPostag() + R_POS_DELIM

			);

			if (ctr<this.size()) {
				str.append(Delim.TRIPLE_DELIM);
			}
		}
		return str.toString();
	}
	
	public String toSyntaxString() {
		StringBuilder str = new StringBuilder();
		
		boolean foundCommon = false;
		for (ConllDependencyNode node : this) {
			if (isCommonNode(node)) {
				foundCommon = true;
				str.append( L_POS_DELIM + node.getPostag() + R_POS_DELIM );
				continue;
			}
			str.append( foundCommon ? 
					DN_ARC_A
					+ L_REL_DELIM + node.getDeprel() + R_REL_DELIM
					+ DN_ARC_B
					+ L_POS_DELIM + node.getPostag() + R_POS_DELIM
					: 
						L_POS_DELIM + node.getPostag() + R_POS_DELIM
						+ UP_ARC_B
						+ L_REL_DELIM + node.getDeprel() + R_REL_DELIM
						+ UP_ARC_A
			);
		}
		return str.toString();
	}

}
