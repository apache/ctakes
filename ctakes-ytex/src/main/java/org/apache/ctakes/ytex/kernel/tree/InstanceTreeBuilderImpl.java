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
package org.apache.ctakes.ytex.kernel.tree;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;

public class InstanceTreeBuilderImpl implements InstanceTreeBuilder {
	static final Log log = LogFactory.getLog(InstanceTreeBuilderImpl.class);	
	JdbcTemplate jdbcTemplate;
	private DataSource dataSource;

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		this.jdbcTemplate = new JdbcTemplate(dataSource);		
	}

	Node nodeFromRow(NodeMappingInfo nodeInfo, Map<String, Object> nodeValues) {
		Node n = null;
		Map<String, Serializable> values = new HashMap<String, Serializable>(
				nodeInfo.getValues().size());
		for (String valueName : nodeInfo.getValues()) {
			if (nodeValues.containsKey(valueName)
					&& nodeValues.get(valueName) != null) {
				values.put(valueName, (Serializable) nodeValues.get(valueName));
			}
		}
		// make sure there is something to put in
		if (!values.isEmpty()) {
			n = new Node();
			n.setType(nodeInfo.getNodeType());
			n.setValue(values);
		}
		return n;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<Long, Node> loadInstanceTrees(String filename)
			throws IOException, ClassNotFoundException {
		ObjectInputStream os = null;
		try {
			os = new ObjectInputStream(new BufferedInputStream(
					new FileInputStream(filename)));
			return (Map<Long, Node>) os.readObject();
		} finally {
			if (os != null)
				os.close();
		}
	}

	@Override
	public void serializeInstanceTrees(TreeMappingInfo mappingInfo,
			String filename) throws IOException {
		ObjectOutputStream os = null;
		try {
			os = new ObjectOutputStream(new BufferedOutputStream(
					new FileOutputStream(filename)));
			os.writeObject(loadInstanceTrees(mappingInfo));
		} finally {
			if (os != null)
				os.close();
		}
	}

	public Map<Long, Node> loadInstanceTrees(TreeMappingInfo mappingInfo) {
		Map<NodeKey, Node> nodeKeyMap = new HashMap<NodeKey, Node>();
		this.prepare(mappingInfo.getPrepareScript(), mappingInfo.getPrepareScriptStatementDelimiter());
		Map<Long, Node> instanceMap = loadInstanceTrees(
				mappingInfo.getInstanceIDField(),
				mappingInfo.getInstanceQueryMappingInfo(), nodeKeyMap);
		if (mappingInfo.getNodeQueryMappingInfos() != null) {
			for (QueryMappingInfo qInfo : mappingInfo
					.getNodeQueryMappingInfos()) {
				this.addChildrenToNodes(nodeKeyMap, qInfo);
			}
		}
		return instanceMap;
	}
	

	/**
	 * run 'preparation' statements.  These may e.g. create temporary tables in the database.
	 * @param prepareStatementList
	 */
	protected void prepare(String prepareScript, String prepareScriptDelimiter) {
		if(prepareScript != null && prepareScript.length() > 0) {
			String[] statements = prepareScript.split(prepareScriptDelimiter);
			List<String> listStatements = new ArrayList<String>(statements.length);
			// throw out empty lines
			for(String sql : statements) {
				if(sql != null && sql.trim().length() > 0)
					listStatements.add(sql);
			}
			JdbcTemplate jt = new JdbcTemplate(this.getDataSource());
			jt.batchUpdate(listStatements.toArray(new String[]{}));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.ctakes.ytex.kernel.tree.InstanceTreeBuilder#loadInstanceTrees(java.util.List,
	 * java.lang.String, java.lang.String, java.util.Map)
	 */
	protected Map<Long, Node> loadInstanceTrees(String instanceIDField,
			QueryMappingInfo qInfo, Map<NodeKey, Node> nodeKeyMap) {
		Node[] currentPath = new Node[qInfo.getNodeTypes().size()];
		Map<Long, Node> instanceMap = new HashMap<Long, Node>();
		List<Map<String, Object>> rowData = jdbcTemplate.queryForList(
				qInfo.getQuery(), qInfo.getQueryArgs());
		for (Map<String, Object> row : rowData) {
			for (int i = 0; i < qInfo.getNodeTypes().size(); i++) {
				Node newNode = this.nodeFromRow(qInfo.getNodeTypes().get(i),
						row);
				if (newNode != null) {
					if (!newNode.equals(currentPath[i])) {
						if (i > 0) {
							// add the node to the parent
							currentPath[i - 1].getChildren().add(newNode);
						} else {
							// this is a new root, i.e. a new instance
							// add it to the instance map
							instanceMap.put(((Number) row.get(instanceIDField)).longValue(),
									newNode);
						}
						// put the new node in the path
						// we don't really care about nodes 'after' this one in
						// the path list
						// because we only add to parents, not to children
						currentPath[i] = newNode;
						if (nodeKeyMap != null)
							nodeKeyMap.put(new NodeKey(newNode), newNode);
					}
				}
			}
		}
		return instanceMap;
	}

	public void addChildrenToNodes(Map<NodeKey, Node> nodeKeyMap,
			QueryMappingInfo qInfo) {
		// run query
		List<Map<String, Object>> rowData = jdbcTemplate.queryForList(
				qInfo.getQuery(), qInfo.getQueryArgs());
		// iterate through rows, adding nodes as children of existing nodes
		for (Map<String, Object> row : rowData) {
			// allocate array for holding node path corresponding to row
			Node[] currentPath = new Node[qInfo.getNodeTypes().size()];
			// get the root of this subtree - temporary node contains values
			Node parentTmp = nodeFromRow(qInfo.getNodeTypes().get(0), row);
			if (parentTmp != null) {
				// get the node from the tree that correponds to this node
				Node parent = nodeKeyMap.get(new NodeKey(parentTmp));
				if (parent == null) {
					if (log.isWarnEnabled()) {
						log.warn("couldn't find node for key: " + parentTmp);
					}
				} else {
					// found the parent - add the subtree
					currentPath[0] = parent;
					for (int i = 1; i < qInfo.getNodeTypes().size(); i++) {
						Node newNode = this.nodeFromRow(qInfo.getNodeTypes()
								.get(i), row);
						if (newNode != null) {
							if (!newNode.equals(currentPath[i])) {
								// null out everything after this index in the path
								Arrays.fill(currentPath, i,
										currentPath.length - 1, null);
								// add the node to the parent
								currentPath[i - 1].getChildren().add(newNode);
								// put the new node in the path
								// we don't really care about nodes 'after' this
								// one in the path list
								// because we only add to parents, not to
								// children
								currentPath[i] = newNode;
								if (nodeKeyMap != null)
									nodeKeyMap.put(new NodeKey(newNode),
											newNode);
							}
						}
					}
				}
			}
		}
	}

}
