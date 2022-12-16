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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;


public class ConvertAndCompare {


	public static void main(String[] args) {
		// key = cni, value = open source
		Hashtable<String, String> map = new Hashtable<String, String>();
		map.put("edu.mayo.bmi.uima.common.type.DocumentID",
				"edu.mayo.bmi.uima.common.types.DocumentIDAnnotation");
		map.put("org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation",
				"edu.mayo.bmi.uima.lookup.types.LookupWindowAnnotation");
//		Specify your mappings here
/*		map.put("",
				"uima.tt.MCAnnotation");
		map.put("",
				"uima.tt.TCAnnotation");
		map.put("",
				"uima.tt.WHPAnnotation");
		map.put("",
				"uima.tt.OBJAnnotation");
		map.put("",
				"uima.tt.PSUBAnnotation");
		map.put("",
				"uima.tt.SUBAnnotation");
		map.put("",
				"uima.tt.AdjAnnotation");
		map.put("",
				"uima.tt.CNPAnnotation");
		map.put("",
				"uima.tt.NPAnnotation");
		map.put("",
				"uima.tt.NPListAnnotation");
		map.put("",
				"uima.tt.NPPAnnotation");
		map.put("",
				"uima.tt.PPAnnotation");
		map.put("",
				"uima.tt.PVGAnnotation");
		map.put("",
				"uima.tt.VGAnnotation");
*/
		map.put("edu.mayo.bmi.uima.cdt.type.RomanNumeralAnnotation",
				"edu.mayo.bmi.uima.cdt.types.RomanNumeralAnnotation");
		map.put("edu.mayo.bmi.uima.cdt.type.FractionAnnotation",
				"edu.mayo.bmi.uima.cdt.types.FractionAnnotation");
		map.put("edu.mayo.bmi.uima.cdt.type.DateAnnotation",
				"edu.mayo.bmi.uima.cdt.types.DateAnnotation");
		map.put("edu.mayo.bmi.uima.cdt.type.ProblemListAnnotation",
				"edu.mayo.bmi.uima.cdt.types.ProblemListAnnotation");
		map.put("edu.mayo.bmi.uima.cdt.type.MeasurementAnnotation",
				"edu.mayo.bmi.uima.cdt.types.MeasurementAnnotation");
		map.put("edu.mayo.bmi.uima.cdt.type.PersonTitleAnnotation",
				"edu.mayo.bmi.uima.cdt.types.PersonTitleAnnotation");
		map.put("org.apache.ctakes.typesystem.type.textspan.Segment",
				"edu.mayo.bmi.uima.common.types.SegmentAnnotation");
		map.put("org.apache.ctakes.typesystem.type.textspan.Sentence",
				"edu.mayo.bmi.uima.common.types.SentenceAnnotation");
		map.put("org.apache.ctakes.typesystem.type.syntax.WordToken",
				"edu.mayo.bmi.uima.common.types.WordTokenAnnotation");
		map.put("org.apache.ctakes.typesystem.type.NumToken",
				"edu.mayo.bmi.uima.common.types.NumTokenAnnotation");
		map.put("org.apache.ctakes.typesystem.type.PunctuationToken",
				"edu.mayo.bmi.uima.common.types.PunctTokenAnnotation");
		map.put("org.apache.ctakes.typesystem.type.SymbolToken",
				"edu.mayo.bmi.uima.common.types.SymbolTokenAnnotation");
		map.put("org.apache.ctakes.typesystem.type.NewlineToken",
				"edu.mayo.bmi.uima.common.types.NewlineTokenAnnotation");
		map.put("org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation",
				"edu.mayo.bmi.uima.common.types.NamedEntityAnnotation");
		map.put("org.apache.ctakes.typesystem.type.UmlsConcept",
				"edu.mayo.bmi.uima.common.types.UmlsConcept");
		map.put("org.apache.ctakes.typesystem.type.OntologyConcept",
				"edu.mayo.bmi.uima.common.types.OntologyConcept");

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
//		Change xf1 to xf2 if the second command line argument is the open source output
		for (XcasAnnotation a : xf1.getAllAnnotations())
			if (map.containsKey(a.getType()))
					a.setType(map.get(a.getType()));
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
