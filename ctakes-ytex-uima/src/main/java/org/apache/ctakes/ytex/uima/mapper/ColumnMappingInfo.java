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

import org.apache.commons.beanutils.Converter;

public class ColumnMappingInfo {
	private String annoFieldName;
	private String columnName;
	private Converter converter;
	private String jxpath;
	private int size;
	private int sqlType;

	private Class<?> targetType;

	private String targetTypeName;

	public ColumnMappingInfo() {
	}

	public ColumnMappingInfo deepCopy() {
		ColumnMappingInfo n = new ColumnMappingInfo();
		n.annoFieldName = this.annoFieldName;
		n.converter = this.converter;
		n.columnName = this.columnName;
		n.targetType = this.targetType;
		n.targetTypeName = this.targetTypeName;
		n.sqlType = this.sqlType;
		n.jxpath = this.jxpath;
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
		ColumnMappingInfo other = (ColumnMappingInfo) obj;
		if (columnName == null) {
			if (other.columnName != null)
				return false;
		} else if (!columnName.equals(other.columnName))
			return false;
		return true;
	}

	public String getAnnoFieldName() {
		return annoFieldName;
	}

	public String getColumnName() {
		return columnName;
	}

	public Converter getConverter() {
		return converter;
	}

	public String getJxpath() {
		return jxpath;
	}

	public int getSize() {
		return size;
	}

	public int getSqlType() {
		return sqlType;
	}

	public Class<?> getTargetType() {
		return targetType;
	}

	public String getTargetTypeName() {
		return targetTypeName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((columnName == null) ? 0 : columnName.hashCode());
		return result;
	}

	public void setAnnoFieldName(String annoFieldName) {
		this.annoFieldName = annoFieldName;
	}

	public void setColumnName(String tableFieldName) {
		this.columnName = tableFieldName;
	}

	public void setConverter(Converter converter) {
		this.converter = converter;
	}

	public void setJxpath(String jxpath) {
		this.jxpath = jxpath;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public void setSqlType(int sqlType) {
		this.sqlType = sqlType;
	}

	public void setTargetTypeName(String targetTypeName) {
		this.targetTypeName = targetTypeName;
		try {
			this.targetType = Class.forName(targetTypeName);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return "ColumnMappingInfo [columnName=" + columnName + "]";
	}

}
