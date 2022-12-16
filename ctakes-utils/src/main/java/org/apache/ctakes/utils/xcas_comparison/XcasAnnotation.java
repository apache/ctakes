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
import java.util.Collection;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Collections;

/**
 * An <code>XcasAnnotation</code> object represents an element under the <code>CAS</code>
 * tag in an XCAS file. It provides members and methods to store and access
 * its type, attributes,and references.
 * <p>
 * Note: this class has a natural ordering that is inconsistent with equals.
 * It is strongly recommended that the use of {@link #compareTo(XcasAnnotation)}
 * be strictly limited to the purpose of an orderly display of multiple objects.
 * @author Mayo Clinic
 *
 */
public class XcasAnnotation implements Cloneable, Comparable<XcasAnnotation> {

	private class MyVector extends Vector<XcasAnnotation> implements Cloneable {
		private static final long serialVersionUID = -8717810064045790243L;
		public MyVector () { super(); }
		public boolean contains (Object obj) {
			if (obj.getClass()!=XcasAnnotation.class) return false;
			for (XcasAnnotation a : this) if (a.equals(obj)) return true;
			return false;
		}
		public boolean containsAll (Collection<?> c) {
			if (c==this || c.size()==0) return true;
			for (Object o : c)
				if (o.getClass()!=XcasAnnotation.class || !contains(o)) return false;
			return true;
		}
		public boolean equals (Object obj) {
			if (obj.getClass()!=getClass() || ((MyVector)obj).size()!=size()) return false;
			return containsAll((MyVector)obj);
		}
		public String toString () {
			StringBuffer ret = new StringBuffer("[");
			Collections.sort(this);
			for (XcasAnnotation a : this)
				ret.append(a.type.equalsIgnoreCase(Const.UIMA_SOFA) ? Const.HIDDEN_TEXT : a.description()).append(",");
			if (size()>0) ret.deleteCharAt(ret.length()-1);
			return ret.append("]").toString();
		}
		public Object clone () {
			MyVector o = null;
			try {
				o = (MyVector)super.clone();
				o.clear();
				for (XcasAnnotation a : this) //TODO is the order guaranteed to be the same?
					o.add((XcasAnnotation)a.clone());
			} catch (Exception e) { e.printStackTrace(); }
			return o;
		}
	}

	protected String type;
	protected Hashtable<String, String> attributes;
	protected Hashtable<String, MyVector> references;
	protected Hashtable<String, Vector<Integer>> refIntArrays;

	/**
	 * Default constructor. Its type is initialized to empty.
	 */
	public XcasAnnotation () {
		type = "";
		attributes = new Hashtable<String, String>();
		references = new Hashtable<String, MyVector>();
		refIntArrays = new Hashtable<String, Vector<Integer>>();
	}

	/**
	 * Constructs an object with the specified type.
	 * @param t Type of the <code>XcasAnnotation</code> object to be created.
	 */
	public XcasAnnotation (String t) { this(); type = t; }

	/**
	 * Sets the type name of this annotation.
	 * @param t Type to be set.
	 */
	public void setType (String t) { type = t; }

	/**
	 * Inserts an (attribute, value) pair to this <code>XcasAnnotation</code>.
	 * @param k Name of the attribute.
	 * @param v Value of the attribute.
	 */
	public void insertAttribute (String k, String v) { attributes.put(k, v); }
	//TODO Check for loop in references
	public void insertReference (String k, XcasAnnotation a) {
		if (references.containsKey(k))
			references.get(k).add(a);
		else {
			MyVector v = new MyVector();
			v.add(a);
			references.put(k, v);
		}
	}

	public void insertIntReference (String k, int[] a) {
		Vector<Integer> v = new Vector<Integer>(a.length);
		for (int i : a) v.add(i);
		refIntArrays.put(k, v);
	}

	/**
	 * Returns the value of the specified attribute.
	 * @param name Name of the attribute.
	 * @return Value of the specified attribute. 
	 */
	public String getAttribute (String name) { return attributes.get(name); }

	/**
	 * Returns the referenced <code>XcasAnnotation</code>s of the specified attribute.
	 * @param name Name of an attribute that references to other <code>XcasAnnotation</code>.
	 * @return A <code>HashSet</code> of referenced <XcasAnnotation</code> objects.
	 */
	public Vector<XcasAnnotation> getReference (String name) { return references.get(name); }

	/**
	 * Determines whether the specified object equals to this one. Specifically,
	 * an <code>XcasAnnotation a</code> equals to another one <code>o</code>
	 * if and only if <code>o</code> is also an <code>XcasAnnotation</code> object
	 * and <code>a.type.equals(o.type) &&  a.attributes.equals(o.attributes) &&
	 * a.references.equals(o.references)</code>.
	 * @param obj An object to compare with this one.
	 * @return <code>true</code> if the specified object equals this one,
	 *         <code>false</code> otherwise.
	 */
	public boolean equals (Object obj) {
		return this==obj || ( obj.getClass()==getClass() &&
				((XcasAnnotation)obj).type.equals(type) &&
				((XcasAnnotation)obj).attributes.equals(attributes) &&
				((XcasAnnotation)obj).references.equals(references) &&
				((XcasAnnotation)obj).refIntArrays.equals(refIntArrays)
				);
	}

	public String getType () { return type; }
	/**
	 * Returns a short description of this <code>XcasAnnotation</code>'s type,
	 * specifically, the last segment as separated by dots.
	 * @return A short type name.
	 */
	public String shortType () { return type.substring(type.lastIndexOf('.')+1); }

	/**
	 * Returns a string representation of all the attributes and references.
	 * Some long values are shortened to ellipses.
	 * @return A string containing all attributes, references and their values.
	 */
	public String allFieldsValues () {
		StringBuffer ret = new StringBuffer("Attributes {");
		Vector<String> v = new Vector<String>(attributes.keySet());
		Collections.sort(v);
		for (String s : v)
			ret.append(s+"="+attributes.get(s)+", ");
		if (v.size()>0) ret.replace(ret.length()-2, ret.length(), "}  References {");
		v.clear();
		v.addAll(references.keySet());
		v.addAll(refIntArrays.keySet());
		Collections.sort(v);
		for (String s : v)
			ret.append(s+"="+(references.containsKey(s)?references.get(s):refIntArrays.get(s))+", ");
		if (v.size()>0) ret.delete(ret.length()-2, ret.length());
		ret.append("}");
		return ret.toString();
//		return "Attributes "+attributes.toString()+"  References "+references.toString();
	}

	/**
	 * Returns a String representation of this object, including its type, and
	 * all attributes and references. Named such so as not to override
	 * the default much shorter String representation
	 * @return A string containing the type and all attributes, references information.
	 */
	public String description () { return shortType()+"  "+allFieldsValues(); }

	/**
	 * Clones this object and its attributes, but leave references blank.
	 * @return
	 */
	private final XcasAnnotation prepareCopy () {
		XcasAnnotation o = null;
		try {
			o = (XcasAnnotation)super.clone();
			o.attributes = new Hashtable<String, String>();
			o.attributes.putAll(attributes);
			o.references = new Hashtable<String, MyVector>();
			o.refIntArrays = new Hashtable<String, Vector<Integer>>();
			for (String s : refIntArrays.keySet())
				o.refIntArrays.put(s, new Vector<Integer>(refIntArrays.get(s)));
		} catch (Exception e) { e.printStackTrace(); }
		return o;
	}

	/**
	 * Recursive, deep copy of this object, including attributes and references.
	 */
	public Object clone () {
		XcasAnnotation o = prepareCopy();
		for (String s : references.keySet())
			o.references.put(s, (MyVector)references.get(s).clone());
		return o;
	}

	/**
	 * Clones this object, its attributes, and reference keys.
	 * @return A shallow copy of this object.
	 */
	public final XcasAnnotation shallowCopy () {
		XcasAnnotation o = prepareCopy();
		for (String s : references.keySet())
			o.references.put(s, new MyVector());
		return o;
	}

	/**
	 * Determines the order of this and the specified <code>XcasAnnotation</code>.
	 * Note: It is strongly recommended that the use of this method be strictly
	 * limited to the purpose of an orderly display of multiple objects.
	 * To determine the equality of two objects, use {@link #equals(Object)}.
	 * @see #equals(Object)
	 */
	public int compareTo(XcasAnnotation o) {
		return description().compareTo(o.description());
	}
}
