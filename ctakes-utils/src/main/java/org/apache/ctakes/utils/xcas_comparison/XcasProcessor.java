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
import java.util.Hashtable;
import java.util.HashMap;
import java.util.LinkedList;
import java.io.File;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A SAX parser that parses an XCAS file.
 * This is done without referencing to the UIMA Type System definition.
 * Therefore, {@link XcasDiff} can be used to compare XCASes from
 * different type systems.
 * @author Mayo Clinic
 *
 */
public class XcasProcessor extends DefaultHandler {

	private Locator loc;
	private SAXParser sp;
	private XcasFile xcasf;
	private Hashtable<String, Integer> pendingRef;
	private Hashtable<Integer, int[]> pendingArr;
	private Hashtable<Integer, int[]> pendingIntArr;
	private HashMap<Integer, int[]> pendingList;
	private String parentTag;
	private int arrID;
	private int arrInd = -1;
	private StringBuffer val;

	public void setDocumentLocator(Locator locator) { loc = locator; }

	/**
	 * Default constructor.
	 */
	public XcasProcessor () {
		pendingRef = new Hashtable<String, Integer>();
		pendingArr = new Hashtable<Integer, int[]>();
		pendingIntArr = new Hashtable<Integer, int[]>();
		pendingList = new HashMap<Integer, int[]>();
		val = new StringBuffer();
		try { sp = SAXParserFactory.newInstance().newSAXParser(); }
		catch (Exception e) { e.printStackTrace(); }
	}

	/**
	 * Parses the specified file and returns a parsed <code>XcasFile</code> object.
	 * @param f A File object.
	 * @return An <code>XcasFile</code> object.
	 */
	public XcasFile process (File f) {
		xcasf = new XcasFile(f);
		pendingRef.clear();
		pendingArr.clear();
		pendingIntArr.clear();
		pendingList.clear();
		val.delete(0, val.length());
		arrInd = -1;
		try { sp.parse(f, this); }
		catch (SAXParseException spe) {
			System.err.println("Error parsing XCAS file: "+f+" at line"+spe.getLineNumber());
			System.err.println(spe.getMessage());
		}
		catch (Exception e) { e.printStackTrace(); }
		return xcasf;
	}

	/**
	 * Parses the specified file and returns a parsed <code>XcasFile</code> object.
	 * @param f File name.
	 * @return An <code>XcasFile</code> object.
	 */
	public XcasFile process (String f) {
		return process(new File(f));
	}

	public void characters (char[] ch, int start, int length) throws SAXException {
		val.append(ch, start, length);
	}

	public void startElement (String uri, String localName, String qName, Attributes attributes) throws SAXException {
		val.delete(0, val.length());
		String s = attributes.getValue(Const.ID);
		int id;
		if (s==null) return;
		else id = Integer.parseInt(s);
		if (qName.equalsIgnoreCase(Const.UIMA_FSARRAY)) {
			pendingArr.put(id, new int[Integer.parseInt(attributes.getValue(Const.UIMA_ARRAY_SIZE_KEYWORD))]);
			parentTag = qName;
			arrID = id;
			arrInd = 0;
		}
		else if (qName.equalsIgnoreCase(Const.UIMA_INTARRAY)) {
			pendingIntArr.put(id, new int[Integer.parseInt(attributes.getValue(Const.UIMA_ARRAY_SIZE_KEYWORD))]);
			parentTag = qName;
			arrID = id;
			arrInd = 0;
		}
		else if (qName.equalsIgnoreCase(Const.UIMA_NONEMPTY_FSLIST)) {
			int[] ref = {Integer.parseInt(attributes.getValue(Const.UIMA_LIST_HEAD_KEYWORD)),
					Integer.parseInt(attributes.getValue(Const.UIMA_LIST_TAIL_KEYWORD))};
			pendingList.put(id, ref);
		}
		else if (qName.equalsIgnoreCase(Const.UIMA_EMPTY_FSLIST)) {
			pendingList.put(id, null);
		}
		else if (!qName.equalsIgnoreCase(Const.UIMA_ARRAY_INDEX_KEYWORD)) {
			XcasAnnotation a = new XcasAnnotation(qName);
			for (int i = attributes.getLength(); i > 0; i--) {
				String q = attributes.getQName(i-1);
				String v = attributes.getValue(i-1);
				if (q.equalsIgnoreCase(Const.ID) || Const.ATTRIBUTES_TO_IGNORE.contains(q)) continue;
				else if (q.startsWith(Const.REF_PREFIX)) pendingRef.put(Integer.toString(id)+":"+q, Integer.parseInt(v));
				else a.insertAttribute(q, v);
			}
			xcasf.addAnnotation(id, a, loc.getLineNumber()+":"+loc.getColumnNumber());
		}
	}

	public void endElement (String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase(Const.UIMA_ARRAY_INDEX_KEYWORD)) {
			if (parentTag.equalsIgnoreCase(Const.UIMA_FSARRAY))
				pendingArr.get(arrID)[arrInd++] = Integer.parseInt(val.toString());
			else if (parentTag.equalsIgnoreCase(Const.UIMA_INTARRAY))
				pendingIntArr.get(arrID)[arrInd++] = Integer.parseInt(val.toString());
		} else if (qName.equalsIgnoreCase(Const.UIMA_FSARRAY)) {
			arrInd = -1;
		} else if (qName.equalsIgnoreCase(Const.UIMA_INTARRAY)) {
			arrInd = -1;
		} else if (qName.equalsIgnoreCase(Const.UIMA_CAS)) {
			for (String s : pendingRef.keySet()) {
				String[] ref = s.split(":");
				int refID = pendingRef.get(s);
				XcasAnnotation a = xcasf.getAnnotation(Integer.parseInt(ref[0]));
				if (pendingIntArr.containsKey(refID)) {
					a.insertIntReference(ref[1], pendingIntArr.get(refID));
					continue;
				}
				int[] arr;
				if (pendingArr.containsKey(refID))
					arr = pendingArr.get(refID);
				else if (pendingList.containsKey(refID)) {
					LinkedList<Integer> ll = new LinkedList<Integer>();
					int[] l = pendingList.get(refID);
					while (l!=null) {
						ll.add(l[0]);
						l = pendingList.get(l[1]);
					}
					arr = new int[ll.size()];
					for (int i = 0; i < arr.length; i++)
						arr[i] = ll.get(i);
				}
				else { arr = new int[1]; arr[0] = refID; }
				for (int i : arr)
					a.insertReference(ref[1], xcasf.getAnnotation(i));
			}
		}
		if (val.length()>0 && !qName.equalsIgnoreCase(Const.UIMA_ARRAY_INDEX_KEYWORD) && !qName.equalsIgnoreCase(Const.UIMA_TCAS_DOCUMENT))
			System.err.println("Unexpected text ("+qName+"): \""+val.toString()+"\"");
		val.delete(0, val.length());
	}
}
