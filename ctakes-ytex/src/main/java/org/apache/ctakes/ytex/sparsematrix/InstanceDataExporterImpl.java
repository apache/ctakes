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
package org.apache.ctakes.ytex.sparsematrix;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import org.apache.ctakes.ytex.kernel.InstanceData;


/**
 * output the instance data
 * 
 * @author vijay
 * 
 */
public class InstanceDataExporterImpl implements InstanceDataExporter {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.ctakes.ytex.sparsematrix.InstanceDataExporter#outputInstanceData(org.apache.ctakes.ytex.kernel
	 * .InstanceData, java.lang.String)
	 */
	@Override
	public void outputInstanceData(InstanceData instanceData, String filename)
			throws IOException {
		outputInstanceData(instanceData, filename, FIELD_DELIM, RECORD_DELIM,
				STRING_ESCAPE, INCLUDE_HEADER);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.ctakes.ytex.sparsematrix.InstanceDataExporter#outputInstanceData(org.apache.ctakes.ytex.kernel
	 * .InstanceData, java.lang.String, java.lang.String, java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public void outputInstanceData(InstanceData instanceData, String filename,
			String fieldDelim, String recordDelim, String stringEscape,
			boolean includeHeader) throws IOException {
		BufferedWriter bw = null;
		try {
			StringWriter w = new StringWriter();
			boolean includeLabel = false;
			boolean includeRun = false;
			boolean includeFold = false;
			boolean includeTrain = false;
			for (String label : instanceData.getLabelToInstanceMap().keySet()) {
				for (int run : instanceData.getLabelToInstanceMap().get(label)
						.keySet()) {
					for (int fold : instanceData.getLabelToInstanceMap()
							.get(label).get(run).keySet()) {
						for (boolean train : instanceData
								.getLabelToInstanceMap().get(label).get(run)
								.get(fold).keySet()) {
							for (Map.Entry<Long, String> instanceClass : instanceData
									.getLabelToInstanceMap().get(label)
									.get(run).get(fold).get(train).entrySet()) {
								// write instance id
								w.write(Long.toString(instanceClass.getKey()));
								w.write(fieldDelim);
								// write class
								appendString(instanceClass.getValue(),
										stringEscape, w);
								// if there are multiple labels, write the label
								if (label.length() > 0) {
									includeLabel = true;
									w.write(fieldDelim);
									appendString(label, stringEscape, w);
								}
								// if there are multiple runs, write the run
								if (run > 0) {
									includeRun = true;
									w.write(fieldDelim);
									w.write(Integer.toString(run));
								}
								// if there are multiple folds, write the fold
								if (fold > 0) {
									includeFold = true;
									w.write(fieldDelim);
									w.write(Integer.toString(fold));
								}
								// if there is a distinction between training/testing, write the train/test flag
								if (instanceData.getLabelToInstanceMap()
										.get(label).get(run).get(fold).size() > 1) {
									includeTrain = true;
									w.write(fieldDelim);
									w.write(train ? "1" : "0");
								}
								w.write(recordDelim);
							}
						}
					}
				}
			}
			bw = new BufferedWriter(new FileWriter(filename));
			if (includeHeader) {
				appendString("instance_id", stringEscape, bw);
				bw.write(fieldDelim);
				appendString("class", stringEscape, bw);
				// write colnames
				if (includeLabel) {
					bw.write(fieldDelim);
					appendString("label", stringEscape, bw);
				}
				if (includeRun) {
					bw.write(fieldDelim);
					appendString("run", stringEscape, bw);
				}
				if (includeFold) {
					bw.write(fieldDelim);
					appendString("fold", stringEscape, bw);
				}
				if (includeTrain) {
					bw.write(fieldDelim);
					appendString("train", stringEscape, bw);
				}
				bw.write(recordDelim);
			}
			// write the rest of the data
			bw.write(w.toString());
		} finally {
			if (bw != null) {
				bw.close();
			}
		}

	}

	private void appendString(String str, String stringEscape, Writer w)
			throws IOException {
		w.write(stringEscape);
		w.write(str);
		w.write(stringEscape);
	}

}
