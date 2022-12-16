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
package org.apache.ctakes.jdl.data.xml.jaxb;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.ctakes.jdl.schema.xdl.ConnType;
import org.apache.ctakes.jdl.schema.xdl.JdbcType;
import org.apache.ctakes.jdl.schema.xdl.LoadType;


/**
 * Utility to mange JAXB factory.
 * 
 * @author mas
 */
public final class ObjectFactoryUtil {
	private static ObjectFactoryBind objectFactoryMapping;

	private ObjectFactoryUtil() {
	}

	private static ObjectFactoryBind getObjectFactoryMapping() throws JAXBException {
		return (objectFactoryMapping == null) ? new ObjectFactoryBind() : objectFactoryMapping;
	}

	private static Object getJAXBElement(final Object obj) {
		return (obj == null) ? obj : ((JAXBElement<?>) obj).getValue();
	}

	private static Object getJAXBElementBySrcXml(final String srcXml) throws JAXBException {
		return getJAXBElement(getObjectFactoryMapping().unmarshalSrcXml(srcXml));
	}

	private static Object getJAXBElementByStrXml(final String strXml) throws JAXBException {
		return getJAXBElement(getObjectFactoryMapping().unmarshalStrXml(strXml));
	}

	/**
	 * @param srcXml
	 *            the srcXml to manage
	 * @return the jdbcType
	 * @throws JAXBException
	 *             exception
	 */
	public static JdbcType getJdbcTypeBySrcXml(final String srcXml) throws JAXBException {
		return getConnTypeBySrcXml(srcXml).getJdbc();
	}

	/**
	 * @param strXml
	 *            the strXml to manage
	 * @return the jdbcType
	 * @throws JAXBException
	 *             exception
	 */
	public static JdbcType getJdbcTypeByStrXml(final String strXml) throws JAXBException {
		return getConnTypeByStrXml(strXml).getJdbc();
	}

	/**
	 * @param srcXml
	 *            the srcXml to manage
	 * @return the connType
	 * @throws JAXBException
	 *             exception
	 */
	public static ConnType getConnTypeBySrcXml(final String srcXml) throws JAXBException {
		return (ConnType) getJAXBElementBySrcXml(srcXml);
	}

	/**
	 * @param strXml
	 *            the strXml to manage
	 * @return the connType
	 * @throws JAXBException
	 *             exception
	 */
	public static ConnType getConnTypeByStrXml(final String strXml) throws JAXBException {
		return (ConnType) getJAXBElementByStrXml(strXml);
	}

	/**
	 * @param srcXml
	 *            the srcXml to manage
	 * @return the loadType
	 * @throws JAXBException
	 *             exception
	 */
	public static LoadType getLoadTypeBySrcXml(final String srcXml) throws JAXBException {
		return (LoadType) getJAXBElementBySrcXml(srcXml);
	}

	/**
	 * @param strXml
	 *            the strXml to manage
	 * @return the loadType
	 * @throws JAXBException
	 *             exception
	 */
	public static LoadType getLoadTypeByStrXml(final String strXml) throws JAXBException {
		return (LoadType) getJAXBElementByStrXml(strXml);
	}
}
