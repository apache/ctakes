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
import java.util.LinkedList;
import java.util.Collections;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 * An <code>XcasDiff</code> object represents the comparison results of
 * two XCAS files. It also provides methods to output these results.
 * @author Mayo Clinic
 *
 */
public class XcasDiff {

	private XcasFile f1, f2;
	private LinkedList<XcasAnnotation> f1_uniq;
	private LinkedList<XcasAnnotation> f2_uniq;
	private LinkedList<XcasAnnotation> common;

	/**
	 * Default constructor.
	 */
	public XcasDiff () {
		f1_uniq = new LinkedList<XcasAnnotation>();
		f2_uniq = new LinkedList<XcasAnnotation>();
		common = new LinkedList<XcasAnnotation>();
	}

	/**
	 * Construct an <code>XCASDiff</code> object from two <code>XcasFile</code>
	 * objects.
	 * @param xf1 An <code>XcasFile</code> object.
	 * @param xf2 Another <code>XcasFile</code> object.
	 */
	public XcasDiff (XcasFile xf1, XcasFile xf2) {
		this();
		f1 = xf1;
		f2 = xf2;
		recalculate();
	}

	/**
	 * Calculates the differences or recalculates after the two files are updated.
	 * Works on an copy of the original annotations. Original objects are not touched.
	 * Must be called if the original XcasFile is modified elsewhere.
	 */
	public final void recalculate () {
		f1_uniq.clear();
		f2_uniq.clear();
		common.clear();
		LinkedList<XcasAnnotation> l1 = f1.annotationsClone();
		LinkedList<XcasAnnotation> l2 = f2.annotationsClone();
		for (XcasAnnotation a : l1) {
			if (l2.contains(a)) { common.add(a); l2.remove(a); }
			else f1_uniq.add(a);
		}
		f2_uniq.addAll(l2);
		Collections.sort(f1_uniq);
		Collections.sort(f2_uniq);
		Collections.sort(common);
	}

	/**
	 * Prints common elements along with their attributes in the two XCAS files
	 * to <code>stdout</code>.
	 */
	public void printCommon () {
		for (XcasAnnotation a : common)
			System.out.println(a.description());
	}

	/**
	 * Prints elements that are unique to each XCAS file to <code>stdout</code>.
	 */
	public void printDiff () {
		try {
			System.out.println("-------- Unique to "+f1.f.getCanonicalFile().toString()+" --------");
			printUniq(1);
			System.out.println("-------- Unique to "+f2.f.getCanonicalFile().toString()+" --------");
			printUniq(2);
		} catch (Exception e) { e.printStackTrace(); }
	}

	/**
	 * Prints elements that are unique to XCAS file <code>i</code>.
	 * @param i XCAS file number, specified when constructing this object,
	 *          can only be 1 or 2.
	 */
	public void printUniq (int i) {
		LinkedList<XcasAnnotation> toPrint;
		XcasFile f;
		if (i==1) { toPrint = f1_uniq; f = f1; }
		else if (i==2) { toPrint = f2_uniq; f = f2; }
		else { System.err.println("Not a legal file number: "+i); return; }
		for (XcasAnnotation a : toPrint)
			System.out.println(a.shortType()+" ~@"+f.getPositionOther(a)+"  "+(a.shortType().equals(Const.UIMA_SOFA)?Const.HIDDEN_TEXT:a.allFieldsValues()));
	}

	/**
	 * Writes an HTML summary of the comparison of the two XCAS files.
	 * @param w An {@link java.io.Writer} object to write to.
	 */
	public void printHTML (Writer w) {
		BufferedWriter bw = new BufferedWriter(w);
		try {
			String fName1 = f1.f.getCanonicalFile().toString();
			String fName2 = f2.f.getCanonicalFile().toString();
			bw.write("<html>"); bw.newLine();
			bw.write("  <head>"); bw.newLine();
			bw.write("    <title>Comparison of "+fName1+" and "+fName2+"</title>"); bw.newLine();
			bw.write("    <script language=\"javascript\">"); bw.newLine();
			bw.write("      function toggle_agreements() {"); bw.newLine();
			bw.write("        var ele = document.getElementById('xcasdiff');"); bw.newLine();
			bw.write("        if ((ele.rows[1].style.display=='none')) {"); bw.newLine();
			bw.write("          for (i=1;i<"+(common.size()+1)+";i++) ele.rows[i].style.display='';"); bw.newLine();
			bw.write("        } else {"); bw.newLine();
			bw.write("          for (i=1;i<"+(common.size()+1)+";i++) ele.rows[i].style.display='none';"); bw.newLine();
			bw.write("        }"); bw.newLine();
			bw.write("      }"); bw.newLine();
			bw.write("    </script>"); bw.newLine();
			bw.write("  </head>"); bw.newLine();
			bw.write("<body>"); bw.newLine();
			bw.write("<p><button onClick=\"toggle_agreements()\">Toggle Aggreements</button> Click this button to show or hide agreements.</p>"); bw.newLine();
			bw.write("<p>Positions are <i>approximate</i> line and column numbers of the element in a file. Yellow background cells indicate a similar element in a file without an exact match. A \"null\" means no similar match found.</p>"); bw.newLine();
			bw.write("<p>File 1: <a href=\"file://"+fName1+"\">"+fName1+"</a><br>"); bw.newLine();
			bw.write("File 2: <a href=\"file://"+fName2+"\">"+fName2+"</a></p>"); bw.newLine();
			bw.write("<p>Agreed on "+common.size()+" elements."); bw.newLine();
			bw.write("File 1 has "+f1_uniq.size()+" unique elements, and file 2 has "+f2_uniq.size()+" unique elements.</p>"); bw.newLine();
			bw.write("<table border=\"1\" id=\"xcasdiff\">"); bw.newLine();
			bw.write("<tr>"); bw.newLine();
			bw.write("<th></th>"); bw.newLine();
			bw.write("<th>Element</th>"); bw.newLine();
			bw.write("<th>Attributes</th>"); bw.newLine();
			bw.write("<th>Position in File 1</th>"); bw.newLine();
			bw.write("<th>Position in File 2</th>"); bw.newLine();
			bw.write("</tr>"); bw.newLine();
			int i = 0;
			for (XcasAnnotation a : common) {
				bw.write("<tr>"); bw.newLine();
				if (i++==0) { bw.write("<th rowspan=\""+common.size()+"\">Agreements</th>"); bw.newLine(); }
				bw.write("<td>"+a.shortType()+"</td>"); bw.newLine();
				bw.write("<td>"+(a.type.equals(Const.UIMA_SOFA)?Const.HIDDEN_TEXT:a.allFieldsValues())+"</td>"); bw.newLine();
				bw.write("<td>"+f1.getPositionOther(a)+"</td>"); bw.newLine();
				bw.write("<td>"+f2.getPositionOther(a)+"</td>"); bw.newLine();
			}
			if (i>0) { bw.write("</tr>"); bw.newLine(); i=0; }
			for (XcasAnnotation a : f1_uniq) {
				bw.write("<tr>"); bw.newLine();
				if (i++==0) { bw.write("<th rowspan=\""+(f1_uniq.size()+f2_uniq.size())+"\">Disagreements</th>"); bw.newLine(); }
				bw.write("<td>"+a.shortType()+"</td>"); bw.newLine();
				bw.write("<td>"+(a.type.equals(Const.UIMA_SOFA)?Const.HIDDEN_TEXT:a.allFieldsValues())+"</td>"); bw.newLine();
				bw.write("<td>"+f1.getPositionOther(a)+"</td>"); bw.newLine();
				bw.write("<td bgcolor=\"yellow\">"+f2.getPositionSimilar(a)+"</td>"); bw.newLine();
			}
			for (XcasAnnotation a : f2_uniq) {
				bw.write("<tr>"); bw.newLine();
				bw.write("<td>"+a.shortType()+"</td>"); bw.newLine();
				bw.write("<td>"+(a.type.equals(Const.UIMA_SOFA)?Const.HIDDEN_TEXT:a.allFieldsValues())+"</td>"); bw.newLine();
				bw.write("<td bgcolor=\"yellow\">"+f1.getPositionSimilar(a)+"</td>"); bw.newLine();
				bw.write("<td>"+f2.getPositionOther(a)+"</td>"); bw.newLine();
			}
			if (i>0) { bw.write("</tr>"); bw.newLine(); }
			bw.write("</table>"); bw.newLine();
			bw.write("</body>"); bw.newLine();
			bw.write("</html>"); bw.newLine();
			bw.close();
		} catch (IOException e) { e.printStackTrace(); }
	}

	/**
	 * Prints elements that are unique to each XCAS file to <code>stdout</code>.
	 * First constructs an anonymous <code>XcasDiff</code> object, and calls its
	 * {@link #printDiff()}. Avoid using this method if you plan to use
	 * <code>XcasDiff</code> object later.
	 * @param f1 An <code>XcasFile</code> object.
	 * @param f2 Another <code>XcasFile</code> object.
	 * @see #printDiff()
	 */
	public static void printDiff (XcasFile f1, XcasFile f2) {
		new XcasDiff(f1, f2).printDiff();
	}

	/**
	 * Writes an HTML summary of the comparison of the two specified XCAS files.
	 * First constructs an anonymous <code>XcasDiff</code> object, and calls its
	 * {@link #printHTML(Writer)}. Avoid using this method if you plan to use
	 * <code>XcasDiff</code> object later.
	 * @param f1 An <code>XcasFile</code> object.
	 * @param f2 Another <code>XcasFile</code> object.
	 * @param w An {@link java.io.Writer} object to write to.
	 */
	public static void printHTML (XcasFile f1, XcasFile f2, Writer w) {
		new XcasDiff(f1, f2).printHTML(w);
	}
}
