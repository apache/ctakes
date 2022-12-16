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
package org.apache.ctakes.ytex.libsvm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

public class LibSVMUtilImpl implements LibSVMUtil {
	private JdbcTemplate jdbcTemplate = null;
	public DataSource getDataSource() {
		return jdbcTemplate.getDataSource();
	}

	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}
	
	/* (non-Javadoc)
	 * @see org.apache.ctakes.ytex.libsvm.LibSVMUtil#loadClassLabels(java.lang.String, java.util.Set)
	 */
	public SortedMap<Integer, Map<String, Integer>> loadClassLabels(
			String strQuery, final Set<String> labels) {
		final SortedMap<Integer, Map<String, Integer>> instanceLabelsMap = new TreeMap<Integer, Map<String, Integer>>();
		jdbcTemplate.query(strQuery, new RowCallbackHandler() {

			@Override
			public void processRow(ResultSet rs) throws SQLException {
				int instanceId = rs.getInt(1);
				String label = rs.getString(2);
				int classID = rs.getInt(3);
				Map<String, Integer> instanceLabels = instanceLabelsMap
						.get(instanceId);
				if (instanceLabels == null) {
					instanceLabels = new HashMap<String, Integer>(1);
					instanceLabelsMap.put(instanceId, instanceLabels);
				}
				labels.add(label);
				instanceLabels.put(label, classID);
			}
		});
		return instanceLabelsMap;
	}
	public void outputInstanceIds(String outdir,
			SortedMap<Integer, Map<String, Integer>> trainInstanceLabelMap,
			String string) throws IOException {
		StringBuilder bFileName = new StringBuilder(outdir).append(
				File.separator).append(string).append("_instance_ids").append(
				".txt");
		BufferedWriter w = null;
		try {
			w = new BufferedWriter(new FileWriter(bFileName.toString()));
			for (int instanceId : trainInstanceLabelMap.keySet()) {
				w.write(Integer.toString(instanceId));
				w.newLine();
			}
		} finally {
			if (w != null)
				w.close();
		}
	}

}
