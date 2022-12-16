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
package org.apache.ctakes.core.util;

import org.apache.ctakes.typesystem.type.refsem.Date;
import org.apache.uima.jcas.JCas;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @deprecated use core.util CalendarUtil
 */
@Deprecated
public class DateParser {

	private static DateFormat df = DateFormat.getDateInstance();
	private static Calendar cal = Calendar.getInstance();

	// month names in FULL and SHORT formats
	private static List<String> monthFullNames = new ArrayList<String>();
	private static List<String> monthShortNames = new ArrayList<String>();
	
	static {
		monthFullNames.add("january");
		monthFullNames.add("february");
		monthFullNames.add("march");
		monthFullNames.add("april");
		monthFullNames.add("may");
		monthFullNames.add("june");
		monthFullNames.add("july");
		monthFullNames.add("august");
		monthFullNames.add("september");
		monthFullNames.add("october");
		monthFullNames.add("november");
		monthFullNames.add("december");

		monthShortNames.add("jan");
		monthShortNames.add("feb");
		monthShortNames.add("mar");
		monthShortNames.add("apr");
		monthShortNames.add("may");
		monthShortNames.add("jun");
		monthShortNames.add("jul");
		monthShortNames.add("aug");
		monthShortNames.add("sep"); 	//monthShortNames.add("sept");
		monthShortNames.add("oct");
		monthShortNames.add("nov");
		monthShortNames.add("dec");
		if (monthShortNames.get(11).equals("dec")) {
			// good, do nothing, order is correct
		} else {
			throw new RuntimeException("Check initialization of monthShortNames");
		}

	}

	/**
	 * First try parsing full date (month, day and year) using java.util.Date
	 * If that fails, try extracting at least part of the date
    * @deprecated use core.util CalendarUtil
	 */
	public static Date parse(JCas jcas, String dateString) {
		Date date = new Date(jcas);
		try {
		
			java.util.Date jud = df.parse(dateString);
			// if no parse exception, create the CTS Date
			date = new Date(jcas); // create new CTS Date
			cal.setTime(jud);
			date.setDay(Integer.toString(cal.get(Calendar.DAY_OF_MONTH)));// df.cal.get(Calendar.DAY_OF_MONTH, jud)));
			date.setMonth(Integer.toString(cal.get(Calendar.MONTH)+1));
			date.setYear(Integer.toString(cal.get(Calendar.YEAR)));

		} catch (ParseException e) {
			
			// An exception while parsing the date using java.util.Date
			// Try to get just month and year or month and day etc

			// possibilities to consider just for US dates
			// m dd  (ambiguous pattern with m yy, for at least some values)
			// m yy  (ambiguous pattern with m dd, for at least some values)
			// m yyyy
			// m dd yy
			// m dd yyyy

			dateString = dateString.trim().toLowerCase();
			
			// look for month as string at beginning
			for (int i=0; i<monthShortNames.size(); i++) {
				if (dateString.startsWith(monthShortNames.get(i))) {
					date.setMonth(dateString.substring(0, getIndexFirstNonLetter(dateString)));
				}
			}
			

			// look for month as number

			// look for day
			
			// look for year as yy
			
			// look for year as yyyy at end
			int yearPosition = getIndexAfterLastNonDigit(dateString);
			if (yearPosition+4==dateString.length()) {
				date.setYear(dateString.substring(yearPosition));
			}

			
		}
		
		return date;
	}
	
	/**
	 * 
	 * @return if entire string is letters, returns length of s
    * @deprecated use core.util CalendarUtil
	 */
	public static int getIndexFirstNonLetter(String s) {
		for (int i=0; i<s.length(); i++) {
			if (!Character.isLetter(s.charAt(i))) {
				return i;
			}
		}
		return s.length();
	}

	/**
	 * 
	 * @return if entire string is letters, returns 0
    * @deprecated use core.util CalendarUtil
	 */
	public static int getIndexAfterLastNonDigit(String s) {
		for (int i=s.length(); i>0 ;) {
			i--;
			if (!Character.isDigit(s.charAt(i))) {
				return i+1;
			}
		}
		return 0;
	}
}
