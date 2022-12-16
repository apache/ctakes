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
package org.apache.ctakes.preprocessor;

/**
 *
 * @author Mayo Clinic
 */
public class Utility
{
	/**
	 * Replaces any non-ascii characters with the specified char.
	 * @param sb
	 */
	public static void replaceNonAsciiChars(StringBuffer sb, char replacementChar)
	{
		for (int i = 0; i < sb.length(); i++)
		{
			char c = sb.charAt(i);
			// Unicode range 0000-007f Basic Latin
			// equivalent to ASCII charset
			if (c > 0x007f)
			{
				// character is outside ASCII range of unicode char set
				sb.setCharAt(i, replacementChar);
			}
		}
	}

	/**
	 * remove leading and trailing whitespace from each line
	 * @param sb
	 * @return
	 */
	public static String compress(StringBuffer sb)
	{
		StringBuffer compressedSB = new StringBuffer();
		if (sb == null)
		{
			return compressedSB.toString();
		}

		int indexOfLastNewline = 0;
		// use for loop to handle through the last newline character
		for (int i = 0; i < sb.length(); i++)
		{
			char currentChar = sb.charAt(i);
			if (currentChar == '\n')
			{
				if ((i - indexOfLastNewline) > 1)
				{
					String lineText = sb.substring(indexOfLastNewline, i);
					String compressedText = lineText.trim();
					if (compressedText.length() > 0)
					{
						compressedSB.append(compressedText);
						compressedSB.append('\n');
					}
				}
				indexOfLastNewline = i;
			}
		}
		
		// handles text after last newline character, or text if 
		// there were no newline characters
		if (indexOfLastNewline < sb.length())
		{
			String lineText = sb.substring(indexOfLastNewline, sb.length());
			String compressedText = lineText.trim();
			compressedSB.append(compressedText);
		}

		return compressedSB.toString();
	}
}
