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

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.Converter;
import org.apache.ctakes.ytex.uima.annotators.DateAnnotator;

/**
 * convert ISO8601 formatted date to Date/Timestamp object
 * @author vijay
 *
 */
public class ISO8601Converter implements Converter {
	private ThreadLocal<SimpleDateFormat> tlDateFormat = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat(DateAnnotator.DATE_FORMAT);
		}
	};

	@Override
	public Object convert(Class targetClass, Object input)
			throws ConversionException {
		if (!(input instanceof String)) {
			throw new ConversionException("input not a string: "
					+ input.getClass());
		}
		Date dt;
		try {
			dt = tlDateFormat.get().parse((String) input);
			if (targetClass.equals(Date.class))
				return dt;
			else if (targetClass.equals(Timestamp.class))
				return new Timestamp(dt.getTime());
			else
				throw new ConversionException("bad target type: "
						+ targetClass.getName());
		} catch (ParseException e) {
			throw new ConversionException(e);
		}
	}

}
