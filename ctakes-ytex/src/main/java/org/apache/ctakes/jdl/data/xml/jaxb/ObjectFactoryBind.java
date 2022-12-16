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

import java.io.File;
import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.ctakes.jdl.schema.xdl.ObjectFactory;


/**
 * Bind JAXB factory.
 * 
 * @author mas
 */
public class ObjectFactoryBind {
	private Unmarshaller unmarshaller;

	/**
	 * @throws JAXBException
	 *             exception
	 */
	public ObjectFactoryBind() throws JAXBException {
		unmarshaller = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName()).createUnmarshaller();
	}

	/**
	 * @param srcXml
	 *            the srcXml to unmarshal
	 * @return the object unmarshalled
	 * @throws JAXBException
	 *             exception
	 */
	public final Object unmarshalSrcXml(final String srcXml) throws JAXBException {
		try {
			return unmarshaller.unmarshal(new File(srcXml));
		} catch (JAXBException e) {
			throw e;
		}
	}

	/**
	 * @param strXml
	 *            the strXml to unmarshal
	 * @return the object unmarshalled
	 * @throws JAXBException
	 *             exception
	 */
	public final Object unmarshalStrXml(final String strXml) throws JAXBException {
		try {
			return unmarshaller.unmarshal(new StringReader(strXml));
		} catch (JAXBException e) {
			throw e;
		}
	}
}
