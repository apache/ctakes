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
package org.apache.ctakes.utils.tree;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleTree {
	public String cat;
	public ArrayList<SimpleTree> children;
	public SimpleTree parent = null;
	static boolean generalizeLeaf = false;
	static Pattern ptPatt = Pattern.compile("\\(([^ (]+) +([^ )]+)\\)");
	
	public SimpleTree(String c){
		this(c,null);
	}
	
	public SimpleTree(String c, SimpleTree p){	
		cat = escapeCat(c);
		children = new ArrayList<SimpleTree>();
		parent = p;
	}
	
	public SimpleTree(String c, SimpleTree p, boolean g){	
		cat = escapeCat(c);
		children = new ArrayList<SimpleTree>();
		parent = p;
		generalizeLeaf = g;
	}
	
	public static String escapeCat(String c) {
		c = c.replaceAll("\\(", "LPAREN");
		c = c.replaceAll("\\)", "RPAREN");
		return c;
	}

	public void addChild(SimpleTree t){
		children.add(t);
	}
	
	public void insertChild(int index, SimpleTree t){
		children.add(index, t);
	}
	
	public SimpleTree removeChild(int index){
		return children.remove(index);
	}
	
	@Override
	public String toString(){
		StringBuffer buff = new StringBuffer();
		
		buff.append("(");
		buff.append(cat);
		buff.append(" ");
		if(children.size() == 1 && children.get(0).children.size() == 0){
			if(generalizeLeaf){
				buff.append("LEAF");
			}else{
				buff.append(children.get(0).cat);
			}
		}else{
			for(int i = 0; i < children.size(); i++){
				if(i != 0){
					buff.append(" ");
				}
				buff.append(children.get(i).toString());
			}
		}
		buff.append(")");
		return buff.toString();
	}
	
	public static SimpleTree fromString(String string){
		SimpleTree tree = null;
		
		// pre-terminal case is the base case:
		Matcher m = ptPatt.matcher(string);
		if(m.matches()){
			tree = new SimpleTree(m.group(1));
			SimpleTree leaf = new SimpleTree(m.group(2));
			tree.addChild(leaf);
			leaf.parent = tree;
		}else{
			int firstWS = string.indexOf(' ');
			tree = new SimpleTree(string.substring(1, firstWS));
			String[] childStrings = splitChildren(string.substring(firstWS+1, string.length()-1));
			for(int i = 0; i < childStrings.length; i++){
				SimpleTree child = fromString(childStrings[i]);
				child.parent = tree;
				tree.addChild(child);
			}
		}
		return tree;
	}
	
	private static String[] splitChildren(String s){
		ArrayList<String> children = new ArrayList<String>();
		char[] chars = s.toCharArray();
		int numParens = 0;
		int startIndex = 0;
		for(int i = 0; i < chars.length; i++){
			if(chars[i] == '('){
				numParens++;
				if(numParens == 1){
					startIndex = i;
				}
			}else if(chars[i] == ')'){
				numParens--;
				if(numParens == 0){
					children.add(s.substring(startIndex, i+1));
				}else if(numParens < 0){
					break;
				}
			}
		}
		return children.toArray(new String[]{});
	}
	
	public static void main(String[] args){
		SimpleTree t = new SimpleTree("TOP");
		t.addChild(new SimpleTree("S"));
		t.children.get(0).addChild(new SimpleTree("NP"));
		t.children.get(0).addChild(new SimpleTree("VP"));
		t.children.get(0).children.get(0).addChild(new SimpleTree("i"));
		t.children.get(0).children.get(1).addChild(new SimpleTree("ran"));
		System.out.println(t.toString());
		
		SimpleTree t2 = SimpleTree.fromString("(S (NP (PRP it)) (VP (VBZ is) (JJ red)))");
		System.out.println(t2.toString());
	}

	// abstract away representation of leaf (children == null vs. children.size() == 0) because
	// i keep forgetting how it's implemented
	public boolean isLeaf() {
		return children == null || children.size() == 0;
	}

	public void setGeneralizeLeaf(boolean b) {
		generalizeLeaf=b;
		
	}
}
