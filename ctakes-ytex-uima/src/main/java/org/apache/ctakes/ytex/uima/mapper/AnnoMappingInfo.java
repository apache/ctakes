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
package org.apache.ctakes.ytex.uima.mapper;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class AnnoMappingInfo {
	String annoClassName;
	Set<ColumnMappingInfo> columnMappingInfos;
	ColumnMappingInfo coveredTextColumn;
	SortedMap<String, ColumnMappingInfo> mapField = new TreeMap<String, ColumnMappingInfo>();

	String sql;

	String tableName;
	int uimaTypeId;
	String uimaTypeIdColumnName;

	public AnnoMappingInfo() {
	}

	/**
	 * copy values from other annoMappingInfo
	 * 
	 * @param o
	 */
	public AnnoMappingInfo deepCopy() {
		AnnoMappingInfo n = new AnnoMappingInfo();
		n.annoClassName = this.annoClassName;
		n.tableName = this.tableName;
		n.sql = this.sql;
		n.coveredTextColumn = this.coveredTextColumn != null ? this.coveredTextColumn
				.deepCopy() : null;
		Set<ColumnMappingInfo> ciCopy = new HashSet<ColumnMappingInfo>();
		for (ColumnMappingInfo e : this.columnMappingInfos) {
			ciCopy.add(e.deepCopy());
		}
		n.setColumnMappingInfos(ciCopy);
		return n;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AnnoMappingInfo other = (AnnoMappingInfo) obj;
		if (annoClassName == null) {
			if (other.annoClassName != null)
				return false;
		} else if (!annoClassName.equals(other.annoClassName))
			return false;
		return true;
	}

	public String getAnnoClassName() {
		return annoClassName;
	}

	public Set<ColumnMappingInfo> getColumnMappingInfos() {
		return columnMappingInfos;
	}

	public ColumnMappingInfo getCoveredTextColumn() {
		return coveredTextColumn;
	}

	public SortedMap<String, ColumnMappingInfo> getMapField() {
		return mapField;
	}

	public String getSql() {
		return sql;
	}

	public String getTableName() {
		return tableName;
	}

	public int getUimaTypeId() {
		return uimaTypeId;
	}

	public String getUimaTypeIdColumnName() {
		return uimaTypeIdColumnName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((annoClassName == null) ? 0 : annoClassName.hashCode());
		return result;
	}

	public void setAnnoClassName(String annoClassName) {
		this.annoClassName = annoClassName;
	}

	public void setColumnMappingInfos(Set<ColumnMappingInfo> columnMappingInfos) {
		this.columnMappingInfos = columnMappingInfos;
		for (ColumnMappingInfo ci : columnMappingInfos) {
			this.mapField.put(ci.getColumnName(), ci);
		}
	}

	public void setCoveredTextColumn(ColumnMappingInfo coveredTextColumn) {
		this.coveredTextColumn = coveredTextColumn;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public void setUimaTypeId(int uimaTypeId) {
		this.uimaTypeId = uimaTypeId;
	}

	public void setUimaTypeIdColumnName(String uimaTypeIdColumnName) {
		this.uimaTypeIdColumnName = uimaTypeIdColumnName;
	}

	@Override
	public String toString() {
		return "AnnoMappingInfo [mapField=" + mapField + ", tableName="
				+ tableName + "]";
	}

}
