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
package org.apache.ctakes.coreference.eval.helpers;

public class Span {

	int[][] s;
	int length;

	public Span () {}
	public Span (int[] i) {
		if (i.length % 2 == 0) {
			length = 0;
			s = new int[i.length/2][2];
			for (int j = 0; j < i.length; j+=2) {
				s[j/2][0] = i[j];
				s[j/2][1] = i[j+1];
				length += i[j+1] - i[j];
			}
		}
	}

	public Span (String str) {
		String[] ss = str.split("[-:]");
		if (ss.length % 2 == 0) {
			s = new int[ss.length/2][2];
			for (int i = 0; i < ss.length; i+=2) {
				s[i/2][0] = Integer.parseInt(ss[i]);
				s[i/2][1] = Integer.parseInt(ss[i+1]);
				length += s[i/2][1] - s[i/2][0];
			}
		}
	}

	public int size () { return s.length; }
	public int length () { return length; }
	public int[] get (int i) { return s[i]; }

	public Span tail () {
		if (s.length==1) return new Span();
		int[] ret = new int[(s.length-1)*2];
		for (int i = 1; i < s.length; i++) {
			ret[(i-1)*2] = s[i][0];
			ret[i*2-1] = s[i][1];
		}
		return new Span(ret);
	}

	// 2 * intersect / (length of s1 + length of s2)
	public static double score (Span s1, Span s2) {
		double a = 0;
		double b = 0;
		// there is a more efficient way
		for (int i = 0; i < s1.size(); i++)
			for (int j = 0; j < s2.size(); j++)
				a += overlap(s1.get(i), s2.get(j));
		for (int i = 0; i < s1.size(); i++)
			b += s1.get(i)[1] - s1.get(i)[0];
		for (int i = 0; i < s2.size(); i++)
			b += s2.get(i)[1] - s2.get(i)[0];
		return a==0 ? -1 : a/b;
	}

	private static int overlap (int[] a, int[] b) {
	    int ret;
		if (a[0] >= b[0])
			ret = (a[1]>b[1] ? b[1] : a[1]) - a[0];
		else
			ret = (a[1]<b[1] ? a[1] : b[1]) - b[0];
		if ((ret*=2) < 0) ret= 0;
		return ret;
	}

	public double gap () {
		return 0;
	}

	public String toString () {
		StringBuffer sb = new StringBuffer();
		for (int i[] : s)
			sb.append(i[0]).append("-").append(i[1]).append(":");
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}

	@Override
	public boolean equals (Object o) {
		if (! (o instanceof Span)) return false;
		Span span = (Span) o;
		if (size() != span.size()) return false;
		SpanOffsetComparator soc = new SpanOffsetComparator();
		return soc.compare(this, span)==0;
	}

	@Override
	public int hashCode () {
		return toString().hashCode();
	}
//////////////////////////////////////////////////////////////////////////////
//	int c;
//	static int[][] m;
//	public Span (char c) {
//		switch(c) {
//		case 'A': this.c=0; break;
//		case 'C': this.c=1; break;
//		case 'G': this.c=2; break;
//		case 'T': this.c=3; break;
//		}
//		mat();
//	}
//	public Span () {}
//	private void mat() {
//		m = new int[4][4];
//		m[0][0] = 2;
//		m[0][1] = -1;
//		m[0][2] = 1;
//		m[0][3] = -1;
//		m[1][0] = -1;
//		m[1][1] = 2;
//		m[1][2] = -1;
//		m[1][3] = 1;
//		m[2][0] = 1;
//		m[2][1] = -1;
//		m[2][2] = 2;
//		m[2][3] = -1;
//		m[3][0] = -1;
//		m[3][1] = 1;
//		m[3][2] = -1;
//		m[3][3] = 2;
//	}
//	public int get () { return c; }
//	public String toString() { if (c==0) return "A"; if (c==1) return "C"; if (c==2) return "G"; if (c==3) return "T"; return ""; }
//	public static double score (Span s1, Span s2) {
//		return m[s1.get()][s2.get()];
//	}
//	public double gap () { return -2; }
}
