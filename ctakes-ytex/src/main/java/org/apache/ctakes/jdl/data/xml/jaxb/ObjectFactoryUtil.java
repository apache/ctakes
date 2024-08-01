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

import org.apache.ctakes.jdl.schema.xdl.*;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;


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
		if ( objectFactoryMapping == null ) {
			objectFactoryMapping = new ObjectFactoryBind();
		}
		return objectFactoryMapping;
	}

	private static <O> O getJAXBElement( final Object obj, final Class<O> oType ) {
		if ( oType.isInstance( obj ) ) {
			return oType.cast( obj );
		}
		if ( obj instanceof JAXBElement ) {
			final Object value = ((JAXBElement<?>)obj).getValue();
			if ( oType.isInstance( value ) ) {
				return oType.cast( value );
			}
		}
		return null;
	}

	private static <O> O getJAXBElementBySrcXml(final String srcXml, final Class<O> oType ) throws JAXBException {
		return getJAXBElement (getObjectFactoryMapping().unmarshalSrcXml(srcXml, oType), oType );
	}

	private static <O> O getJAXBElementByStrXml(final String strXml, final Class<O> oType ) throws JAXBException {
		return getJAXBElement( getObjectFactoryMapping().unmarshalStrXml(strXml, oType), oType );
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
		return getJAXBElementBySrcXml( srcXml, ConnType.class );
	}

	/**
	 * @param strXml
	 *            the strXml to manage
	 * @return the connType
	 * @throws JAXBException
	 *             exception
	 */
	public static ConnType getConnTypeByStrXml(final String strXml) throws JAXBException {
		return getJAXBElementByStrXml( strXml, ConnType.class );
	}

	/**
	 * @param srcXml
	 *            the srcXml to manage
	 * @return the loadType
	 * @throws JAXBException
	 *             exception
	 */
	public static LoadType getLoadTypeBySrcXml(final String srcXml) throws JAXBException {
		return getJAXBElementBySrcXml( srcXml, LoadType.class );
	}

	/**
	 * @param strXml
	 *            the strXml to manage
	 * @return the loadType
	 * @throws JAXBException
	 *             exception
	 */
	public static LoadType getLoadTypeByStrXml(final String strXml) throws JAXBException {
		return getJAXBElementByStrXml( strXml, LoadType.class );
	}

	/**
	 * @param srcXml
	 *            the srcXml to manage
	 * @return the loadType
	 * @throws JAXBException
	 *             exception
	 */
	public static XmlLoadType getXmlLoadTypeBySrcXml( final String srcXml) throws JAXBException {
		return getJAXBElementBySrcXml( srcXml, XmlLoadType.class );
	}

	/**
	 * @param strXml
	 *            the strXml to manage
	 * @return the loadType
	 * @throws JAXBException
	 *             exception
	 */
	public static XmlLoadType getXmlLoadTypeByStrXml(final String strXml) throws JAXBException {
		return getJAXBElementByStrXml( strXml, XmlLoadType.class );
	}

	/**
	 * @param srcXml
	 *            the srcXml to manage
	 * @return the loadType
	 * @throws JAXBException
	 *             exception
	 */
	public static CsvLoadType getCsvLoadTypeBySrcXml(final String srcXml) throws JAXBException {
		return getJAXBElementBySrcXml( srcXml, CsvLoadType.class );
	}

	/**
	 * @param strXml
	 *            the strXml to manage
	 * @return the loadType
	 * @throws JAXBException
	 *             exception
	 */
	public static CsvLoadType getCsvLoadTypeByStrXml(final String strXml) throws JAXBException {
		return getJAXBElementByStrXml( strXml, CsvLoadType.class );
	}

}
