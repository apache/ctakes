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

import org.junit.Test;

import org.apache.log4j.Logger;

import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class SpanTest {

	private Logger log = Logger.getLogger(SpanTest.class);

	@Test
	public void testSpanCreation () {
		Span[] s1 = new Span[4];
		Span[] s2 = new Span[4];
		int[] a = new int[2];
		int[] b = {41,47,100,128,150,157}; s1[0] = new Span(b);
		a[0] = 100; a[1] = 128; s1[1] = new Span(a);
		a[0] = 116; a[1] = 128; s1[2] = new Span(a);
		a[0] = 150; a[1] = 157; s1[3] = new Span(a);
		int[] c = {41,49,100,128}; s2[0] = new Span(c);
		a[0] = 100; a[1] = 128; s2[1] = new Span(a);
		a[0] = 110; a[1] = 128; s2[2] = new Span(a);
		a[0] = 200; a[1] = 208; s2[3] = new Span(a);

		SpanAlignment sa = new SpanAlignment (s1, s2);

		int[] id1 = sa.get1();
		String line1 = IntStream.of(sa.get1()).boxed().map(i -> i.toString()).collect(Collectors.joining(" "));
		assertEquals("1 2 3 4", line1);
		log.info( line1 );
		String line2 = IntStream.of(sa.get2()).boxed().map(i -> i.toString()).collect(Collectors.joining(" "));
		assertEquals("1 2 3 5", line2);
		log.info( line2 );
	}
}
