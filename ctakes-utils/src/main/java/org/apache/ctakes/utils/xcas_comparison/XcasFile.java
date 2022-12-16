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
import java.util.LinkedList;
import java.util.Vector;
import java.io.File;

/**
 * An <code>XcasFile</code> wraps all <code>XcasAnnotations</code>s
 * in it and also contains their position information in terms of
 * line and column numbers.
 * @author Mayo Clinic
 *
 */
public class XcasFile implements Cloneable {

	protected File f;
	protected Hashtable<Integer, XcasAnnotation> annotations;
	protected Hashtable<XcasAnnotation, String> positions;

	/**
	 * Default constructor.
	 */
	public XcasFile () {
		annotations = new Hashtable<Integer, XcasAnnotation>();
		positions = new Hashtable<XcasAnnotation, String>();
	}

	/**
	 * Constructs an <code>XcasFile</code> with the specified name.
	 * @param f File name.
	 */
	public XcasFile (String f) { this(); this.f = new File(f); }

	/**
	 * Constructs an <code>XcasFile</code> with the specified name.
	 * @param f A File object.
	 */
	public XcasFile (File f) { this(); this.f = f; }

	/**
	 * Creates a new <code>XcasFile</code> object from the specified file.
	 * Avoid using this method if you plan to parse multiple files,
	 * as this method creates an anonymous <code>XcasProcessor</code> instance
	 * each time called, which could be used to parse multiple files.
	 * @param f A string containing the XCAS file name.
	 * @return A parsed <code>XcasFile</code> object.
	 */
	public static XcasFile process (String f) {
		return (new XcasProcessor()).process(f);
	}

	/**
	 * Inserts a new annotation with the specified internal <code>id</code>
	 * to this <code>XcasFile</code>.
	 * @param id UIMA CAS internal <code>_id</code>.
	 * @param a An <code>XcasAnnotation</code> object to add.
	 * @see #addAnnotation(int, XcasAnnotation, String)
	 * @see #addAnnotation(int, XcasAnnotation, int, int)
	 */
	public void addAnnotation (int id, XcasAnnotation a) { annotations.put(id, a); }

	/**
	 * Inserts a new annotation, along with its position in the file,
	 * to this <code>XcasFile</code> object.
	 * @param id UIMA CAS internal <code>_id</code>.
	 * @param a An <code>XcasAnnotation</code> object to add.
	 * @param pos Line and column number of the specified annotation,
	 *        in the form of <code>line_number:column_number</code>.
	 */
	public void addAnnotation (int id, XcasAnnotation a, String pos) { addAnnotation(id, a); positions.put(a, pos); }

	/**
	 * Inserts a new annotation, along with its position in the file,
	 * to this <code>XcasFile</code> object.
	 * @param id UIMA CAS internal <code>_id</code>.
	 * @param a An <code>XcasAnnotation</code> object to add.
	 * @param lineNum Line number of the specified annotation.
	 * @param colNum Column number of the specified annotation.
	 */
	public void addAnnotation (int id, XcasAnnotation a, int lineNum, int colNum) { addAnnotation(id, a, Integer.toString(lineNum)+":"+Integer.toString(colNum)); }

	/**
	 * Returns the <code>XcasAnnotation</code> object associated with
	 * the specified internal id.
	 * @param id UIMA CAS internal <code>_id</code>.
	 * @return The <code>XcasAnnotation</code> with the specified id.
	 */
	public XcasAnnotation getAnnotation (int id) { return annotations.get(id); }

	public java.util.Collection<XcasAnnotation> getAllAnnotations () { return annotations.values(); }
	public String getFileName () { return f.getName(); }

	/**
	 * Returns the line and column numbers of the specified <code>XcasAnnotation</code>,
	 * which is included in this <code>XcasFile</code> object.
	 * @return A string containing the line and column numbers of the specified object,
	 *         in the form of <code>line_number:column_number</code>.
	 * @see #getPositionOwn(int)
	 * @see #getPositionOther(XcasAnnotation)
	 */
	public String getPositionOwn (XcasAnnotation a) { return positions.get(a); }

	/**
	 * Returns the line and column numbers of the <code>XcasAnnotation</code>,
	 * specified by the original XCAS internal <code>_id</code> field.
	 * @param id UIMA CAS internal <code>_id</code>.
	 * @return A string containing the line and column numbers of the specified object,
	 *         in the form of <code>line_number:column_number</code>.
	 * @see #getPositionOwn(XcasAnnotation)
	 * @see #getPositionOther(XcasAnnotation)
	 */
	public String getPositionOwn (int id) { return positions.get(annotations.get(id)); }

	/**
	 * Finds an <code>XcasAnnotation</code> with the same attributes as specified,
	 * and returns its line and column numbers. 
	 * @return A string containing the line and column numbers of the specified object,
	 *         in the form of <code>line_number:column_number</code>.
	 * @see #getPositionOwn(int)
	 * @see #getPositionOwn(XcasAnnotation)
	 */
	public String getPositionOther (XcasAnnotation a) {
		for (XcasAnnotation o : positions.keySet())
			if (o.equals(a)) return positions.get(o);
		return null;
	}

	/**
	 * Returns the line and column numbers of the specified <code>XcasAnnotation</code>.
	 * <p>
	 * Do not use this method if you know the specified <code>XcasAnnotation</code>
	 * object is in this <code>XcasFile</code>. Instead, use
	 * {@link #getPositionOwn(XcasAnnotation)}, which is faster.
	 * @param a An 
	 * @return A string containing the line and column numbers of the specified object,
	 *         in the form of <code>line_number:column_number</code>.
	 * @see #getPositionOwn(XcasAnnotation)
	 * @see #getPositionOther(XcasAnnotation)
	 */
	public String getPosition (XcasAnnotation a) {
		if (positions.keySet().contains(a)) return positions.get(a);
		else return getPositionOther(a);
	}

	/**
	 * Finds an <code>XcasAnnotation</code> of the same type as specified, and
	 * a same text span, then returns its line and column number.
	 * @param a An <code>XcasAnnotation</code> against which a similar
	 *          <code>XcasAnnotation</code> in this <code>XcasFile</code>
	 *          is to be matched.
	 * @return A string containing the line and column numbers of the specified object,
	 *         in the form of <code>line_number:column_number</code>.
	 * @see #getPositionOther(XcasAnnotation)
	 */
	public String getPositionSimilar (XcasAnnotation a) {
		for (XcasAnnotation o : positions.keySet())
			if (o.type.equals(a.type)) {
				int oBegin = o.attributes.containsKey("begin") ? Integer.parseInt(o.getAttribute("begin")) : -1;
				int oEnd = o.attributes.containsKey("end") ? Integer.parseInt(o.getAttribute("end")) : -1;
				int aBegin = a.attributes.containsKey("begin") ? Integer.parseInt(a.getAttribute("begin")) : -2;
				int aEnd = a.attributes.containsKey("end") ? Integer.parseInt(a.getAttribute("end")) : -2;
				if (oBegin==aBegin && oEnd==aEnd) return positions.get(o);
				else if (o.attributes.containsKey("key") && a.attributes.containsKey("key") && o.getAttribute("key").equals(a.getAttribute("key")))
					return positions.get(o);
			}
		return null;
	}

	/**
	 * Checks whether this XCAS file has an annotation with the specified id.
	 * @param id UIMA CAS internal <code>_id</code>.
	 * @return <code>true</code> if file has an annotation with the specified id,
	 *         <code>false</code> otherwise.
	 */
	public boolean hasAnnotation (int id) { return annotations.containsKey(id); }

	/**
	 * Checks whether this XCAS file has the specified <code>XcasAnnotation</code>.
	 * If there is an <code>XcasAnnotation</code> object that has exactly the same
	 * type, attributes, and references, return <code>true</code>.
	 * @param a An <code>XcasAnnotation</code> to check.
	 * @return <code>true</code> if there is one <code>XcasAnnotation</code> equals
	 *         the specified one, <code>false</code> otherwise.
	 * @see XcasAnnotation#equals(Object)
	 */
	public boolean hasAnnotation (XcasAnnotation a) { return annotations.containsValue(a); }

	/**
	 * Checks whether the specified object has the same set of annotations. First check
	 * whether the specified is an <code>XcasFile</code> object. If so, check whether
	 * its annotation set is of the same size as in this <code>XcasFile</code>, then check
	 * whether these two sets are equal. 
	 * @param obj An object to compare to.
	 * @return <code>true if the specified object is an <code>XcasFile</code> object and
	 *         has a same set of <code>XcasAnnotations</code>, <code>false</code> otherwise.
	 */
	public boolean equals (Object obj) {
		if (obj.getClass()!=getClass() || annotations.values().size()!=((XcasFile)obj).annotations.values().size()) return false;
		return annotations.values().containsAll(((XcasFile)obj).annotations.values());
	}

	public LinkedList<XcasAnnotation> annotationsClone () {
		LinkedList<XcasAnnotation> ret = new LinkedList<XcasAnnotation>();
		Hashtable<XcasAnnotation, XcasAnnotation> cloneMap = new Hashtable<XcasAnnotation, XcasAnnotation>();
		for (XcasAnnotation a : annotations.values()) {
			XcasAnnotation c = a.shallowCopy();
			cloneMap.put(a, c);
			ret.add(c);
		}
		for (XcasAnnotation a : annotations.values())
			for (String s : a.references.keySet())
				for (XcasAnnotation r : (Vector<XcasAnnotation>)a.references.get(s))
					((Vector<XcasAnnotation>)cloneMap.get(a).references.get(s)).add(cloneMap.get(r));
		return ret;
	}

	public Object clone () {
		return null; //TODO implement clone?
		// Should not use XcasAnnotation.clone()
		// otherwise, XcasAnnotation objects referenced by multiple objects will be cloned more than once.
	}
}
