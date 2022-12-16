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
package org.apache.ctakes.utils.xcas_comparison;
import java.io.*;

/**
 * This class illustrates the basic usage of this tool.
 * @author Mayo Clinic
 *
 */
public class Compare {

	public static void main(String[] args) {
//		Prepare the list of attributes to ignore when comparing elements
		Const.init();
//		Initialize a processor
		XcasProcessor p = new XcasProcessor();
//		Process/parse the two files specified in args[0] and args[1]
		File f1 = new File(args[0]);
		if (!f1.exists()) { System.err.println(args[0]+" not exist!"); System.exit(1); }
		File f2 = new File(args[1]);
		if (!f2.exists()) { System.err.println(args[1]+" not exist!"); System.exit(1); }
		XcasFile xf1 = p.process(f1);
		XcasFile xf2 = p.process(f2);
//		Construct an XcasDiff object from the two XcasFiles
		XcasDiff d = new XcasDiff(xf1, xf2);
//		Print differences to stdout
		d.printDiff();
//		Print an HTML summary to file specified in args[2]
		try {
			d.printHTML(new FileWriter(args[2]));
			System.out.println();
			System.out.println("HTML summary written to "+args[2]);
		} catch (IOException e) { e.printStackTrace(); }
	}
}
