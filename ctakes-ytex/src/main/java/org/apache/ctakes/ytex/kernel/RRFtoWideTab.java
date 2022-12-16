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
package org.apache.ctakes.ytex.kernel;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Convert RRF files to UCS2 tab-delimited format for import into SQL Server.
 * This adds the Unicode Byte Order Marker to the output file.
 * 
 * @author vijay
 */
public class RRFtoWideTab {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		InputStream fis = null;
		OutputStream fos = null;
		boolean bCloseFis = false;
		boolean bCloseFos = false;
		if (args.length == 1 || args.length == 2) {
			fis = new FileInputStream(args[0]);
			bCloseFis = true;
		} else {
			fis = System.in;
		}
		if (args.length == 2) {
			fos = new FileOutputStream(args[1]);
			bCloseFos = true;
		} else {
			fos = System.out;
		}
		//add the byte order mark
	    byte[] utf16lemessage = new byte[2];
	    utf16lemessage[0] = (byte)0xFF;
	    utf16lemessage[1] = (byte)0xFE;
		fos.write(utf16lemessage);
		//convert from utf8 to utf16
		BufferedReader r = new BufferedReader(
				new InputStreamReader(fis, "UTF8"));
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(fos,
				"UTF-16LE"));
		for (String s = ""; (s = r.readLine()) != null;) {
			// chop off the trailing '|'
			if(s.charAt(s.length()-1) == '|') {
				s = s.substring(0,s.length()-1);
			}
			// replace | with tab
			s = s.replace('|', '\t');
			w.write(s);
			w.newLine();
		}
		w.flush();
		if (bCloseFis) {
			r.close();
		}
		if (bCloseFos) {
			w.close();
		}
	}
}
