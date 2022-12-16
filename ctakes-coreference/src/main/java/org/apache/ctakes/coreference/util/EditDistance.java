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

public class EditDistance {

//	double insCost, delCost, substCost;
//	double[][] cost;
//	
//	public EditDistance (double insCost, double delCost, double substCost) {
//		this.insCost = insCost;
//		this.delCost = delCost;
//		this.substCost = substCost;
//	}

	public static int distance (String s1, String s2) {
		return (int) distance(s1, s2, false);
	}

	public static double normalizedDistance (String s1, String s2) {
		return distance(s1, s2, true);
	}

	public static double distance (String s1, String s2, boolean normalize) {
		String ls1 = s1.toLowerCase();
		String ls2 = s2.toLowerCase();
		int l1 = s1.length();
		int l2 = s2.length();
		int[][] dist = new int[l1+1][l2+1];
		byte[][] track = new byte[l1+1][l2+1];

		dist[0][0] = 0;
		track[0][0] = '\0';
		for (int i = 1; i <= l1; i++) {
			dist[i][0] = i;
			track[i][0] = 'd';
		}
		for (int i = 1; i <= l2; i++) {
			dist[0][i] = i;
			track[0][i] = 'i';
		}

		for (int i = 1; i <= l1; i++)
			for (int j = 1; j <= l2; j++) {
				int d1 = dist[i][j-1] + 1;
				int d2 = dist[i-1][j] + 1;
				byte b;
				int c;
				if (ls1.charAt(i-1)==ls2.charAt(j-1)) { b = 'm'; c = 0; }
				else { b = 's'; c = 1; }
				int d3 = dist[i-1][j-1] + c;
				if (d1 <= d2 && d1 <= d3) { dist[i][j] = d1; track[i][j] = 'i'; }
				else if (d2 <= d1 && d2 <= d3) { dist[i][j] = d2; track[i][j] = 'd'; }
				else if (d3 <= d1 && d3 <= d2) { dist[i][j] = d3; track[i][j] = b; }
			}

		return normalize ? ((double) dist[l1][l2]/(l1>l2?l1:l2)) : dist[l1][l2];
	}
}
