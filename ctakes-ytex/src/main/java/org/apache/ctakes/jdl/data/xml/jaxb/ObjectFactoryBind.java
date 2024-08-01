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

import org.apache.ctakes.core.resource.FileLocator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;


/**
 * Bind JAXB factory.
 * 
 * @author mas
 */
public class ObjectFactoryBind {
//	private Unmarshaller unmarshaller;

	static private final Map<String,Unmarshaller> UNMARSHALLER_MAP = new HashMap<>();

	/**
	 * @throws JAXBException
	 *             exception
	 */
	public ObjectFactoryBind() throws JAXBException {
//		unmarshaller = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName()).createUnmarshaller();
//		unmarshaller = JAXBContext.newInstance( ObjectFactory.class ).createUnmarshaller();
	}

	private <O> Unmarshaller getUnmarshaller( final Class<O> oType ) throws JAXBException {
		Unmarshaller unmarshaller = UNMARSHALLER_MAP.get( oType.getName() );
		if ( unmarshaller != null ) {
			return unmarshaller;
		}
		final JAXBContext context = JAXBContext.newInstance( oType );
		unmarshaller = context.createUnmarshaller();
		UNMARSHALLER_MAP.put( oType.getName(), unmarshaller );
		return unmarshaller;
	}

	/**
	 * @param srcXml the srcXml to unmarshal
	 * @param oType class of expected return
	 * @return the object unmarshalled
	 * @throws JAXBException -
	 */
	public final <O> O unmarshalSrcXml(final String srcXml, final Class<O> oType ) throws JAXBException {
		final Unmarshaller unmarshaller = getUnmarshaller( oType );
		try {
//			return oType.cast( unmarshaller.unmarshal( FileLocator.getAsStream( srcXml ) ) );
//			return oType.cast( unmarshaller.unmarshal( new StreamSource( FileLocator.getAsStream( srcXml ) ), oType ) );
			final Source source = new StreamSource( FileLocator.getAsStream( srcXml ) );
//			System.out.println( "ObjectFactoryBind.unmarshalSrcXml srcXml: " + srcXml + " path " + FileLocator.getFileQuiet( srcXml ).getPath() );
			final JAXBElement<O> element = unmarshaller.unmarshal( source, oType );
			return element.getValue();
		} catch ( FileNotFoundException fnfE ) {
			throw new JAXBException( fnfE );
		}
	}

	/**
	 * @param strXml
	 *            the strXml to unmarshal
	 * @return the object unmarshalled
	 * @throws JAXBException
	 *             exception
	 */
	public final <O> O unmarshalStrXml( final String strXml, final Class<O> oType ) throws JAXBException {
		final Unmarshaller unmarshaller = getUnmarshaller( oType );
		return oType.cast( unmarshaller.unmarshal( new StringReader(strXml) ) );
	}
}
