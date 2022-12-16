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
package org.apache.ctakes.ytex.kernel.dao;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;

public class SortedSetUserType {
	private static final Log log = LogFactory.getLog(SortedSetUserType.class);

	public int[] sqlTypes() {
		return new int[] { Types.CHAR };
	}

	public Class<SortedSet> returnedClass() {
		return SortedSet.class;
	}

	public boolean equals(Object x, Object y) {
		return (x == y)
				|| (x != null && y != null && java.util.Arrays.equals(
						(int[]) x, (int[]) y));
	}

	private String sortedSetToString(SortedSet<String> set) {
		StringBuilder b = new StringBuilder();
		Iterator<String> iter = set.iterator();
		while (iter.hasNext()) {
			b.append(iter.next());
			if (iter.hasNext()) {
				b.append("|");
			}
		}
		return b.toString();
	}

	private Set<String> stringToSortedSet(String s) {
		String[] elements = s.split("\\|");
		SortedSet<String> set = new TreeSet<String>();
		set.addAll(Arrays.asList(elements));
		return set;
	}

	public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
			throws HibernateException, SQLException {
		String s = rs.getString(names[0]);
		return stringToSortedSet(s);
	}

	@SuppressWarnings("unchecked")
	public void nullSafeSet(PreparedStatement st, Object value, int index)
			throws HibernateException, SQLException {
		st.setString(index, sortedSetToString((SortedSet<String>) value));
	}

	public Object deepCopy(Object value) {
		if (value == null)
			return null;

		byte[] bytes = (byte[]) value;
		byte[] result = new byte[bytes.length];
		System.arraycopy(bytes, 0, result, 0, bytes.length);

		return result;
	}

	public boolean isMutable() {
		return true;
	}

	public Object assemble(Serializable cached, Object owner)
			throws HibernateException {
		return cached;
	}

	public Serializable disassemble(Object value) throws HibernateException {
		return (Serializable) value;
	}

	public Object replace(Object original, Object target, Object owner)
			throws HibernateException {
		return original;
	}

	public int hashCode(Object x) throws HibernateException {
		return x.hashCode();
	}

}
