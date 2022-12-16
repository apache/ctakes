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
package org.apache.ctakes.ytex.kernel;

import java.text.MessageFormat;

public class IRMetrics {
	private static ThreadLocal<MessageFormat> doubleFormat = new ThreadLocal<MessageFormat>() {

		@Override
		protected MessageFormat initialValue() {
			return new MessageFormat("{0,number,#.###}");
		}

	};

	public static String formatDouble(double d) {
		return doubleFormat.get().format(new Object[] { new Double(d) });
	}

	int tp;
	int fp;
	int tn;
	int fn;

	public IRMetrics() {
		super();
	}

	public IRMetrics(int tp, int fp, int tn, int fn) {
		super();
		this.tp = tp;
		this.fp = fp;
		this.tn = tn;
		this.fn = fn;
	}

	public int getTp() {
		return tp;
	}

	public void setTp(int tp) {
		this.tp = tp;
	}

	public int getFp() {
		return fp;
	}

	public void setFp(int fp) {
		this.fp = fp;
	}

	public int getTn() {
		return tn;
	}

	public void setTn(int tn) {
		this.tn = tn;
	}

	public int getFn() {
		return fn;
	}

	public void setFn(int fn) {
		this.fn = fn;
	}

	public double getPrecision() {

		return (tp + fp) > 0 ? (double) (tp) / ((double) (tp + fp)) : 0;
	}

	public double getRecall() {
		return (tp + fn) > 0 ? (double) (tp) / ((double) (tp + fn)) : 0;
	}

	public double getF1() {
		return (getPrecision() + getRecall()) > 0 ? 2 * getPrecision()
				* getRecall() / (getPrecision() + getRecall()) : 0;
	}

	/**
	 * return tab delimited ir metrics:<br/>
	 * tp fp tn fn precision recall f1
	 */
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(this.getTp()).append("\t").append(this.getFp()).append("\t")
				.append(this.getTn()).append("\t").append(this.getFn())
				.append("\t").append(formatDouble(this.getPrecision()))
				.append("\t").append(formatDouble(this.getRecall()))
				.append("\t").append(formatDouble(this.getF1()));
		return b.toString();
	}
}
