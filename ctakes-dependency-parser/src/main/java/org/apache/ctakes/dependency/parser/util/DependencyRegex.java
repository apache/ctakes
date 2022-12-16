/*
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
package org.apache.ctakes.dependency.parser.util;

import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;

import com.googlecode.clearnlp.dependency.DEPNode;

/*import clear.dep.DepLib;
import clear.dep.DepNode;
import clear.ftr.FtrLib;
*/

/**
 * @author m081914
 *
 */
public class DependencyRegex {

	/** Static identifiers used to build Regex expressions */
	public static String ANY_TOKEN  = "[^"+Delim.R_TOK_DELIM+"]*"; 
	public static String ANY_DEPREL = "[^"+Delim.R_REL_DELIM+"]*";
	public static String ANY_POS    = "\\w*";
	public static String ANY_NOUN   = "N..?";
	public static String ANY_VERB   = "V..?";
	public static String ANY_ADJECTIVE = "J..?";
	public static String fromSet ( Set<String> okwords ) {
		StringBuilder str = new StringBuilder();
		Iterator<String> it = okwords.iterator();
		if (it.hasNext()) {
			str.append("((?:"+it.next()+")");
		}
		while (it.hasNext()) {
			str.append("|(?:"+it.next()+")");
		}
		if ( str.toString()!="" ) {
			str.append(")");
		}
		return str.toString();
	}

	private static String L_TOK_DELIM = metaReplace(Delim.L_TOK_DELIM); 
	private static String R_TOK_DELIM = metaReplace(Delim.R_TOK_DELIM); 
	private static String L_POS_DELIM = metaReplace(Delim.L_POS_DELIM); 
	private static String R_POS_DELIM = metaReplace(Delim.R_POS_DELIM); 
	private static String L_REL_DELIM = metaReplace(Delim.L_REL_DELIM); 
	private static String R_REL_DELIM = metaReplace(Delim.R_REL_DELIM); 
	private static String UP_ARC_A    = metaReplace(Delim.UP_ARC_A); 
	private static String UP_ARC_B    = metaReplace(Delim.UP_ARC_B); 
	private static String DN_ARC_A    = metaReplace(Delim.DN_ARC_A); 
	private static String DN_ARC_B    = metaReplace(Delim.DN_ARC_B); 

	private Pattern regex;
  private String name = "";

	public DependencyRegex(String name, String dummy) {
	  this.name = name;
	}

	/**
	 * @param path a DependencyPath from which to make and/or modify a regex
	 */
	public DependencyRegex( DependencyPath path, String name) {
		String str = path.toString();
		Pattern regex = Pattern.compile(str,Pattern.CASE_INSENSITIVE);
		this.regex = regex;
		this.name = name;
	}

	/**
	 * @param str a string containing the Java-format regular expression to set
	 */
	public DependencyRegex( String str ) {
		this.regex = compile(str,Pattern.CASE_INSENSITIVE);
	}

	public String getName(){
	  return this.name;
	}
	
	/**
	 * @param regnodes
	 * @param commonNodeIndex The index (starting from 1) of the lowest common node in the dependency tree
	 */
  public DependencyRegex(DEPNode[] regnodes, int commonNodeIndex) {
    this(regnodes, commonNodeIndex, "");
  }
  
  public DependencyRegex(DEPNode[] regnodes, int commonNodeIndex, String name) {
		StringBuilder str = new StringBuilder();

//		str.append(".*");
		for (int i=0; i<regnodes.length; i++) {
			if (regnodes[i].form==null)    regnodes[i].form   = ANY_TOKEN;
			if (regnodes[i].pos==null)     regnodes[i].pos    = ANY_POS;
//			if (regnodes[i].deprel==FtrLib.TAG_NULL)  regnodes[i].deprel = ANY_DEPREL;
			if (regnodes[i].getLabel()==null) regnodes[i].setLabel(ANY_DEPREL);
			
			if (i==commonNodeIndex-1) {
				str.append( L_TOK_DELIM + regnodes[i].form + R_TOK_DELIM );
				str.append( L_POS_DELIM + regnodes[i].pos  + R_POS_DELIM );
				continue;
			}
			str.append( i>=commonNodeIndex-1 ? 
					DN_ARC_A
					+ L_REL_DELIM + regnodes[i].getLabel() + R_REL_DELIM
					+ DN_ARC_B
					+ L_TOK_DELIM + regnodes[i].form   + R_TOK_DELIM
					+ L_POS_DELIM + regnodes[i].pos    + R_POS_DELIM
					: 
						L_TOK_DELIM + regnodes[i].form  + R_TOK_DELIM
						+ L_POS_DELIM + regnodes[i].pos + R_POS_DELIM
						+ UP_ARC_B
						+ L_REL_DELIM + regnodes[i].getLabel() + R_REL_DELIM
						+ UP_ARC_A
			);
		}
		
//		str.append(".*");
		this.regex = compile(str.toString(),Pattern.CASE_INSENSITIVE);
		this.name = name;
	}
	
	/**
	 * @param regnodes
	 * @param commonNodeIndex The index (starting from 1) of the lowest common node in the dependency tree
	 */
	public DependencyRegex(ConllDependencyNode[] regnodes, int commonNodeIndex) {
		StringBuilder str = new StringBuilder();

//		str.append(".*");
		for (int i=0; i<regnodes.length; i++) {
			if (regnodes[i].getForm()==null)   regnodes[i].setForm(ANY_TOKEN);
			if (regnodes[i].getPostag()==null) regnodes[i].setPostag(ANY_POS);
			if (regnodes[i].getDeprel()==null) regnodes[i].setDeprel(ANY_DEPREL);
			if (i==commonNodeIndex-1) {
				str.append( L_TOK_DELIM + regnodes[i].getForm()   + R_TOK_DELIM );
				str.append( L_POS_DELIM + regnodes[i].getPostag() + R_POS_DELIM );
				continue;
			}
			str.append( i>=commonNodeIndex-1 ? 
					DN_ARC_A
					+ L_REL_DELIM     + regnodes[i].getDeprel() + R_REL_DELIM
					+ DN_ARC_B
					+ L_TOK_DELIM     + regnodes[i].getForm()   + R_TOK_DELIM
					+ L_POS_DELIM     + regnodes[i].getPostag() + R_POS_DELIM
					: 
						L_TOK_DELIM   + regnodes[i].getForm()   + R_TOK_DELIM
						+ L_POS_DELIM + regnodes[i].getPostag() + R_POS_DELIM
						+ UP_ARC_B
						+ L_REL_DELIM + regnodes[i].getDeprel() + R_REL_DELIM
						+ UP_ARC_A
			);
		}
		
//		str.append(".*");
		this.regex = compile(str.toString(),Pattern.CASE_INSENSITIVE);
	}
	private static String metaReplace( String str ) {
		str = Pattern.compile("\\{").matcher(str).replaceAll("\\\\{");
		str = Pattern.compile("\\}").matcher(str).replaceAll("\\\\}");
		str = Pattern.compile("\\[").matcher(str).replaceAll("\\\\[");
		str = Pattern.compile("\\]").matcher(str).replaceAll("\\\\]");
		str = Pattern.compile("\\+").matcher(str).replaceAll("\\\\+");
		str = Pattern.compile("\\*").matcher(str).replaceAll("\\\\*");
		str = Pattern.compile("\\(").matcher(str).replaceAll("\\\\(");
		str = Pattern.compile("\\)").matcher(str).replaceAll("\\\\)");
		str = Pattern.compile("\\^").matcher(str).replaceAll("\\\\^");
		str = Pattern.compile("\\$").matcher(str).replaceAll("\\\\$");
		str = Pattern.compile("\\.").matcher(str).replaceAll("\\\\.");
		return str;
	}

	public Pattern compile(String str) {
		return Pattern.compile(str,Pattern.CASE_INSENSITIVE);
	}

	public Pattern compile(String str, int flag) {
		return Pattern.compile(str, flag);
	}

	/**
	 * @return the regex
	 */
	public String get() {
		return regex.toString();
	}


	/**
	 * @return the regex
	 */
	public String toString() {
		return regex.toString();
	}

	public Matcher matcher( CharSequence input ) {
		return regex.matcher( input );
	}
	
	public boolean matches( String str ) {
		Matcher matcher = regex.matcher( str );
		boolean flag = matcher.matches();
//		if (flag) System.out.println(" matched: "+ matcher.group());
		return flag;
	}

	public boolean find( String str ) {
		Matcher matcher = regex.matcher( str );
		boolean flag = matcher.find();
//		if (flag) System.out.println(" found: "+ matcher.group());
		return flag;
	}

	public String[] split(CharSequence input) {
		return regex.split(input);
	}

//	// Add to DependencyRegexes... especially optional ones
//	public void append( DependencyRegex dregex ) {
//		this.regex = compile( this.toString() + dregex.toString(),
//				Pattern.CASE_INSENSITIVE);
//	}
//	
//	public void appendOptional( DependencyRegex dregex ) {
//		this.regex = compile( this.toString() + "("+dregex.toString()+")*",
//				Pattern.CASE_INSENSITIVE);
//	}
//
//	public void prepend( DependencyRegex dregex ) {
//		this.regex = compile( dregex.toString() + this.toString(),
//				Pattern.CASE_INSENSITIVE);
//	}
//	
//	public void prependOptional( DependencyRegex dregex ) {
//		this.regex = compile( "("+dregex.toString()+")*?" + this.toString(),
//				Pattern.CASE_INSENSITIVE);
//	}

	// Combine DependencyRegexes 
	public DependencyRegex append( DependencyRegex dregex ) {
		DependencyRegex newregex = new DependencyRegex(this.name + "_mod", null);
		newregex.regex = compile( this.toString() + dregex.toString(),
				Pattern.CASE_INSENSITIVE);
		return newregex;
	}
	
	public DependencyRegex appendOptional( DependencyRegex dregex ) {
		DependencyRegex newregex = new DependencyRegex(this.name + "_mod", null);
		newregex.regex = compile( this.toString() + "("+dregex.toString()+")*",
				Pattern.CASE_INSENSITIVE);
		return newregex;
	}

	public DependencyRegex prepend( DependencyRegex dregex ) {
		DependencyRegex newregex = new DependencyRegex(this.name + "_mod", null);
		newregex.regex = compile( dregex.toString() + this.toString(),
				Pattern.CASE_INSENSITIVE);
		return newregex;
	}
	
	public DependencyRegex prependOptional( DependencyRegex dregex ) {
		DependencyRegex newregex = new DependencyRegex(this.name + "_mod", null);
		newregex.regex = compile( "("+dregex.toString()+")*?" + this.toString(),
				Pattern.CASE_INSENSITIVE);
		return newregex;
	}

	// Add dnodes to DependencyRegex
	public DependencyRegex append( DEPNode[] dnodes ) {
		DependencyRegex addregex = new DependencyRegex(dnodes,-1);
		return append(addregex);
	}
	
	public DependencyRegex appendOptional( DEPNode[] dnodes ) {
		DependencyRegex addregex = new DependencyRegex(dnodes,-1);
		return appendOptional(addregex);
	}

	public DependencyRegex prepend( DEPNode[] dnodes ) {
		DependencyRegex addregex = new DependencyRegex(dnodes,1024);
		return prepend(addregex);
	}
	
	public DependencyRegex prependOptional( DEPNode[] dnodes ) {
		DependencyRegex addregex = new DependencyRegex(dnodes,1024);
		return prependOptional(addregex);
	}

	// Add ConllDependencyNodes to DependencyRegex
	public DependencyRegex append( ConllDependencyNode[] dnodes ) {
		DependencyRegex addregex = new DependencyRegex(dnodes,-1);
		return append(addregex);
	}
	
	public DependencyRegex appendOptional( ConllDependencyNode[] dnodes ) {
		DependencyRegex addregex = new DependencyRegex(dnodes,-1);
		return appendOptional(addregex);
	}

	public DependencyRegex prepend( ConllDependencyNode[] dnodes ) {
		DependencyRegex addregex = new DependencyRegex(dnodes,1024);
		return prepend(addregex);
	}
	
	public DependencyRegex prependOptional( ConllDependencyNode[] dnodes ) {
		DependencyRegex addregex = new DependencyRegex(dnodes,1024);
		return prependOptional(addregex);
	}
}
